/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.transport.ws.extension;

import java.io.IOException;

import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.WsMessage;

/**
 * WebSocketContext provides the extensions the ability to participate in the message flow in both directions. WebSocketContext
 * exercises the registered hooks of all the negotiated extensions. It also provides extensions the ability to send WebSocket
 * frames from the registered hooks.
 *
 */
public abstract class WebSocketContext {
    protected final WsURLConnectionImpl connection;
    private String errorMessage;

    public WebSocketContext(WsURLConnectionImpl connection) {
        this.connection = connection;
    }

    public abstract WebSocketExtensionSpi nextExtension();

    public String getErrorMessage() {
        return errorMessage;
    }

    public void onError(String message) throws IOException {
        this.errorMessage = message;
        nextExtension().onError.accept(this);
    }

    /**
     * Exercises the <code>onBinaryReceived</code> hook of a negotiated extension.
     *
     * @param frame incoming BINARY frame
     * @throws IOException
     */
    public void onBinaryReceived(Frame frame) throws IOException {
        nextExtension().onBinaryReceived.accept(this, frame);
    }

    /**
     * Exercises the <code>onCloseReceived</code> hook of a negotiated extension.
     *
     * @param frame incoming CLOSE frame
     * @throws IOException
     */
    public void onCloseReceived(Frame frame) throws IOException {
        nextExtension().onCloseReceived.accept(this, frame);
    }

    /**
     * Exercises the <code>onContinuationReceived</code> hook of a negotiated extension.
     *
     * @param frame incoming CONTINUATION frame
     * @throws IOException
     */
    public void onContinuationReceived(Frame frame) throws IOException {
        nextExtension().onContinuationReceived.accept(this, frame);
    }

    /**
     * Exercises the <code>onPingReceived</code> hook of a negotiated extension.
     *
     * @param frame incoming PING frame
     * @throws IOException
     */
    public void onPingReceived(Frame frame) throws IOException {
        nextExtension().onPingReceived.accept(this, frame);
    }

    /**
     * Exercises the <code>onPongReceived</code> hook of a negotiated extension.
     *
     * @param frame incoming PONG frame
     * @throws IOException
     */
    public void onPongReceived(Frame frame) throws IOException {
        nextExtension().onPongReceived.accept(this, frame);
    }

    /**
     * Exercises the <code>onTextReceived</code> hook of a negotiated extension.
     *
     * @param frame incoming TEXT frame
     * @throws IOException
     */
    public void onTextReceived(Frame frame) throws IOException {
        nextExtension().onTextReceived.accept(this, frame);
    }

    /**
     * Exercises the <code>onBinarySent</code> hook of a negotiated extension.
     *
     * @param frame outgoing BINARY frame
     * @throws IOException
     */
    public void onBinarySent(Frame frame) throws IOException {
        nextExtension().onBinarySent.accept(this, frame);
    }

    /**
     * Exercises the <code>onCloseSent</code> hook of a negotiated extension.
     *
     * @param frame outgoing CLOSE frame
     * @throws IOException
     */
    public void onCloseSent(Frame frame) throws IOException {
        nextExtension().onCloseSent.accept(this, frame);
    }

    /**
     * Exercises the <code>onContinuationSent</code> hook of a negotiated extension.
     *
     * @param frame outgoing CONTINUATION frame
     * @throws IOException
     */
    public void onContinuationSent(Frame frame) throws IOException {
        nextExtension().onContinuationSent.accept(this, frame);
    }

    /**
     * Exercises the <code>onPongSent</code> hook of a negotiated extension.
     *
     * @param frame outgoing PONG frame
     * @throws IOException
     */
    public void onPongSent(Frame frame) throws IOException {
        nextExtension().onPongSent.accept(this, frame);
    }

    /**
     * Exercises the <code>onTextSent</code> hook of a negotiated extension.
     *
     * @param frame outgoing TEXT frame
     * @throws IOException
     */
    public void onTextSent(Frame frame) throws IOException {
        nextExtension().onTextSent.accept(this, frame);
    }

    /**
     * Writes out a BINARY frame on the wire.
     *
     * @param frame outgoing BINARY frame
     * @throws IOException
     */
    public void doSendBinary(Frame dataFrame) throws IOException {
        WebSocketOutputStateMachine.instance().processFrame(connection, dataFrame);
    }

    /**
     * Writes out a CLOSE frame on the wire.
     *
     * @param frame outgoing CLOSE frame
     * @throws IOException
     */
    public void doSendClose(Frame closeFrame) throws IOException {
        WebSocketOutputStateMachine.instance().processFrame(connection, closeFrame);
    }

    /**
     * Writes out a CONTINUATION frame on the wire.
     *
     * @param frame outgoing CONTINUATION frame
     * @throws IOException
     */
    public void doSendContinuation(Frame dataFrame) throws IOException {
        WebSocketOutputStateMachine.instance().processFrame(connection, dataFrame);
    }

    /**
     * Writes out a PONG frame on the wire.
     *
     * @param frame outgoing PONG frame
     * @throws IOException
     */
    public void doSendPong(Frame pongFrame) throws IOException {
        WebSocketOutputStateMachine.instance().processFrame(connection, pongFrame);
    }

    /**
     * Writes out a TEXT frame on the wire.
     *
     * @param frame outgoing TEXT frame
     * @throws IOException
     */
    public void doSendText(Frame dataFrame) throws IOException {
        WebSocketOutputStateMachine.instance().processFrame(connection, dataFrame);
    }
}
