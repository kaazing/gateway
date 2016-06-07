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
package org.kaazing.gateway.transport.http;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

public class DefaultHttpCookie implements MutableHttpCookie, Comparable<DefaultHttpCookie> {

    private final String name;
    private String comment;
    private String domain;
    private long maxAge;
    private String path;
    private boolean secure;
    private String value;
    private int version;

    public DefaultHttpCookie(String name, String domain, String path, String value) {
        this(name, domain, value);
        this.path = path;
    }

    public DefaultHttpCookie(String name, String domain, String value) {
        this(name, value);
        this.domain = domain;
    }

    public DefaultHttpCookie(String name, String value) {
        this(name);
        this.value = value;
    }

    public DefaultHttpCookie(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public long getMaxAge() {
        return maxAge;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if ( o == this ) {
            return true;
        }
        if (o == null || !(o instanceof DefaultHttpCookie)) {
            return false;
        }

        DefaultHttpCookie that = (DefaultHttpCookie) o;

        if (maxAge != that.maxAge) return false;
        if (secure != that.secure) return false;
        if (version != that.version) return false;
        if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;
        if (domain != null ? !domain.equals(that.domain) : that.domain != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        return !(value != null ? !value.equals(that.value) : that.value != null);

    }

    @Override
    public int compareTo(DefaultHttpCookie that) {
        if ( that == this ) {
            return 0;
        }
        if (that == null || !(that instanceof DefaultHttpCookie)) {
            return MIN_VALUE;
        }
        
        int comparison = 0;
        if (comparison == 0) {
            comparison = (int) (that.maxAge - this.maxAge);
        }
        if (comparison == 0) {
            comparison = (that.secure ? 1 : 0) - (this.secure ? 1 : 0);
        }
        if (comparison == 0) {
            comparison = (that.version - this.version);
        }
        if (comparison == 0) {
            comparison = (comment != null) ? comment.compareTo(that.comment) : (that.comment == null) ? 0 : MAX_VALUE;
        }
        if (comparison == 0) {
            comparison = (domain != null) ? domain.compareTo(that.domain) : (that.domain == null) ? 0 : MAX_VALUE;
        }
        if (comparison == 0) {
            comparison = (name != null) ? name.compareTo(that.name) : (that.name == null) ? 0 : MAX_VALUE;
        }
        if (comparison == 0) {
            comparison = (path != null) ? path.compareTo(that.path) : (that.path == null) ? 0 : MAX_VALUE;
        }
        if (comparison == 0) {
            comparison = (value != null) ? value.compareTo(that.value) : (that.value == null) ? 0 : MAX_VALUE;
        }

        return comparison;
    }

    @Override
    public String toString() {
        return "HTTP-COOKIE [" + "name=" + name + ", " + "domain=" + domain + ", " + "path=" + path + ", " +
                "maxAge=" + maxAge + ", " + "secure=" + secure + ", " + "version=" + version + ", " +
                "value=" + value + "]";
    }
}
