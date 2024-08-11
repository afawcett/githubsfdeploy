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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.force.sdk.oauth.connector.ForceOAuthConnector;
import com.force.sdk.oauth.context.SecurityContextService;

/**
 * 
 * Redirects user to the configured logout URL. Called after all logout handlers are processed.
 * Can be configured to also log the user out of Force.com. Both options can be configured on the fss:oauth element.
 * The logout URL comes from the default-logout-success attribute and the flag for logging out from Force.com comes
 * from the logout-from-force-dot-com attribute. See @doclink spring-security for more information.
 *
 * @author Fiaz Hossain
 * @author John Simone
 */
public class LogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler
    implements InitializingBean {

    static final String FORCE_ENDPOINT_ATTRIBUTE = "__force_endpoint__";
    
    private boolean logoutFromForceDotCom;
    private ForceOAuthConnector oauthConnector;
    private SecurityContextService securityContextService;

    /**
     * Default constructor. No values defaulted. 
     * The oauthConnector and securityContextService must be set manually if not created in a Spring container.
     */
    public LogoutSuccessHandler() {
    }

    public void setLogoutFromForceDotCom(boolean logoutFromForceDotCom) {
        this.logoutFromForceDotCom = logoutFromForceDotCom;
    }

    public void setOauthConnector(ForceOAuthConnector oauthConnector) {
        this.oauthConnector = oauthConnector;
    }

    
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Override
    public void afterPropertiesSet() {
        if (logoutFromForceDotCom) {
            Assert.notNull(oauthConnector, "oauthConnector bean must be set");
            Assert.notNull(securityContextService, "securityContextService bean must be set");
        }
    }
    
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        if (isAlwaysUseDefaultTargetUrl()) {
            return getDefaultTargetUrl();
        }
        String targetUrl = request.getParameter(getTargetUrlParameter());

        if (StringUtils.hasLength(targetUrl)) return targetUrl;

        targetUrl = (String) request.getAttribute(FORCE_ENDPOINT_ATTRIBUTE);
        if (logoutFromForceDotCom && StringUtils.hasLength(targetUrl)) {
            return oauthConnector.getForceLogoutUrl(request, targetUrl, getDefaultTargetUrl());
        }
        return super.determineTargetUrl(request, response);
    }

    /**
     * Clears the security context. This is necessary when cookie based storage is used. 
     * 
     * @param request {@code HttpServletRequest}
     * @param response {@code HttpServletResponse}
     * @param authentication {@code Authentication} 
     * @throws IOException only from call to super class
     * @throws ServletException only from call to super class
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        securityContextService.clearSecurityContext(request, response);
        super.onLogoutSuccess(request, response, authentication);
    }
}
