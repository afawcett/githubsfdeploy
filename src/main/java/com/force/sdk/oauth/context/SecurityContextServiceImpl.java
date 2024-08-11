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

import com.force.sdk.oauth.context.store.ContextStoreException;
import com.force.sdk.oauth.context.store.ForceEncryptionException;
import com.force.sdk.oauth.context.store.SecurityContextStorageService;
import com.force.sdk.oauth.userdata.UserDataRetrievalService;
import com.sforce.ws.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 
 * Provides the basic functionality for managing a security context
 * in the session of the authenticated user.
 * 
 * The implementation provided here is customizable because it delegates user data retrieval to a
 * {@code UserDataRetrievalService} and the security context storage to a {@code SecurityContextStorageService}.
 * The implementation used for each of these can vary
 * depending on whether a custom user data retriever is being used and whether server side session or browser cookie based
 * security context storage is used.
 *
 * @author John Simone
 */
public class SecurityContextServiceImpl implements SecurityContextService {

    private UserDataRetrievalService userDataRetrievalService = null;
    private SecurityContextStorageService securityContextStorageService = null;
    private String cookiePath = null;
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityContextServiceImpl.class);

    public void setUserDataRetrievalService(UserDataRetrievalService userDataRetrievalService) {
        this.userDataRetrievalService = userDataRetrievalService;
    }

    public void setSecurityContextStorageService(SecurityContextStorageService securityContextStorageService) {
        this.securityContextStorageService = securityContextStorageService;
    }
    
    /**
     * Uses the {@code SecurityContextStorageService} to store the security context.
     * {@inheritDoc}
     */
    @Override
    public void setSecurityContextToSession(HttpServletRequest request, HttpServletResponse response, SecurityContext sc) {
        try {
            securityContextStorageService.storeSecurityContext(request, response, sc);
        } catch (ContextStoreException e) {
            LOGGER.error("Cannot store security information: ", e);
        }
        SecurityContextUtil.setCookieValues(sc, response, SecurityContextUtil.useSecureCookies(request), cookiePath);
    }
    
    /**
     * Retrieves the security context. The security context
     * will either come out of the session or will be built from a call
     * to the SOAP partner API. The partner API will be called if:
     * - There is no security context in the session, but there is 
     * a session id available in a cookie
     * - There is a security context in the session, but the session id
     * that it contains doesn't match the one available in the cookie
     * {@inheritDoc}
     */
    @Override
    public SecurityContext getSecurityContextFromSession(HttpServletRequest request) {
        //check for a security context
        SecurityContext sc = null;
        try {
            sc = securityContextStorageService.retreiveSecurityContext(request);
        } catch (ContextStoreException e) {
            LOGGER.warn("Could not retrieve security information, refreshing. "
                + "Set up an encryption key file to avoid this warning.");
            return null;
        }
        
        sc = verifyAndRefreshSecurityContext(sc, request);
        
        return sc;
    }
    
    /**
     * Verifies the security context against the browser cookies. This will
     * ensure that the necessary cookies exist and that the values match those
     * in the security context. It will create a fresh security context with data 
     * from the SOAP partner API if necessary.
     * {@inheritDoc}
     */
    @Override
    public SecurityContext verifyAndRefreshSecurityContext(SecurityContext sc, HttpServletRequest request) {
        String sessionId = null;
        String endpoint = null;
        
        //get the session id and endpoint from cookies
        Map<String, String> cookieValueMap = SecurityContextUtil.getCookieValues(request);
        sessionId = cookieValueMap.get(SecurityContextUtil.FORCE_FORCE_SESSION);
        endpoint = cookieValueMap.get(SecurityContextUtil.FORCE_FORCE_ENDPOINT);
        
        //if we found a security context in the session we must make sure that it still
        //matches the values from the cookies. Otherwise throw it away so that we can
        //refresh it.
        if (sc != null) {
            if (sessionId == null || !sessionId.equals(sc.getSessionId())
                    || endpoint == null || !endpoint.equals(sc.getEndPoint())) {
                sc = null;
            }
        }
        
        //populate the security context with user information
        if (sessionId != null && endpoint != null && sc == null) {
            //attempt to connect to the partner API and retrieve the user data
            //if this fails, set the security context to null because we'll
            //need to redo the oauth handshake.
            try {
                sc = userDataRetrievalService.retrieveUserData(sessionId, endpoint, null);
            } catch (ConnectionException e) {
            	LOGGER.info("Force.com session is invalid. Refreshing... ");
                sc = null;
            }
        }
        
        return sc;
    }

    /**
     * Clears the security context from the security context store and uses the
     * {@code SecurityContextUtil} to clear the other security related cookies.
     * {@inheritDoc}
     */
    @Override
    public void clearSecurityContext(HttpServletRequest request, HttpServletResponse response) {
        securityContextStorageService.clearSecurityContext(request, response);
        SecurityContextUtil.clearCookieValues(response);
    }

    @Override
    public SecretKeySpec getSecretKey() throws ForceEncryptionException {
        return securityContextStorageService.getSecureKey();
    }
    
    public void setCookiePath(String cookiePath) {
      this.cookiePath = cookiePath;
    }
    
}
