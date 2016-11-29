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

import java.util.List;
import javax.management.ObjectName;
import org.kaazing.gateway.management.config.ClusterConfigurationBean;

public class ClusterConfigurationMXBeanImpl implements ClusterConfigurationMXBean {

    private final ClusterConfigurationBean clusterConfigBean;

    public ClusterConfigurationMXBeanImpl(ObjectName name, ClusterConfigurationBean clusterConfigBean) {
        this.clusterConfigBean = clusterConfigBean;
    }

    @Override
    public String getName() {
        return clusterConfigBean.getName();
    }

    @Override
    public String getAccepts() {
        return stringifyList(clusterConfigBean.getAccepts());
    }

    @Override
    public String getConnects() {
        return stringifyList(clusterConfigBean.getConnects());
    }

    @Override
    public String getConnectOptions() {
        return clusterConfigBean.getConnectOptions();
    }

    public String stringifyList(List<String> values) {
        StringBuffer sb = new StringBuffer();
        if (values != null) {
            boolean first = true;
            for (String str : values) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(str);
                first = false;
            }
        }
        return sb.toString();
    }
}
