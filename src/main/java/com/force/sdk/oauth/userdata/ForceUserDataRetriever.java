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

package com.force.sdk.oauth.userdata;

import com.force.sdk.oauth.context.*;
import com.sforce.ws.ConnectionException;

/**
 * 
 * Provides the default implementation of {@code UserDataRetriever}. It calls out to obtain the default user data
 * and should be called to populate the {@code ForceSecurityContext} whether or not a customized
 * {@code UserDataRetriever} is provided.
 *
 * @author John Simone
 */
public class ForceUserDataRetriever implements UserDataRetriever {

    private String sessionId;
    private String endpoint;
    private String refreshToken;
    private boolean storeUsername;
    
    /**
     * Retrieves the default user data from the partner API.
     * @return SecurityContext
     * @throws ConnectionException if a connection to the partner API cannot be made
     */
    @Override
    public SecurityContext retrieveUserData() throws ConnectionException {

        if (sessionId == null) {
            throw new IllegalArgumentException("session id must not be null");
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        
        SecurityContext sc = new ForceSecurityContext();
        sc.setSessionId(sessionId);
        sc.setEndPoint(endpoint);
        if (endpoint.indexOf("/services/Soap/u") > 0) {
            sc.setEndPointHost(endpoint.substring(0, endpoint.indexOf("/services/Soap/u")));
        }

        if (refreshToken != null) {
            sc.setRefreshToken(refreshToken);
        }

        // Now populate the user
        SecurityContextUtil.initializeSecurityContextFromApi(sc);
        
        if (!storeUsername) {
            sc.setUserName(null);
        }
        
        return sc;
    }

    @Override
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    protected String getSessionId() {
        return sessionId;
    }
    
    protected String getEndpoint() {
        return endpoint;
    }
    
    protected String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Returns whether or not the username should be stored.
     * @return boolean  
     */
    public boolean isStoreUsername() {
        return storeUsername;
    }

    @Override
    public void setStoreUsername(boolean storeUsername) {
        this.storeUsername = storeUsername;
    }

}
