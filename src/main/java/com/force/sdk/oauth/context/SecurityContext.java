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

/**
 * 
 * Interface representing the standard fields stored when a user is authenticated.
 * After authentication the security context is stored and made available to the thread processing the user's request.
 *
 * @author John Simone
 */
public interface SecurityContext {
    
    /**
     * Initializes the security context from a {@code GetUserInfoResult} object.
     * @param userInfo GetUserInfoResult
     */
    void init(GetUserInfoResult userInfo);
    /**
     * Sets the orgid.
     * @param orgId String
     */
    void setOrgId(String orgId);
    /**
     * Gets the orgId. 
     * @return orgId
     */
    String getOrgId();
    /**
     * Sets the userId.
     * @param userId String
     */
    void setUserId(String userId);
    /**
     * Gets the userId.
     * @return userId
     */
    String getUserId();
    /**
     * Sets the endPoint.
     * @param endPoint String
     */
    void setEndPoint(String endPoint);
    /**
     * Gets the endPoint.
     * @return endPoint String
     */
    String getEndPoint();
    /**
     * Sets the host of endPoint.
     * @param endPointHost String
     */
    void setEndPointHost(String endPointHost);
    /**
     * Gets the host of endPoint.
     * @return endPointHost String
     */
    String getEndPointHost();
    /**
     * Sets the sessionId.
     * @param sessionId String
     */
    void setSessionId(String sessionId);
    /**
     * Gets the sessionId.
     * @return sessionId
     */
    String getSessionId();
    /**
     * Sets the refreshToken.
     * @param refreshToken String
     */
    void setRefreshToken(String refreshToken);
    /**
     * Gets the refreshToken.
     * @return refreshToken
     */
    String getRefreshToken();
    /**
     * Sets the userName.
     * @param userName String
     */
    void setUserName(String userName);
    /**
     * Gets the userName.
     * @return userName
     */
    String getUserName();
    /**
     * Sets the language.
     * @param language String
     */
    void setLanguage(String language);
    /**
     * Gets the language.
     * @return language
     */
    String getLanguage();
    /**
     * Sets the locale.
     * @param locale String
     */
    void setLocale(String locale);
    /**
     * Gets the locale.
     * @return locale
     */
    String getLocale();
    /**
     * Sets the timeZone.
     * @param timeZone String
     */
    void setTimeZone(String timeZone);
    /**
     * Gets the timeZone.
     * @return timeZone
     */
    String getTimeZone();
    /**
     * Sets the role.
     * @param role String
     */
    void setRole(String role);
    /**
     * Gets the role.
     * @return role
     */
    String getRole();
    /**
     * Gets the forceSecurityContext.
     * @return forceSecurityContext
     */
    SecurityContext getForceSecurityContext();
}
