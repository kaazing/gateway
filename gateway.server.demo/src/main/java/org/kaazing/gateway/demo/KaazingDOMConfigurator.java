/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kaazing.gateway.demo;

import java.util.Properties;

import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Element;

public class KaazingDOMConfigurator extends DOMConfigurator {
    private Properties properties;
    public KaazingDOMConfigurator(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected void setParameter(Element elem, PropertySetter propSetter) {
        // Log4J typically replaces ${variable} with the value of System.getProperty("variable").
        // Since the Demo Service do not set System properties, we need to do our own variable
        // substitution by looking for these variable strings and replacing them with values
        // from the Demo Service's configured properties.
        String paramName = OptionConverter.substVars(elem.getAttribute("name"), properties);
        String value = elem.getAttribute("value");
        value = OptionConverter.substVars(OptionConverter.convertSpecialChars(value), properties);
        propSetter.setProperty(paramName, value);
    }
}