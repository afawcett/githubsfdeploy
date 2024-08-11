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

import com.sforce.soap.partner.GetUserInfoResult;

import java.io.Serializable;

/**
 * 
 * Abstract class that should be extended when using a custom data retriever. The base functionality
 * wraps {@code ForceSecurityContext} and provides the methods necessary to implement {@code SecurityContext}.
 * The extension should contain fields along with getters and setters that represent any custom user data
 * that will be stored for the authenticated user. See @doclink retrieve-user-data for more information.
 *
 * @author John Simone
 */
public abstract class CustomSecurityContext implements SecurityContext, Serializable {

    private static final long serialVersionUID = 1L;

    private SecurityContext forceSecurityContext;
    
    /**
     * Initializes the SecurityContext from a {@code GetUserInfoResult} object.
     * 
     * @param userInfo a user info result from the SOAP partner API
     */
    @Override
    public final void init(GetUserInfoResult userInfo) {
        forceSecurityContext.init(userInfo);
    }

    @Override
    public final void setOrgId(String orgId) {
        forceSecurityContext.setOrgId(orgId);
    }

    @Override
    public final String getOrgId() {
        return forceSecurityContext.getOrgId();
    }

    @Override
    public final void setUserId(String userId) {
        forceSecurityContext.setUserId(userId);
    }

    @Override
    public final String getUserId() {
        return forceSecurityContext.getUserId();
    }

    @Override
    public final void setEndPoint(String endPoint) {
        forceSecurityContext.setEndPoint(endPoint);
    }

    @Override
    public final String getEndPoint() {
        return forceSecurityContext.getEndPoint();
    }

    @Override
    public final void setEndPointHost(String endPointHost) {
        forceSecurityContext.setEndPoint(endPointHost);
    }

    @Override
    public final String getEndPointHost() {
        return forceSecurityContext.getEndPointHost();
    }

    @Override
    public final void setSessionId(String sessionId) {
        forceSecurityContext.setSessionId(sessionId);
    }

    @Override
    public final String getSessionId() {
        return forceSecurityContext.getSessionId();
    }

    @Override
    public final void setRefreshToken(String refreshToken) {
        forceSecurityContext.setRefreshToken(refreshToken);
    }

    @Override
    public final String getRefreshToken() {
        return forceSecurityContext.getRefreshToken();
    }

    @Override
    public final void setUserName(String userName) {
        forceSecurityContext.setUserName(userName);
    }

    @Override
    public final String getUserName() {
        return forceSecurityContext.getUserName();
    }

    @Override
    public final void setLanguage(String language) {
        forceSecurityContext.setLanguage(language);
    }

    @Override
    public final String getLanguage() {
        return forceSecurityContext.getLanguage();
    }

    @Override
    public final void setLocale(String locale) {
        forceSecurityContext.setLocale(locale);
    }

    @Override
    public final String getLocale() {
        return forceSecurityContext.getLocale();
    }

    @Override
    public final void setTimeZone(String timeZone) {
        forceSecurityContext.setTimeZone(timeZone);
    }

    @Override
    public final String getTimeZone() {
        return forceSecurityContext.getTimeZone();
    }
    
    @Override
    public String getRole() {
        return forceSecurityContext.getRole();
    }
    
    @Override
    public void setRole(String role) {
        forceSecurityContext.setRole(role);
    }
    
    public final void setForceSecurityContext(SecurityContext sc) {
        this.forceSecurityContext = sc;
    }
    
    @Override
    public final SecurityContext getForceSecurityContext() {
        return this.forceSecurityContext;
    }

}
