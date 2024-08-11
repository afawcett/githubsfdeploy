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

package com.force.sdk.oauth.context;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.force.sdk.oauth.context.store.ForceEncryptionException;

/**
 * 
 * Provides the basic functionality for managing a security context
 * in the session of the authenticated user.
 *
 * @author John Simone
 */
public interface SecurityContextService {
    
    /**
     * Sets the security context to the session.
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param sc SecurityContext
     */
    void setSecurityContextToSession(HttpServletRequest request, HttpServletResponse response, SecurityContext sc);
    
    /**
     * Retrieves the security context. The security context
     * will either come out of the session or will be built from a call
     * to the partner API. The partner API will be called if:
     * - There is no security context in the session, but there is 
     * a session id available in a cookie
     * - There is a security context in the session, but the session id
     * that it contains doesn't match the one available in the cookie
     * 
     * @param request HttpServletRequest
     * @return SecurityContext
     */
    SecurityContext getSecurityContextFromSession(HttpServletRequest request);
    
    /**
     * Verifies the security context against the browser cookies. This will
     * make sure that the necessary cookies exist and that the values match those
     * in the security context. It will create a fresh security context with data 
     * from the partner API, if necessary.
     * 
     * @param sc SecurityContext
     * @param request HttpServletRequest
     * @return SecurityContext
     */
    SecurityContext verifyAndRefreshSecurityContext(SecurityContext sc, HttpServletRequest request);
    
    /**
     * Clears the security context from the context store. 
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    void clearSecurityContext(HttpServletRequest request, HttpServletResponse response);
    
    /**
     * Returns the secret key if one is being used.
     * 
     * @return SecretKeySpec
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    SecretKeySpec getSecretKey() throws ForceEncryptionException;

}
