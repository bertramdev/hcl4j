package com.bertramlabs.plugins.hcl4j.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ServiceResponse is a generic that allows you to strongly type models.
 *
 * Some scenarios are:
 * 	Respond with text/html of content.
 * 	Respond with data that will be serialized as json.
 * 	Respond with data that will be used in a template model.
 *
 * Response headers and cookies can also be set if required.
 *
 * @param <T> The class that holds the data - must be serializable. Usually a Map or List.
 */
public class ServiceResponse<T> {
	/**
	 * The Key of key in the errors map that holds the errors.
	 */
	private static final String DEFAULT_ERROR_KEY = "error";
	private Boolean success = false;
	private Boolean warning = false;
	private String msg = null;
	private Map<String,String> errors = new LinkedHashMap<>();
	private T data;
	private Map<String,Object> headers;
	private String content;
	private String errorCode;
	// Holds the parsed json map or array.
	// TODO: Add jackson or a java json lib.
	private Object results;
	private Map<String, String> cookies;
	public Boolean inProgress = false;

	public ServiceResponse() { }


	public ServiceResponse(Boolean success, String msg, Map<String, String> errors, T data) {
		this.success = success;
		this.msg = msg;
		if(errors != null) {
			this.errors = errors;
		}
		this.data = data;
	}

	/**
	 * Helper to initialize a base response.
	 * @return A generic respose assuming the repsonse is an error if not converted to a success response.
	 */
	public static ServiceResponse prepare() {
		return new ServiceResponse(false, null, null, null);
	}

	/**
	 * Helper to initialize a base response with initial data.
	 * @return A generic respose assuming the repsonse is an error if not converted to a success response.
	 */
	public static ServiceResponse prepare(Object data) {
		return new ServiceResponse(false, null, null, data);
	}

	/**
	 * Helper to build an error response from a generic map.
	 * @return A success or error response based on the boolean value of success in the map.
	 */
	public static ServiceResponse create(Map<String, Object> config) {
		Boolean configSuccess = config.get("success") == null ? false : (Boolean)config.get("success");
		String configMsg = config.get("msg") != null ? (String)config.get("message") : null;
		Map<String, String> configErrors = (LinkedHashMap<String, String>)config.getOrDefault("errors", new LinkedHashMap<String, String>());
		Object configData = config.getOrDefault("data", new LinkedHashMap<String, Object>());
		ServiceResponse rtn = new ServiceResponse(configSuccess, configMsg, configErrors, configData);
		Boolean inProgress = (Boolean)config.get("inProgress");
		if(inProgress != null) {
			rtn.inProgress = inProgress;
		}
		Boolean warning = (Boolean)config.get("warning");
		if(warning != null) {
			rtn.warning = warning;
		}
		return rtn;
	}

	/**
	 * Helper to create service response from an existing service response.
	 * Primarly a convenience method to prevent errors when a map has already been converted to a service response.
	 * @return An unmodified service response.
	 */
	public static ServiceResponse create(ServiceResponse source) {
		return source;
	}

	/**
	 * Helper to return a generic error response.
	 * @return A generic error scenario.
	 */
	public static ServiceResponse error() {
		ServiceResponse serviceResponse = new ServiceResponse(false, null, null, null);
		serviceResponse.setError("error");
		return serviceResponse;
	}

	/**
	 * Helper to return a error message
	 * @param msg Message to send to the user.
	 * @return a ServiceResponse
	 */
	public static ServiceResponse error(String msg) {
		ServiceResponse serviceResponse = new ServiceResponse(false, null, null, null);
		serviceResponse.setError(msg);
		return serviceResponse;
	}

	/**
	 * Detailed error message with a list of errors.
	 * @param msg Message to send to the user.
	 * @param errors Detailed list of errors
	 * @return a ServiceResponse
	 */
	public static ServiceResponse error(String msg, Map<String,String> errors) {
		return new ServiceResponse(false, msg, errors, null);
	}
	/**
	 * Detailed error message with a list of errors.
	 * @param msg Message to send to the user.
	 * @param errors Detailed list of errors
	 * @param data Any additional data needed for the view.
	 * @return a ServiceResponse
	 */
	public static ServiceResponse error(String msg, Map<String,String> errors, Object data) {
		return new ServiceResponse(false, msg, errors, data);
	}

	/**
	 * Helper to return a success object with a message.
	 * @param data object to pass back in success
	 * @param msg success message
	 * @return a ServiceResponse
	 */
	static ServiceResponse success(Object data, String msg) {
		return new ServiceResponse(true, msg, null, data);
	}

	/**
	 * Helper to return a success message.
	 * @param data object to pass back in success
	 * @return a ServiceResponse
	 */
	public static ServiceResponse success(Object data) {
		return new ServiceResponse(true, null, null, data);
	}

	/**
	 * Create a generic success response
	 * @return success response
	 */
	public static ServiceResponse success() {
		return new ServiceResponse(true, null, null, null);
	}

	/**
	 * Build a Map from this object with keys success, msg, errors, data
	 * @return response Map
	 */
	public Map<String,Object> toMap() {
		return toMap(null);
	}

	/**
	 * Serializes the ServiceResponse to a map.
	 * @param dataKeyName the name to assign the data keys key in the map
	 * @return A Map
	 */
	public Map<String,Object> toMap(String dataKeyName) {
		Map<String,Object> returnMap = new LinkedHashMap<>();
		returnMap.put("success",success);
		returnMap.put("msg",msg);
		returnMap.put("errors",errors);
		returnMap.put( (dataKeyName != null ? dataKeyName : "data"),data);
		return returnMap;
	}

	/**
	 * Return if the ServiceResponse has any errors set
	 * @param key Check a specific key
	 * @return boolean
	 */
	public boolean hasError(String key) {
		if(errors == null) return false;
		return errors.containsKey(key);
	}

	/**
	 * Return if the ServiceResponse has any errors set
	 * @return boolean
	 */
	public boolean hasErrors() {
		if(errors == null) return false;
		if(!success) return true;
		return errors.size() > 0;
	}

	/**
	 * String representation of the toMap() method
	 * @return the response as a String
	 */
	public String toString() {
		return toMap().toString();
	}

	// Getters & Setters
	public Boolean getSuccess() {
		return this.success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public Map<String, String> getErrors() {
		return errors;
	}

	public void setErrors(Map<String, String> errors) {
		if (errors != null) {
			this.success = false;
		}
		this.errors = errors;
	}

	public void addError(String value) {
		this.success = false;
		this.errors.put(DEFAULT_ERROR_KEY, value);
	}

	public void addError(String key, String value) {
		this.success = false;
		this.errors.put(key, value);
	}

	public void removeError() {
		this.errors.remove(DEFAULT_ERROR_KEY);
		if (this.errors.size() == 0) {
			this.success = true;
		}
	}

	public void clearErrors() {
		this.errors.clear();
		this.success = true;
	}

	public void removeError(String key) {
		this.errors.remove(key);
	}

	/**
	 * Returns the specific error message for a given key.
	 * @param key that contains the error
	 * @return The error value
	 */
	public String getError(String key) {
		return this.errors.getOrDefault(key, null);
	}

	/**
	 * Provided for backwards compatibility with existing getError()
	 * @return Error message
	 */
	public String getError() {
		return this.errors.getOrDefault(DEFAULT_ERROR_KEY, null);
	}

	/**
	 * Provided for backwards compatibility with existing setError(msg)
	 * @param value value to set
	 */
	public void setError(String value) {
		this.success = false;
		this.errors.put(DEFAULT_ERROR_KEY, value);
	}

	public Map<String, Object> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	public void addHeader(String key, Object value) {
		if(this.headers == null)
			this.headers = new HashMap<>();
		this.headers.put(key, value);
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public Object getResults() {
		return results;
	}

	public void setResults(Object results) {
		this.results = results;
	}

	public Map getCookies() {
		return cookies;
	}

	public void setCookies(Map cookies) {
		this.cookies = cookies;
	}

	/**
	 * Add a Cookie to the response
	 * @param key cookie name
	 * @param value cookie value
	 */
	public void addCookie(String key, Object value) {
		if(this.cookies == null)
			this.cookies = new HashMap<>();
		this.cookies.put(key, value.toString());
	}

	/**
	 * Find a cookie
	 * @param key cookie name
	 * @return the cookie value
	 */
	public String getCookie(String key) {
		if(this.cookies == null)
			return null;
		return this.cookies.getOrDefault(key, null);
	}
}
