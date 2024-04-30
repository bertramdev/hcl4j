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

import java.util.List;

public class HCLBlock extends GenericSymbol {

	public HCLBlock(List<String> blockNames, Symbol parent,Integer line, Integer column, Long position) {
		super(blockNames.get(0),line,column,position);
		this.blockNames = blockNames;
		this.setParent(parent);
	}

	public String getSymbolName() {
		return "Block";
	}

	public List<String> blockNames;


}
