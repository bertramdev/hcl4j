package com.bertramlabs.plugins.hcl4j.utils;


import org.apache.commons.beanutils.PropertyUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map;
import org.xml.sax.SAXParseException;

/**
 * Utility methods for calling external APIs in a standardized way.
 *
 * @author David Estes
 * @since 0.8.0
 */
public class HttpApiClient {

	HttpClient httpClient;
	HttpClientConnectionManager connectionManager;
	BasicCookieStore cookieStore = new BasicCookieStore();
	/**
	 * Sets a throttle rate (in milliseconds) between HTTP Calls. This is used to slow down queries to the remote server.
	 */
	public Long throttleRate = 0L;

	private Date lastCallTime;

	static Logger log = LoggerFactory.getLogger(HttpApiClient.class);

	static final Integer WEB_CONNECTION_TIMEOUT = 120 * 1000;

	public ServiceResponse callApi(String url, String path, String username, String password) throws URISyntaxException, Exception {
		return callApi(url,path,username,password,new RequestOptions(),"POST");
	}

	public ServiceResponse callApi(String url, String path, String username, String password, RequestOptions opts) throws URISyntaxException, Exception {
		return callApi(url,path,username,password,opts,"POST");
	}

	private void sleepIfNecessary() {
		try {
			Long tmpThrottleRate = throttleRate;
			if(tmpThrottleRate != null && tmpThrottleRate > 0) {
				if(lastCallTime != null) {
					Date now = new Date();
					Long timeDiff = now.getTime() - lastCallTime.getTime();
					tmpThrottleRate = tmpThrottleRate - timeDiff;
					if(tmpThrottleRate > 0) {
						Thread.sleep(tmpThrottleRate);
					}
				} else {
					Thread.sleep(tmpThrottleRate);
				}

			}
		} catch(InterruptedException ignore) {

		}

	}

	public ServiceResponse callApi(String url, final String path, String username, String password, RequestOptions opts, String method) throws URISyntaxException, Exception {
		ServiceResponse rtn = new ServiceResponse();
		LinkedHashMap<String,Object> data = new LinkedHashMap<>();
		rtn.setData(data);
		URIBuilder uriBuilder = new URIBuilder(url);
		try {

			sleepIfNecessary();
			lastCallTime = new Date();

			String existingPath = uriBuilder.getPath();
			// retain path on base url if one exists
			String newPath = path;
			if(path != null && path.length() > 0) {
				if(existingPath != null && existingPath.length() > 0 && !path.startsWith(existingPath)) {
					if(existingPath.endsWith("/") && path.startsWith("/")) {
						existingPath = existingPath.substring(0, existingPath.length() - 1);
					} else if(!existingPath.endsWith("/") && !path.startsWith("/")) {
						existingPath += "/";
					}
					newPath = existingPath + path;
				}
				uriBuilder.setPath(newPath);
			}
			if(opts.queryParams != null && !opts.queryParams.isEmpty()) {
				for(CharSequence queryKey : opts.queryParams.keySet()) {
					uriBuilder.addParameter(queryKey.toString(), opts.queryParams.get(queryKey).toString());
				}
			}

			HttpRequestBase request;
			switch(method.toUpperCase()) {
				case "HEAD":
					request = new HttpHead(uriBuilder.build());
					break;
				case "PUT":
					request = new HttpPut(uriBuilder.build());
					break;
				case "POST":
					request = new HttpPost(uriBuilder.build());
					break;
				case "PATCH":
					request = new HttpPatch(uriBuilder.build());
					break;
				case "GET":
					request = new HttpGet(uriBuilder.build());
					break;
				case "DELETE":
					request = new HttpDelete(uriBuilder.build());
					break;
				default:
					throw new Exception("method was not specified");
			}
			if(username != null && username.length() > 0 && password != null && password.length() > 0) {
				String creds = username + ":" + password;
				String credHeader = "Basic " + Base64.getEncoder().encodeToString(creds.getBytes());
				request.addHeader("Authorization",credHeader);
			}

			//if bearer token
			if(opts.apiToken != null) {
				int newLine = opts.apiToken.indexOf('\n');
				if(newLine > -1)
					opts.apiToken = opts.apiToken.substring(0, newLine);
				request.addHeader("Authorization", "Bearer " + opts.apiToken);
			}


			// Headers
			if(opts.headers == null || opts.headers.isEmpty() || !opts.headers.containsKey("Content-Type")) {
				request.addHeader("Content-Type", "application/json");
			}

			if(opts.headers != null && !opts.headers.isEmpty()) {
				for (CharSequence headerKey : opts.headers.keySet()) {
					request.addHeader(headerKey.toString(), opts.headers.get(headerKey).toString());
				}
			}

			if (opts.body != null) {
				HttpEntityEnclosingRequestBase postRequest = (HttpEntityEnclosingRequestBase) request;
				if(opts.body instanceof Map) {
					if (opts.contentType == "form") {
						List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
						Map<String, Object> bodyMap = (Map<String, Object>) opts.body;
						for (String key : bodyMap.keySet()) {
							Object v = bodyMap.get(key);
							Object rowValue;
							if (v instanceof CharSequence) {
								rowValue = v;
							} else {
								rowValue = v.toString();
							}
							urlParameters.add(new BasicNameValuePair(key, rowValue.toString()));
						}
						postRequest.setEntity(new UrlEncodedFormEntity(urlParameters));
					} else if (opts.contentType == "multi-part-form") {
						MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
						String rowBoundary = "--" + java.util.UUID.randomUUID().toString() + "--";
						Map<String, Object> bodyMap = (Map<String, Object>) opts.body;
						for (String key : bodyMap.keySet()) {
							Object v = bodyMap.get(key);
							//if multiples..
							if (v instanceof Collection) {
								for (String rowValue : (Collection<String>) v) {
									StringBody rowBody = new StringBody(rowValue.toString(), ContentType.create("text/plain", "UTF-8"));
									entityBuilder.addPart(key, rowBody);
								}
							} else {
								Object rowValue;
								//convert it
								if (v instanceof CharSequence) {
									rowValue = v;
								} else {
									rowValue = v.toString();
								}
								StringBody rowBody = new StringBody(rowValue.toString(), ContentType.create("text/plain", "UTF-8"));
								entityBuilder.addPart(key, rowBody);
							}
						}
						entityBuilder.setContentType(ContentType.MULTIPART_FORM_DATA);
						entityBuilder.setBoundary(rowBoundary);
						postRequest.setEntity(entityBuilder.build());
						//replace the header
						if (request.containsHeader("Content-Type")) {
							//append the boundary
							Header currentType = request.getFirstHeader("Content-Type");
							String newValue = currentType.getValue();
							newValue = newValue + "; boundary=" + rowBoundary;
							request.setHeader("Content-Type", newValue);
						}
					}
				} else if(opts.body instanceof byte[]) {
					postRequest.setEntity(new ByteArrayEntity((byte[]) opts.body));
				} else if(opts.body instanceof InputStream) {
					postRequest.setEntity(new InputStreamEntity((InputStream)(opts.body), opts.contentLength != null ? opts.contentLength : -1));
				} else {
					postRequest.setEntity(new StringEntity(opts.body.toString()));
				}
			}

			withClient(opts,(HttpClient client, BasicCookieStore cookieStore) -> {

				CloseableHttpResponse response = null;
				try {

					response = (CloseableHttpResponse)client.execute(request);
					if(response.getStatusLine().getStatusCode() <= 399) {
						for(Header header : response.getAllHeaders()) {
							rtn.addHeader(header.getName(), header.getValue());
						}


						for(Header header : response.getHeaders("Set-Cookie")) {
							Map<String,String> cookies = extractCookie(header.getValue());
							for(String cookieKey : cookies.keySet()) {
								BasicClientCookie cookie = new BasicClientCookie(cookieKey, cookies.get(cookieKey));
								cookie.setPath("/");
								cookie.setDomain(request.getURI().getHost());
								cookieStore.addCookie(cookie);
							}
						}


						HttpEntity entity = response.getEntity();
						if(entity != null) {
							rtn.setContent(EntityUtils.toString(entity));
							if(!opts.suppressLog) {
								log.debug("results of SUCCESSFUL call to {}/{}, results: {}",url,path,rtn.getContent());
							}
						} else {
							rtn.setContent(null);
						}
						rtn.setErrorCode(Integer.toString(response.getStatusLine().getStatusCode()));
						rtn.setSuccess(true);
					} else {
						if(response.getEntity() != null) {
							rtn.setContent(EntityUtils.toString(response.getEntity()));
						}
						rtn.setSuccess(false);
						rtn.setErrorCode(Integer.toString(response.getStatusLine().getStatusCode()));
						log.warn("path: {} error: {} - {}",path,rtn.getErrorCode(),rtn.getContent());
					}
				} catch(Exception ex) {
					try {
						log.error("Error occurred processing the response for {} : {}",uriBuilder.build().toString(),ex.getMessage(), ex);
						rtn.setError("Error occurred processing the response for " + uriBuilder.build().toString() + " : " + ex.getMessage());
					} catch(URISyntaxException uie) {
						log.error("Error occurred processing the response for {} : {}","invalid uri",ex.getMessage(), ex);
						rtn.setError("Error occurred processing the response for invalid uri  : " + ex.getMessage());

					}

					rtn.setSuccess(false);
				} finally {
					lastCallTime = new Date();
					if(response != null) {
						try {
							response.close();
						} catch (IOException ignored) {
							//ignored exception
						}
					}
				}
			});

		} catch(SSLProtocolException sslEx) {
			log.error("Error Occurred calling API (SSL Exception): {}",sslEx.getMessage(),sslEx);
			rtn.addError("sslHandshake",  "SSL Handshake Exception (is SNI Misconfigured): " + sslEx.getMessage());
			rtn.setSuccess(false);
		} catch (Exception e) {
			log.error("Error Occurred calling API: " + e.getMessage(),e);
			rtn.addError("error", e.getMessage());
			rtn.setSuccess(false);
		}
		return rtn;
	}


	public Map<CharSequence,CharSequence> addRequiredHeader(Map<CharSequence,CharSequence> headers, String name, String value) {
		if(headers == null) {
			headers = new LinkedHashMap<>();
		}
		headers.putIfAbsent(name,value);
		return headers;
	}

	Map<String, String> extractCookie(String rawCookie) {
		if(rawCookie == null || rawCookie.length() == 0) return null;
		String[] cookieArgs = rawCookie.split("=");
		String name = cookieArgs[0];
		String data = rawCookie.split(name + "=")[1].split(";")[0];
		String value = "";
		if(data != null && data.length() > 0) {
			value = data.substring(1,data.length() - 1);
		}
		Map<String,String> cookieMap = new LinkedHashMap<>();
		cookieMap.put(name,value);
		return cookieMap;
	}

	private void withClient(RequestOptions opts, WithClientFunction withClientFunction) {

		Boolean ignoreSSL = opts.ignoreSSL;
		if(httpClient != null) {
			withClientFunction.method(httpClient,cookieStore);
			return;
		} else {
			HttpClientBuilder clientBuilder = HttpClients.custom();


			RequestConfig.Builder reqConfigBuilder = RequestConfig.custom();
			reqConfigBuilder.setCookieSpec(CookieSpecs.STANDARD);
			if(opts.connectionTimeout != null) {
				reqConfigBuilder.setConnectTimeout(opts.connectionTimeout);
				reqConfigBuilder.setConnectionRequestTimeout(opts.connectionTimeout);
			}
			if(opts.readTimeout != null) {
				reqConfigBuilder.setSocketTimeout(opts.readTimeout);
			}
			clientBuilder.setDefaultRequestConfig(reqConfigBuilder.build());


			clientBuilder.setDefaultCookieStore(cookieStore);
			clientBuilder.setHostnameVerifier(new X509HostnameVerifier() {
				public boolean verify(String host, SSLSession sess) {
					return true;
				}

				public void verify(String host, SSLSocket ssl) {

				}

				public void verify(String host, String[] cns, String[] subjectAlts) {

				}

				public void verify(String host, X509Certificate cert) {

				}

			});
			SSLConnectionSocketFactory sslConnectionFactory;
			SSLContext sslcontext = null;
			if(ignoreSSL) {
				try {
					sslcontext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
						@Override
						public boolean isTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
							return true;
						}
					}).build();
				} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ignored) {
					//;
				}
				sslConnectionFactory = new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER) {

					@Override
					protected void prepareSocket(SSLSocket socket) {
						try {
							PropertyUtils.setProperty(socket, "host", null);
						} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
							e.printStackTrace();
						}
						List<SNIServerName> serverNames  = Collections.<SNIServerName> emptyList();
						SSLParameters sslParams = socket.getSSLParameters();
						sslParams.setServerNames(serverNames);
						socket.setSSLParameters(sslParams);
					}
					@Override
					public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException, ConnectTimeoutException {
						if(socket instanceof SSLSocket) {
							try {
								String[] enabledProtocols = {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"};
								SSLSocket sslSocket = (SSLSocket)socket;
								sslSocket.setEnabledProtocols(enabledProtocols);
								PropertyUtils.setProperty(socket, "host", host.getHostName());
							} catch (Exception ex) {
								log.error("We have an unhandled exception when attempting to connect to {} ignoring SSL errors",host, ex);
							}
						}
						return super.connectSocket(WEB_CONNECTION_TIMEOUT, socket, host, remoteAddress, localAddress, context);
					}
				};
			} else {
				sslcontext = SSLContexts.createSystemDefault();
				sslConnectionFactory = new SSLConnectionSocketFactory(sslcontext) {
					@Override
					public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException, ConnectTimeoutException {
						if(socket instanceof SSLSocket) {
							try {
								String[] enabledProtocols = {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"};
								SSLSocket sslSocket = (SSLSocket)socket;
								sslSocket.setEnabledProtocols(enabledProtocols);
								PropertyUtils.setProperty(socket, "host", host.getHostName());
							} catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
								//;
							}
						}

						return super.connectSocket(opts.timeout != null ? opts.timeout : 30000, socket, host, remoteAddress, localAddress, context);
					}
				};
			}


			HttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory() {
				@Override
				public HttpMessageParser<HttpResponse> create(SessionInputBuffer ibuffer, MessageConstraints constraints) {
					LineParser lineParser = new BasicLineParser() {

						@Override
						public Header parseHeader(final CharArrayBuffer buffer) {
							try {
								return super.parseHeader(buffer);
							} catch (ParseException ex) {
								return new BasicHeader(buffer.toString(), null);
							}
						}

					};
					return new DefaultHttpResponseParser(
							ibuffer, lineParser, DefaultHttpResponseFactory.INSTANCE, constraints != null ? constraints : MessageConstraints.DEFAULT) {
						@Override
						protected boolean reject(final CharArrayBuffer line, int count) {
							//We need to break out of forever head reads
							if(count > 100) {
								return true;
							}
							return false;

						}

					};
				}
			};

			clientBuilder.setSSLSocketFactory(sslConnectionFactory);
			Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("https", sslConnectionFactory)
					.register("http", PlainConnectionSocketFactory.INSTANCE)
					.build();

			HttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();

			HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(
					requestWriterFactory, responseParserFactory);
			BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(registry, connFactory);
			clientBuilder.setConnectionManager(connectionManager);


			HttpClient client = clientBuilder.build();
			httpClient = client;
			this.connectionManager = connectionManager;

			withClientFunction.method(client,cookieStore);
		}

	}

	public void shutdownClient() {
		if(connectionManager != null) {
			try {
				connectionManager.shutdown();
			} catch(Exception ex) {
				log.error("Error Shutting Down Keep-Alive {}",ex.getMessage(),ex);
			}
		}
	}

	@FunctionalInterface
	public static interface WithClientFunction {
		void method(HttpClient client, BasicCookieStore cookieStore);
	}

	public static class RequestOptions {
		public Object body;
		public String contentType; //bodyType originally
		public Map<CharSequence,CharSequence> headers;
		public Map<CharSequence,CharSequence> queryParams;
		public Boolean suppressLog = true;
		public Boolean ignoreSSL=true;
		public Integer timeout = 30000;
		public Integer connectionTimeout = null;
		public Integer readTimeout = null;
		public Long contentLength = null;

		public OauthOptions oauth;
		public String apiToken;
		public HttpClient httpClient; //optional pass the client
		public HttpClientConnectionManager connectionManager;

		public static class OauthOptions {
			public String version;
			public String consumerKey;
			public String consumerSecret;
			public String apiKey;
			public String apiSecret;
		}
	}
}

