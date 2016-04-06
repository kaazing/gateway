<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright 2007-2016, Kaazing Corporation. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<!--
    Pre-parse transformation for gateway-config.xml files.
    Adds xsi:type to login-module and service elements.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output encoding="UTF-8" method="xml" omit-xml-declaration="no"/>

    <!--
        Copy content over unmodified except for &lt;login-module&gt; and
        &lt;service&gt; elements.
    -->
    <xsl:template
            match="*[local-name()!='login-module' and local-name()!='service']|@*|text()|comment()|processing-instruction()">
        <xsl:copy>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <!-- Add xsi:type to &lt;login-module&gt;. -->
    <xsl:template match="*[local-name()='login-module']">
        <xsl:copy>
            <xsl:attribute name="xsi:type" namespace="http://www.w3.org/2001/XMLSchema-instance">
                <xsl:choose>
                    <xsl:when test="starts-with(./*[local-name()='type'],'class:')">CustomLoginModuleType</xsl:when>
                    <xsl:otherwise><xsl:value-of select="normalize-space(./*[local-name()='type'])"/>LoginModuleType
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <!--
        Add xsi:type to &lt;service&gt;. The name of the xsi:type is derived
        from the &lt;type&gt; of the &lt;service&gt; so that the stylesheet can
        be used for any type of service. Note that only XSL 1.0 functions can be used.
    -->
    <xsl:template match="*[local-name()='service']">
        <xsl:copy>
            <xsl:attribute name="xsi:type" namespace="http://www.w3.org/2001/XMLSchema-instance">
                <xsl:choose>
                    <xsl:when test="contains(./*[local-name()='type'],'-extension')">ExtensionServiceType</xsl:when>
                    <xsl:otherwise><xsl:value-of select="normalize-space(./*[local-name()='type'])"/>ServiceType
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
