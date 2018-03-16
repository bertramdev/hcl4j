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
import com.bertramlabs.plugins.hcl4j.symbols.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

/**
 * This class is a HCL lexer Generated from jflex.
 * @author David Estes
 */

%%

%class HCLLexer
%unicode
%line
%column
%char
%type Symbol

%yylexthrow{
HCLParserException
%yylexthrow}

%{
  StringBuffer string = new StringBuffer();
  String endOfMultiLineSymbol;
  Boolean isMultiLineFirstNewLine = true;
  int curleyBraceCounter = 0;
  int interpolatedCurleyBraceCounter = 0;
  HCLValue currentValue;
  String currentMapKey;
  ArrayList<Symbol> elementStack = new ArrayList<Symbol>();
  ArrayList<String> blockNames = null;
  Boolean inMap = false;
  Boolean fromMapKey = false;
  HCLAttribute attribute;

  HCLBlock currentBlock = null;
  private Symbol hclBlock(List<String> blockNames) {
  	HCLBlock block = new HCLBlock(blockNames,currentBlock,yyline,yycolumn-1,yychar-1);
    if(currentBlock == null) {
    	elementStack.add(block);
    } else {
    	currentBlock.appendChild(block);
    }
    currentBlock = block;
	return currentBlock;
  }

  private Symbol exitBlock() {
  	Symbol result = null;
  	if(currentBlock != null) {
  		if(currentBlock.getParent() == null) {
  			result = currentBlock;
  		}
  		currentBlock = (HCLBlock) currentBlock.getParent();
  	}
  	return result;
  }


  private Symbol exitAttribute() {
  	if(currentBlock == null) {
  		yybegin(YYINITIAL);
  		Symbol result = attribute;
  		attribute = null;
  		return result;
  	} else {
  		currentBlock.appendChild(attribute);
  		attribute = null;
  		yybegin(HCLINBLOCK);
  		return null;
  	}
  }


%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]
WhiteSpaceOpt = [ \t\f]+?
WhiteSpaceSL = [ \t\f]+

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment} | {EndOfLineCommentHash} | {DocumentationComment}


TraditionalComment   = "/*" [^*]+ ~"*/" | "/*" "*"+ "/"
// Comment can be the last line of the file, without line terminator.
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}?
EndOfLineCommentHash     = "#" {InputCharacter}* {LineTerminator}?
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )* 

AnyChar = [^]

Identifier = [:jletter:] [:jletterdigit:]*
True = true
False = false
DigitValue = [0-9\.\-]+

HCLAttributeName = [:jletter:] [a-zA-Z0-9\-\_]*

HCLBlock = {HCLAttributeName} {HCLBlockAttribute}* "{" [^]* "}" | {HCLAttributeName} {WhiteSpaceOpt} "{" [^]* "}"

HCLBlockAttribute = {WhiteSpaceOpt} "\"" {HCLDoubleStringCharacters} "\"" {WhiteSpaceOpt} | {WhiteSpace} "\'" {HCLSingleStringCharacters} "\'" {WhiteSpaceOpt}

HCLAttribute = {HCLAttributeName} {WhiteSpaceOpt} "="

HCLAttributeValue = "\"" {HCLDoubleStringCharacters} "\"" | "\'" {HCLSingleStringCharacters} "\'" | {True} | {False} | {DigitValue} | [\[] {AnyChar} [\]] | [\{] {AnyChar} [\}]

MapKeyDef = {MapKey} ":"
MapKey = {HCLAttributeName} | "\"" {HCLDoubleStringCharacters} "\""

HCLDoubleStringCharacters = {HCLDoubleStringCharacter}*
HCLSingleStringCharacters = {HCLSingleStringCharacter}*
HCLDoubleStringCharacter = [^\n\r]
HCLSingleStringCharacter = [^\']
EscapedInterpolation = [\$] [\$]
InterpolationSyntax = [\$]
MLineStart = [\<] [\<] {HCLAttributeName}

/* Children */

JSXText = {JSXTextCharacter}+
JSXTextCharacter = [^\{\}\<\>]
AssignmentExpression = [^]

%state STRINGDOUBLE
%state STRINGSINGLE
%state HCLINBLOCK
%state HCLBLOCKHEADER
%state HCLBLOCKATTRIBUTES
%state HCLATTRIBUTE
%state HCLATTRIBUTEVALUE
%state HCLARRAY
%state HCLMAP
%state HCLMAPKEY
%state HCLMAPKEYDEF
%state HCLMAPVALUE
%state STRINGINTERPOLATED
%state MULTILINESTRING

%%

/* keywords */
<YYINITIAL> {
  /* identifiers */ 
  {HCLBlock}        {yybegin(HCLBLOCKHEADER);yypushback(yylength()); }
  {HCLAttribute}	{yybegin(HCLATTRIBUTE);yypushback(yylength()); }
  /* comments */
  {Comment}                      { /* ignore */ }
 
  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }
  {AnyChar}                      { /* ignore */ }
}

<STRINGDOUBLE, STRINGSINGLE> {
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }
  \\r                            { string.append('\r'); }
  \\                             { string.append('\\'); }
}
<STRINGDOUBLE> {

  \"                             { if(blockNames != null) { blockNames.add(string.toString());  yybegin(HCLBLOCKATTRIBUTES); } else if(currentValue != null && currentValue instanceof HCLMap) { if(currentMapKey == null) { currentMapKey = string.toString() ; yybegin(HCLMAPKEYDEF); } else { ((HCLMap)currentValue).add(currentMapKey,new HCLValue("string",string.toString())); currentMapKey = null; yybegin(HCLMAP); }} else if(currentValue != null) { ((HCLArray)currentValue).add(new HCLValue("string",string.toString())); yybegin(HCLARRAY); } else if(attribute != null) { attribute.setValue(new HCLValue("string",string.toString()));  Symbol attr = exitAttribute(); if(attr != null) { return attr;} } else { throw new HCLParserException("String block found outside of block or attribute assignment."); } }
  \\\"                           { string.append('\"'); }
  {EscapedInterpolation}         { string.append( yytext() );}
  {InterpolationSyntax}          { string.append(yytext()); yybegin(STRINGINTERPOLATED); }
  [^\$\n\r\"\\]+                   { string.append( yytext() ); }
}

<STRINGSINGLE> {
  [^\n\r\'\\]+                   { string.append( yytext() ); }
  \'                             { if(blockNames != null) { blockNames.add(string.toString());  yybegin(HCLBLOCKATTRIBUTES); } else if(attribute != null) { attribute.setValue(new HCLValue("string",string.toString())) ; Symbol attr = exitAttribute(); if(attr != null) { return attr;} } }
  \\'                            { string.append('\''); }
}

<MULTILINESTRING> {
	[\r\n]					   { if(isMultiLineFirstNewLine) {isMultiLineFirstNewLine = false; } else {string.append( yytext() );} }
	[^\n\r]+                   { if(yytext().equals(endOfMultiLineSymbol)) { endOfMultiLineSymbol = null; if(blockNames != null) { blockNames.add(string.toString());  yybegin(HCLBLOCKATTRIBUTES); } else if(currentValue != null && currentValue instanceof HCLMap) { if(currentMapKey == null) { currentMapKey = string.toString() ; yybegin(HCLMAPKEYDEF); } else { ((HCLMap)currentValue).add(currentMapKey,new HCLValue("string",string.toString())); currentMapKey = null; yybegin(HCLMAP); }} else if(currentValue != null) { ((HCLArray)currentValue).add(new HCLValue("string",string.toString())); yybegin(HCLARRAY); } else if(attribute != null) { attribute.setValue(new HCLValue("string",string.toString())) ; Symbol attr = exitAttribute(); if(attr != null) { return attr;} } else { throw new HCLParserException("String block found outside of block or attribute assignment."); }} else {string.append( yytext() );} }
}

<STRINGINTERPOLATED> {
  \}                             { string.append(yytext()); if(interpolatedCurleyBraceCounter > 1) {interpolatedCurleyBraceCounter--;} else { interpolatedCurleyBraceCounter--; yybegin(STRINGDOUBLE);} }
  \{                             { string.append(yytext()); interpolatedCurleyBraceCounter++; }
  \"							 {string.append(yytext());}
  [^\{\}\n\r\"\\]+                   { string.append( yytext() ); }
}

<HCLBLOCKHEADER> {
  {HCLAttributeName}               {yybegin(HCLBLOCKATTRIBUTES);blockNames = new ArrayList<String>(); blockNames.add(yytext());}
  /* WhiteSpacespace */
  {WhiteSpace}                   { /* ignore */ }
}

<HCLBLOCKATTRIBUTES> {
	\{                             { curleyBraceCounter++ ; hclBlock(blockNames) ; blockNames = null ; yybegin(HCLINBLOCK); }
	\"                             {yybegin(STRINGDOUBLE); string.setLength(0);}
	{WhiteSpace}                   { /* ignore */ }
}

<HCLINBLOCK> {
	{HCLBlock}                     {yybegin(HCLBLOCKHEADER);yypushback(yylength()); }
	{HCLAttribute}				   {yybegin(HCLATTRIBUTE);yypushback(yylength()); }
	/* comments */
	{Comment}                      { /* ignore */ }
	\}							   { if(curleyBraceCounter > 1) { curleyBraceCounter--; exitBlock();} else if (curleyBraceCounter > 0) { curleyBraceCounter--; yybegin(YYINITIAL); return exitBlock();}}
	/* whitespace */
	{WhiteSpace}                   { /* ignore */ }
}

<HCLATTRIBUTE> {
	{HCLAttributeName}             {attribute = new HCLAttribute(yytext(),null,yyline,yycolumn,yychar); }
  	\=                              {yybegin(HCLATTRIBUTEVALUE); }
  	/* whitespace */
  	{WhiteSpace}                   { /* ignore */ }	
}


<HCLMAP> {

	{MapKeyDef}                    { yypushback(yylength()); yybegin(HCLMAPKEY); }
	,							   { /* should probably process this but due to simplicity we dont need to */ }
	\}							   { if(currentValue.parent != null && currentValue.parent instanceof HCLMap) { currentValue = currentValue.parent; currentMapKey = null; } else if(currentValue.parent != null && currentValue.parent instanceof HCLArray) { yybegin(HCLARRAY); currentValue = currentValue.parent; } else if(attribute != null && currentValue.parent == null) { attribute.setValue(currentValue) ; currentValue = null; Symbol attr = exitAttribute(); if(attr != null) { return attr;} }}
    {WhiteSpace}                   { /* ignore */ }
}
<HCLMAPVALUE> {
		\[                             { currentValue = new HCLArray((HCLMap)currentValue, currentMapKey); yybegin(HCLARRAY);/* process an array */ }
		\{							   { currentValue = new HCLMap((HCLMap)currentValue, currentMapKey); yybegin(HCLMAP); }
		\"                             {yybegin(STRINGDOUBLE); string.setLength(0); }
		{MLineStart}				   {yybegin(MULTILINESTRING); isMultiLineFirstNewLine = true; string.setLength(0) ; endOfMultiLineSymbol = yytext().substring(2);}
    	{True}						   { ((HCLMap)currentValue).add(currentMapKey,new HCLValue("boolean","true")); currentMapKey = null; yybegin(HCLMAP); }
    	{False}						   { ((HCLMap)currentValue).add(currentMapKey,new HCLValue("boolean","false")); currentMapKey = null; yybegin(HCLMAP); }
    	{DigitValue}				   { ((HCLMap)currentValue).add(currentMapKey,new HCLValue("number",yytext())); currentMapKey = null; yybegin(HCLMAP); }
    	{WhiteSpace}                   { /* ignore */ }
}

<HCLMAPKEYDEF> {
{MapKey}                           { yybegin(HCLMAPKEY); yypushback(yylength()); }
":"                                { yybegin(HCLMAPVALUE); }
{WhiteSpace}                       { /* ignore */ }
}

<HCLMAPKEY> {
	\"                             {yybegin(STRINGDOUBLE); string.setLength(0); fromMapKey = true; }
	{HCLAttributeName}             { currentMapKey = yytext() ; yybegin(HCLMAPKEYDEF);}
	{WhiteSpace}                   { /* ignore */ }
}

<HCLARRAY> {
		\[                             { currentValue = new HCLArray((HCLArray)currentValue); yybegin(HCLARRAY);/* process an array */ }
		\{							   { currentValue = new HCLMap((HCLArray)currentValue); yybegin(HCLMAP); }
		\"                             {yybegin(STRINGDOUBLE); string.setLength(0); }
		{MLineStart}				   {yybegin(MULTILINESTRING); isMultiLineFirstNewLine = true; string.setLength(0) ; endOfMultiLineSymbol = yytext().substring(2);}
    	{True}						   { ((HCLArray)currentValue).add(new HCLValue("boolean","true")); }
    	{False}						   { ((HCLArray)currentValue).add(new HCLValue("boolean","false")); }
    	{DigitValue}				   { ((HCLArray)currentValue).add(new HCLValue("number",yytext())); }
    	\]							   { if(currentValue.parent != null && currentValue.parent instanceof HCLArray) { currentValue = currentValue.parent; } else if(currentValue.parent != null && currentValue.parent instanceof HCLMap) {  currentValue = currentValue.parent; yybegin(HCLMAP); } else if(attribute != null && currentValue.parent == null) { attribute.setValue(currentValue) ; currentValue = null; Symbol attr = exitAttribute(); if(attr != null) { return attr;} } }
    	,							   { /* should probably process this but due to simplicity we dont need to */ }
    	{WhiteSpace}                   { /* ignore */ }
}


<HCLATTRIBUTEVALUE> {
	\[                             { currentValue = new HCLArray(); yybegin(HCLARRAY);/* process an array */ }
	\{							   { currentValue = new HCLMap() ; inMap = true; yybegin(HCLMAP);}
	\"                             {yybegin(STRINGDOUBLE); string.setLength(0); }
	{MLineStart}				   {yybegin(MULTILINESTRING) ; isMultiLineFirstNewLine = true ; string.setLength(0) ; endOfMultiLineSymbol = yytext().substring(2);}
	{True}						   { attribute.setValue(new HCLValue("boolean","true")) ; currentBlock.appendChild(attribute); Symbol attr = exitAttribute(); if(attr != null) { return attr;} }
	{False}						   { attribute.setValue(new HCLValue("boolean","false")) ; currentBlock.appendChild(attribute);  Symbol attr = exitAttribute(); if(attr != null) { return attr;}}
	{DigitValue}				   { attribute.setValue(new HCLValue("number",yytext())) ; currentBlock.appendChild(attribute);  Symbol attr = exitAttribute(); if(attr != null) { return attr;}}
	{Comment}                      { /* ignore */ }
	{WhiteSpace}                   { /* ignore */ }

}



/* error fallback */
    [^]                              { throw new HCLParserException("Illegal character <"+
                                                        yytext() + yystate()+"> found on line: " + (yyline+1) + " col: " + (yycolumn+1) ); }
