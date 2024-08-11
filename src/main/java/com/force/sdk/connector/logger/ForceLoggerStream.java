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

package com.force.sdk.connector.logger;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that writes to a log file.
 *
 * @author Fiaz Hossain
 */
public class ForceLoggerStream extends OutputStream {

    private static final int DEFAULT_BUFFER_LENGTH = 4096; // 4K buffer
    private static final int MAX_BUFFER_LENGTH = 65536; // 64K max then flush

    private StreamLogger logger;
    private byte[] buf;
    private int count;
    private boolean isClosed;
    
    /**
     * Logger interface.
     */
    public interface StreamLogger {
        
        /**
         * Writes the given message to a log file.
         * 
         * @param msg the message to be written to a log file
         */
        void log(String msg);
    }
    
    /**
     * Intializes a {@code ForceLoggerStream} with the
     * given {@code StreamLogger}.
     * <p>
     * Anytime a caller writes bytes to this {@code OutputStream},
     * those bytes will be forwarded to the log file represented
     * by the given {@code StreamLogger}.
     * 
     * @param logger a {@code StreamLogger} to which written bytes
     *               should be forwarded
     */
    public ForceLoggerStream(StreamLogger logger) {
        this.logger = logger;
        reset();
    }
    
    @Override
    public void close() throws IOException {
        flush();
        isClosed = true;
    }
    
    @Override
    public void write(int b) throws IOException {
        if (isClosed) {
            throw new IOException("Logger stream has been closed");
        }
        if (buf.length == count) {
            // If current buffer is at max size just flush it out as a log record
            if (buf.length == MAX_BUFFER_LENGTH) {
                flush();
            } else {
                int len = buf.length * 2 > MAX_BUFFER_LENGTH ? MAX_BUFFER_LENGTH : buf.length * 2;
                byte [] tbuf = new byte[len];
                System.arraycopy(buf, 0, tbuf, 0, count);
                buf = tbuf;
            }
        }
        buf[count++] = (byte) b;
    }

    @Override
    public void flush() throws IOException {
        if (count == 0 || (count == 1 && (((char) buf[0]) == '\n' || ((char) buf[0]) == '\r'))) {
            reset();
            return;
        }

        logger.log(new String(buf, 0, count));
        reset();
    }
    
    private void reset() {
        count = 0;
        buf = new byte[DEFAULT_BUFFER_LENGTH];
    }
}
