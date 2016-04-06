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
<!-- Transforms a Dragonfire gateway-config.xml to production Excalibur -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:g="http://xmlns.kaazing.com/gateway-config/excalibur"
                xmlns:dragonfire="http://xmlns.kaazing.com/gateway-config/dragonfire"
                version="1.0">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" omit-xml-declaration="no" indent="yes"/>

    <!--  Copy attributes, text, comments and processing instructions unmodified -->
    <xsl:template match="@*|text()|comment()|processing-instruction()">
        <xsl:copy>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <!--  Remove all but the last management entry -->
    <xsl:template match="/dragonfire:gateway-config/dragonfire:management[position() &lt; last()]">
        <xsl:comment>Unused management configurations have been removed.</xsl:comment>
    </xsl:template>

    <!--  Remove all but the last network entry -->
    <xsl:template match="/dragonfire:gateway-config/dragonfire:network[position() &lt; last()]">
        <xsl:comment>Unused network configurations have been removed.</xsl:comment>
    </xsl:template>

    <!--  Change all elements to the Excalibur namespace -->
    <xsl:template match="dragonfire:*">
        <xsl:element name="g:{local-name()}" namespace="http://xmlns.kaazing.com/gateway-config/excalibur">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
