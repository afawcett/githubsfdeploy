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

import com.force.sdk.oauth.context.SecurityContext;
import com.force.sdk.oauth.context.SecurityContextUtil;
import com.sforce.ws.util.Base64;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * 
 * Handles the storage of a SecurityContext via browser cookies.
 *
 * @author John Simone
 */
public class SecurityContextCookieStore implements
        SecurityContextStorageService {

    /**
     * Constant that stores the name of the security context cookie.
     */
    public static final String SECURITY_CONTEXT_COOKIE_NAME = "security_context";
    private SecretKeySpec skeySpec = null;
    private boolean encrypted = true;
    private String cookiePath = null;
    
    /**
     * Saves the security context to a browser cookie.
     * {@inheritDoc}
     */
    @Override
    public void storeSecurityContext(HttpServletRequest request,
            HttpServletResponse response, SecurityContext securityContext) throws ContextStoreException {
        try {
            Cookie contextCookie = null;
            //Refresh tokens should not be stored in cookies. Set it to null.
            securityContext.setRefreshToken(null);
            byte[] securityContextSer = serializeSecurityContext(securityContext, encrypted);
            contextCookie = new Cookie(SECURITY_CONTEXT_COOKIE_NAME, URLEncoder.encode(b64encode(securityContextSer), "UTF-8"));
            contextCookie.setSecure(SecurityContextUtil.useSecureCookies(request));
            contextCookie.setPath(cookiePath);
            response.addCookie(contextCookie);
        } catch (ForceEncryptionException e) {
            throw new ContextStoreException(e);
        } catch (IOException e) {
            throw new ContextStoreException(e);
        }
    }

    /**
     * Retrieves the security context from a browser cookie.
     * {@inheritDoc}
     */
    @Override
    public SecurityContext retreiveSecurityContext(HttpServletRequest request) throws ContextStoreException {
        
        try {
            Cookie[] cookies = request.getCookies();
            String value = null;
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (SECURITY_CONTEXT_COOKIE_NAME.equals(cookie.getName())) {
                        value = URLDecoder.decode(cookie.getValue(), "UTF-8");
                    }
                }
            }
            
            if (value != null) {
                return deserializeSecurityContext(b64decode(value), encrypted);
            }
        } catch (IOException e) {
            throw new ContextStoreException(e);
        } catch (ForceEncryptionException e) {
            throw new ContextStoreException(e);
        } catch (ClassNotFoundException e) {
            throw new ContextStoreException(e);
        }
        
        return null;
    }
    
    /**
     * Serializes and encrypts the security context so that it can be stored in a cookie.
     * 
     * @param sc
     * @param encrypt
     * @return the serialized and encrypted security context
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     * @throws IOException
     */
    private byte[] serializeSecurityContext(SecurityContext sc, boolean encrypt)
        throws ForceEncryptionException, IOException {
        // Serialize to a byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(sc);
        out.close();
        
        byte[] securityContextSer = bos.toByteArray();
        if (encrypt) {
            securityContextSer = AESUtil.encrypt(securityContextSer, getSecureKey());
        }
        
        return securityContextSer;
    }
    
    /**
     * Decrypts and deserializes the security context.
     * 
     * @param securityContextSer
     * @param isEncrypted
     * @return the security context
     * @throws ForceEncryptionException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private SecurityContext deserializeSecurityContext(byte[] securityContextSer, boolean isEncrypted)
        throws ForceEncryptionException, IOException, ClassNotFoundException {
        if (isEncrypted) {
            securityContextSer =
                AESUtil.decrypt(securityContextSer, getSecureKey());
        }
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(securityContextSer));
        return (SecurityContext) in.readObject();
    }
    
    @Override
    public SecretKeySpec getSecureKey() throws ForceEncryptionException {
        if (skeySpec == null) {
            skeySpec = AESUtil.getSecretKey();
        }
        return skeySpec;
    }

    /**
     * Sets the key file name and reads the key from the file.
     * @param fileName String
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    public void setKeyFileName(String fileName) throws ForceEncryptionException {
        skeySpec  = AESUtil.getSecretKey(fileName);
    }
    
    /**
     * Sets the key directly
     * @param fileName String
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    public void setKey(String key) throws ForceEncryptionException {
    	skeySpec  = AESUtil.getSecretKeySpec(key);
    }
    
    /**
     * Returns a base-64 encoded byte array.
     * 
     * @param b - byte array
     * @return b64 String representation
     */
    public static String b64encode(byte[] b) {
        return new String(Base64.encode(b));
    }
    
    /**
     * Decodes a base-64 encoded string and returns a byte array.
     * 
     * @param s - the encoded string
     * @return the byte array
     */
    public static byte[] b64decode(String s) {
        return Base64.decode(s.getBytes());
    }

    /**
     * Clears the security context cookies from the browser.
     * {@inheritDoc}
     */
    @Override
    public void clearSecurityContext(HttpServletRequest request, HttpServletResponse response) {
        Cookie clearCookie = new Cookie(SECURITY_CONTEXT_COOKIE_NAME, "");
        clearCookie.setMaxAge(0);
        response.addCookie(clearCookie);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isContextStored(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        
        for (int i = 0; i < cookies.length; i++) {
            if (SECURITY_CONTEXT_COOKIE_NAME.equals(cookies[i].getName())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Sets the path for the cookie to use
     * @param cookiePath
     */
    public void setCookiePath(String cookiePath) {
    	this.cookiePath = cookiePath;
    }

}
