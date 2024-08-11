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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.force.sdk.oauth.connector.ForceOAuthConnector;

/**
 * 
 * Filter that is added to the spring security filter chain as the FORM_LOGIN_FILTER.
 * There are two URLs of interest to this filter:
 * <ul>
 *  <li>The oauth access code callback (_auth)
 *  <li>The application's configured login page url. This is configured in the login-url attribute of the fss:oauth element. 
 *  See @doclink spring-security for more information.
 *  </ul>
 * 
 *
 * @author Fiaz Hossain
 * @author John Simone
 */
public class AuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private ForceOAuthConnector oauthConnector;
    private String filterProcessesUrlOverride;
    private AuthenticationEntryPoint authenticationEntryPoint;

    protected AuthenticationProcessingFilter() {
        super(ForceOAuthConnector.REDIRECT_AUTH_URI);
        setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher(ForceOAuthConnector.REDIRECT_AUTH_URI));
    }

    public void setOauthConnector(ForceOAuthConnector oauthConnector) {
        this.oauthConnector = oauthConnector;
    }

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }
    
    @Override
    public void setFilterProcessesUrl(String filterProcessesUrl) {
        // do not set this value on super class. we let the superclass work with the default value only
        this.filterProcessesUrlOverride = filterProcessesUrl;
    }
    
    /**
     * The main purpose of this filter is to decide if this is a request for the application's configured login URL.
     * If it is, a redirect to the Force.com login page will be sent to begin the OAuth handshake.
     * 
     * @param req {@code ServletRequest }
     * @param res {@code ServletResponse }
     * @param chain {@code FilterChain }
     * @throws IOException when an error occurs sending the redirect
     * @throws ServletException only thrown by parent class
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String uri = request.getRequestURI();
        if (filterProcessesUrlOverride != null
                && isAutenticationCandidate(request.getContextPath(), uri, filterProcessesUrlOverride)) {
            String referer = request.getHeader("Referer");
            if (StringUtils.hasLength(referer)) {
                request.setAttribute(ForceOAuthConnector.LOGIN_REDIRECT_URL_ATTRIBUTE, referer);
            } else {
                request.setAttribute(ForceOAuthConnector.LOGIN_REDIRECT_URL_ATTRIBUTE, "");
            }
            authenticationEntryPoint.commence(request, response, null);
            return;
        }
        super.doFilter(request, response, chain);
        return;
        
    }
    
    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        Assert.notNull(oauthConnector, "oauthConnector bean must be set");
        if (filterProcessesUrlOverride != null) {
            Assert.notNull(authenticationEntryPoint, "authenticationEntryPoint bean must be set when filterProcessesUrl is used");
        }
    }
    
    /**
     * Called when the access code callback is received. This will eventually trigger the {@code OAuthAuthenticationProvider}
     * registered as a provider to the authentication manager. 
     * 
     * @param request {@code HttpServletRequest}
     * @param response {@code HttpServletResponse}
     * @return {@code Authentication}
     * @throws AuthenticationException when an error occurs
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException {
        OAuthAuthenticationToken ot =
            new OAuthAuthenticationToken(oauthConnector.getAccessCode(request)); // Use this for OAuth 2.0
        ot.setDetails(oauthConnector.getRedirectUri(request));
        return this.getAuthenticationManager().authenticate(ot);
    }

    private boolean isAutenticationCandidate(String contextPath, String uri, String candidate) {
        if ("".equals(contextPath)) {
            return uri.endsWith(candidate);
        }

        return uri.endsWith(contextPath + candidate);
    }
       
}
