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

import com.force.sdk.oauth.context.SecurityContext;
import com.sforce.ws.ConnectionException;

/**
 * Retrieves data about the authenticated user for storage in the {@code SecurityContext}.
 * 
 * @author John Simone
 */
public class UserDataRetrievalService {

    private UserDataRetriever forceDataRetriever;
    private boolean storeUsername;

    /**
     * Creates the service with the given value for the {@code storeUsername} flag.
     * 
     * @param storeUsername boolean
     */
    public UserDataRetrievalService(boolean storeUsername) {
        this.forceDataRetriever = new ForceUserDataRetriever();
        this.storeUsername = storeUsername;
    }

    /**
     * Creates a {@code ForceUserDataRetriever}.
     */
    public UserDataRetrievalService() {
        this.forceDataRetriever = new ForceUserDataRetriever();
    }

    /**
     * Sets up the user data retriever and retrieves data about the user.
     * 
     * @param sessionId String
     * @param endpoint String
     * @param refreshToken String
     * @return SecurityContext containing data about the user
     * @throws ConnectionException connection error
     */
    public SecurityContext retrieveUserData(String sessionId, String endpoint, String refreshToken)
            throws ConnectionException {

        forceDataRetriever.setSessionId(sessionId);
        forceDataRetriever.setEndpoint(endpoint);
        forceDataRetriever.setRefreshToken(refreshToken);
        forceDataRetriever.setStoreUsername(storeUsername);
        return forceDataRetriever.retrieveUserData();
    }

    public void setStoreUsername(boolean storeUsername) {
        this.storeUsername = storeUsername;
    }


    /**
     * Returns whether or not the username should be stored.
     * @return boolean  
     */
    public boolean isStoreUsername() {
        return storeUsername;
    }

}
