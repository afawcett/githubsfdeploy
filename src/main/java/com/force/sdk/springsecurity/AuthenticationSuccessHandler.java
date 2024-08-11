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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

/**
 * 
 * Called after a successful authentication. In this case, that is after the access code callback is received.
 * The target URL is determined and the user is redirected there.
 *
 * @author Fiaz Hossain
 * @author John Simone
 */
public class AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /**
     * Default constructor. No default values set here.
     */
    public AuthenticationSuccessHandler() {
    }

    /**
     * Creates a handler with the given {@code defaultTargetUrl}.
     * 
     * @param defaultTargetUrl the default URL to redirect to after authentication
     */
    public AuthenticationSuccessHandler(String defaultTargetUrl) {
        super(defaultTargetUrl);
    }

    /**
     * Determines the target URL.
     * The target URL either comes from the state parameter in the callback request or the default target url is used.
     * The default target url is configured in the default-login-success attribute of the fss:oauth element. 
     * See @doclink spring-security for more information.
     */
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        // After login we take the state out of request parameter and redirect there. 
        if (!isAlwaysUseDefaultTargetUrl()) {
            String targetUrl = request.getParameter("state");
            if (targetUrl != null) return targetUrl;
        }
        return super.determineTargetUrl(request, response);
    }
}
