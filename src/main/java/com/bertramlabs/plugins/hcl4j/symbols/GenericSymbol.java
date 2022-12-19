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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.ArrayList;

public abstract class GenericSymbol implements Symbol {
	private Integer line;
	private Integer column;
	private Integer position;
	private Integer length;

	private String name;

	private List<Symbol> children = new ArrayList<Symbol>();
	private List<Symbol> attributes = new ArrayList<Symbol>();

	@JsonIgnore
	private Symbol parent;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getLine() {
		return line;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	public Integer getColumn() {
		return column;
	}

	public Integer getPosition() {
		return position;
	}

	public List<Symbol> getChildren() {
		return children;
	}

	public List<Symbol> getAttributes() {
		return attributes;
	}

	public Symbol getParent() {
		return parent;
	}

	public void setParent(Symbol symbol) {
		this.parent = symbol;
	}

	public void appendChild(Symbol symbol) {
		children.add(symbol); symbol.setParent(this);
	}

	public void appendAttribute(Symbol symbol) {
		attributes.add(symbol);
	}

	public GenericSymbol(String name) {
		this.name = name;
	}

	public GenericSymbol(String name,Integer line, Integer column,Integer position) {
		this.name = name;
		this.line = line;
		this.column = column;
		this.position = position;
	}

	public String toString() {
		return getSymbolName() + ":" + getName();
	}
}
