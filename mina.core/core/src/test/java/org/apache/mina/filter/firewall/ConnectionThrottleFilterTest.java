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
package org.apache.mina.filter.firewall;

import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.apache.mina.core.session.DummySession;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ConnectionThrottleFilterTest extends TestCase
{
    private ConnectionThrottleFilter filter;

    private DummySession sessionOne;
    private DummySession sessionTwo;

    @Override
    protected void setUp() throws Exception
    {
        filter = new ConnectionThrottleFilter();

        sessionOne = new DummySession();
        sessionOne.setRemoteAddress( new InetSocketAddress(1234) );
        sessionTwo = new DummySession();
        sessionTwo.setRemoteAddress( new InetSocketAddress(1235) );
    }

    @Override
    protected void tearDown() throws Exception
    {
        filter = null;
    }

    public void testGoodConnection(){
        filter.setAllowedInterval( 100 );
        filter.isConnectionOk( sessionOne );
        try
        {
            Thread.sleep( 1000 );
        }
        catch ( InterruptedException e )
        {
            //e.printStackTrace();
        }

        boolean result = filter.isConnectionOk( sessionOne );
        assertTrue( result );
    }

    public void testBadConnection(){
        filter.setAllowedInterval( 1000 );
        filter.isConnectionOk( sessionTwo );
        assertFalse(filter.isConnectionOk( sessionTwo ));
    }

    public static void main(String[] args) {
     junit.textui.TestRunner.run( ConnectionThrottleFilterTest.class );
    }
}
