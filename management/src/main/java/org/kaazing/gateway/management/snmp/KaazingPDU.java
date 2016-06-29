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
package org.kaazing.gateway.management.snmp;

import java.io.IOException;
import java.util.Vector;
import org.snmp4j.PDU;
import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.VariableBinding;

/**
 * An override of the standard PDU class, so we can implement the non-standard 'GetSubtree' operation and its variants.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class KaazingPDU extends PDU {

    public static final int KAAZING_NOTIFICATION_SUBSCRIPTION = BER.ASN_CONTEXT | BER.ASN_CONSTRUCTOR | 0xA;
    public static final int GETSUBTREE = BER.ASN_CONTEXT | BER.ASN_CONSTRUCTOR | 0xB;

    public KaazingPDU() {
        super();
    }

    public KaazingPDU(PDU other) {
        super(other);
    }

    /**
     * Decode an incoming encoded request. The following is a modified copy of SNMP4J.PDU's version of this method to support our
     * methods.
     */
    @Override
    public void decodeBER(BERInputStream inputStream) throws IOException {
        BER.MutableByte pduType = new BER.MutableByte();
        int length = BER.decodeHeader(inputStream, pduType);
        int pduStartPos = (int) inputStream.getPosition();
        switch (pduType.getValue()) {
            case PDU.SET:
                break;
            case PDU.GET:
                break;
            case PDU.GETNEXT:
                break;
            case PDU.GETBULK:
                break;
            case PDU.INFORM:
                break;
            case PDU.REPORT:
                break;
            case PDU.TRAP:
                break;
            case PDU.RESPONSE:
                break;
            case KaazingPDU.GETSUBTREE:
                break;  // so I can break here specifically during debugging.
            case KaazingPDU.KAAZING_NOTIFICATION_SUBSCRIPTION:
                break;  // so I can break here specifically during debugging.
            default:
                throw new IOException("Unsupported PDU type: " + pduType.getValue());
        }

        this.type = pduType.getValue();
        if (length == 0) {
            return;
        }

        requestID.decodeBER(inputStream);
        errorStatus.decodeBER(inputStream);
        errorIndex.decodeBER(inputStream);

        pduType = new BER.MutableByte();
        int vbLength = BER.decodeHeader(inputStream, pduType);
        if (pduType.getValue() != BER.SEQUENCE) {
            throw new IOException("Encountered invalid tag, SEQUENCE expected: " +
                    pduType.getValue());
        }
        // rest read count
        int startPos = (int) inputStream.getPosition();
        variableBindings = new Vector();
        while (inputStream.getPosition() - startPos < vbLength) {
            VariableBinding vb = new VariableBinding();
            vb.decodeBER(inputStream);
            variableBindings.add(vb);
        }
        if (inputStream.getPosition() - startPos != vbLength) {
            throw new IOException("Length of VB sequence (" + vbLength +
                    ") does not match real length: " +
                    ((int) inputStream.getPosition() - startPos));
        }
        if (BER.isCheckSequenceLength()) {
            BER.checkSequenceLength(length,
                    (int) inputStream.getPosition() - pduStartPos,
                    this);
        }
    }

    @Override
    public Object clone() {
        // The following if statement only exists to help checkstyle
        // think we are calling super.clone() when we in fact do not want that.
        if (false) {
            super.clone();
        }
        return new KaazingPDU(this);
    }

    /**
     * Gets a string representation of the supplied PDU type.
     *
     * @param type a PDU type.
     * @return a string representation of <code>type</code>, for example "GET".
     */
    public static String getTypeString(int type) {
        if (type == KaazingPDU.GETSUBTREE) {
            return "GETSUBTREE";
        } else if (type == KaazingPDU.KAAZING_NOTIFICATION_SUBSCRIPTION) {
            return "KAAZING_NOTIFICATION_SUBSCRIPTION";
        }
        return PDU.getTypeString(type);
    }

    /**
     * Gets the PDU type identifier for a string representation of the type.
     *
     * @param type the string representation of a PDU type: <code>GET, GETNEXT, GETBULK, SET, INFORM, RESPONSE, REPORT, TRAP,
     *             V1TRAP, GETSUBTREE)</code>.
     * @return the corresponding PDU type constant, or <code>Integer.MIN_VALUE</code> of the supplied type is unknown.
     */
    public static int getTypeFromString(String type) {
        if (type.equals("GETSUBTREE")) {
            return KaazingPDU.GETSUBTREE;
        } else if (type.equals("KAAZING_NOTIFICATION_SUBSCRIPTION")) {
            return KaazingPDU.KAAZING_NOTIFICATION_SUBSCRIPTION;
        }
        return PDU.getTypeFromString(type);
    }
}
