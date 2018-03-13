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
package com.bertramlabs.plugins.hcl4j.symbols;

public class HCLAttribute  extends GenericSymbol {


	public HCLAttribute(String name, HCLValue value, Integer line, Integer column,Integer position) {
		super(name,value,line,column,position);
		this.value = value;
	}

	public String getSymbolName() {
		return "Attribute";
	}


	public HCLValue value;

}