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
package org.kaazing.gateway.server.util.version;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class DuplicateJarFinderTest {

    private static final String MOCK_JAR_FILE_NAME = "org.kaazing:gateway.server";
    private static final String MOCK_JAR_FILE_NAME2 = "org.codehaus:some.jar";
    protected static URL MOCK_URL;
    protected static URL MOCK_URL2;

    private Mockery context;

    {
        {
            try {
                MOCK_URL = new URL("http://doesntmatter.com");
                MOCK_URL2 = new URL("http://doesntmatter2.com");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    @Before
    public void setUp() {
        context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
    }

    @Test
    public void testFindDuplicateJarsShouldNotThrowExceptionIfOneKaazingProduct() throws IOException, DuplicateJarsException {
        final ManifestReader classPathParser = context.mock(ManifestReader.class);
        final Logger gatewayLogger = context.mock(Logger.class);
        final Enumeration<URL> manifestURLs = context.mock(Enumeration.class);
        DuplicateJarFinder duplicateJarFinder = new DuplicateJarFinder(gatewayLogger);
        duplicateJarFinder.setClassPathParser(classPathParser);

        context.checking(new Expectations() {
            {
                oneOf(classPathParser).getManifestURLs();
                will(returnValue(manifestURLs));
                oneOf(manifestURLs).hasMoreElements();
                will(returnValue(true));
                oneOf(manifestURLs).nextElement();
                will(returnValue(MOCK_URL));
                oneOf(manifestURLs).hasMoreElements();
                will(returnValue(false));
                oneOf(classPathParser).getManifestAttributesFromURL(MOCK_URL);
                will(returnValue(getAttributesForKaazingProduct()));
                allowing(gatewayLogger).debug(with(any(String.class)), with(any(Object.class)), with(any(Object.class)));
                never(gatewayLogger).error(with(any(String.class)), with(any(Object.class)));
            }
        });

        duplicateJarFinder.findDuplicateJars();
        context.assertIsSatisfied();

    }

    @Test
    public void testFindDuplicateJarsShouldNotThrowExceptionIfNoneKaazingProduct() throws IOException, DuplicateJarsException {
        final ManifestReader classPathParser = context.mock(ManifestReader.class);
        final Logger gatewayLogger = context.mock(Logger.class);
        final Enumeration<URL> manifestURLs = context.mock(Enumeration.class);
        DuplicateJarFinder duplicateJarFinder = new DuplicateJarFinder(gatewayLogger);
        duplicateJarFinder.setClassPathParser(classPathParser);

        context.checking(new Expectations() {
            {
                oneOf(classPathParser).getManifestURLs();
                will(returnValue(manifestURLs));
                oneOf(manifestURLs).hasMoreElements();
                will(returnValue(true));
                oneOf(manifestURLs).nextElement();
                will(returnValue(MOCK_URL));
                oneOf(manifestURLs).hasMoreElements();
                will(returnValue(false));
                oneOf(classPathParser).getManifestAttributesFromURL(MOCK_URL);
                will(returnValue(getAttributesForNoneKaazingProduct()));
                allowing(gatewayLogger).debug(with(any(String.class)), with(any(Object.class)), with(any(Object.class)));
                never(gatewayLogger).error(with(any(String.class)), with(any(Object.class)));
            }
        });
        duplicateJarFinder.findDuplicateJars();
        context.assertIsSatisfied();
    }

    @Test(
            expected = DuplicateJarsException.class)
    public void testFindDuplicateJarsShouldThrowExceptionIfDuplicateKaazingProducts() throws IOException, DuplicateJarsException {
        final ManifestReader classPathParser = context.mock(ManifestReader.class);
        final Logger gatewayLogger = context.mock(Logger.class);
        final Enumeration<URL> manifestURLs = context.mock(Enumeration.class);
        DuplicateJarFinder duplicateJarFinder = new DuplicateJarFinder(gatewayLogger);
        duplicateJarFinder.setClassPathParser(classPathParser);

        context.checking(new Expectations() {
            {
                oneOf(classPathParser).getManifestURLs();
                will(returnValue(manifestURLs));
                oneOf(manifestURLs).hasMoreElements();
                will(returnValue(true));
                oneOf(manifestURLs).nextElement();
                will(returnValue(MOCK_URL));
                oneOf(manifestURLs).hasMoreElements();
                will(returnValue(true));
                oneOf(manifestURLs).nextElement();
                will(returnValue(MOCK_URL2));
                oneOf(manifestURLs).hasMoreElements();
                will(returnValue(false));
                oneOf(classPathParser).getManifestAttributesFromURL(MOCK_URL);
                will(returnValue(getAttributesForKaazingProduct()));
                allowing(classPathParser).getManifestAttributesFromURL(MOCK_URL2);
                will(returnValue(getAttributesForKaazingProduct()));
                allowing(gatewayLogger).debug(with(any(String.class)), with(any(Object.class)), with(any(Object.class)));
                allowing(gatewayLogger).error(with(any(String.class)), with(any(Object.class)), with(any(Object.class)));
            }
        });

        duplicateJarFinder.findDuplicateJars();
        context.assertIsSatisfied();
    }

    private Attributes getAttributesForKaazingProduct() {
        Attributes attributes = new Attributes();
        attributes.putValue("Implementation-Version", "1.0");
        attributes.putValue("Artifact-Name", MOCK_JAR_FILE_NAME);
        return attributes;
    }

    private Attributes getAttributesForNoneKaazingProduct() {
        Attributes attributes = new Attributes();
        attributes.putValue("Implementation-Version", "1.0");
        attributes.putValue("Artifact-Name", MOCK_JAR_FILE_NAME2);
        return attributes;
    }

}
