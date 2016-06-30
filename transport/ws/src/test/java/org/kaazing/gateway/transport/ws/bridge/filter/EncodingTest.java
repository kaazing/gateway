/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.transport.ws.bridge.filter;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.kaazing.gateway.util.Encoding;

public class EncodingTest {

	@Test
	public void mapped() throws Exception {
		byte[] input = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes();
		byte[] expected = {(byte)0,(byte)16,(byte)131,(byte)16,(byte)81,(byte)135,(byte)32,(byte)146,(byte)139,(byte)48,(byte)211,(byte)143,(byte)65,(byte)20,(byte)147,(byte)81,(byte)85,(byte)151,(byte)97,(byte)150,(byte)155,(byte)113,(byte)215,(byte)159,(byte)130,(byte)24,(byte)163,(byte)146,(byte)89,(byte)167,(byte)162,(byte)154,(byte)171,(byte)178,(byte)219,(byte)175,(byte)195,(byte)28,(byte)179,(byte)211,(byte)93,(byte)183,(byte)227,(byte)158,(byte)187,(byte)243,(byte)223,(byte)191};
		ByteBuffer out = Encoding.BASE64.decode(ByteBuffer.wrap(input));
		assertEquals(out, ByteBuffer.wrap((expected)));
	}
	
}
