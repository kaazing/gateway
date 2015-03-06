/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsr.util;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.kaazing.gateway.transport.ws.util.WsUtils;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class Amf3Utils {

    public enum Type {
        UNDEFINED(0), NULL(1), FALSE(2), TRUE(3), INTEGER(4), DOUBLE(5), STRING(6), XMLDOC(7), DATE(8), ARRAY(9), OBJECT(10), XML(11), BYTEARRAY(12);

        private final byte code;

        Type(int code) {
            this.code = (byte) code;
        }

        Type(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }
        
        public boolean skip(IoBufferEx buf) {
        	switch(this) {
            case UNDEFINED:
            case NULL:
            case FALSE:
            case TRUE:
            	return true;
            case STRING:
                return skipString(buf);
            case BYTEARRAY:
            	return skipByteArray(buf);
            case OBJECT:
            case ARRAY:
        		default:
        			throw new UnsupportedOperationException();
        	}
        }
    }

    private static final Charset utf8 = Charset.forName("UTF-8");
    private static final CharsetEncoder utf8encoder = utf8.newEncoder();

    /**
     * Decode and throw away the next Object of any type
     * 
     * @param buf
     */
    public static boolean skipType(IoBufferEx buf) {
    	Type type = Type.values()[buf.get()];
    	return type.skip(buf);
    }
    
    /**
     * Decode variable-length U29 values
     * 
     * @param buf
     * @return
     */
    public static int decodeLength(IoBufferEx buf) {
    	int l = 0;
    	int width = 1;
    	byte b;
    	while (width <= 4) {
    		b = buf.get();
    		if (width < 4) {
    			l <<= 7;
    			l += (b & 0x7f);
    			
    			// check for continue bit if this is not the last byte
    			if (b >> 7 == 0) {
    				break;
    			}
    		} else {
    			// use all 8 bits from the 4th byte
    			l <<= 7;
    			l += b;
    		}
    		width++;
    	}
    	return l;
    }
    
    public static int decodeLengthWithLowFlag(IoBufferEx buf) {
    	int l = decodeLength(buf);
    	// remove least significant bit
    	l >>= 1;
    	return l;
    }
    
    public static IoBufferEx encodeLength(IoBufferEx buf, int l) {
    	WsUtils.encodeLength(buf.buf(), l);
    	return buf;
    }
    
    public static IoBufferEx encodeLengthWithLowFlag(IoBufferEx buf, int l) {
    	// shift up 1, and set the lowest bit without
    	// altering the U29 encoded value
    	int n = (l<<1) | 1;
    	return encodeLength(buf, n); 
    }
    
    public static CharSequence decodeString(IoBufferEx buf) {
        int length = decodeLength(buf);
        IoBufferEx slice = buf.getSlice(length);
        CharSequence s = utf8.decode(slice.buf());
        return s;
    }
    
    /**
     * Skip over a String
     * 
     * @param buf
     * @return
     */
    public static boolean skipString(IoBufferEx buf) {
        int length = decodeLength(buf);
        buf.skip(length);
        return true;
    }
    
    public static IoBufferEx decodeByteArray(IoBufferEx buf) {
        int length = decodeLengthWithLowFlag(buf);
        IoBufferEx slice = buf.getSlice(length);
        return slice;
    }
    
    /**
     * Skip over a ByteArray
     * 
     * @param buf
     * @return
     */
    public static boolean skipByteArray(IoBufferEx buf) {
        int length = decodeLengthWithLowFlag(buf);
        buf.skip(length);
        return true;
    }
   

    public static IoBuffer encodeString(IoBuffer buf, CharSequence s) {
        buf.put(Type.STRING.getCode());
        return encodeStringTypeless(buf, s);
    }

    public static IoBuffer encodeStringTypeless(IoBuffer buf, CharSequence s) {
        // advance position by two to leave room for U16 length
        int lengthAt = buf.position();
        buf.position(lengthAt + 2);
        try {
            buf.putString(s, utf8encoder);
        } catch (CharacterCodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // write string length
        buf.putShort(lengthAt, (short) (buf.position() - (lengthAt + 2)));
        return buf;
    }

    public static IoBuffer encodeDouble(IoBuffer buf, double n) {
        buf.put(Type.DOUBLE.getCode());
        buf.putDouble(n);
        return buf;
    }
        
    public static IoBuffer encodeUndefined(IoBuffer buf) {
        buf.put(Type.UNDEFINED.getCode());
        return buf;
    }
    
    public static IoBuffer encodeNull(IoBuffer buf) {
        buf.put(Type.NULL.getCode());
        return buf;
    }
    
    public static IoBuffer encodeBoolean(IoBuffer buf, boolean val) {
        buf.put(val ? Type.TRUE.getCode() : Type.FALSE.getCode());
        return buf;
    }
	
	public static IoBufferEx encodeByteArray(IoBufferEx buf, IoBufferEx b) {
        buf.put(Type.BYTEARRAY.getCode());
		int length = b.limit() - b.position();
		encodeLengthWithLowFlag(buf, length);
		buf.put(b);
		return buf;
	}

}
