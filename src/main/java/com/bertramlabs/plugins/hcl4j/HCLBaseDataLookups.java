package com.bertramlabs.plugins.hcl4j;

import com.bertramlabs.plugins.hcl4j.utils.HttpApiClient;
import com.bertramlabs.plugins.hcl4j.utils.ServiceResponse;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class HCLBaseDataLookups {
	static void registerBaseFunctions(HCLParser parser) {
		parser.registerDataLookup("http",(properties) -> {
			LinkedHashMap<String,Object> schema = new LinkedHashMap<>(properties);
			HttpApiClient client = new HttpApiClient();
			try {
				String method = (String)(properties.get("method"));
				if(method == null) {
					method = "GET"; //default GET Method
				}
				HttpApiClient.RequestOptions requestOptions = new HttpApiClient.RequestOptions();
				requestOptions.body = properties.get("request_body");
				requestOptions.headers = (Map<CharSequence,CharSequence>)(properties.get("request_headers") );
				requestOptions.ignoreSSL = properties.get("insecure") != null && (Boolean) (properties.get("insecure"));
				if(properties.get("request_timeout_ms")!=null) {
					requestOptions.timeout = Integer.parseInt((String)(properties.get("request_timeout_ms")));
				}
				ServiceResponse response = client.callApi((String)(properties.get("url")),null,null,null,requestOptions,method);

				schema.put("id",properties.get("url"));
				schema.put("status_code",Integer.parseInt(response.getErrorCode()));
				schema.put("body",response.getContent());
				String body = response.getContent();
				if(body != null) {
					String encodedBody = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
					schema.put("response_body_base64",encodedBody);
				}
				schema.put("response_headers",response.getHeaders());
				schema.put("response_body",response.getContent());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			} catch (Exception e2) {
				throw new RuntimeException(e2);
			} finally {
				client.shutdownClient();
			}
			return schema;
		});
	}
}

