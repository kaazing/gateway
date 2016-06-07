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
package org.kaazing.gateway.transport.ssl;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.ExpectationError;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import org.kaazing.mina.core.session.IoSessionEx;

public class SslAcceptProcessorTest {

    @SuppressWarnings("unchecked")
    @Test(expected = ExpectationError.class)
    // test case for KG-1685
    public void sslEncryptionDisabled() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final SslSession session = context.mock(SslSession.class);
        final IoSessionEx parent = context.mock(IoSessionEx.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        
        context.checking(new Expectations() {
            {
                exactly(2).of(session).getParent(); will(returnValue(parent));
                oneOf(parent).getFilterChain(); will(returnValue(filterChain));
                oneOf(filterChain).getEntry(with(any(Class.class))); will(returnValue(null));
                
                // Do not include following line because it causes an (expected) NPE in 
                // AbstractIoSession.close (which class imposterizer can't override because it's final).
                // Instead we allow the test to throw ExpectationError due to the "unexpected" invocation
                // of session.close
                //oneOf(session).close(); will(throwException(new NullPointerException()));
            }
        });
        
        SslAcceptProcessor processor = new SslAcceptProcessor();
        processor.removeInternal(session);
        context.assertIsSatisfied();
    }

}
