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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.force.sdk.oauth.context.SecurityContext;

/**
 * 
 * Defines the interface for storing, retrieving, and clearing a {@code SecurityContext} to/from 
 * a storage service. Implementors of this interface will represent different methods 
 * of handling the storage of a {@code SecurityContext}.
 *
 * @author John Simone
 */
public interface SecurityContextStorageService {

    /**
     * Stores the security context. The means of storage will vary by implementation.
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param securityContext SecurityContext
     * @throws ContextStoreException {@link ContextStoreException}
     */
    void storeSecurityContext(
            HttpServletRequest request, HttpServletResponse response, SecurityContext securityContext)
            throws ContextStoreException;
    
    /**
     * Retrieves the security context. The means of storage will vary by implementation.
     * 
     * @param request HttpServletRequest
     * @return the stored SecurityContext
     * @throws ContextStoreException {@link ContextStoreException}
     */
    SecurityContext retreiveSecurityContext(HttpServletRequest request) throws ContextStoreException;
    
    /**
     * Clears the security context from storage. This won't be relevant for all storage types.
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    void clearSecurityContext(HttpServletRequest request, HttpServletResponse response);
    
    /**
     * Retrieves the secret key if one is used in this security context store.
     * @return SecretKeySpec
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    SecretKeySpec getSecureKey() throws ForceEncryptionException;
    
    /**
     * Detects whether or not there is a security context stored via this storage method.
     * 
     * @param request HttpServletRequest
     * @return whether or not the security context is stored using this storage method
     */
    boolean isContextStored(HttpServletRequest request);
}
