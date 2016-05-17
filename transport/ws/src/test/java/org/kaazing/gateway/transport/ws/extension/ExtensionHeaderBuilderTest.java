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
package org.kaazing.gateway.transport.ws.extension;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ExtensionHeaderBuilderTest {

    @Test(expected = NullPointerException.class)
    public void shouldNotBuildExtensionNullToken()
        throws Exception {

        String token = null;
        ExtensionHeaderBuilder web = new ExtensionHeaderBuilder(token);
    }

    @Test
    public void shouldBuildExtensionFromStringWithoutParameters()
        throws Exception {

        String token = "x-kaazing-foo";
        ExtensionHeaderBuilder web = new ExtensionHeaderBuilder(token);
        Assert.assertTrue(String.format("Expected extension token '%s', got '%s'", token, web.getExtensionToken()), web.getExtensionToken().equals(token));
        Assert.assertTrue("Expected no parameters", web.hasParameters() == false);
    }

    @Test
    public void shouldBuildExtensionFromStringWithParameters()
        throws Exception {

        String token = "x-kaazing-foo; a=1; b=2; ahoy";
        ExtensionHeaderBuilder web = new ExtensionHeaderBuilder(token);

        String extName = "x-kaazing-foo";
        Assert.assertTrue(String.format("Expected extension token '%s', got '%s'", extName, web.getExtensionToken()), web.getExtensionToken().equals(extName));
        Assert.assertTrue("Expected parameters", web.hasParameters() == true);

        List<ExtensionParameter> params = web.getParameters();
        Assert.assertTrue(String.format("Expected 3 parameters, got %d", params.size()), params.size() == 3);

        ExtensionParameter param = new ExtensionParameterBuilder("a", "1");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));

        param = new ExtensionParameterBuilder("b", "2");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));

        param = new ExtensionParameterBuilder("ahoy");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));
    }

    @Test
    public void shouldBuildExtensionFromStringWithParametersDanglingSemicolon()
        throws Exception {

        String token = "x-kaazing-foo; a=1; b=2; ahoy;";
        ExtensionHeaderBuilder web = new ExtensionHeaderBuilder(token);

        String extName = "x-kaazing-foo";
        Assert.assertTrue(String.format("Expected extension token '%s', got '%s'", extName, web.getExtensionToken()), web.getExtensionToken().equals(extName));
        Assert.assertTrue("Expected parameters", web.hasParameters() == true);

        List<ExtensionParameter> params = web.getParameters();
        Assert.assertTrue(String.format("Expected 3 parameters, got %d", params.size()), params.size() == 3);

        ExtensionParameter param = new ExtensionParameterBuilder("a", "1");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));

        param = new ExtensionParameterBuilder("b", "2");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));

        param = new ExtensionParameterBuilder("ahoy");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));
    }

    @Test
    public void shouldBuildExtensionFromStringWithParametersMultipleEquals()
        throws Exception {

        String token = "x-kaazing-foo; a=1; b=2; ahoy=; yarr=rum=eyepatch=";
        ExtensionHeaderBuilder web = new ExtensionHeaderBuilder(token);

        String extName = "x-kaazing-foo";
        Assert.assertTrue(String.format("Expected extension token '%s', got '%s'", extName, web.getExtensionToken()), web.getExtensionToken().equals(extName));
        Assert.assertTrue("Expected parameters", web.hasParameters() == true);

        List<ExtensionParameter> params = web.getParameters();
        Assert.assertTrue(String.format("Expected 4 parameters, got %d", params.size()), params.size() == 4);

        ExtensionParameter param = new ExtensionParameterBuilder("a", "1");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));

        param = new ExtensionParameterBuilder("b", "2");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));

        // We have to assume a value here of empty string.
        param = new ExtensionParameterBuilder("ahoy", "");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));

        param = new ExtensionParameterBuilder("yarr", "rum=eyepatch=");
        Assert.assertTrue(String.format("Expected presence of param '%s'", param), params.contains(param));
    }

    @Test
    public void shouldCompareExtensionsWithoutParameters()
        throws Exception {

        String token1 = "x-kaazing-foo";
        ExtensionHeaderBuilder ext1 = new ExtensionHeaderBuilder(token1);

        String token2 = "x-kaazing-foo";
        ExtensionHeaderBuilder ext2 = new ExtensionHeaderBuilder(token2);

        Assert.assertTrue(String.format("Expected exts %s to %s to be equal", ext1, ext2), ext1.equals(ext2));

        String token3 = "x-kaazing-bar";
        ExtensionHeaderBuilder ext3 = new ExtensionHeaderBuilder(token3);

        Assert.assertTrue(String.format("Expected exts %s to %s to NOT be equal", ext2, ext3), ext2.equals(ext3) == false);
    }

    @Test
    public void shouldCompareExtensionsWithDifferentParameters()
        throws Exception {

        String token1 = "x-kaazing-foo; a=1; b=2";
        ExtensionHeaderBuilder ext1 = new ExtensionHeaderBuilder(token1);

        String token2 = "x-kaazing-foo; x=y";
        ExtensionHeaderBuilder ext2 = new ExtensionHeaderBuilder(token2);

        Assert.assertTrue(String.format("Expected exts %s to %s to be equal", ext1, ext2), ext1.equals(ext2));

        String token3 = "x-kaazing-foo";
        ExtensionHeaderBuilder ext3 = new ExtensionHeaderBuilder(token3);
        ExtensionHeaderBuilder ext4 = new ExtensionHeaderBuilder(ext3);
        ext4.appendParameter("bar");

        Assert.assertTrue(String.format("Expected exts %s to %s to be equal", ext3, ext4), ext3.equals(ext4));
    }

    @Test
    public void shouldBuildExtensionFromExtension()
        throws Exception {

        String token1 = "x-kaazing-foo";
        ExtensionHeaderBuilder ext1 = new ExtensionHeaderBuilder(token1);
        ExtensionHeaderBuilder ext2 = new ExtensionHeaderBuilder(ext1);

        Assert.assertTrue(String.format("Expected exts %s to %s to be equal", ext1, ext2), ext1.equals(ext2));
    }
    
    @Test
    public void toStringShouldProduceHeaderWithWithNameEqValueParameter() throws Exception {
        String in = "x-kaazing-foo; param1=value1";
        ExtensionHeaderBuilder ext1 = new ExtensionHeaderBuilder(in);
        assertEquals(in, ext1.toString());
    }
    
    @Test
    public void toStringShouldProduceHeaderWithNameOnlyParameter() throws Exception {
        String in = "x-kaazing-foo; param1";
        ExtensionHeaderBuilder ext1 = new ExtensionHeaderBuilder(in);
        assertEquals(in, ext1.toString());
    }
    
}

