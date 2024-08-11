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
 * Default implementation of a {@code SecurityContext}. Holds an authenticated user's data
 * if a custom user data retriever is not used. When using a custom user data retriever, the {@code CustomSecurityContext}
 * will wrap a {@code ForceSecurityContext} and add the additional custom fields.
 *
 * @author John Simone
 */
public final class ForceSecurityContext implements SecurityContext, Serializable {
    
    private static final long serialVersionUID = 1L;

    private String orgId;
    private String userId;
    private String endPoint;
    private String endPointHost;
    private String sessionId;
    private String refreshToken;
    private String userName;
    private String language;
    private String locale;
    private String timeZone;
    private String role;
    
    /**
     * Initializes the security context from a GetUserInfoResult object.
     * {@inheritDoc}
     */
    @Override
    public void init(GetUserInfoResult userInfo) {
        this.orgId = userInfo.getOrganizationId();
        this.userId = userInfo.getUserId();
        this.userName = userInfo.getUserName();
        this.language = userInfo.getUserLanguage();
        this.locale = userInfo.getUserLocale();
        this.timeZone = userInfo.getUserTimeZone();
    }
    
    @Override
    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }
    
    @Override
    public String getOrgId() {
        return orgId;
    }
    
    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Override
    public String getUserId() {
        return userId;
    }
    
    @Override
    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    public String getEndPoint() {
        return endPoint;
    }

    @Override
    public String getEndPointHost() {
        return endPointHost;
    }

    @Override
    public void setEndPointHost(String endPointHost) {
        this.endPointHost = endPointHost;
    }

    @Override
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
    @Override
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    @Override
    public String getRefreshToken() {
        return refreshToken;
    }
    
    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
    }
    
    @Override
    public String getLanguage() {
        return language;
    }
    
    @Override
    public void setLocale(String locale) {
        this.locale = locale;
    }
    
    @Override
    public String getLocale() {
        return locale;
    }
    
    @Override
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    @Override
    public String getTimeZone() {
        return timeZone;
    }

    @Override
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String getRole() {
        return role;
    }
    
    @Override
    public SecurityContext getForceSecurityContext() {
        return this;
    }
}

