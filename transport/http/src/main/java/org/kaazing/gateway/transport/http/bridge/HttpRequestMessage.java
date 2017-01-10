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
package org.kaazing.gateway.transport.http.bridge;

import static java.lang.Boolean.valueOf;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.security.auth.Subject;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.http.HttpMethod;


public class HttpRequestMessage extends HttpStartMessage {

    private enum QueryUpdate { DECODE, ENCODE }

    private static final Map<String, List<String>> EMPTY_PARAMETERS = Collections.emptyMap();

	private boolean secure;
	private Map<String, List<String>> parameters;
	private URI requestURI;
	private URI absoluteRequestURI;

	private HttpMethod method;
	private ResourceAddress localAddress;
	private Subject subject;
	private ResultAwareLoginContext loginContext;
	private String externalURI;

	private QueryUpdate queryUpdate;


	@Override
	public Kind getKind() {
		return Kind.REQUEST;
	}

    public ResultAwareLoginContext getLoginContext() {
        return loginContext;
    }

    public void setLoginContext(ResultAwareLoginContext loginContext) {
        this.loginContext = loginContext;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setLocalAddress(ResourceAddress localAddress) {
        this.localAddress = localAddress;
    }

    public ResourceAddress getLocalAddress() {
        return localAddress;
    }

    public void setExternalURI(String externalURI) {
        this.externalURI = externalURI;
    }

    public String getExternalURI() {
        return externalURI;
    }

	public String getParameter(String parameterName) {
		List<String> parameterValues = getParameterValues(parameterName, false);
		if (parameterValues == null || parameterValues.isEmpty()) {
			return null;
		}
		return parameterValues.get(0);
	}

	public List<String> getParameterValues(String parameterName) {
		return getParameterValues(parameterName, false);
	}

	public void addParameter(String parameterName, String parameterValue) {
		if (parameterName == null) {
			throw new NullPointerException("parameterName");
		}
		if (parameterValue == null) {
			throw new NullPointerException("parameterValue");
		}
		List<String> parameterValues = getParameterValues(parameterName, true);
		parameterValues.add(parameterValue);
	}

	public void clearParameters() {
		Map<String, List<String>> parameters = getParameters(false);
		if (parameters != null) {
			parameters.clear();
			queryUpdate = QueryUpdate.ENCODE;
		}
	}

	public String removeParameter(String parameterName) {
		Map<String, List<String>> parameters = getParameters(false);
		if (parameters != null) {
			List<String> parameterValues = parameters.remove(parameterName);
			if (parameterValues != null && !parameterValues.isEmpty()) {
				queryUpdate = QueryUpdate.ENCODE;
				return parameterValues.get(0);
			}
		}
		return null;
	}

	public void setParameter(String parameterName, String parameterValue) {
		if (parameterName == null) {
			throw new NullPointerException("parameterName");
		}
		if (parameterValue == null) {
			throw new NullPointerException("parameterValue");
		}
		List<String> parameterValues = getParameterValues(parameterName, true);
		parameterValues.clear();
		parameterValues.add(parameterValue);
	}

	public void setParameters(Map<String, List<String>> newParameters) {
		Map<String, List<String>> parameters = getParameters(true);
		parameters.clear();
		parameters.putAll(newParameters);
	}

	public Map<String, List<String>> getParameters() {
		Map<String, List<String>> parameters = getParameters(false);
		return (parameters != null && !parameters.isEmpty()) ? Collections.unmodifiableMap(parameters) : EMPTY_PARAMETERS;
	}

	public void setAbsoluteRequestURI(URI absoluteRequestURI) {
		this.absoluteRequestURI = absoluteRequestURI;
	}

	public URI getAbsoluteRequestURI() {
		return absoluteRequestURI;
	}

	public void setRequestURI(URI requestURI) {
		this.requestURI = requestURI;

		// clear existing parameters
		Map<String, List<String>> parameters = getParameters(false);
		if (parameters != null) {
			parameters.clear();
		}

		// schedule query decode if necessary
		if (requestURI != null && requestURI.getQuery() != null) {
			queryUpdate = QueryUpdate.DECODE;
		}
	}

	@SuppressWarnings("deprecation")
	public URI getRequestURI() {
		if (queryUpdate == QueryUpdate.ENCODE) {
			queryUpdate = null;

			Map<String, List<String>> parameters = getParameters(false);
			if (parameters != null && !parameters.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				if (requestURI != null) {
					sb.append(requestURI.getPath());
				}
				sb.append('?');
				int baseSize = sb.length();
				for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
					String parameterName = entry.getKey();
					List<String> parameterValues = entry.getValue();
					if (parameterValues != null) {
						for (String parameterValue : parameterValues) {
							if (sb.length() > baseSize) {
								sb.append('&');
							}
							sb.append(URLEncoder.encode(parameterName));
							sb.append('=');
							if (parameterValue != null) {
								sb.append(URLEncoder.encode(parameterValue));
							}
						}
					}
				}

				if (sb.length() > 1) {
					URI relativeURI = URI.create(sb.toString());
					requestURI = (requestURI != null) ? requestURI.resolve(relativeURI) : relativeURI;
				}
			}
            else {
                requestURI = requestURI.resolve(requestURI.getPath());
            }
		}
		return requestURI;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isSecure() {
		return secure;
	}

    @Override
    public String toString() {
        return String.format("%s: %s %s %s %s %s", getKind(), getVersion(), getMethod(), getRequestURI(), getContent(), (isComplete() ? "" : " [...]"));
    }

    @Override
    public String toVerboseString() {
        return String.format("%s: %s %s %s HEADERS: %s %s %s", getKind(), getVersion(), getMethod(), getRequestURI(), getHeaders(), getContent(), (isComplete() ? "" : " [...]"));
    }

    @Override
    public int hashCode() {
        // canonicalize requestURI and query parameters
        if (queryUpdate != null) {
            getRequestURI();
            getParameters();
        }

        int hashCode = super.hashCode();
        if (secure) {
            hashCode ^= valueOf(secure).hashCode();
        }
        if (parameters != null) {
            hashCode ^= parameters.hashCode();
        }
        if (requestURI != null) {
            hashCode ^= requestURI.hashCode();
        }
        if (method != null) {
            hashCode ^= method.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HttpRequestMessage)) {
            return false;
        }

        return equals((HttpRequestMessage)o);
    }

    protected boolean equals(HttpRequestMessage that) {

        // canonicalize requestURI and query parameters
        if (this.queryUpdate != null) {
            this.getRequestURI();
            this.getParameters();
        }

        if (that.queryUpdate != null) {
            that.getRequestURI();
            that.getParameters();
        }

        return (super.equals(that) &&
                this.secure == that.secure &&
                sameOrEquals(this.method, that.method) &&
                sameOrEquals(this.requestURI, that.requestURI) &&
                sameOrEquals(this.parameters, that.parameters));
    }

	@Override
	protected Map<String, List<String>> createHeaders() {
		return new TreeMap<>(HttpHeaderNameComparator.INSTANCE);
	}

	@SuppressWarnings("deprecation")
	private Map<String, List<String>> getParameters(boolean createIfNull) {

		// lazily create parameters
		if (parameters == null && (createIfNull || queryUpdate == QueryUpdate.DECODE)) {
		    // maintain original parameter ordering
			parameters = new LinkedHashMap<>();
		}

		// re-validate parameters if necessary
		if (queryUpdate == QueryUpdate.DECODE) {
			// avoid recursive decode
			queryUpdate = null;

			String query = requestURI.getRawQuery();
			if (query != null) {
				String[] nvPairs = query.split("&");
				for (String nvPair : nvPairs) {
					int equalAt = nvPair.indexOf('=');
					if (equalAt != -1) {
						String parameterName = nvPair.substring(0, equalAt);
						String parameterValue = nvPair.substring(equalAt + 1);
						addParameter(URLDecoder.decode(parameterName), URLDecoder.decode(parameterValue));
					}
					else {
						addParameter(URLDecoder.decode(nvPair), "");
					}
				}
			}

			// fully decoded, avoid encoding next time
			queryUpdate = null;
		}

		// detect parameter mutation, schedule query encode
		if (createIfNull) {
			queryUpdate = QueryUpdate.ENCODE;
		}

		return parameters;
	}

	private List<String> getParameterValues(String parameterName, boolean createIfNull) {
		Map<String, List<String>> parameters = getParameters(createIfNull);
		if (parameters == null) {
			return null;
		}
		List<String> parameterValues = parameters.get(parameterName);
		if (parameterValues == null && createIfNull) {
			parameterValues = new ArrayList<>();
			parameters.put(parameterName, parameterValues);
		}
		return parameterValues;
	}

}
