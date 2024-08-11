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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.force.sdk.oauth.ForceUserPrincipal;
import com.force.sdk.oauth.connector.ForceOAuthConnector;
import com.force.sdk.oauth.context.SecurityContext;

/**
 * 
 * The {@code AuthenticationProvider} used for Force.com authentication. Uses the 
 * {@code ForceOAuthConnector} to get the access token when an access code is present.
 *
 * @author John Simone
 */
public class OAuthAuthenticationProvider implements AuthenticationProvider {

    private static final Log LOGGER = LogFactory.getLog(OAuthAuthenticationProvider.class);
    
    private ForceOAuthConnector oauthConnector;

    public void setOauthConnector(ForceOAuthConnector oauthConnector) {
        this.oauthConnector = oauthConnector;
    }

    /**
     * If this is the access code callback request, use the {@code ForceOAuthConnector} to retrieve the
     * access token.
     * 
     * @param authentication {@code Authentication}
     * @return {@code Authentication}
     * @throws AuthenticationException when authentication fails
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuthAuthenticationToken authToken = (OAuthAuthenticationToken) authentication;
        try {
            Object details = authToken.getDetails();
            SecurityContext sc;
            if (details instanceof SecurityContext) {
                sc = (SecurityContext) details;
            } else {
                sc = oauthConnector.getAccessToken((String) authToken.getCredentials(), (String) authToken.getDetails());
            }
            return createAuthentication(sc);
        } catch (IOException ie) {
            LOGGER.error("Unable to get access token", ie);
            throw new CredentialsExpiredException("OAuth login invalid or expired access token");
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean supports(Class authentication) {
        return OAuthAuthenticationToken.class.isAssignableFrom(authentication);
    }
    
    /**
     * Creates an authentication object from the {@code SecurityContext}.
     * 
     * @param sc {@code SecurityContext}
     * @return an {@code OAuthAuthenticationToken} that is used as the {@code Authentication} object by the Spring Security
     */
    public static Authentication createAuthentication(SecurityContext sc) {
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(sc.getRole()));
        OAuthAuthenticationToken newAuthToken =
            new OAuthAuthenticationToken(new ForceUserPrincipal(sc.getUserName(), sc.getSessionId()), null,
            authorities);
        newAuthToken.setDetails(sc.getForceSecurityContext());
        return newAuthToken;
    }
}
