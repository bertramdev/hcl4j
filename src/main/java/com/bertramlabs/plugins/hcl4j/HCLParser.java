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

import com.bertramlabs.plugins.hcl4j.RuntimeSymbols.*;
import com.bertramlabs.plugins.hcl4j.symbols.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	static Logger log = LoggerFactory.getLogger(HCLParser.class);
	//Time to parse the AST Tree into a Map
	protected Map<String,Object> result = new LinkedHashMap<>();
	protected Map<String,Object> variables = new LinkedHashMap<>();
	protected Map<String,Object> dataLookups = new LinkedHashMap<>();
	protected Map<String,HCLFunction> functionRegistry = new LinkedHashMap<>();
	protected Map<String,HCLDataLookup> dataLookupRegistry = new LinkedHashMap<>();

	public HCLParser() {
		HCLBaseFunctions.registerBaseFunctions(this);
		HCLBaseDataLookups.registerBaseFunctions(this);
	}

	/**
	 * Parses Var  files into the variables context (example would be a tfvars file from terraform)
	 * @param input String input containing HCL syntax
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parseVars(String input, Boolean ignoreParseException) throws HCLParserException, IOException {
		HCLParser  varLoader = new HCLParser();
		Map<String,Object> results = varLoader.parse(input,ignoreParseException);
		for(String key : results.keySet()) {
			variables.put(key,results.get(key));
		}
		return variables;
	}

	/**
	 * Registers Function implementations for HCL Common functions. By default the {@link HCLBaseFunctions} are loaded.
	 * Additional functions can be defined via this method for custom method overrides if necessary.
	 * @param functionName the name of the function to be called
	 * @param function the lambda function implementation to be evaluated during the parse
	 */
	public void registerFunction(String functionName, HCLFunction function) {
		this.functionRegistry.put(functionName,function);
	}

	/**
	 * Registers Data Lookup implementations for HCL references. By default, the {@link HCLDataLookup} are loaded.
	 * Additional data lookups can be defined via this method for custom lookup providers if necessary.
	 * @param lookupName the name of the data lookup to be called
	 * @param dataLookup the lambda function implementation to be evaluated during the parse
	 */
	public void registerDataLookup(String lookupName, HCLDataLookup dataLookup) {
		this.dataLookupRegistry.put(lookupName,dataLookup);
	}

	/**
	 * Sets a variable value individually. One can also use the parseTfVars method to load tf vars.
	 * @param variableName the name of the variable being defined
	 * @param value the value of the variable
	 */
	public void setVariable(String variableName, Object value) {
		this.variables.put(variableName,value);
	}

	/**
	 * Sets a Map of variables into the context of the HCLParser for parse runtime operations
	 * @param variableMap A Map of variables to be bulk applied to the parser Context
	 */
	public void setVariables(Map<String,Object> variableMap) {
		for(String key : variableMap.keySet()) {
			variables.put(key,variableMap.get(key));
		}
	}


	/**
	 * Parses terraform configuration language from a String
	 * @param input String input containing HCL syntax
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(String input) throws HCLParserException, IOException {
		return parse(input,false);
	}

	/**
	 * Parses terraform configuration language from a String
	 * @param input String input containing HCL syntax
	 * @param ignoreParserExceptions if set to true, we ignore any parse exceptions and still return the symbol map
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(String input, Boolean ignoreParserExceptions) throws HCLParserException, IOException {
		StringReader reader = new StringReader(input);
		return parse(reader,ignoreParserExceptions);
	}


	/**
	 * Parses terraform syntax as it comes from a File.
	 * @param input A source file to process with a default charset of UTF-8
	 * @param ignoreParserExceptions if set to true, we ignore any parse exceptions and still return the symbol map
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(File input, Boolean ignoreParserExceptions) throws HCLParserException, IOException, UnsupportedEncodingException {
		return parse(input,"UTF-8",ignoreParserExceptions);
	}

	/**
	 * Parses terraform syntax as it comes from a File.
	 * @param input A source file to process with a default charset of UTF-8
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(File input) throws HCLParserException, IOException, UnsupportedEncodingException {
		return parse(input,"UTF-8",false);
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
		return parse(input,charsetName,false);
	}

	/**
	 * Parses terraform syntax as it comes from a File.
	 * @param input A source file to process
	 * @param charsetName The name of a supported charset
	 * @param ignoreParserExceptions if set to true, we ignore any parse exceptions and still return the symbol map
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 * @throws UnsupportedEncodingException If the charset ( UTF-8 by default if unspecified) encoding is not supported
	 */
	public Map<String,Object> parse(File input, String charsetName, Boolean ignoreParserExceptions) throws HCLParserException, IOException {
		InputStream is = null;
		try {
			is = new FileInputStream(input);
			return parse(is,charsetName,ignoreParserExceptions);
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
		return parse(input,charsetName,false);
	}

	/**
	 * Parses terraform syntax as it comes from an input stream. The end user is responsible for ensuring the stream is
	 * closed at the end of the parse operation (commonly via wrapping in a finally block)
	 * @param input Streamable input of text going to the lexer
	 * @param charsetName String lookup of the character set this stream is providing (default UTF-8)
	 * @param ignoreParserExceptions if set to true, we ignore any parse exceptions and still return the symbol map
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 * @throws UnsupportedEncodingException If the charset ( UTF-8 by default if unspecified) encoding is not supported.
	 */
	public Map<String,Object> parse(InputStream input, String charsetName, Boolean ignoreParserExceptions) throws HCLParserException, IOException, UnsupportedEncodingException {

		InputStreamReader reader;
		if(charsetName != null) {
			reader = new InputStreamReader(input,charsetName);
		} else {
			reader = new InputStreamReader(input,"UTF-8");
		}
		return parse(reader,ignoreParserExceptions);
	}

	/**
	 * Parses terraform configuration language from a Reader
	 * @param reader A reader object used for absorbing various streams or String variables containing the hcl code
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(Reader reader) throws HCLParserException, IOException {
		return parse(reader,false);
	}

	/**
	 * Parses terraform configuration language from a Reader
	 * @param reader A reader object used for absorbing various streams or String variables containing the hcl code
	 * @param ignoreParserExceptions if set to true, we ignore any parse exceptions and still return the symbol map
	 * @return Mapped result of object tree coming from HCL (values of keys can be variable).
	 * @throws HCLParserException Any type of parsing errors are returned as this exception if the syntax is invalid.
	 * @throws IOException In the event the reader is unable to pull from the input source this exception is thrown.
	 */
	public Map<String,Object> parse(Reader reader, Boolean ignoreParserExceptions) throws HCLParserException, IOException {
		HCLLexer lexer = new HCLLexer(reader);
		ArrayList<Symbol> rootBlocks;
		dataLookups = new LinkedHashMap<>();
		try {
			lexer.yylex();	
			rootBlocks = lexer.elementStack;


			result = new LinkedHashMap<>();
			Map<String,Object> mapPosition = result;

			for(Symbol currentElement : rootBlocks) {
				processSymbolPass1(currentElement,mapPosition);
			}

			//pass2
			if(result.get("locals") != null) {
				processSymbolPass2(result.get("locals"),result);
			}
			if(result.get("variable") != null) {
				processSymbolPass2(result.get("variable"),result);
			}
			if(result.get("data") != null) {
				processSymbolPass2(result.get("data"),result);
			}
			for(String key : result.keySet()) {
				if(!Objects.equals(key, "variable") && !Objects.equals(key, "data") && !Objects.equals(key, "locals")) {
					processSymbolPass2(result.get(key),result);
				}
			}
		} catch(Exception ex) {
			log.error("Error Parsing HCL...{}",ex.getMessage(),ex);
			if(ignoreParserExceptions != true) { //its nullable so thats why we look at inverse
				throw new RuntimeException(ex);
			}
		}
		return result;
	}


	private Object processSymbolPass1(Symbol symbol, Map<String,Object> mapPosition) throws HCLParserException {

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
					processSymbolPass1(child,mapPosition);
				}
			}
			return mapPosition;
		} else if(symbol instanceof HCLMap) {
			Map<String,Object> nestedMap = new LinkedHashMap<>();
			if(symbol.getChildren() != null) {
				for(Symbol child : symbol.getChildren()) {
					processSymbolPass1(child, nestedMap);
				}
			}
			return nestedMap;
		} else if(symbol instanceof HCLArray) {
			if(symbol.getChildren() != null) {
				List<Object> objectList = new ArrayList<>();
				for(Symbol child : symbol.getChildren()) {
					Map<String,Object> nestedMap = new LinkedHashMap<>();
					Object result = processSymbolPass1(child,nestedMap);
					objectList.add(result);
				}
				return objectList;
			} else {
				return null;
			}
		} else if(symbol instanceof HCLValue) {
			return processValue((HCLValue) symbol);
		} else if(symbol instanceof PrimitiveType) {
			return symbol;
		} else if(symbol instanceof EvalSymbol) {
			return processEvaluation((EvalSymbol) symbol,null);
		} else if(symbol instanceof HCLAttribute) {
			String symName = symbol.getName();
			if(symName == null && ((HCLAttribute) symbol).runtimeName != null) {
				Map<String,Object> nestedMap = new LinkedHashMap<>();
				symName = (String)(processSymbolPass2(((HCLAttribute) symbol).runtimeName,nestedMap));
			}

			if(symbol.getChildren().size() == 1 && symbol.getChildren().get(0) instanceof HCLBlock) {
				Object results = null;
				Map<String,Object> nestedMap = new LinkedHashMap<>();
				results = processSymbolPass1(symbol.getChildren().get(0),nestedMap);
				mapPosition.put(symName,results);
			} else {
				mapPosition.put(symName,symbol);
			}

			return mapPosition;
		}
		return null;
	}

	private Object processSymbolPass2(Object val, Map<String,Object> mapPosition) throws HCLParserException {


		if(val instanceof Map) {
			Map<String, Object> subMap = (Map<String, Object>) (val);
			for (String key : subMap.keySet()) {
				processSymbolPass2(subMap.get(key), subMap);
			}
		} else if(val instanceof ArrayList) {
			ArrayList currentCollection = (ArrayList)(val);
			for(int x=0;x<currentCollection.size();x++) {
				Object obj = currentCollection.get(x);
				Object res = processSymbolPass2(obj,mapPosition);
				if(res != null) {
					currentCollection.set(x,res);
				}
			}
		} else if(val instanceof HCLMap) {
			 HCLMap symbol = (HCLMap)(val);
			Map<String,Object> nestedMap = new LinkedHashMap<>();
			if((symbol.getChildren() != null)) {
				for(Symbol child : symbol.getChildren()) {
					processSymbolPass2(child, nestedMap);
				}
			}
			return nestedMap;
		} else if(val instanceof HCLBlock) {
			 HCLBlock symbol = (HCLBlock)(val);
			Map<String,Object> nestedMap = new LinkedHashMap<>();
			if((symbol.getChildren() != null)) {
				for(Symbol child : symbol.getChildren()) {
					processSymbolPass2(child, nestedMap);
				}
			}
			log.info("Nested Map: {}",nestedMap);
			return nestedMap;
		} else if(val instanceof HCLArray) {
			 HCLArray symbol = (HCLArray)(val);
			if(symbol.getChildren() != null) {

				List<Object> objectList = new ArrayList<>();
				for(Symbol child : symbol.getChildren()) {
					Map<String,Object> nestedMap = new LinkedHashMap<>();
					Object result = processSymbolPass2(child,nestedMap);
					objectList.add(result);
				}
				return objectList;
			} else {
				return null;
			}
		} else if(val instanceof HCLValue) {
			return processValue((HCLValue) val);
		} else if(val instanceof PrimitiveType) {
			return val;
		} else if(val instanceof EvalSymbol) {
			return processEvaluation((EvalSymbol) val,null);
		} else if(val instanceof HCLAttribute || val instanceof GroupedExpression || val instanceof StringInterpolatedExpression) {

			 Symbol symbol = (Symbol) val;

			Map<String,Object> nestedMap = new LinkedHashMap<>();
			if(symbol.getChildren().size() > 0) {
				Object results = null;
				for(int x = 0;x<symbol.getChildren().size();x++) {
					Symbol child = symbol.getChildren().get(x);
					if(child instanceof Operator) {
						switch(child.getName()) {
							case "&&":
							case "||":
								GroupedExpression groupedConditional = new GroupedExpression(null,null,null);
								Symbol nextConditionalElement = symbol.getChildren().get(++x);
								groupedConditional.appendChild(nextConditionalElement);
								while(x < symbol.getChildren().size() - 1 ) {
									nextConditionalElement = symbol.getChildren().get(++x);
									if(!nextConditionalElement.getName().equals("&&") && !nextConditionalElement.getName().equals("||") && !nextConditionalElement.getName().equals("?") && !nextConditionalElement.equals(":")) {
										groupedConditional.appendChild(nextConditionalElement);
									} else {
										--x;
										break;
									}
								}
								Object andResult = processSymbolPass2(groupedConditional,nestedMap);


								if(child.getName().equals("||")) {
									if((results instanceof Boolean && ((Boolean) results) || (!(results instanceof Boolean) && results != null)) || (andResult instanceof Boolean && ((Boolean) andResult) || (!(andResult instanceof Boolean) && andResult != null))) {
										results = true;
									} else {
										results = false;
									}
								} else { //and
									if((results instanceof Boolean && ((Boolean) results) || (!(results instanceof Boolean) && results != null)) && (andResult instanceof Boolean && ((Boolean) andResult) || (!(andResult instanceof Boolean) && andResult != null))) {
										results = true;
									} else {
										results = false;
									}
								}

								break;
							case "?":
								//if left side of result is false we need to skip between the ? and the :
								if(results instanceof Boolean && !((Boolean) results) || results == null ) {
									//skip children until ":" operator
									Symbol nextElement = symbol.getChildren().get(++x);
									while(!(nextElement instanceof Operator) && nextElement.getName() != ":") {
										nextElement = symbol.getChildren().get(++x);
									}
								}
								break;
							case ":":
								//if we got to a colon operator then we need to skip everything after it as its processed with the ? operator above
								x = symbol.getChildren().size();
								break;
							case "==":
								Object compareResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
								if(compareResult == results || (compareResult != null && compareResult.equals(results))) {
									results = true;
								} else {
									results = false;
								}
								break;
							case ">":
								if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = ((Double)results > (Double)rightResult);
									} else {
										//TODO: Exception?
									}
								}
								break;
							case ">=":
								if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = ((Double)results >= (Double)rightResult);
									} else {
										//TODO: Exception?
									}
								}
								break;
							case "<":
								if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = ((Double)results < (Double)rightResult);
									} else {
										//TODO: Exception?
									}
								}
								break;
							case "<=":
								if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = ((Double)results <= (Double)rightResult);
									} else {
										//TODO: Exception?
									}
								}
								break;
							case "!=":
								Object compareResult2 = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
								if(compareResult2 != results && (compareResult2 == null || !compareResult2.equals(results))) {
									results = true;
								} else {
									results = false;
								}
								break;
							case "!":
								Object notResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
								if(notResult == null || notResult.equals(false)) {
									results = true;
								} else {
									results = false;
								}
								break;
							case "+":
								if(results instanceof String) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null) {
										results = (String)results + rightResult.toString();
									} else {
										//TODO: Exception?
									}
								} else if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results + (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								} else if(results == null) {
									results = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
								}
								break;
							case "-":
								if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results - (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								} else if(results == null) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results =  - (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								}
								break;
							case "/":
								if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results / (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								}
								break;
							case "%":
								if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results % (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								}
								break;
							case "*":
								if(results instanceof Double) {
									Object rightResult = processSymbolPass2(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results * (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								}
								break;
						}
					} else {
						results = processSymbolPass2(child,nestedMap);
					}
				}

				if(symbol instanceof GroupedExpression || symbol instanceof StringInterpolatedExpression) {
					return results;
				} else {
					mapPosition.put(symbol.getName(),results);
				}

			} else {
				if(symbol instanceof GroupedExpression || symbol instanceof StringInterpolatedExpression) {
					return null;
				} else {
					mapPosition.put(symbol.getName(),null);
				}

			}
			return mapPosition;
		}
		return null;
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
		} else if(symbol instanceof PrimitiveType) {
			return symbol;
		} else if(symbol instanceof EvalSymbol) {
			return processEvaluation((EvalSymbol) symbol,null);
		} else if(symbol instanceof HCLAttribute || symbol instanceof GroupedExpression) {
			Map<String,Object> nestedMap = new LinkedHashMap<>();
			if(symbol.getChildren().size() > 0) {
				Object results = null;
				for(int x = 0;x<symbol.getChildren().size();x++) {
					Symbol child = symbol.getChildren().get(x);
					if(child instanceof Operator) {
						switch(child.getName()) {
							case "+":
								if(results instanceof String) {
									Object rightResult = processSymbol(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null) {
										results = (String)results + rightResult.toString();
									} else {
										//TODO: Exception?
									}
								} else if(results instanceof Double) {
									Object rightResult = processSymbol(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results + (Double)rightResult;
									} else {
										//TODO: Exception?
									}

								}
								break;
							case "-":
								if(results instanceof Double) {
									Object rightResult = processSymbol(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results - (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								}
								break;
							case "/":
								if(results instanceof Double) {
									Object rightResult = processSymbol(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results / (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								}
								break;
							case "*":
								if(results instanceof Double) {
									Object rightResult = processSymbol(symbol.getChildren().get(++x),nestedMap);
									if(rightResult != null && rightResult instanceof Double) {
										results = (Double)results * (Double)rightResult;
									} else {
										//TODO: Exception?
									}
								}
								break;
						}
					} else {
						results = processSymbol(symbol.getChildren().get(0),nestedMap);
					}
				}

				if(symbol instanceof GroupedExpression) {
					return results;
				} else {
					mapPosition.put(symbol.getName(),results);
				}

			} else {
				if(symbol instanceof GroupedExpression) {
					return null;
				} else {
					mapPosition.put(symbol.getName(),null);
				}

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
		} else if (value.type.equals("null")) {
			return null;
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

	protected Object evaluateFunctionCall(String functionName,Function functionSymbol) throws HCLParserException {
		if(functionRegistry.get(functionName) != null) {
			HCLFunction functionMethod = functionRegistry.get(functionName);
			ArrayList<Object> functionArguments = new ArrayList<>();
			for(Symbol child : functionSymbol.getChildren()) {
				Object elementResult = null;
				if(child instanceof EvalSymbol) {
					elementResult = processEvaluation((EvalSymbol) child,null);
				} else if(child instanceof  Symbol) {
					elementResult = processSymbol(child,result);
				}
				if(elementResult instanceof VariableTree) { //we were unable to evaluate a sub argument... abort function evaluation
					return null; // function evaluation aborted. perhaps throw error for later based on if ignoreExceptions is on or not
				}
				functionArguments.add(elementResult);
			}
			return functionMethod.method(functionArguments);
		} else {
			//TODO: DO we throw a method missing exception at some point
			return null;
		}
	}

	protected Map<String,Object> evaluateDataLookup(String lookupName,String name,Map<String,Object> properties) throws HCLParserException {
		if(dataLookupRegistry.get(lookupName) != null) {
			try {
				HCLDataLookup lookupMethod = dataLookupRegistry.get(lookupName);
				Map mapPosition = (Map)(((Map)(result.get("data"))).get(lookupName));
				processSymbolPass2(mapPosition.get(name),mapPosition);
				Map<String,Object> lookupResults = lookupMethod.method(properties);

				Map<String,Object> dataTypeLookup = (Map<String,Object>)(dataLookups.get(lookupName));
				if(dataTypeLookup == null) {
					dataTypeLookup = new LinkedHashMap<>();
					dataLookups.put(lookupName,dataTypeLookup);
				}
				dataTypeLookup.put(name,lookupResults);
				return lookupResults;
			} catch(Exception ex) {
				log.warn("Error Occurred Performing Data Source Lookup on {} from provider {}: {}",name,lookupName,ex.getMessage(),ex);
			}
			return null;
		} else {
			//TODO: DO we throw a method missing exception at some point
			return null;
		}
	}
	protected Object processEvaluation(EvalSymbol evalSymbol, Object context) throws HCLParserException{
		Boolean variableLookup  = false;
		Boolean dataLookup = false;
		String dataLookupName = null;

		if(evalSymbol instanceof VariableTree) {
			for(int x = 0;x<evalSymbol.getChildren().size();x++) {
				Symbol child = evalSymbol.getChildren().get(x);
				if(child instanceof Variable && evalSymbol.getChildren().size() > x+1 && evalSymbol.getChildren().get(x+1) instanceof Function) {
					//This may not be necessary anymore but is there to catch a function traversal potential error
					return evaluateFunctionCall(child.getName(),(Function)(evalSymbol.getChildren().get(x+1)));
				}else if(child instanceof Variable) {
					if(context == null && x == 0) {
						switch(child.getName()) {
							case "local":
								context = flattenContext(result.get("locals"));
								break;
							case "var":
								variableLookup = true;
								context = variables;
								break;
							case "data":
								log.info("Data Lookup Detected!");
								dataLookup = true;
								context = dataLookups;
								break;
							default:
								context = result.get(child.getName());
						}

					} else if(context != null){
						if(dataLookup && dataLookupName == null) {
							dataLookupName = child.getName();
						}
						context = ((Map)context).get(child.getName());
						if(variableLookup && context == null) {
							if(result.get("variable") != null && result.get("variable") instanceof Map) {
								Map variableDefinitions = (Map)( result.get("variable"));
								if(variableDefinitions.get(child.getName()) instanceof Map) {
									Map varDefinition = (Map)(variableDefinitions.get(child.getName()));
									if(varDefinition != null) {
										return varDefinition.get("default");
									} else {
										return null;
									}
								} else {
									return variableDefinitions.get(child.getName());
								}


							}
						}
					} else {
						//if we have not cached a data lookup call we run it this way
						if(dataLookup && dataLookupName == null) {
							dataLookupName = child.getName();
						} else if (dataLookup && dataLookupName != null) {
							if(result.get("data") != null && result.get("data") instanceof Map) {
								Map dataDefinitions = (Map)(result.get("data"));
								if(dataDefinitions.get(dataLookupName) instanceof Map) {
									Map dataLookupDefinition = (Map) dataDefinitions.get(dataLookupName);
									if(dataLookupDefinition.get(child.getName()) instanceof Map) {
										//we have a data lookup definition we need to run
										context = evaluateDataLookup(dataLookupName,child.getName(),(Map<String,Object>)(dataLookupDefinition.get(child.getName())));
									}
								}
							}
						}
					}

				} else if(child instanceof HCLArray) {
					//TODO: If there are more elements throw exception
					Symbol firstElement = child.getChildren().get(0);
					Object elementResult = null;
					if(firstElement instanceof EvalSymbol) {
						elementResult = processEvaluation((EvalSymbol) firstElement,null);
					} else if(firstElement instanceof  HCLValue) {
						elementResult = processValue((HCLValue)firstElement);
					}

					if(elementResult != null && elementResult instanceof String && context != null) {
						context = ((Map)context).get(elementResult);
						if(variableLookup && context == null ) {
							if(result.get("variable") != null && result.get("variable") instanceof Map) {
								Map variableDefinitions = (Map)( result.get("variable"));
								if(variableDefinitions.get(elementResult) instanceof Map) {
									Map varDefinition = (Map)(variableDefinitions.get(elementResult));
									if(varDefinition != null) {
										return varDefinition.get("default");
									} else {
										return null;
									}
								} else {
									return variableDefinitions.get(elementResult);
								}


							}
						}

					} else if (elementResult != null && elementResult instanceof Double && context != null) {
						//index position
						Double elementDouble = (Double)elementResult;
						if(context instanceof Map) {
							context = ((Map)context).get(elementDouble.intValue());
						} else {
							context = ((List)context).get(elementDouble.intValue());	
						}
					} else {
						context = null;
					}

				}
			}
			if(context != null) {
				if(context instanceof HCLAttribute) {
					LinkedHashMap<String,Object> nestedMap = new LinkedHashMap<>();
					context = processSymbolPass2(context,nestedMap);
					//not my favorite way to grab this value but works for now
					for(String key : nestedMap.keySet()) {
						context = nestedMap.get(key);
					}
				}
				return context;
			} else {
				return evalSymbol;
			}

		}
		else if(evalSymbol instanceof Function) {
			Function thisFunction = (Function)evalSymbol;
			if(thisFunction.getName() != null && thisFunction.getName().length() > 0) {
				return evaluateFunctionCall(thisFunction.getName(),thisFunction);
			} else {
				return null;
			}
		}
		else if(evalSymbol instanceof Variable || evalSymbol instanceof ComputedTuple || evalSymbol instanceof ComputedObject) {
			return evalSymbol;
		} else {
			return null;
		}
	}

	protected Object flattenContext(Object context) {
		if(context == null) {
			return null;
		} else if(context instanceof Collection) {
			LinkedHashMap<String,Object> contextMap = new LinkedHashMap<>();
			for(Object contextObj : (Collection)context) {
				if(contextObj instanceof Map) {
					Map<String,Object> currentContextMap = (Map<String,Object>)contextObj;
					for(String key : currentContextMap.keySet()) {
						contextMap.put(key,currentContextMap.get(key));
					}
				}
			}
			return contextMap;
		} else {
			return context;
		}
	}
}