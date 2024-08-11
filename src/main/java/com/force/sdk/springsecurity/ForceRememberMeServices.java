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

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;

import com.force.sdk.oauth.context.SecurityContext;
import com.force.sdk.oauth.context.SecurityContextService;

/**
 * Used by the standard spring {@code RememberMeAuthenticationFilter}.
 * This is how the {@code SecurityContext} is retrieved from the {@code SecurityContextService}. Normally
 * this piece of the Spring Security framework is used for a simple remember me cookie, but in this case it is used
 * to validate the presence of a {@code SecurityContext} in the {@code SecurityContextStorageService} so that the
 * authentication can be skipped completely, if necessary.
 *
 * @author John Simone
 */
public class ForceRememberMeServices implements RememberMeServices {

    private SecurityContextService securityContextService = null;

    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Override
    public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
        
        SecurityContext sc = securityContextService.getSecurityContextFromSession(request);
        
        if (sc != null) {
            return OAuthAuthenticationProvider.createAuthentication(sc);
        }
        
        return null;
    }

    @Override
    public void loginFail(HttpServletRequest request, HttpServletResponse response) {
        // login fail won't ever happen since a failed login in OAuth will just result in the user
        // not getting redirected back.
    }

    @Override
    public void loginSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication successfulAuthentication) {
        Object authDetails  = successfulAuthentication.getDetails();
        if (authDetails instanceof SecurityContext) {
            securityContextService.setSecurityContextToSession(request, response, (SecurityContext) authDetails);
        }
    }

}
