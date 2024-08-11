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

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Utility class to handle encryption logic with AES. 
 *
 * @author John Simone
 */
public final class AESUtil {
    static final String ALGORITHM  = "AES";
    static final String PRIVATE_KEY_PROPERTY = "private-key";
    static final String PRIVATE_KEY_PATH = "encryption.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(AESUtil.class);
    private static final int KEY_SIZE = 128;
    
    private AESUtil() {  }
    
    /**
     * Encrypts the value using the passed in key.
     * 
     * @param value data to encrypt
     * @param skeySpec encryption key
     * @return The encrypted data
     * @throws ForceEncryptionException {@link ForceEncryptionException} 
     */
    public static byte[] encrypt(byte[] value, SecretKeySpec skeySpec) throws ForceEncryptionException {
        try {
            Cipher cipher = null;
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = null;
            encrypted = cipher.doFinal(value);
            return encrypted;
        } catch (BadPaddingException e) {
            throw new ForceEncryptionException(e);
        } catch (InvalidKeyException e) {
            throw new ForceEncryptionException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new ForceEncryptionException(e);
        } catch (NoSuchPaddingException e) {
            throw new ForceEncryptionException(e);
        } catch (IllegalBlockSizeException e) {
            throw new ForceEncryptionException(e);
        }
    }
    
    /**
     * Decrypts the value using the passed in key.
     * 
     * @param value data to decrypt
     * @param skeySpec The encryption key
     * @return the decrypted value
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    public static byte[] decrypt(byte[] value, SecretKeySpec skeySpec) throws ForceEncryptionException {
        
        try {
            Cipher cipher = null;
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] original = null;
            original = cipher.doFinal(value);
            return original;
        } catch (BadPaddingException e) {
            throw new ForceEncryptionException(e);
        } catch (InvalidKeyException e) {
            throw new ForceEncryptionException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new ForceEncryptionException(e);
        } catch (NoSuchPaddingException e) {
            throw new ForceEncryptionException(e);
        } catch (IllegalBlockSizeException e) {
            throw new ForceEncryptionException(e);
        }
        
    }
    
    /**
     * Creates a SecretKeySpec object from an AES key. This String could come from a 
     * properties file or other means of persistent configuration.
     * 
     * @param key String
     * @return encryption key
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    public static SecretKeySpec recreateSecretKeySpec(String key) throws ForceEncryptionException {
        SecretKeySpec secretKey = null;
        if (key != null && key.trim().length() > 0) {
            secretKey = new SecretKeySpec(SecurityContextCookieStore.b64decode(key.trim()), ALGORITHM);
        }
        
        if (secretKey == null) {
            secretKey = AESUtil.generateRandomKey();
        }
        
        return secretKey;
    }
    
    /**
     * Calls {@code getSecretKey(String fileName)} with the default filename.
     * 
     * @return encryption key
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    public static SecretKeySpec getSecretKey() throws ForceEncryptionException {
        return getSecretKey(PRIVATE_KEY_PATH);
    }
    
    /**
     * Reads in a stored secret key from a properties file and creates a {@code SecretKeySpec} object from it.
     * 
     * @param fileName String
     * @return encryption key
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    public static SecretKeySpec getSecretKey(String fileName) throws ForceEncryptionException {
        if (fileName == null || fileName.length() == 0) {
            fileName = PRIVATE_KEY_PATH;
        }
        
        InputStream is = AESUtil.class.getResourceAsStream("/" + fileName);
        String key = null;
        if (is == null) {
            LOGGER.warn("Could not open file at  path " + fileName + ". Generating private key... ");
        } else {
            Properties encryptionProps = new Properties();
            try {
                encryptionProps.load(is);
            }  catch (IOException e) {
                LOGGER.warn("Could not open file at  path " + fileName + ". Generating private key... ");
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignored) {
                        // Exception ignored
                    }
                }
            }
            
            key = encryptionProps.getProperty(PRIVATE_KEY_PROPERTY);
            if (key == null ||  key.trim().length() == 0) {
                LOGGER.warn(PRIVATE_KEY_PROPERTY + " property was null in file " + fileName + ". Generating private key... ");
            }
        }
        
        return getSecretKeySpec(key);
    }
    
    public static SecretKeySpec getSecretKeySpec(String key) throws ForceEncryptionException {
       SecretKeySpec secretKey;
       secretKey = AESUtil.recreateSecretKeySpec(key);
       
       return secretKey;
    }
    
    /**
     * Generates a random secret key.
     * 
     * @return encryption key
     * @throws ForceEncryptionException {@link ForceEncryptionException}
     */
    public static SecretKeySpec generateRandomKey() throws ForceEncryptionException {
        KeyGenerator keyGen;
        try {
            keyGen = KeyGenerator.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new ForceEncryptionException(e);
        }
        
        keyGen.init(KEY_SIZE);
        return new SecretKeySpec(keyGen.generateKey().getEncoded(), ALGORITHM);
    }
}
