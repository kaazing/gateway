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

import org.kaazing.mina.core.buffer.IoBufferEx;

public class Amf0Utils {

    public enum Type {
        NUMBER(0), BOOLEAN(1), STRING(2), OBJECT(3), MOVIE_CLIP(4), NULL(5), UNDEFINED(6), REFERENCE(7), ARRAY(8), OBJECT_END(9), STRICT_ARRAY(0x0a), DATE(0x0b), LONG_STRING(0x0c),
        UNSUPPORTED(0x0d), RECORDSET(0x0e), XML_DOCUMENT(0x0f), TYPED_OBJECT(0x10), AMF3(0x11);

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


        public boolean skip(IoBufferEx in) {
            switch (this) {
            case NUMBER:
            	return skipNumber(in);
            case BOOLEAN:
            	return skipBoolean(in);
            case STRING:
            	return skipString(in);
            case OBJECT:
                return skipObject(in);
            case UNDEFINED:
            case NULL:
            	// these types only have type markers, no encoded value
            	return true;
            case OBJECT_END:
            	return false;
            case ARRAY:
            	return skipArray(in);
            case AMF3:
            	return Amf3Utils.skipType(in);
            default:
                throw new UnsupportedOperationException();
            }
        }
    }
    
    public static double getNumber(IoBufferEx buf) throws Exception {
        Amf0Utils.Type type = Amf0Utils.Type.values()[buf.get()];
        if (type != Type.NUMBER) {
        	throw new Exception("Unxpected Type");
        }
        return buf.getDouble();
    }

   public static Type decodeTypeMarker(IoBufferEx buf) {
       Amf0Utils.Type type = Amf0Utils.Type.values()[buf.get()];
       return type;
   }
    
    /**
     * Decode and throw away the next Object of any type
     * 
     * @param in
     */
    public static boolean skipType(IoBufferEx in) {
    	Amf0Utils.Type type = Amf0Utils.Type.values()[in.get()];
    	return type.skip(in);
    }
    

    private static final Charset utf8 = Charset.forName("UTF-8");
    private static final CharsetEncoder utf8encoder = utf8.newEncoder();

    /**
     * Decode a string
     * 
     * @param buf
     * @return
     */
    public static String decodeString(IoBufferEx buf) {
        int length = buf.getUnsignedShort();
        IoBufferEx slice = buf.getSlice(length);
        CharSequence s = utf8.decode(slice.buf());
        return s.toString();
    }

    /**
     * Skip over a String
     * 
     * @param buf
     */
    public static boolean skipString(IoBufferEx buf) {
        int length = buf.getUnsignedShort();
        buf.skip(length);
        return true;
    }
    
    /**
     * Decode a Number
     * 
     * @param buf
     * @return
     */
    public static double decodeNumber(IoBufferEx buf) {
        return buf.getDouble();
    }
    
    /**
     * Skip over 8 bytes, the size of an AMF0 Number
     * 
     * @param buf
     */
    public static boolean skipNumber(IoBufferEx buf) {
    	buf.skip(8);
    	return true;
    }

    public static boolean skipArray(IoBufferEx buf) {
        buf.skip(4); // skip int length
        while (buf.hasRemaining()) {
        	skipType(buf); // skip key
            boolean value = skipType(buf);
            // break at object end marker
            if (!value) {
                break;
            }
        }
        return true;
    }
    
    /**
     * Decode a Boolean
     * 
     * @param buf
     * @return
     */
    public static boolean decodeBoolean(IoBufferEx buf) {
        return buf.get() != 0x00;
    }
    
    /**
     * Skip a Boolean
     * 
     * @param buf
     */
    public static boolean skipBoolean(IoBufferEx buf) {
    	buf.skip(1);
    	return true;
    }   
    
    /**
     * Skip an AMF0 encoded ECMAScript Object.
     * 
     * @param buf
     * @return
     */
    public static boolean skipObject(IoBufferEx buf) {
        while (buf.hasRemaining()) {
            skipString(buf); // skip key
            boolean value = skipType(buf);
            // break at object end marker
            if (!value) {
                break;
            } 
        }
        return true;
    }

    public static IoBufferEx encodeString(IoBufferEx buf, CharSequence s) {
        buf.put(Type.STRING.getCode());
        return encodeStringTypeless(buf, s);
    }
    
    public static IoBufferEx encodeStringTypeless(IoBufferEx buf, CharSequence s) {
        try {
            buf.putPrefixedString(s, 2, utf8encoder);
        } catch (CharacterCodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return buf;
    }
    
    public static IoBufferEx encodeNumber(IoBufferEx buf, double d) {
        buf.put(Type.NUMBER.getCode());
        return buf.putDouble(d);
    }
    
    public static IoBufferEx encodeUndefined(IoBufferEx buf) {
        buf.put(Type.UNDEFINED.getCode());
        return buf;
    }
    
    public static IoBufferEx encodeNull(IoBufferEx buf) {
        buf.put(Type.NULL.getCode());
        return buf;
    }
    
    public static IoBufferEx encodeBoolean(IoBufferEx buf, boolean val) {
    	buf.put(Type.BOOLEAN.getCode());
        buf.put(val ? (byte)1 : (byte)0);
        return buf;
    }

    public static IoBufferEx encodeObjectStart(IoBufferEx buf) {
        return buf.put(Type.OBJECT.getCode());
    	
    }

    public static IoBufferEx encodeObjectEnd(IoBufferEx buf) {
    	encodeStringTypeless(buf, "");
        return buf.put(Type.OBJECT_END.getCode());
    }
    

	public static void encodeByteArray(IoBufferEx buf, IoBufferEx in) {
		buf.put(Type.AMF3.getCode());
		Amf3Utils.encodeByteArray(buf, in);
	}

	public static String decodeObjectKey(IoBufferEx in) {
		// TODO Auto-generated method stub
		return null;
	}

}
