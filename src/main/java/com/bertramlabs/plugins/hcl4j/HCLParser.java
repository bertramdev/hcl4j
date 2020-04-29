/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bertramlabs.plugins.hcl4j;

import com.bertramlabs.plugins.hcl4j.RuntimeSymbols.EvalSymbol;
import com.bertramlabs.plugins.hcl4j.RuntimeSymbols.Variable;
import com.bertramlabs.plugins.hcl4j.symbols.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for the Hashicorp Configuration Language (HCL). This is the primary endpoint and converts the HCL syntax into a {@link Map}.
 * This parser utilizes a lexer to generate symbols based on the HCL spec. String interpolation is not evaluated at this point in the parsing.
 *
 * <p>
 *     Below is an example of how HCL might be parsed.
 * </p>
 * <pre>
 *     {@code
 *     import com.bertramlabs.plugins.hcl4j.HCLParser;
 *
 *     File terraformFile = new File("terraform.tf");
 *
 *     Map results = new HCLParser().parse(terraformFile);
 *     }
 * </pre>
 * @author David Estes
 */
public class HCLParser {


	public HCLParser() {

	}


	/**
	 * Parses terraform configuration language from a String
	 * @param input String input containing HCL syntax
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(String input) throws HCLParserException, IOException {
		StringReader reader = new StringReader(input);
		return parse(reader);
	}


	/**
	 * Parses terraform syntax as it comes from a File.
	 * @param input A source file to process with a default charset of UTF-8
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(File input) throws HCLParserException, IOException, UnsupportedEncodingException {
		return parse(input,"UTF-8");
	}


	/**
	 * Parses terraform syntax as it comes from a File.
	 * closed at the end of the parse operation (commonly via wrapping in a finally block)
	 * @param input A source file to process
	 * @param cs A charset
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(File input, Charset cs) throws HCLParserException, IOException{
		InputStream is = null;
		try {
			is = new FileInputStream(input);
			return parse(is,cs);
		} finally {
			if(is != null) {
				is.close();
			}
		}
	}


	/**
	 * Parses terraform syntax as it comes from a File.
	 * @param input A source file to process
	 * @param charsetName The name of a supported charset
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 * @throws UnsupportedEncodingException If the charset ( UTF-8 by default if unspecified) encoding is not supported
	 */
	public Map<String,Object> parse(File input, String charsetName) throws HCLParserException, IOException {
		InputStream is = null;
		try {
			is = new FileInputStream(input);
			return parse(is,charsetName);
		} finally {
			if(is != null) {
				is.close();
			}
		}
	}



	/**
	 * Parses terraform syntax as it comes from an input stream. The end user is responsible for ensuring the stream is
	 * closed at the end of the parse operation (commonly via wrapping in a finally block)
	 * @param input Streamable input of text going to the lexer
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(InputStream input) throws HCLParserException, IOException {
		return parse(input,"UTF-8");
	}

	/**
	 * Parses terraform syntax as it comes from an input stream. The end user is responsible for ensuring the stream is
	 * closed at the end of the parse operation (commonly via wrapping in a finally block)
	 * @param input Streamable input of text going to the lexer
	 * @param cs CharSet with which to read the contents of the stream   (default UTF-8)
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(InputStream input, Charset cs) throws HCLParserException, IOException {

		InputStreamReader reader;
		if(cs != null) {
			reader = new InputStreamReader(input,cs);
		} else {
			reader = new InputStreamReader(input,"UTF-8");
		}
		return parse(reader);
	}

	/**
	 * Parses terraform syntax as it comes from an input stream. The end user is responsible for ensuring the stream is
	 * closed at the end of the parse operation (commonly via wrapping in a finally block)
	 * @param input Streamable input of text going to the lexer
	 * @param charsetName String lookup of the character set this stream is providing (default UTF-8)
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 * @throws UnsupportedEncodingException If the charset ( UTF-8 by default if unspecified) encoding is not supported.
	 */
	public Map<String,Object> parse(InputStream input, String charsetName) throws HCLParserException, IOException, UnsupportedEncodingException {

		InputStreamReader reader;
		if(charsetName != null) {
			reader = new InputStreamReader(input,charsetName);
		} else {
			reader = new InputStreamReader(input,"UTF-8");
		}
		return parse(reader);
	}

	/**
	 * Parses terraform configuration language from a Reader
	 * @param reader A reader object used for absorbing various streams or String variables containing the hcl code
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(Reader reader) throws HCLParserException, IOException {
		HCLLexer lexer = new HCLLexer(reader);
		ArrayList<Symbol> rootBlocks;
		lexer.yylex();

		rootBlocks = lexer.elementStack;

		//Time to parse the AST Tree into a Map
		Map<String,Object> result = new LinkedHashMap<>();

		Map<String,Object> mapPosition = result;

		for(Symbol currentElement : rootBlocks) {
			processSymbol(currentElement,mapPosition);

		}
		return result;
	}


	private Object processSymbol(Symbol symbol, Map<String,Object> mapPosition) throws HCLParserException {

		if(symbol instanceof HCLBlock) {
			HCLBlock block = (HCLBlock)symbol;
			for(int counter = 0 ; counter < block.blockNames.size() ; counter++) {
				String blockName = block.blockNames.get(counter);
				if(mapPosition.containsKey(blockName)) {
					if(counter == block.blockNames.size() - 1 && mapPosition.get(blockName) instanceof Map) {
						List<Map<String,Object>> objectList = new ArrayList<>();
						Map<String,Object> addedObject = new LinkedHashMap<String,Object>();
						objectList.add((Map)mapPosition.get(blockName));
						objectList.add(addedObject);
						mapPosition.put(blockName,objectList);
						mapPosition = addedObject;
					} else if(mapPosition.get(blockName) instanceof Map) {
						mapPosition = (Map<String,Object>) mapPosition.get(blockName);
					} else if(counter == block.blockNames.size() - 1 && mapPosition.get(blockName) instanceof List) {
						Map<String,Object> addedObject = new LinkedHashMap<String,Object>();
						((List<Map>)mapPosition.get(blockName)).add(addedObject);
						mapPosition = addedObject;
					} else {
						if(mapPosition.get(blockName) instanceof List) {
							throw new HCLParserException("HCL Block expression scope traverses an object array");
						} else {
							throw new HCLParserException("HCL Block expression scope traverses an object value");
						}
					}
				} else {
					mapPosition.put(blockName,new LinkedHashMap<String,Object>());
					mapPosition = (Map<String,Object>) mapPosition.get(blockName);
				}
			}
			if(symbol.getChildren() != null) {
				for(Symbol child : block.getChildren()) {
					processSymbol(child,mapPosition);
				}
			}
			return mapPosition;
		} else if(symbol instanceof HCLMap) {
			Map<String,Object> nestedMap = new LinkedHashMap<>();
			if(symbol.getChildren() != null) {
				for(Symbol child : symbol.getChildren()) {
					processSymbol(child, nestedMap);
				}
			}
			return nestedMap;
		} else if(symbol instanceof HCLArray) {
			if(symbol.getChildren() != null) {
				List<Object> objectList = new ArrayList<>();
				for(Symbol child : symbol.getChildren()) {
					Map<String,Object> nestedMap = new LinkedHashMap<>();
					Object result = processSymbol(child,nestedMap);
					objectList.add(result);
				}
				return objectList;
			} else {
				return null;
			}
		} else if(symbol instanceof HCLValue) {
			return processValue((HCLValue) symbol);
		} else if(symbol instanceof EvalSymbol) {
			return processEvaluation((EvalSymbol) symbol);
		} else if(symbol instanceof HCLAttribute) {
			Map<String,Object> nestedMap = new LinkedHashMap<>();
			if(symbol.getChildren().size() > 0) {
				Object results = processSymbol(symbol.getChildren().get(0),nestedMap);
				mapPosition.put(symbol.getName(),results);
			} else {
				mapPosition.put(symbol.getName(),null);
			}

			return mapPosition;
		}
		return null;
	}


	protected Object processValue(HCLValue value) throws HCLParserException {
		if(value.type.equals("string")) {
			return value.value;
		} else if (value.type.equals("boolean")) {
			if(value.value.equals("true")) {
				return new Boolean(true);
			} else {
				return new Boolean(false);
			}
		} else if (value.type.equals("number")) {
			try {
				Double numericalValue = Double.parseDouble((String) (value.value));
				return numericalValue;
			} catch(NumberFormatException ex) {
				throw new HCLParserException("Error Parsing Numerical Value in HCL Attribute ", ex);
			}
		} else {
			throw new HCLParserException("HCL Attribute value not recognized by parser (not implemented yet).");
		}
	}

	protected Object processEvaluation(EvalSymbol evalSymbol) {
//		return null;
		if(evalSymbol instanceof Variable) {
			return evalSymbol;
		} else {
			return null;
		}
	}
}
