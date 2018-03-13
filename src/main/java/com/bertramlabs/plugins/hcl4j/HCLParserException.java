package com.bertramlabs.plugins.hcl4j;

/**
* When a HCL Lexical parser error is reached this exception is thrown.
* @author David Estes
*/
public class HCLParserException extends Exception {
	public HCLParserException(String message) {
        super(message);
    }
	public HCLParserException(String message,Exception ex) {
		super(message,ex);
	}
}