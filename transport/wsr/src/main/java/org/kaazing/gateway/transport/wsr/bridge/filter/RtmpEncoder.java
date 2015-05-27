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

package org.kaazing.gateway.transport.wsr.bridge.filter;

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;
import static org.kaazing.gateway.transport.wsr.RtmpHandshakeMessage.NONCE_LENGTH;
import static org.kaazing.gateway.transport.wsr.util.Amf0Utils.encodeNull;
import static org.kaazing.gateway.transport.wsr.util.Amf0Utils.encodeNumber;
import static org.kaazing.gateway.transport.wsr.util.Amf0Utils.encodeObjectEnd;
import static org.kaazing.gateway.transport.wsr.util.Amf0Utils.encodeObjectStart;
import static org.kaazing.gateway.transport.wsr.util.Amf0Utils.encodeString;
import static org.kaazing.gateway.transport.wsr.util.Amf0Utils.encodeStringTypeless;

import java.nio.ByteBuffer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.wsr.RtmpBinaryDataMessage;
import org.kaazing.gateway.transport.wsr.RtmpCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpConnectCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpConnectResponseCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpCreateStreamResultCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpDataMessage;
import org.kaazing.gateway.transport.wsr.RtmpHandshakeMessage;
import org.kaazing.gateway.transport.wsr.RtmpMessage;
import org.kaazing.gateway.transport.wsr.RtmpPingRequestMessage;
import org.kaazing.gateway.transport.wsr.RtmpPingResponseMessage;
import org.kaazing.gateway.transport.wsr.RtmpPlayResponseCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpSetChunkSizeMessage;
import org.kaazing.gateway.transport.wsr.RtmpSetPeerBandwidthMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamBeginMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamEofMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamMessage;
import org.kaazing.gateway.transport.wsr.RtmpUserControlMessage;
import org.kaazing.gateway.transport.wsr.RtmpUserControlMessage.UserControlKind;
import org.kaazing.gateway.transport.wsr.RtmpWindowAcknowledgmentSizeMessage;
import org.kaazing.gateway.transport.wsr.util.Amf0Utils;
import org.kaazing.gateway.transport.wsr.util.Amf0Utils.Type;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class RtmpEncoder extends ProtocolEncoderAdapter {

	public static final int MAXIMUM_CHUNK_SIZE = RtmpEncodingSupport.MAXIMUM_CHUNK_SIZE;

	private static final byte[] DOWNSTREAM_MESSAGE_HEADER = {0x00, Type.STRING.getCode(), 0x00, 0x01, 0x64}; // "d"
	private static final int DOWNSTREAM_CHUNKSTREAM_ID = 8;
	private final IoBufferAllocatorEx<?> allocator;

	// TODO variable size chunks
	private final int maximumChunkSize = MAXIMUM_CHUNK_SIZE;

    private final CachingMessageEncoder cachingEncoder;

	public RtmpEncoder(IoBufferAllocatorEx<?> allocator) {
	    this(IO_MESSAGE_ENCODER, allocator);
	}

    public RtmpEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        this.cachingEncoder = cachingEncoder;
		this.allocator = allocator;
	}

    @Override
	public void encode(IoSession session, Object message,
			ProtocolEncoderOutput out) throws Exception {
		RtmpMessage rtmpMessage = (RtmpMessage) message;
		switch (rtmpMessage.getKind()) {
		case VERSION:
			out.write(allocator.wrap(ByteBuffer.wrap(new byte[] { (byte) 0x03 } )));
			break;
		case HANDSHAKE_REQUEST:
		case HANDSHAKE_RESPONSE:
			doEncodeHandshake(session, (RtmpHandshakeMessage) message, out);
			break;
		case STREAM:
			RtmpStreamMessage rtmpStream = (RtmpStreamMessage) message;
			switch (rtmpStream.getStreamKind()) {
			case SET_CHUNK_SIZE:
				doEncodeSetChunkSize(session, (RtmpSetChunkSizeMessage)message, out);
				break;
			case USER:
				doEncodeUser(session, (RtmpUserControlMessage) message, out);
				break;
			case WINDOW_ACKNOWLEDGMENT_SIZE:
				doEncodeWindowAcknowledgmentSize(session,
						(RtmpWindowAcknowledgmentSizeMessage) message, out);
				break;
			case SET_PEER_BANDWIDTH:
				doEncodeSetPeerBandwidth(session,
						(RtmpSetPeerBandwidthMessage) message, out);
				break;
			case DATA_AMF0:
				doEncodeAmf0Data(session, (RtmpDataMessage) message, out);
				break;
			case DATA_AMF3:
				doEncodeData(session, (RtmpDataMessage) message, out);
				break;
			case COMMAND_AMF0:
				doEncodeCommand(session, (RtmpCommandMessage) message, out);
				break;
			default:
				throw new IllegalArgumentException(
						"Unrecognized stream message kind: "
								+ rtmpStream.getStreamKind());
			}
			break;
		default:
			throw new IllegalArgumentException("Unrecognized message kind: "
					+ rtmpMessage.getKind());
		}
	}

	private void doEncodeWindowAcknowledgmentSize(IoSession session,
			RtmpWindowAcknowledgmentSizeMessage message,
			ProtocolEncoderOutput out) {
		IoBufferEx buf = allocator.wrap(allocator.allocate(1000));
		buf.setAutoExpander(allocator);
		buf.putInt((int) message.getWindowSize());
		buf.flip();
		out.write(doEncodeChunk0(message.getChunkStreamId(), buf, message));
	}

	private void doEncodeSetPeerBandwidth(IoSession session,
			RtmpSetPeerBandwidthMessage message, ProtocolEncoderOutput out) {
		IoBufferEx buf = allocator.wrap(allocator.allocate(1000));
		buf.setAutoExpander(allocator);
		buf.putInt((int) message.getWindowSize());
		buf.put((byte) message.getLimitType().ordinal());
		buf.flip();
		out.write(doEncodeChunk0(message.getChunkStreamId(), buf, message));
	}

	private void doEncodeSetChunkSize(IoSession session,
			RtmpSetChunkSizeMessage message, ProtocolEncoderOutput out) {

		int chunkStreamId = message.getChunkStreamId();
		int chunkHeaderLength = 12;
		int messageStreamLength = 4;

		IoBufferEx buf = allocator.wrap(allocator.allocate(chunkHeaderLength + messageStreamLength));
        int allocatedPos = buf.position();
		doEncodeChunk0Header(chunkStreamId, messageStreamLength, message, buf);
		buf.putInt(MAXIMUM_CHUNK_SIZE);
		buf.flip();
		buf.position(allocatedPos);

		session.write(buf);
	}

	private void doEncodeUser(IoSession session,
			RtmpUserControlMessage message, ProtocolEncoderOutput out) {
		IoBufferEx buf = allocator.wrap(allocator.allocate(1000));
		buf.setAutoExpander(allocator);

		UserControlKind kind = message.getUserControlKind();
		buf.putShort((short) kind.ordinal());
		switch (kind) {
		case STREAM_BEGIN:
			RtmpStreamBeginMessage streamBeginMessage = (RtmpStreamBeginMessage) message;
			buf.putInt(streamBeginMessage.getStreamId());
			break;
		case STREAM_EOF:
			RtmpStreamEofMessage streamEofMessage = (RtmpStreamEofMessage) message;
			buf.putInt(streamEofMessage.getStreamId());
			break;
		case PING_REQUEST:
			RtmpPingRequestMessage pingRequestMessage = (RtmpPingRequestMessage) message;
			buf.putInt(pingRequestMessage.getTimestamp());
			break;
		case PING_RESPONSE:
			RtmpPingResponseMessage pingResponseMessage = (RtmpPingResponseMessage) message;
			buf.putInt(pingResponseMessage.getTimestamp());
			break;
		}

		buf.flip();
		out.write(doEncodeChunk0(message.getChunkStreamId(), buf, message));
	}

	private void doEncodeAmf0Data(IoSession session, RtmpDataMessage message,
			ProtocolEncoderOutput out) {
		IoBufferEx buf = allocator.wrap(allocator.allocate(1000));
		buf.setAutoExpander(allocator);

		// positional arguments
		encodeString(buf, message.getName());
		// optional arguments
		// TODO AMF0 encoding

		buf.flip();
		out.write(doEncodeChunk0(message.getChunkStreamId(), buf, message));
	}

    private static byte[] EMPTY_BYTES = new byte[0];






	private void doEncodeData(IoSession session, RtmpDataMessage message,
			ProtocolEncoderOutput out) {

		switch (message.getDataKind()) {
		case BINARY: {
			RtmpBinaryDataMessage binaryMessage = (RtmpBinaryDataMessage)message;
			IoBufferEx buf = binaryMessage.getBytes();
            out.write(RtmpEncodingSupport.doBinaryEncode(allocator, buf, message, maximumChunkSize));
			break;
		}
		case SAMPLE_ACCESS: {
			IoBufferEx buf = allocator.wrap(allocator.allocate(100));
			// FIXME use shared byte array
			Amf0Utils.encodeBoolean(buf, false);
			Amf0Utils.encodeBoolean(buf, false);
			buf.flip();
			out.write(doEncodeChunk0(DOWNSTREAM_CHUNKSTREAM_ID, buf, message));
			break;
		}
		}

	}




    private void doEncodeHandshake(IoSession session,
			RtmpHandshakeMessage message, ProtocolEncoderOutput out) {
		IoBufferEx buf = allocator.wrap(allocator.allocate(8 + NONCE_LENGTH));
		buf.putInt(message.getTimestamp1());
		buf.putInt(message.getTimestamp2());
		buf.put(message.getNonce());
		buf.flip();
		out.write(buf);
	}

	private void doEncodeCommand(IoSession session, RtmpCommandMessage command,
			ProtocolEncoderOutput out) throws Exception {
		IoBufferEx buf = allocator.wrap(allocator.allocate(1000));
		buf.setAutoExpander(allocator);

		encodeString(buf, command.getName());
		encodeNumber(buf, command.getTransactionId());

		switch (command.getCommandKind()) {
		case CONNECT:
			RtmpConnectCommandMessage connectCommand = (RtmpConnectCommandMessage) command;

			encodeObjectStart(buf);
			encodeStringTypeless(buf, "swfUrl");
			encodeString(buf, connectCommand.getSwfUrl());
			encodeStringTypeless(buf, "tcUrl");
			encodeString(buf, connectCommand.getTcUrl());

			encodeObjectEnd(buf);
			break;
		case CONNECT_RESULT:
			RtmpConnectResponseCommandMessage connectResult = (RtmpConnectResponseCommandMessage) command;

			// ignored positional argument
			encodeNull(buf);

			encodeObjectStart(buf);
			encodeStringTypeless(buf, "level");
			encodeString(buf, RtmpConnectResponseCommandMessage.LEVEL);
			encodeStringTypeless(buf, "code");
			encodeString(buf, RtmpConnectResponseCommandMessage.CODE);
			encodeStringTypeless(buf, "description");
			encodeString(buf, RtmpConnectResponseCommandMessage.DESCRIPTION);
			encodeStringTypeless(buf, "details");
			Amf0Utils.encodeNull(buf);
			encodeStringTypeless(buf, "objectEncoding");
			encodeNumber(buf, RtmpConnectResponseCommandMessage.OBJECT_ENCODING);
			encodeObjectEnd(buf);
			break;
		case CREATE_STREAM_RESULT:
			RtmpCreateStreamResultCommandMessage createResult = (RtmpCreateStreamResultCommandMessage) command;
			// ignored positional argument
			encodeNull(buf);
			Amf0Utils.encodeNumber(buf, createResult.getStreamId());
			break;
		case PLAY_RESONSE:
			RtmpPlayResponseCommandMessage playResponse = (RtmpPlayResponseCommandMessage) command;
			// ignored positional argument
			encodeNull(buf);

			encodeObjectStart(buf);
			encodeStringTypeless(buf, "level");
			encodeString(buf, RtmpPlayResponseCommandMessage.LEVEL);
			encodeStringTypeless(buf, "code");
			encodeString(buf, RtmpPlayResponseCommandMessage.CODE);
			encodeStringTypeless(buf, "description");
			encodeString(buf, RtmpPlayResponseCommandMessage.DESCRIPTION);
			encodeStringTypeless(buf, "details");
			Amf0Utils.encodeNull(buf);
			encodeObjectEnd(buf);
			break;
		case CREATE_STREAM:
			// RtmpCreateStreamCommandMessage createStream = (RtmpCreateStreamCommandMessage) command;
			// ignored positional argument
			encodeNull(buf);
			break;
		case PLAY:
			// two positional arguments
			encodeNull(buf);
			encodeNull(buf);
			break;
		case PUBLISH:
			// two positional arguments
			encodeNull(buf);
			encodeNull(buf);
			break;
		case PUBLISH_RESPONSE:
			break;
		default:
			throw new Exception("Unexpected command message: " + command);
		}


		buf.flip();
		out.write(doEncodeChunk0(command.getChunkStreamId(), buf, command));
	}

	private void encodeConnectComand(IoSession session,
			RtmpConnectCommandMessage connectCommand, ProtocolDecoderOutput out)
			throws Exception {
	}

	private IoBufferEx doEncodeChunk0(int chunkStreamId, IoBufferEx messageBuffer,
			RtmpStreamMessage message) {
		IoBufferEx buf = allocator.wrap(allocator.allocate(1000));
		buf.setAutoExpander(allocator);

		int streamMessageLength = messageBuffer.remaining();
		doEncodeChunk0Header(chunkStreamId, streamMessageLength, message, buf);

		// write message

		// chunk every "maximum-chunk-length" bytes
		while (messageBuffer.remaining() > 0) {
			if (messageBuffer.remaining() <= maximumChunkSize) {
				buf.put(messageBuffer);
			} else {
				IoBufferEx part = messageBuffer.getSlice(maximumChunkSize);
				buf.put(part);
				doEncodeChunk3Format(chunkStreamId, buf);
			}
		}

		buf.flip();
		return buf;
	}

	private static void doEncodeChunk0Header(int chunkStreamId, int streamMessageLength,
			RtmpStreamMessage message, IoBufferEx buf) {
		RtmpEncodingSupport.doEncodeChunk0Header(chunkStreamId, streamMessageLength, message, buf);
	}

	private static void doEncodeChunk3Format(int chunkStreamId, IoBufferEx buf) {
		RtmpEncodingSupport.doEncodeChunk3Format(chunkStreamId, buf);
	}

}
