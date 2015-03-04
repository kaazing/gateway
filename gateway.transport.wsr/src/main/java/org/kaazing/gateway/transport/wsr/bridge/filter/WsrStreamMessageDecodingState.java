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

import static org.kaazing.gateway.transport.wsr.util.Amf0Utils.decodeString;

import java.util.Arrays;
import java.util.Collection;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.gateway.transport.wsr.RtmpAckMessage;
import org.kaazing.gateway.transport.wsr.RtmpBinaryDataMessage;
import org.kaazing.gateway.transport.wsr.RtmpCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpConnectCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpConnectResponseCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpCreateStreamCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpCreateStreamResultCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpDataMessage;
import org.kaazing.gateway.transport.wsr.RtmpDeleteStreamCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpPingRequestMessage;
import org.kaazing.gateway.transport.wsr.RtmpPingResponseMessage;
import org.kaazing.gateway.transport.wsr.RtmpPlayCommandMessage;
import org.kaazing.gateway.transport.wsr.RtmpSetBufferLengthMessage;
import org.kaazing.gateway.transport.wsr.RtmpSetChunkSizeMessage;
import org.kaazing.gateway.transport.wsr.RtmpSetPeerBandwidthMessage;
import org.kaazing.gateway.transport.wsr.RtmpSetPeerBandwidthMessage.LimitType;
import org.kaazing.gateway.transport.wsr.RtmpStreamBeginMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamDryMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamEofMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamMessage.StreamKind;
import org.kaazing.gateway.transport.wsr.RtmpStreamRecordedMessage;
import org.kaazing.gateway.transport.wsr.RtmpUserControlMessage;
import org.kaazing.gateway.transport.wsr.RtmpUserControlMessage.UserControlKind;
import org.kaazing.gateway.transport.wsr.RtmpWindowAcknowledgmentSizeMessage;
import org.kaazing.gateway.transport.wsr.util.Amf0Utils;
import org.kaazing.gateway.transport.wsr.util.Amf0Utils.Type;
import org.kaazing.gateway.transport.wsr.util.Amf3Utils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public abstract class WsrStreamMessageDecodingState implements DecodingState {

	private int timestamp;
	private int messageLength;
	private StreamKind streamKind;
	private int messageStreamId;
	
	private final IoBufferAllocatorEx<?> allocator;
	private final int chunkStreamId;

	private IoBufferEx buffer;
	private static final Collection<Object> ALLOWED_AMF0_NAMES = Arrays.<Object>asList("|RtmpSampleAccess", "onStatus", "onMetaData");

	public WsrStreamMessageDecodingState(IoBufferAllocatorEx<?> allocator, int chunkStreamId) {
	    this.allocator = allocator;
		this.chunkStreamId = chunkStreamId;
	}

	public int getMessageRemaining() {
		return (buffer != null) ? messageLength - buffer.position()
				: messageLength;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setMessageLength(int messageLength) {
		this.messageLength = messageLength;
	}

	public int getMessageLength() {
		return messageLength;
	}

	public void setMessageStreamId(int messageStreamId) {
		this.messageStreamId = messageStreamId;
	}

	public int getMessageStreamId() {
		return messageStreamId;
	}

	public void setStreamKind(StreamKind streamKind) {
		this.streamKind = streamKind;
	}

	public StreamKind getStreamKind() {
		return streamKind;
	}

	@Override
	public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
			throws Exception {
		if (buffer == null) {
			if (in.remaining() < messageLength) {
				buffer = allocator.wrap(allocator.allocate(messageLength));
				buffer.put((IoBufferEx) in);
				return this;
			}

			if (messageLength < in.remaining()) {
				int limit = in.limit();
				in.limit(in.position() + messageLength);
				DecodingState state = decodeStreamMessage((IoBufferEx) in, out);
				in.limit(limit);
				return state;
			} else {
				return decodeStreamMessage((IoBufferEx) in, out);
			}

		} else {
			if (buffer.remaining() < in.remaining()) {
				int limit = in.limit();
				in.limit(in.position() + buffer.remaining());
				buffer.put((IoBufferEx) in);
				in.limit(limit);
			} else {
				buffer.put((IoBufferEx) in);
			}

			if (buffer.position() < messageLength) {
				return this;
			}

			buffer.flip();
			IoBufferEx buf = buffer;
			buffer = null;

			return decodeStreamMessage(buf, out);
		}
	}

	private DecodingState decodeStreamMessage(IoBufferEx in,
			ProtocolDecoderOutput out) throws Exception {

		switch (streamKind) {
		case SET_CHUNK_SIZE:
			RtmpSetChunkSizeMessage rtmpSetChunkSize = new RtmpSetChunkSizeMessage();
			int chunkSize = in.getInt();
			rtmpSetChunkSize.setChunkSize(chunkSize);
			// this has to change the chunk size immediately
			return finishDecode(rtmpSetChunkSize, out);
		case ACKNOWLEDGMENT:
			RtmpAckMessage rtmpAck = new RtmpAckMessage();
			rtmpAck.setChunkStreamId(chunkStreamId);
			rtmpAck.setTimestamp(timestamp);
			rtmpAck.setMessageStreamId(messageStreamId);
			rtmpAck.setSequenceNumber(in.getUnsignedInt());
			return finishDecode(rtmpAck, out);
		case USER:
			RtmpUserControlMessage rtmpUserControl = decodeUserControlMessage(in);
			rtmpUserControl.setChunkStreamId(chunkStreamId);
			rtmpUserControl.setTimestamp(timestamp);
			rtmpUserControl.setMessageStreamId(messageStreamId);
			return finishDecode(rtmpUserControl, out);
		case WINDOW_ACKNOWLEDGMENT_SIZE:
			RtmpWindowAcknowledgmentSizeMessage rtmpWindowAckSize = new RtmpWindowAcknowledgmentSizeMessage();
			rtmpWindowAckSize.setChunkStreamId(chunkStreamId);
			rtmpWindowAckSize.setTimestamp(timestamp);
			rtmpWindowAckSize.setMessageStreamId(messageStreamId);
			rtmpWindowAckSize.setWindowSize(in.getUnsignedInt());
			return finishDecode(rtmpWindowAckSize, out);
		case SET_PEER_BANDWIDTH:
			RtmpSetPeerBandwidthMessage rtmpSetPeerBandwidth = new RtmpSetPeerBandwidthMessage();
			rtmpSetPeerBandwidth.setChunkStreamId(chunkStreamId);
			rtmpSetPeerBandwidth.setTimestamp(timestamp);
			rtmpSetPeerBandwidth.setMessageStreamId(messageStreamId);
			rtmpSetPeerBandwidth.setWindowSize(in.getUnsignedInt());
			rtmpSetPeerBandwidth.setLimitType(LimitType.values()[in.get()]);
			return finishDecode(rtmpSetPeerBandwidth, out);
		case DATA_AMF3: {
			// throw one byte away
			in.skip(1);

			// skip over stream name
			Amf0Utils.skipType(in);

			// TODO assert that amf3 and byte array types follow
			in.skip(2);
			
			IoBufferEx bytes = Amf3Utils.decodeByteArray(in);
			
			RtmpDataMessage rtmpData = new RtmpBinaryDataMessage(bytes);

			rtmpData.setChunkStreamId(chunkStreamId);
			rtmpData.setTimestamp(timestamp);
			rtmpData.setMessageStreamId(messageStreamId);
			return finishDecode(rtmpData, out);
		}
		case DATA_AMF0: {
			Type nameType = Amf0Utils.decodeTypeMarker(in);
			String name = Amf0Utils.decodeString(in).toString();
			if (!ALLOWED_AMF0_NAMES.contains(name)) {
				// DATA_AMF0 messages are forbidden by the WSR spec
				throw new ProtocolDecoderException("Unexpected message type: "
						+ streamKind);
			}
			return this;
		}
		case COMMAND_AMF3:
			in.skip(1);
			// fall-through
		case COMMAND_AMF0: {
			Type nameType = Amf0Utils.decodeTypeMarker(in);
			String name = Amf0Utils.decodeString(in).toString();
			
			RtmpCommandMessage command = null;
			if (name.equals(RtmpConnectCommandMessage.CONNECT_NAME)) {
				RtmpConnectCommandMessage connectCommand = new RtmpConnectCommandMessage();
				connectCommand.setTransactionId(Amf0Utils.getNumber(in));

		    	Type type = Amf0Utils.Type.values()[in.get()];
		        if (type != Type.OBJECT) {
		        	throw new Exception("Unxpected Type");
		        }
				
				CharSequence key;
				while (in.hasRemaining()) {
					key = Amf0Utils.decodeString(in);
					
					if ("swfUrl".equals(key)) {
						Amf0Utils.Type swfUrlType = Amf0Utils.Type.values()[in.get()];
                        switch (swfUrlType) {
                        case NULL:
                        case UNDEFINED:
                            connectCommand.setSwfUrl(null);
                            break;
                        case STRING:
                            connectCommand.setSwfUrl(decodeString(in));
                            break;
                        default:
                            throw new Exception("Unxpected Type: " + swfUrlType);
                        }
					} else if ("app".equals(key)) {
						in.skip(1);
						connectCommand.setApp(decodeString(in));
					} else if ("flashVer".equals(key)) {
						in.skip(1);
						connectCommand.setFlashVer(decodeString(in));
					} else if ("tcUrl".equals(key)) {
						Amf0Utils.Type tcType = Amf0Utils.Type.values()[in.get()];
                        switch (tcType) {
                        case NULL:
                        case UNDEFINED:
                            connectCommand.setTcUrl(null);
                            break;
                        case STRING:
                            connectCommand.setTcUrl(decodeString(in));
                            break;
                        default:
                            throw new Exception("Unxpected Type: " + tcType);
                        }
					} else {
						// unknown key
						boolean value = Amf0Utils.skipType(in);
						// break on object end marker
						if (!value) {
							break;
						}
					}
				}
				command = connectCommand;
			}
			else if (name.equals("_result")) {
				// result arguments
				double txId = Amf0Utils.getNumber(in);
				// ignored positional argument
				Amf0Utils.skipType(in);
				if (txId == 0) {
			    	Amf0Utils.Type type = Amf0Utils.Type.values()[in.get()];
			        if (type != Type.OBJECT) {
			        	throw new Exception("Unxpected Type");
			        }					
					// results have an event code that determines what kind of result
					String code = null;					
					String key;
					while (in.hasRemaining()) {
						key = Amf0Utils.decodeString(in);
						
						if ("code".equals(key)) {
							in.skip(1);
							code = decodeString(in);
						} else {
							// unknown key
							boolean value = Amf0Utils.skipType(in);
							// break on object end marker
							if (!value) {
								break;
							}
						}
					}
					
					if (RtmpConnectResponseCommandMessage.CODE.equals(code)) {
						RtmpConnectResponseCommandMessage connectCommandResult = new RtmpConnectResponseCommandMessage();
						command = connectCommandResult; 
					} else {
						throw new ProtocolDecoderException("Unknown result code: " + code);
					}
				} else {
					RtmpCreateStreamResultCommandMessage createCommandResult = new RtmpCreateStreamResultCommandMessage();
					createCommandResult.setStreamId(Amf0Utils.getNumber(in));
					command = createCommandResult;
				}
			}
			else if (name.equals(RtmpCreateStreamCommandMessage.CREATE_STREAM_NAME)) {
				RtmpCreateStreamCommandMessage createCommand = new RtmpCreateStreamCommandMessage();
				createCommand.setTransactionId(Amf0Utils.getNumber(in));
				// ignore next positional argument
				Amf0Utils.skipType(in);
				
				command = createCommand;
			}
            else if(name.equals(RtmpDeleteStreamCommandMessage.DELETE_STREAM_NAME)) {
                RtmpDeleteStreamCommandMessage deleteCommand = new RtmpDeleteStreamCommandMessage();
                deleteCommand.setTransactionId(Amf0Utils.getNumber(in));
                // ignore next two positional arguments
                Amf0Utils.skipType(in);
                Amf0Utils.skipType(in);
                
                command = deleteCommand;
            }
			else if (name.equals(RtmpPlayCommandMessage.PLAY_NAME)) {
				RtmpPlayCommandMessage playCommand = new RtmpPlayCommandMessage();
				playCommand.setTransactionId(Amf0Utils.getNumber(in));
				// ignore two positional arguments
				Amf0Utils.skipType(in);
				Amf0Utils.skipType(in);
				
				command = playCommand;
			}
			else if (name.equals("publish")) {
				RtmpPublishCommandMessage publishCommand = new RtmpPublishCommandMessage();
				publishCommand.setTransactionId(Amf0Utils.getNumber(in));
				
				// ignore all positional arguments
				Amf0Utils.skipType(in);
				Amf0Utils.skipType(in);
				
				command = publishCommand;
			}
			else if (name.equals("onStatus")) {
				return this;
			}
			else {
				throw new ProtocolDecoderException("Unexpected command name : "
						+ name);
			}
			
			command.setChunkStreamId(chunkStreamId);
			command.setTimestamp(timestamp);
			command.setMessageStreamId(messageStreamId);
			return finishDecode(command, out);
		}
		default:
			throw new ProtocolDecoderException("Unexpected message type: "
					+ streamKind);
		}
	}

	private RtmpUserControlMessage decodeUserControlMessage(IoBufferEx in)
			throws Exception {
		UserControlKind userControlKind = UserControlKind.values()[in
				.getShort()];

		switch (userControlKind) {
		case STREAM_BEGIN:
			RtmpStreamBeginMessage streamBeginMessage = new RtmpStreamBeginMessage();
			streamBeginMessage.setStreamId(in.getInt());
			return streamBeginMessage;
		case STREAM_EOF:
			RtmpStreamEofMessage streamEofMessage = new RtmpStreamEofMessage();
			streamEofMessage.setStreamId(in.getInt());
			return streamEofMessage;
		case STREAM_DRY:
			RtmpStreamDryMessage streamDryMessage = new RtmpStreamDryMessage();
			streamDryMessage.setStreamId(in.getInt());
			return streamDryMessage;
		case SET_BUFFER_LENGTH:
			RtmpSetBufferLengthMessage setBufferLengthMessage = new RtmpSetBufferLengthMessage();
			setBufferLengthMessage.setStreamId(in.getInt());
			setBufferLengthMessage.setBufferLength(in.getInt());
			return setBufferLengthMessage;
		case STREAM_RECORDED:
			RtmpStreamRecordedMessage streamRecordedMessage = new RtmpStreamRecordedMessage();
			streamRecordedMessage.setStreamId(in.getInt());
			return streamRecordedMessage;
		case PING_REQUEST:
			RtmpPingRequestMessage pingRequestMessage = new RtmpPingRequestMessage();
			pingRequestMessage.setTimestamp(in.getInt());
			return pingRequestMessage;
		case PING_RESPONSE:
			RtmpPingResponseMessage pingResponseMessage = new RtmpPingResponseMessage();
			pingResponseMessage.setTimestamp(in.getInt());
			return pingResponseMessage;
		default:
			throw new ProtocolDecoderException(
					"Unexpected User Control Message Kind");
		}
	}

	protected abstract DecodingState finishDecode(RtmpStreamMessage message,
			ProtocolDecoderOutput out);

	@Override
	public DecodingState finishDecode(ProtocolDecoderOutput out)
			throws Exception {
		return null;
	}
}
