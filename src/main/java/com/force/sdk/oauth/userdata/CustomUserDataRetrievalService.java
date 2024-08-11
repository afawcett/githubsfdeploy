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

import com.force.sdk.oauth.context.CustomSecurityContext;
import com.force.sdk.oauth.context.SecurityContext;
import com.sforce.ws.ConnectionException;

/**
 * Defines the logic for calling a {@code CustomUserDataRetriever}. When a custom user data retrieval is used, a
 * {@code CustomUserDataRetriever} that returns the {@code CustomSecurityContext} object will be needed. The
 * {@code CustomUserDataRetrievalService} is called to retrieve custom user data and it will, in turn, call the application
 * specific {@code CustomUserDataRetriever}.
 * 
 * @author John Simone
 */
public final class CustomUserDataRetrievalService extends UserDataRetrievalService {

    private CustomUserDataRetriever<?> customDataRetriever;

    protected CustomUserDataRetrievalService() {

    }

    /**
     * Creates a custom user data retrieval service with this {@code CustomUserDataRetriever}.
     * 
     * @param customDataRetriever CustomUserDataRetriever
     * @param storeUsername
     *            flag that controls whether or not the username will be stored
     */
    public CustomUserDataRetrievalService(CustomUserDataRetriever<?> customDataRetriever, boolean storeUsername) {
        super(storeUsername);
        this.customDataRetriever = customDataRetriever;
    }

    /**
     * Creates a custom user data retrieval service with this {@code CustomUserDataRetriever}.
     *
     * @param customDataRetriever CustomUserDataRetriever
     */
    public CustomUserDataRetrievalService(CustomUserDataRetriever<?> customDataRetriever) {
        this.customDataRetriever = customDataRetriever;
    }

    public void setCustomDataRetriever(CustomUserDataRetriever<?> customDataRetriever) {
        this.customDataRetriever = customDataRetriever;
    }

    /**
     * Calls the super method to retrieve the default user data. Then sets up the custom user data retriever and uses it to
     * retrieve the extended user data.
     * 
     * @param sessionId String
     * @param endpoint String
     * @param refreshToken String
     * @throws ConnectionException connection error 
     * @return SecurityContext 
     */
    @Override
    public SecurityContext retrieveUserData(String sessionId, String endpoint, String refreshToken)
            throws ConnectionException {

        SecurityContext sc = super.retrieveUserData(sessionId, endpoint, refreshToken);

        customDataRetriever.setSessionId(sessionId);
        customDataRetriever.setEndpoint(endpoint);
        customDataRetriever.setRefreshToken(refreshToken);

        CustomSecurityContext customSecurityContext = customDataRetriever.retrieveUserData();

        customSecurityContext.setForceSecurityContext(sc);
        sc = customSecurityContext;

        return sc;
    }

}
