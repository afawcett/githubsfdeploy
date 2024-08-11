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

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;

import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.force.sdk.oauth.context.SecurityContext;
import com.sforce.ws.*;

/**
 * 
 * Handles the logout from the partner API. Called when a logout takes place.
 *
 * @author John Simone
 */
public class ForceLogoutHandler implements org.springframework.security.web.authentication.logout.LogoutHandler, SessionRenewer {

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null
                || !(authentication.getDetails() instanceof SecurityContext)) return;
        ForceConnectorConfig config = new ForceConnectorConfig();
        try {
            SecurityContext sc = ((SecurityContext) authentication.getDetails());
            // Use the value from session and not the login endpoint
            request.setAttribute(LogoutSuccessHandler.FORCE_ENDPOINT_ATTRIBUTE, sc.getEndPoint());
            config.setServiceEndpoint(sc.getEndPoint());
            config.setSessionId(sc.getSessionId());
            config.setSessionRenewer(this);
            ForceServiceConnector connector = new ForceServiceConnector();
            connector.setConnectorConfig(config);
            //logout from the partner API
            connector.getConnection().logout();
        } catch (ConnectionException e) {
            if (config.getSessionId() != null) {
                // If the session id is null that means we visited the renewer method below and the session is dead anyways
                throw new AuthenticationServiceException("Unable to logout from Salesforce", e);
            }
        }
    }

    @Override
    public SessionRenewalHeader renewSession(ConnectorConfig config) throws ConnectionException {
        config.setSessionId(null);
        return null;
    }
}
