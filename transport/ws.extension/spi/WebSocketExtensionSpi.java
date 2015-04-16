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

import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.extension.function.WebSocketConsumer;
import org.kaazing.gateway.transport.ws.extension.function.WebSocketFrameConsumer;

/**
 * {@link WebSocketExtensionSpi} is part of <i>Service Provider Interface</i> <em>(SPI)</em> for extension developers.
 * <p>
 * Developing an extension involves implementing:
 * <UL>
 *   <LI> a sub-class of {@link WebSocketExtensionFactorySpi}
 *   <LI> a sub-class of {@link WebSocketExtensionSpi}
 * </UL>
 * <p>
 * When an enabled extension is successfully negotiated, an instance of this class is created using the corresponding
 * {@link WebSocketExtensionFactorySpi} that is registered through META-INF/services. This class is used to instantiate the
 * hooks that can be exercised as the state machine transitions from one state to another while
 * handling the WebSocket traffic. Based on the functionality of the extension, the extension developer can decide which
 * hooks to code.
 */
public abstract class WebSocketExtensionSpi {

    /**
     * onInitialized hook is exercised when an extension is successfully negotiated.
     */
    public WebSocketConsumer onInitialized = new WebSocketConsumer() {

        @Override
        public void accept(WebSocketContext context) {
            return;
        }
    };

    /**
     * onError hook is exercised in case of an error.
     */
    public WebSocketConsumer onError = new WebSocketConsumer() {

        @Override
        public void accept(WebSocketContext context) {
            return;
        }
    };

    /**
     * onBinaryReceived hook is exercised when a BINARY frame is received.
     */
    public WebSocketFrameConsumer onBinaryReceived = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onBinaryReceived(frame);
        }
    };

    /**
     * onBinarySent hook is exercised when sending a BINARY frame.
     */
    public WebSocketFrameConsumer onBinarySent = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onBinarySent(frame);
        }
    };

    /**
     * onContinuationReceived hook is exercised when a CONTINUATION frame is being received.
     */
    public WebSocketFrameConsumer onContinuationReceived = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onContinuationReceived(frame);
        }
    };

    /**
     * onContinuationSent hook is exercised when sending a CONTINUATION frame.
     */
    public WebSocketFrameConsumer onContinuationSent = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onContinuationSent(frame);
        }
    };

    /**
     * onCloseReceived hook is exercised when a CLOSE frame is received.
     */
    public WebSocketFrameConsumer onCloseReceived = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onCloseReceived(frame);
        }
    };

    /**
     * onCloseSent hook is exercised when sending a CLOSE frame.
     */
    public WebSocketFrameConsumer onCloseSent = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onCloseSent(frame);
        }
    };

    /**
     * onPingReceived hook is exercised when a PING frame is received.
     */
    public WebSocketFrameConsumer onPingReceived = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onPingReceived(frame);
        }
    };

    /**
     * onPongReceived hook is exercised when a PONG frame is received.
     */
    public WebSocketFrameConsumer onPongReceived = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onPongReceived(frame);
        }
    };

    /**
     * onPongSent hook is exercised when sending a PONG frame.
     */
    public WebSocketFrameConsumer onPongSent = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onPongSent(frame);
        }
    };

    /**
     * onTextReceived hook is exercised when a TEXT frame is received.
     */
    public WebSocketFrameConsumer onTextReceived = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onTextReceived(frame);
        }
    };

    /**
     * onTextSent hook is exercised when sending a TEXT frame.
     */
    public WebSocketFrameConsumer onTextSent = new WebSocketFrameConsumer() {

        @Override
        public void accept(WebSocketContext context, Frame frame) throws IOException {
            context.onTextSent(frame);
        }
    };
}
