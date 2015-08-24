package org.kaazing.gateway.server.util.version;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.jar.Attributes;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class DuplicateJarFinderTest {

    private static final String EXCEPTION_EXPECTED_MSG = "Exception was expected.";
    private static final String[] MOCK_CLASS_PATH_ENTRIES = {"gateway.server.jar"};
    private static final String[] MOCK_CLASS_DUPLICATE_PATH_ENTRIES = {"gateway.server.jar", "gateway.server.jar"};
    private static final String MOCK_JAR_FILE_NAME = "org.kaazing:gateway.server";
    private static final String MOCK_JAR_FILE_NAME2 = "org.codehaus:some.jar";

    private Mockery context;

    @Before
    public void setUp() {
        context = new Mockery();

        context.setImposteriser(ClassImposteriser.INSTANCE);
    }

    @Test
    public void testFindDuplicateJarsShouldNotThrowExceptionIfOneKaazingProduct() throws IOException, DuplicateJarsException {
        final ClassPathParser classPathParser = context.mock(ClassPathParser.class);
        final Logger gatewayLogger = context.mock(Logger.class);
        DuplicateJarFinder duplicateJarFinder = new DuplicateJarFinder(gatewayLogger);
        duplicateJarFinder.setClassPathParser(classPathParser);

        context.checking(new Expectations() {
            {
                oneOf(classPathParser).getClassPathEntries();
                will(returnValue(MOCK_CLASS_PATH_ENTRIES));
                oneOf(classPathParser).getManifestAttributesFromClassPathEntry(MOCK_CLASS_PATH_ENTRIES[0]);
                will(returnValue(getAttributesForKaazingProduct()));
                allowing(gatewayLogger).debug(with(any(String.class)), with(any(Object.class)), with(any(Object.class)));
                never(gatewayLogger).error(with(any(String.class)), with(any(Object.class)));
            }
        });

        duplicateJarFinder.findDuplicateJars();

    }

    @Test
    public void testFindDuplicateJarsShouldNotThrowExceptionIfNoneKaazingProduct() throws IOException, DuplicateJarsException {
        final ClassPathParser classPathParser = context.mock(ClassPathParser.class);
        final Logger gatewayLogger = context.mock(Logger.class);
        DuplicateJarFinder duplicateJarFinder = new DuplicateJarFinder(gatewayLogger);
        duplicateJarFinder.setClassPathParser(classPathParser);

        context.checking(new Expectations() {
            {
                oneOf(classPathParser).getClassPathEntries();
                will(returnValue(MOCK_CLASS_PATH_ENTRIES));
                oneOf(classPathParser).getManifestAttributesFromClassPathEntry(MOCK_CLASS_PATH_ENTRIES[0]);
                will(returnValue(getAttributesForNoneKaazingProduct()));
                allowing(gatewayLogger).debug(with(any(String.class)), with(any(Object.class)), with(any(Object.class)));
                never(gatewayLogger).error(with(any(String.class)), with(any(Object.class)));
            }
        });
        duplicateJarFinder.findDuplicateJars();
    }

    @Test(
            expected = DuplicateJarsException.class)
    public void testFindDuplicateJarsShouldThrowExceptionIfDuplicateKaazingProducts() throws IOException, DuplicateJarsException {
        final ClassPathParser classPathParser = context.mock(ClassPathParser.class);
        final Logger gatewayLogger = context.mock(Logger.class);
        DuplicateJarFinder duplicateJarFinder = new DuplicateJarFinder(gatewayLogger);
        duplicateJarFinder.setClassPathParser(classPathParser);

        context.checking(new Expectations() {
            {
                oneOf(classPathParser).getClassPathEntries();
                will(returnValue(MOCK_CLASS_DUPLICATE_PATH_ENTRIES));
                allowing(classPathParser).getManifestAttributesFromClassPathEntry(MOCK_CLASS_DUPLICATE_PATH_ENTRIES[0]);
                will(returnValue(getAttributesForKaazingProduct()));
                allowing(gatewayLogger).debug(with(any(String.class)), with(any(Object.class)), with(any(Object.class)));
                allowing(gatewayLogger).error(with(any(String.class)), with(any(Object.class)));
            }
        });

        duplicateJarFinder.findDuplicateJars();

        fail(EXCEPTION_EXPECTED_MSG);
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
