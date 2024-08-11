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

package com.force.sdk.oauth.context.store;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.*;

import com.force.sdk.oauth.context.SecurityContext;

/**
 * 
 * Handles the storage of a {@code SecurityContext} via server side session.
 *
 * @author John Simone
 */
public class SecurityContextSessionStore implements
        SecurityContextStorageService {

    /**
     * The key used to store the {@code SecurityContext} in the session.
     */
    public static final String SECURITY_CONTEXT_SESSION_KEY = "com.force.sdk.securitycontext";

    /**
     * Stores the security context in the session.
     * {@inheritDoc}
     */
    @Override
    public void storeSecurityContext(HttpServletRequest request,
            HttpServletResponse response, SecurityContext securityContext)
            throws ContextStoreException {
        HttpSession session = request.getSession();
        session.setAttribute(SECURITY_CONTEXT_SESSION_KEY, securityContext);
    }

    /**
     * Retrieves the security context from the session.
     * {@inheritDoc}
     */
    @Override
    public SecurityContext retreiveSecurityContext(HttpServletRequest request)
            throws ContextStoreException {
        HttpSession session = request.getSession();
        SecurityContext sc =
            (SecurityContext) session.getAttribute(SECURITY_CONTEXT_SESSION_KEY);
        return sc;
    }

    /**
     * This invalidates the session when server side sessions are being used for {@code SecurityContext} storage.
     * {@inheritDoc}
     */
    @Override
    public void clearSecurityContext(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public SecretKeySpec getSecureKey() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isContextStored(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            if (session.getAttribute(SECURITY_CONTEXT_SESSION_KEY) != null) {
                return true;
            }
        }
        return false;
    }

}
