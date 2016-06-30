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
package org.kaazing.gateway.management.jmx;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import org.kaazing.gateway.management.service.ServiceManagementBean;

public class ServiceMXBeanTest {
    private Mockery context;

    @Before
    public void setUp() throws Exception {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        context.setThreadingPolicy(new Synchroniser());
    }

    @Test
    public void shouldCleanSessionsByPrincipal() throws Exception {
        final JmxManagementServiceHandler handler = context.mock(JmxManagementServiceHandler.class);
        final ServiceManagementBean serviceManagementBean = context.mock(ServiceManagementBean.class);
        final SessionMXBean sessionBean1 = context.mock(SessionMXBean.class, "sessionBean1");
        final SessionMXBean sessionBean4 = context.mock(SessionMXBean.class, "sessionBean2");
        final ObjectName objectName = context.mock(ObjectName.class);
        final Map<Long, Map<String, String>> sessionPrincipalMap = new HashMap<>();
        final Map<String, String> s1Principals = new HashMap<>();
        final Map<String, String> s2Principals = new HashMap<>();
        final Map<String, String> s3Principals = new HashMap<>();
        final Map<String, String> s4Principals = new HashMap<>();
        final Map<Long, Map<String, String>> activeSessionPrincipalMap = new HashMap<>();

        context.checking(new Expectations() {
            {
                oneOf(serviceManagementBean).getLoggedInSessions();
                will(returnValue(sessionPrincipalMap));

                oneOf(handler).getSessionMXBean(1); will(returnValue(sessionBean1));
                oneOf(handler).getSessionMXBean(4); will(returnValue(sessionBean4));

                oneOf(sessionBean1).close();
                oneOf(serviceManagementBean).removeSessionManagementBean(1);
                will(new CustomAction("Delete sessionBean1") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        activeSessionPrincipalMap.remove(1L);
                        return null;
                    }
                });

                oneOf(sessionBean4).close();
                oneOf(serviceManagementBean).removeSessionManagementBean(4);
                will(new CustomAction("Delete sessionBean4") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        activeSessionPrincipalMap.remove(4L);
                        return null;
                    }
                });
            }
        });

        s1Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s1Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s2Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s2Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s3Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s3Principals.put("joe", "com.kaazing.demo.auth.MyAnotherUserPrincipal");

        s4Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s4Principals.put("MARKETING", "com.kaazing.demo.auth.MyRolePrincipal");

        sessionPrincipalMap.put(1L, s1Principals);
        sessionPrincipalMap.put(2L, s2Principals);
        sessionPrincipalMap.put(3L, s3Principals);
        sessionPrincipalMap.put(4L, s4Principals);

        activeSessionPrincipalMap.putAll(sessionPrincipalMap);

        final ServiceMXBean jmxServiceBean = new ServiceMXBeanImpl(handler, objectName, serviceManagementBean);
        jmxServiceBean.closeSessions("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        context.assertIsSatisfied();
        assertEquals(2, activeSessionPrincipalMap.size());
    }

    @Test
    public void shouldFailCleanSessionsByNullUsername() throws Exception {
        final JmxManagementServiceHandler handler = context.mock(JmxManagementServiceHandler.class);
        final ServiceManagementBean serviceManagementBean = context.mock(ServiceManagementBean.class);
        final SessionMXBean sessionBean1 = context.mock(SessionMXBean.class, "sessionBean1");
        final SessionMXBean sessionBean4 = context.mock(SessionMXBean.class, "sessionBean2");
        final ObjectName objectName = context.mock(ObjectName.class);
        final Map<Long, Map<String, String>> sessionPrincipalMap = new HashMap<>();
        final Map<String, String> s1Principals = new HashMap<>();
        final Map<String, String> s2Principals = new HashMap<>();
        final Map<String, String> s3Principals = new HashMap<>();
        final Map<String, String> s4Principals = new HashMap<>();
        final Map<Long, Map<String, String>> activeSessionPrincipalMap = new HashMap<>();

        context.checking(new Expectations() {
            {
                never(serviceManagementBean).getLoggedInSessions();
                will(returnValue(sessionPrincipalMap));

                never(handler).getSessionMXBean(1); will(returnValue(sessionBean1));
                never(handler).getSessionMXBean(4); will(returnValue(sessionBean4));

                never(sessionBean1).close();
                never(serviceManagementBean).removeSessionManagementBean(1);

                never(sessionBean4).close();
                never(serviceManagementBean).removeSessionManagementBean(4);
            }
        });

        s1Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s1Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s2Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s2Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s3Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s3Principals.put("joe", "com.kaazing.demo.auth.MyAnotherUserPrincipal");

        s4Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s4Principals.put("MARKETING", "com.kaazing.demo.auth.MyRolePrincipal");

        sessionPrincipalMap.put(1L, s1Principals);
        sessionPrincipalMap.put(2L, s2Principals);
        sessionPrincipalMap.put(3L, s3Principals);
        sessionPrincipalMap.put(4L, s4Principals);

        activeSessionPrincipalMap.putAll(sessionPrincipalMap);

        final ServiceMXBean jmxServiceBean = new ServiceMXBeanImpl(handler, objectName, serviceManagementBean);
        jmxServiceBean.closeSessions(null, "com.kaazing.demo.auth.MyUserPrincipal");
        context.assertIsSatisfied();
        assertEquals(4, activeSessionPrincipalMap.size());
    }

    @Test
    public void shouldFailCleanSessionsByNullPrincipalClassName() throws Exception {
        final JmxManagementServiceHandler handler = context.mock(JmxManagementServiceHandler.class);
        final ServiceManagementBean serviceManagementBean = context.mock(ServiceManagementBean.class);
        final SessionMXBean sessionBean1 = context.mock(SessionMXBean.class, "sessionBean1");
        final SessionMXBean sessionBean4 = context.mock(SessionMXBean.class, "sessionBean2");
        final ObjectName objectName = context.mock(ObjectName.class);
        final Map<Long, Map<String, String>> sessionPrincipalMap = new HashMap<>();
        final Map<String, String> s1Principals = new HashMap<>();
        final Map<String, String> s2Principals = new HashMap<>();
        final Map<String, String> s3Principals = new HashMap<>();
        final Map<String, String> s4Principals = new HashMap<>();
        final Map<Long, Map<String, String>> activeSessionPrincipalMap = new HashMap<>();

        context.checking(new Expectations() {
            {
                never(serviceManagementBean).getLoggedInSessions();
                will(returnValue(sessionPrincipalMap));

                never(handler).getSessionMXBean(1); will(returnValue(sessionBean1));
                never(handler).getSessionMXBean(4); will(returnValue(sessionBean4));

                never(sessionBean1).close();
                never(serviceManagementBean).removeSessionManagementBean(1);

                never(sessionBean4).close();
                never(serviceManagementBean).removeSessionManagementBean(4);
            }
        });

        s1Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s1Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s2Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s2Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s3Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s3Principals.put("joe", "com.kaazing.demo.auth.MyAnotherUserPrincipal");

        s4Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s4Principals.put("MARKETING", "com.kaazing.demo.auth.MyRolePrincipal");

        sessionPrincipalMap.put(1L, s1Principals);
        sessionPrincipalMap.put(2L, s2Principals);
        sessionPrincipalMap.put(3L, s3Principals);
        sessionPrincipalMap.put(4L, s4Principals);

        activeSessionPrincipalMap.putAll(sessionPrincipalMap);

        final ServiceMXBean jmxServiceBean = new ServiceMXBeanImpl(handler, objectName, serviceManagementBean);
        jmxServiceBean.closeSessions("joe", null);
        context.assertIsSatisfied();
        assertEquals(4, activeSessionPrincipalMap.size());
    }

    @Test
    public void shouldFailCleanSessionsByEmptyUsername() throws Exception {
        final JmxManagementServiceHandler handler = context.mock(JmxManagementServiceHandler.class);
        final ServiceManagementBean serviceManagementBean = context.mock(ServiceManagementBean.class);
        final SessionMXBean sessionBean1 = context.mock(SessionMXBean.class, "sessionBean1");
        final SessionMXBean sessionBean4 = context.mock(SessionMXBean.class, "sessionBean2");
        final ObjectName objectName = context.mock(ObjectName.class);
        final Map<Long, Map<String, String>> sessionPrincipalMap = new HashMap<>();
        final Map<String, String> s1Principals = new HashMap<>();
        final Map<String, String> s2Principals = new HashMap<>();
        final Map<String, String> s3Principals = new HashMap<>();
        final Map<String, String> s4Principals = new HashMap<>();
        final Map<Long, Map<String, String>> activeSessionPrincipalMap = new HashMap<>();

        context.checking(new Expectations() {
            {
                never(serviceManagementBean).getLoggedInSessions();
                will(returnValue(sessionPrincipalMap));

                never(handler).getSessionMXBean(1); will(returnValue(sessionBean1));
                never(handler).getSessionMXBean(4); will(returnValue(sessionBean4));

                never(sessionBean1).close();
                never(serviceManagementBean).removeSessionManagementBean(1);

                never(sessionBean4).close();
                never(serviceManagementBean).removeSessionManagementBean(4);
            }
        });

        s1Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s1Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s2Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s2Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s3Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s3Principals.put("joe", "com.kaazing.demo.auth.MyAnotherUserPrincipal");

        s4Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s4Principals.put("MARKETING", "com.kaazing.demo.auth.MyRolePrincipal");

        sessionPrincipalMap.put(1L, s1Principals);
        sessionPrincipalMap.put(2L, s2Principals);
        sessionPrincipalMap.put(3L, s3Principals);
        sessionPrincipalMap.put(4L, s4Principals);

        activeSessionPrincipalMap.putAll(sessionPrincipalMap);

        final ServiceMXBean jmxServiceBean = new ServiceMXBeanImpl(handler, objectName, serviceManagementBean);
        jmxServiceBean.closeSessions("", "com.kaazing.demo.auth.MyUserPrincipal");
        context.assertIsSatisfied();
        assertEquals(4, activeSessionPrincipalMap.size());
    }

    @Test
    public void shouldFailCleanSessionsByEmptyPrincipalClassName() throws Exception {
        final JmxManagementServiceHandler handler = context.mock(JmxManagementServiceHandler.class);
        final ServiceManagementBean serviceManagementBean = context.mock(ServiceManagementBean.class);
        final SessionMXBean sessionBean1 = context.mock(SessionMXBean.class, "sessionBean1");
        final SessionMXBean sessionBean4 = context.mock(SessionMXBean.class, "sessionBean2");
        final ObjectName objectName = context.mock(ObjectName.class);
        final Map<Long, Map<String, String>> sessionPrincipalMap = new HashMap<>();
        final Map<String, String> s1Principals = new HashMap<>();
        final Map<String, String> s2Principals = new HashMap<>();
        final Map<String, String> s3Principals = new HashMap<>();
        final Map<String, String> s4Principals = new HashMap<>();
        final Map<Long, Map<String, String>> activeSessionPrincipalMap = new HashMap<>();

        context.checking(new Expectations() {
            {
                never(serviceManagementBean).getLoggedInSessions();
                will(returnValue(sessionPrincipalMap));

                never(handler).getSessionMXBean(1); will(returnValue(sessionBean1));
                never(handler).getSessionMXBean(4); will(returnValue(sessionBean4));

                never(sessionBean1).close();
                never(serviceManagementBean).removeSessionManagementBean(1);

                never(sessionBean4).close();
                never(serviceManagementBean).removeSessionManagementBean(4);
            }
        });

        s1Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s1Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s2Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s2Principals.put("SALES", "com.kaazing.demo.auth.MyRolePrincipal");

        s3Principals.put("jane", "com.kaazing.demo.auth.MyUserPrincipal");
        s3Principals.put("joe", "com.kaazing.demo.auth.MyAnotherUserPrincipal");

        s4Principals.put("joe", "com.kaazing.demo.auth.MyUserPrincipal");
        s4Principals.put("MARKETING", "com.kaazing.demo.auth.MyRolePrincipal");

        sessionPrincipalMap.put(1L, s1Principals);
        sessionPrincipalMap.put(2L, s2Principals);
        sessionPrincipalMap.put(3L, s3Principals);
        sessionPrincipalMap.put(4L, s4Principals);

        activeSessionPrincipalMap.putAll(sessionPrincipalMap);

        final ServiceMXBean jmxServiceBean = new ServiceMXBeanImpl(handler, objectName, serviceManagementBean);
        jmxServiceBean.closeSessions("joe", "");
        context.assertIsSatisfied();
        assertEquals(4, activeSessionPrincipalMap.size());
    }
}

