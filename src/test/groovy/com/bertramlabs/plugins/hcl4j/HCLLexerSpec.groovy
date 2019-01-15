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
package com.bertramlabs.plugins.hcl4j

import com.bertramlabs.plugins.hcl4j.symbols.Symbol
import spock.lang.Specification

/**
 * @author David Estes
 */
class HCLLexerSpec extends Specification {

	void "should generate symbols from hcl"() {
		given:
		ArrayList<Symbol> rootBlocks = new ArrayList<Symbol>();
		Symbol element;
		def hcl = '''
variables {
test = "value"
}

service "myservice" {
  description = "test"
  info {
    name = "my name"
    maxMemory = 1024
    priority = 0.1
    enabled = true
  }
}
'''
		StringReader reader = new StringReader(hcl);
		HCLLexer lexer = new HCLLexer(reader);
		when:
		lexer.yylex();
		rootBlocks = lexer.elementStack

		println rootBlocks?.collect{[it.getSymbolName(),it.getName()]}
		then:
		rootBlocks.size() == 2
	}

}
