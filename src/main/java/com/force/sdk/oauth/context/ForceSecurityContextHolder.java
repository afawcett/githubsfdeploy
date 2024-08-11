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

import com.force.sdk.connector.threadlocal.ForceThreadLocalStore;

/**
 * 
 * Provides a mechanism to access the {@code SecurityContext} for the authenticated user.
 * Security contexts are stored in thread local variables and managed by the {@code ForceThreadLocalStore}.
 *
 * @author Fiaz Hossain
 */
public final class ForceSecurityContextHolder {

    private ForceSecurityContextHolder() {  }
    
    /**
     * Gets the security context stored in the thread local store.
     * Do not create one if it doesn't exist.
     * @return the current security context
     */
    public static SecurityContext get() {
        return get(false);
    }

    /**
     * Gets the security context stored in the thread local store.
     * 
     * @param createIfNeeded Create the security context if there currently isn't one
     * @return the current security context
     */
    public static SecurityContext get(boolean createIfNeeded) {
        
        SecurityContext sc = ForceThreadLocalStore.getThreadLocal(SecurityContext.class);
        if (sc == null && createIfNeeded) {
            sc = new ForceSecurityContext();
            ForceThreadLocalStore.setThreadLocal(SecurityContext.class, sc);
        }
        return sc;
    }
    
    /**
     * Sets the security context to the thread local store.
     * @param sc SecurityContext
     */
    public static void set(SecurityContext sc) {
        ForceThreadLocalStore.setThreadLocal(SecurityContext.class, sc);
    }

    /**
     * Releases the current security context from the thread local store.
     */
    public static void release() {
        ForceThreadLocalStore.setThreadLocal(SecurityContext.class, null);
    }

}

