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

package com.force.sdk.oauth.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 
 * Retrieves the auth token by calling the token request URL.
 *
 * @author John Simone
 */
public class TokenRetrievalServiceImpl implements TokenRetrievalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRetrievalServiceImpl.class);

    /**
     * Obtains an access token by calling the OAuth authentication endpoint and either trading an 
     * access code or refresh token for it.
     * 
     * {@inheritDoc}
     */
    @Override
    public String retrieveToken(
            String hostAndPort, String params, String refreshToken, ForceOAuthConnectionInfo connInfo) throws IOException {

        URL accessURL = new URL(hostAndPort + "/services/oauth2/token");
    
        HttpURLConnection conn = (HttpURLConnection) accessURL.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.print(params);
        } catch (IOException e) {
            throwDetailedException(conn, e);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
        
        BufferedReader reader = null;
        String responsePayload = null;
        try {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            responsePayload = reader.readLine();
        } catch (IOException e) {
            throwDetailedException(conn, e);
        } finally {
            if (reader != null) reader.close();
        }
        
        return responsePayload;
        
    }

    private void throwDetailedException(HttpURLConnection conn, IOException e) throws IOException {
        BufferedReader errorIn = null;
        try {
            if (conn.getErrorStream() != null) {
                errorIn = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String message = errorIn.readLine();
                if (message != null && !"".equals(message)) {
                    throw new IOException(message, e);
                } else {
                    throw e;
                }
            }
        } finally {
            if (errorIn != null) {
                try {
                    errorIn.close();
                } catch (IOException e1) {
                    LOGGER.error("Error closing error-input-stream from api call.");
                }
            }
        }
    }

}
