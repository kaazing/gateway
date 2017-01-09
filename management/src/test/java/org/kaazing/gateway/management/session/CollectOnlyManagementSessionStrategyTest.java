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
package org.kaazing.gateway.management.session;

import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.transport.test.Expectations;

public class CollectOnlyManagementSessionStrategyTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private final ManagementSessionStrategy strategy = getManagementSessionStrategy();

    @Test
    public void doSessionClosed_shouldCallSessionCloseListeners() throws Exception {
        final SessionManagementBean sessionBean = context.mock(SessionManagementBean.class, "sessionBean");

        context.checking(new Expectations() {
            {
                oneOf(sessionBean).doSessionClosed();
                oneOf(sessionBean).doSessionClosedListeners();
            }
        });

        strategy.doSessionClosed(sessionBean);
    }

    protected ManagementSessionStrategy getManagementSessionStrategy() {
        return new CollectOnlyManagementSessionStrategy();
    }

}

