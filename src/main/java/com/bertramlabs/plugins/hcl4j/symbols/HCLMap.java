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

import java.util.LinkedHashMap;
import java.util.Map;

public class HCLMap extends HCLValue {
	public String parentKey;

	public HCLMap() {
		super("map",null);

		this.value = new LinkedHashMap<String,HCLValue>();
	}

	public HCLMap(HCLArray parentArray) {
		super("map",null);
		this.parent = parentArray;

		parentArray.add(this);
		this.value = new LinkedHashMap<String,HCLValue>();
	}
	public HCLMap(HCLMap parent, String parentKey) {
		super("map",null);
		this.parent = parent;
		this.parentKey = parentKey;
		parent.add(parentKey,this);
		this.value = new LinkedHashMap<String,HCLValue>();
	}

	public void add(String key, HCLValue val) {
		((Map<String,HCLValue>)value).put(key,val);
	}
}
