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
package org.kaazing.gateway.service.turn.proxy.stun;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.AbstractAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorPeerAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorRelayAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StunMaskAddressFilter extends IoFilterAdapter {

    static final Logger LOGGER = LoggerFactory.getLogger(StunMaskAddressFilter.class);

    private final byte[] ipv4Mask;
    private final byte[] ipv6Mask;
    private final int portMask;
    private final Orientation orientation;
    private final Orientation.AddressVisitor maskVisitor = new MaskVisitor();

    public enum Orientation {
        INCOMING {
            @Override
            public void visitAddress(StunProxyMessage stunProxyMessage, AddressVisitor visitor) {
                LOGGER.debug("INCOMING stun proxy message, unmasking XOR-PEER-ADDRESS");
                stunProxyMessage.getAttributes()
                    .stream()
                    .filter(attribute -> attribute instanceof XorPeerAddress)
                    .forEach(attribute -> {
                        visitor.visit((AbstractAddress) attribute);
                        stunProxyMessage.setModified(true);
                    });
            }
        },
        OUTGOING {
            @Override
            public void visitAddress(StunProxyMessage stunProxyMessage, AddressVisitor visitor) {
                LOGGER.debug("OUTGOING stun proxy message, masking XOR-RELAY-ADDRESS");
                stunProxyMessage.getAttributes()
                    .stream()
                    .filter(attribute -> attribute instanceof XorRelayAddress)
                    .forEach(attribute -> {
                        visitor.visit((AbstractAddress) attribute);
                        stunProxyMessage.setModified(true);
                    });
            }
        };

        public abstract void visitAddress(StunProxyMessage stunProxyMessage, AddressVisitor visitor);

        @FunctionalInterface
        public interface AddressVisitor {
            void visit(AbstractAddress address);
        }
    }

    public StunMaskAddressFilter(Long mask, Orientation orientation) {
        ipv4Mask = new byte[4];
        long maskKey = mask;
        for (int i = 3; i >= 0; i--) {
            ipv4Mask[i] = (byte)(maskKey & 0xFF);
            maskKey >>= 8;
        }
        ipv6Mask = new byte[16];
        for (byte i = 0; i < 4; i++) {
            System.arraycopy(ipv4Mask, 0, ipv6Mask, i * ipv4Mask.length, ipv4Mask.length);
        }
        portMask = (int) (mask & 0xFFFF);
        this.orientation = orientation;
        if (LOGGER.isDebugEnabled()) {
            logMasks();
        }
    }

    private void logMasks() {
        StringBuilder sb = new StringBuilder();
        for (byte b : ipv4Mask) {
            sb.append(String.format("%02X ", b));
        }
        String ipv4MaskStr = sb.toString();
        sb = new StringBuilder();
        for (byte b : ipv6Mask) {
            sb.append(String.format("%02X ", b));
        }
        String ipv6MaskStr = sb.toString();
        LOGGER.debug(
            String.format("Initialized filter with ipv4Mask: %s, ipv6Mask: %s, portMask: %02X %02X",
                ipv4MaskStr,
                ipv6MaskStr,
                portMask & 0xFF,
                (portMask >> 8 ) & 0xFF
            )
        );
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (message instanceof StunProxyMessage) {
            orientation.visitAddress((StunProxyMessage) message, maskVisitor);
        }
        super.messageReceived(nextFilter, session, message);
    }

    private class MaskVisitor implements Orientation.AddressVisitor {

        @Override
        public void visit(AbstractAddress address) {
            LOGGER.debug("Address before masking is: " + address);
            address.setPort((short) (address.getPort() ^ portMask));
            byte[] mask = ipv4Mask;
            if (address.getFamily().equals(AbstractAddress.Family.IPV6)) {
                mask = ipv6Mask;
            }
            byte[] tmp = new byte[mask.length];
            byte[] add = address.getAddress();
            for (int i = 0; i < mask.length; i++) {
                tmp[i] = (byte) (add[i] ^ mask[i]);
            }
            address.setAddress(tmp);
            LOGGER.debug("Address after masking is: " + address);
        }
    }
}
