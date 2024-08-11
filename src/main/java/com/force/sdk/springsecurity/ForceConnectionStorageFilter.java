/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.sdk.springsecurity;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.force.sdk.oauth.ForceUserPrincipal;
import com.force.sdk.oauth.connector.ForceOAuthConnector;
import com.force.sdk.oauth.context.ForceSecurityContextHolder;
import com.force.sdk.oauth.context.SecurityContext;
import com.force.sdk.oauth.exception.ForceOAuthSessionExpirationException;
import com.sforce.ws.*;

/**
 * Filter that runs after all other Force.com filters in the spring security chain. It ensures
 * that the connection information of the authenticated user is set into the Force.com Connector
 * framework so that it is available to other Force.com SDK frameworks.
 * 
 * It's important that this filter run as the last Force.com Spring Security filter to ensure that
 * the connection storage takes place regardless of which authentication method and data storage method
 * was used. It's also important that the final result of the entire authentication chain is what gets stored in 
 * the connector.
 * 
 * The logic to renew a session is also in this filter. This hook will be called as needed by the Spring Security
 * Framework.
 *
 * @author John Simone
 */
public class ForceConnectionStorageFilter extends GenericFilterBean implements SessionRenewer {

    private ForceOAuthConnector oauthConnector;
    private Boolean useSession;

    public void setUseSession(Boolean useSession) {
        this.useSession = useSession;
    }
    
    /**
     * Extra setter to make spring configuration easier.
     * @param useSession the value to set into useSession
     */
    public void setUseSession(boolean useSession) {
        this.useSession = useSession;
    }

    public boolean isUseSession() {
        return useSession;
    }

    public void setOauthConnector(ForceOAuthConnector oauthConnector) {
        this.oauthConnector = oauthConnector;
    }
    
    
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        //If a user has been authenticated then the information about that user must be stored
        if (auth != null && auth.getPrincipal() instanceof ForceUserPrincipal) {
            ForceUserPrincipal user = (ForceUserPrincipal) auth.getPrincipal();
            ForceConnectorConfig cc = new ForceConnectorConfig();
            cc.setSessionId(user.getSessionId());
            SecurityContext sc = (SecurityContext) auth.getDetails();
            cc.setServiceEndpoint(sc.getEndPoint());
            cc.setSessionRenewer(this);
            //The security context holder handles the storage of the security context
            ForceSecurityContextHolder.set(sc);

            //The ForceServiceConnector handles the storage of the connector config and will use this config going forward
            ForceServiceConnector.setThreadLocalConnectorConfig(cc);
            try {
                chain.doFilter(request, response);
            //After the request is completed clear out the thread local variables.
            } catch (ForceOAuthSessionExpirationException e) {
                logger.debug("User's session expired. Redirecting to login screen");
                //redirect user to login page
                res.sendRedirect(oauthConnector.getLoginRedirectUrl(req));
            } finally {
                //if we aren't relying on server side sessions then clear the spring security context
                //we need to do this because spring will use a session if one exists.
                if (!useSession) {
                    SecurityContextHolder.clearContext();
                }
                ForceServiceConnector.setThreadLocalConnectorConfig(null);
                ForceSecurityContextHolder.release();
            }
            
        } else {
            chain.doFilter(request, response);
        }

    }

    /**
     * Renews a session by sending the user into the OAuth flow. A renewal attempt
     * results in a runtime exception so that it can either be handled by an application or
     * be allowed to bubble up to the servlet filter where the authentication redirect takes place.
     * {@inheritDoc}
     */
    @Override
    public SessionRenewalHeader renewSession(ConnectorConfig config) throws ConnectionException {
        throw new ForceOAuthSessionExpirationException();
    }

}
