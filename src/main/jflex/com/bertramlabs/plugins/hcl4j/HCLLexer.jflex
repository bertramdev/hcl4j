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
import com.bertramlabs.plugins.hcl4j.RuntimeSymbols.*;
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
  Boolean isMultilineModified = false;
  Boolean stringAttributeName = false;
  int curleyBraceCounter = 0;
  int interpolatedCurleyBraceCounter = 0;
  Symbol currentValue;
  String currentMapKey;
  public ArrayList<Symbol> elementStack = new ArrayList<Symbol>();
  ArrayList<String> blockNames = null;
  Boolean inMap = false;
  Boolean fromMapKey = false;
  HCLAttribute attribute;
  SubTypePrimitiveType subTypePrimitiveType;
  Integer primitiveDepth = 0;

  Symbol currentBlock = null;
  private Symbol hclBlock(List<String> blockNames) {
    //System.out.println("Starting Block");
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
    //System.out.println("Leaving Block");
    Symbol result = null;
    if(currentBlock != null) {
      if(currentBlock.getParent() == null) {
        result = currentBlock;
      }
      currentBlock =  currentBlock.getParent();
    }
    return result;
  }

  private void startAttribute(String name) {
        //System.out.println("Starting Attribute");

    HCLAttribute currentAttribute = new HCLAttribute(name,yyline,yycolumn,yychar);
    if(currentBlock == null) {
      elementStack.add(currentAttribute);
    } else {
      currentBlock.appendChild(currentAttribute);
    }
    currentBlock = currentAttribute;
    attribute = currentAttribute;
  }

  private void startMap() {
    HCLMap currentAttribute = new HCLMap(yyline,yycolumn,yychar);
        if(currentBlock == null) {
          elementStack.add(currentAttribute);
        } else {
          currentBlock.appendChild(currentAttribute);
        }
        currentBlock = currentAttribute;
  }

  private void startArray() {
        HCLArray currentAttribute = new HCLArray(yyline,yycolumn,yychar);
            if(currentBlock == null) {
              elementStack.add(currentAttribute);
            } else {
              currentBlock.appendChild(currentAttribute);
            }
            currentBlock = currentAttribute;
            yybegin(HCLARRAY);
  }


  private Symbol exitAttribute(Boolean force) {

    if(currentBlock == null) {
      yybegin(YYINITIAL);
      Symbol result = attribute;
      attribute = null;
      exitBlock();
      return result;
    } else {
      attribute = null;
    if((!(currentBlock instanceof HCLArray) && !(currentBlock instanceof HCLMap)) || force == true) {
      exitBlock();
    }
      if(currentBlock instanceof HCLBlock) {
        yybegin(HCLINBLOCK);
      } else if(currentBlock instanceof HCLArray) {
        yybegin(HCLARRAY);
      } else if(currentBlock instanceof HCLMap) {
        yybegin(HCLMAP);
      } else if(currentBlock instanceof HCLAttribute) {
      exitAttribute();
      } else {
        yybegin(YYINITIAL);
      }
      return null;
    }
  }

  private Symbol exitAttribute() {
    return exitAttribute(false);
  }

     private void startEvalExpression() {
        yypushback(yylength());
      yybegin(EVALUATEDEXPRESSION);
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


Identifier = [:jletter:] [a-zA-Z0-9\-\_]*
IdentifierTree = [:jletter:] [a-zA-Z0-9\-\_\.]*
GetAttr = "." {Identifier}
Function = [:jletter:] [a-zA-Z0-9\-\_]*\(
Arguments = ({Expression} ("," {Expression})* ("," | "...")?)
FunctionCall = {Identifier} "(" {Arguments}? ")"

ArrayModifier = [:jletter:] [a-zA-Z0-9\-\_]*\[
Property = [:jletter:] [a-zA-Z0-9\-\_]*\.
EvaluatedExpression = [\(] | {IdentifierTree} | {ArrayModifier} | {Function} | {Identifier}

True = true
False = false
Null = null

StringPrimitive = string
NumberPrimitive = number
BooleanPrimitive = bool
ListPrimitive = list | list*\(
MapPrimitive = map | map*\(
SetPrimitive = set | set*\(

DigitValue = [0-9\.\-]+

HCLAttributeName = [:jletter:] [a-zA-Z0-9\-\_]*
HCLQuotedPropertyName = [\"] [^\r\n]+ [\"]

HCLBlock = {HCLAttributeName} {HCLBlockAttribute}* "{" [^]* "}" | {HCLAttributeName} {WhiteSpaceOpt} "{" [^]* "}"

HCLBlockAttribute = {WhiteSpaceOpt} "\"" {HCLDoubleStringCharacters} "\"" {WhiteSpaceOpt} | {WhiteSpace} "\'" {HCLSingleStringCharacters} "\'" {WhiteSpaceOpt}

HCLAttribute = {HCLAttributeName} {WhiteSpaceOpt} "=" | {HCLQuotedPropertyName} {WhiteSpaceOpt} "="

MapKeyDef = {MapKey} ":"
MapKey = {HCLAttributeName} | "\"" {HCLDoubleStringCharacters} "\""
MapBlockStart = "{" {WhiteSpaceOpt} {MapKeyDef}

HCLDoubleStringCharacters = {HCLDoubleStringCharacter}*
HCLSingleStringCharacters = {HCLSingleStringCharacter}*
HCLDoubleStringCharacter = [^\r\n]
HCLSingleStringCharacter = [^\']
EscapedInterpolation = [\$] [\$]
InterpolationSyntax = [\$] "{"
MLineModifierStart = [\<] [\<] [\-\~] {HCLAttributeName}
MLineStart = [\<] [\<] [\ ]? {HCLAttributeName}


ExprTerm = {True} | {False} | {Null} | {DigitValue} | {Identifier} | {FunctionCall}
Expression = {ExprTerm} | {Operation} | {Conditional}
/*For Expression*/
ForObjExpr = \{ [\n\t\f\r ]* {ForIntro}
ForTupleExpr = \[ [\n\t\f\r ]* {ForIntro}
ForExpr =  {ForObjExpr} | {ForTupleExpr}

ForIntro = "for" {WhiteSpaceSL} {Identifier}
//ForExpr = {forTupleExpr} | {forObjectExpr};
//forTupleExpr = "[" {forIntro} {Expression} {forCond}? "]"
//forObjectExpr = "{" {forIntro} {Expression} "=>" {Expression} "..."? {forCond}? "}"
//forIntro = "for" {Identifier} ("," {Identifier})? "in" {Expression} ":"
//forCond = "if" {Expression}

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
%state EVALUATEDEXPRESSION
%state FORLOOPEXPRESSION
%state FORTUPLEEXPRESSION
%state FOROBJECTEXPRESSION
%state SUBTYPEPRIMITIVETYPE

%%

/* keywords */
<YYINITIAL> {
  /* identifiers */ 
  {HCLBlock}        {yybegin(HCLBLOCKHEADER);yypushback(yylength()); }
  {HCLAttribute}  {yybegin(HCLATTRIBUTE);yypushback(yylength()); }
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

  \"                             { if(blockNames != null) { blockNames.add(string.toString());  yybegin(HCLBLOCKATTRIBUTES); } else if(currentBlock != null && currentBlock instanceof HCLMap && currentMapKey == null) { currentMapKey = string.toString() ; yybegin(HCLMAPKEYDEF); } else if (stringAttributeName) { stringAttributeName = false ; yybegin(HCLATTRIBUTE); startAttribute(string.toString());} else if(currentBlock != null) { currentBlock.appendChild(new HCLValue("string",string.toString(),yyline,yycolumn,yychar));  exitAttribute(); } else { throw new HCLParserException("String block found outside of block or attribute assignment."); } }
  \\\"                           { string.append('\"'); }
  {EscapedInterpolation}         { string.append( yytext() );}
  {InterpolationSyntax}          { string.append('$');yypushback(yylength()-1); yybegin(STRINGINTERPOLATED); }
  \$[^\{\$\"]                      { string.append( yytext() ); }
  \$\"                            { string.append( "$" ); yypushback(yylength()-1); }
  [^\$\n\r\"\\]+                 { string.append( yytext() ); }
}

<STRINGSINGLE> {
  [^\n\r\'\\]+                   { string.append( yytext() ); }
  \'                             { if(blockNames != null) { blockNames.add(string.toString());  yybegin(HCLBLOCKATTRIBUTES); } else if(currentBlock != null && currentBlock instanceof HCLMap && currentMapKey == null) { currentMapKey = string.toString() ; yybegin(HCLMAPKEYDEF); } else if (stringAttributeName) { stringAttributeName = false ; yybegin(HCLATTRIBUTE); startAttribute(string.toString());} else if(currentBlock != null) { currentBlock.appendChild(new HCLValue("string",string.toString(),yyline,yycolumn,yychar));  exitAttribute(); } else { throw new HCLParserException("String block found outside of block or attribute assignment."); } }
  \\'                            { string.append('\''); }
}

<MULTILINESTRING> {
  {LineTerminator}             { if(isMultiLineFirstNewLine) {isMultiLineFirstNewLine = false; } else {string.append( yytext() );} }
  [^\n\r]+                   { if(yytext().trim().equals(endOfMultiLineSymbol)) { endOfMultiLineSymbol = null; if(blockNames != null) { blockNames.add(string.toString());  yybegin(HCLBLOCKATTRIBUTES); } else if(attribute != null) { attribute.appendChild(new HCLValue("string",string.toString(),yyline,yycolumn,yychar)) ; exitAttribute(); } else { throw new HCLParserException("String block found outside of block or attribute assignment."); }} else {string.append( isMultilineModified ? yytext().trim() : yytext() );} }
}

<STRINGINTERPOLATED> {
  \}                             { string.append(yytext()); if(interpolatedCurleyBraceCounter > 1) {interpolatedCurleyBraceCounter--;} else { interpolatedCurleyBraceCounter--; yybegin(STRINGDOUBLE);} }
  \{                             { string.append(yytext()); interpolatedCurleyBraceCounter++; }
  \"               {string.append(yytext());}
  [^\{\}\"\\]+                   { string.append( yytext() ); }
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
  {HCLAttribute}           {yybegin(HCLATTRIBUTE);yypushback(yylength()); }
  /* comments */
  {Comment}                      { /* ignore */ }
  \}                 { exitAttribute();}
  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }
}

<HCLATTRIBUTE> {
  \"                             {yybegin(STRINGDOUBLE); stringAttributeName = true ;string.setLength(0);}
  {HCLAttributeName}             {startAttribute(yytext());}
    \=                              {yybegin(HCLATTRIBUTEVALUE); }
    /* whitespace */
    {WhiteSpace}                   { /* ignore */ } 
}


<HCLMAP> {

  {MapKeyDef}                    { yypushback(yylength()); yybegin(HCLMAPKEY); }
  ,                { /* should probably process this but due to simplicity we dont need to */ }
  \}                 { exitAttribute(true); }
    {WhiteSpace}                   { /* ignore */ }
}


<HCLMAPKEYDEF> {
{MapKey}                           { yybegin(HCLMAPKEY); yypushback(yylength()); }
":"                                { startAttribute(currentMapKey); currentMapKey = null ; yybegin(HCLATTRIBUTEVALUE); }
{Comment}                      { /* ignore */ }
{WhiteSpace}                       { /* ignore */ }
}

<HCLMAPKEY> {
  \"                             {yybegin(STRINGDOUBLE); string.setLength(0); fromMapKey = true; }
  {HCLAttributeName}             { currentMapKey = yytext() ; yybegin(HCLMAPKEYDEF);}
  {WhiteSpace}                   { /* ignore */ }
}

<HCLARRAY> {
    [^,\]\r\n\ \t]                 { yypushback(yylength()); yybegin(HCLATTRIBUTEVALUE); }
      \]                 { exitAttribute(true); }
      ,                { /* should probably process this but due to simplicity we dont need to */ }
      {Comment}                      { /* ignore */ }
      {WhiteSpace}                   { /* ignore */ }
}


<HCLATTRIBUTEVALUE> {
  {ForExpr}               { yybegin(FORLOOPEXPRESSION); yypushback(yylength()); }
  \[                      { startArray();/* process an array */ }
  {MapBlockStart}         { startMap(); yypushback(yylength()-1) ; yybegin(HCLMAP);}
  \{                      {  blockNames = new ArrayList<String>(); blockNames.add(currentBlock.getName()); curleyBraceCounter++ ; hclBlock(blockNames) ; blockNames = null ; attribute = null ; yybegin(HCLINBLOCK); }
  \"                      {yybegin(STRINGDOUBLE); string.setLength(0); }
  {MLineModifierStart}    {yybegin(MULTILINESTRING) ; isMultiLineFirstNewLine = true ;isMultilineModified = true; string.setLength(0) ; endOfMultiLineSymbol = yytext().substring(3);}
  {MLineStart}            {yybegin(MULTILINESTRING) ; isMultiLineFirstNewLine = true ;isMultilineModified = true; string.setLength(0) ; endOfMultiLineSymbol = yytext().substring(2).trim();}
  {True}                  { currentBlock.appendChild(new HCLValue("boolean","true",yyline,yycolumn,yychar)) ; exitAttribute(); }
  {False}                 { currentBlock.appendChild(new HCLValue("boolean","false",yyline,yycolumn,yychar)) ; exitAttribute(); }
  {Null}                  { currentBlock.appendChild(new HCLValue("null",null,yyline,yycolumn,yychar)) ; exitAttribute(); }
  {DigitValue}            { currentBlock.appendChild(new HCLValue("number",yytext(),yyline,yycolumn,yychar)) ; exitAttribute(); }
  {StringPrimitive}       { currentBlock.appendChild(new StringPrimitiveType(yyline,yycolumn,yychar)); exitAttribute();}
  {NumberPrimitive}       { currentBlock.appendChild(new NumberPrimitiveType(yyline,yycolumn,yychar)); exitAttribute();}
  {BooleanPrimitive}      { currentBlock.appendChild(new BooleanPrimitiveType(yyline,yycolumn,yychar)); exitAttribute();}
  {ListPrimitive}         { subTypePrimitiveType = new ListPrimitiveType(null,yyline,yycolumn,yychar); currentBlock.appendChild(subTypePrimitiveType); yybegin(SUBTYPEPRIMITIVETYPE); }
  {SetPrimitive}         { subTypePrimitiveType = new SetPrimitiveType(null,yyline,yycolumn,yychar); currentBlock.appendChild(subTypePrimitiveType); yybegin(SUBTYPEPRIMITIVETYPE); }
  {MapPrimitive}         { subTypePrimitiveType = new MapPrimitiveType(null,yyline,yycolumn,yychar); currentBlock.appendChild(subTypePrimitiveType); yybegin(SUBTYPEPRIMITIVETYPE); }
  {EvaluatedExpression}          { startEvalExpression(); }
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                   { /* ignore */ }

}

<EVALUATEDEXPRESSION> {
  {IdentifierTree}           { currentBlock.appendChild(new Variable(yytext(),yyline,yycolumn,yychar)); exitAttribute();}
  \}                             { yypushback(yylength()); exitAttribute(true);  }
  {LineTerminator}               { exitAttribute(true);  }
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                   { /* ignore */ }
  [^}\n]+                        { /* ignore */ }
}

<FORLOOPEXPRESSION> {
  {ForObjExpr}     { yybegin(FOROBJECTEXPRESSION); yypushback(yylength()-1);}
  {ForTupleExpr}   { yybegin(FORTUPLEEXPRESSION); yypushback(yylength()-1);}
}

<FORTUPLEEXPRESSION> {
    \]                           { exitAttribute(true);  }
  {Comment}                      { /* ignore */ }
    {WhiteSpace}                 { /* ignore */ }
    {LineTerminator}             { /* ignore */ }
  [^\]\n]+                       { /* ignore */ }
}

<FOROBJECTEXPRESSION> {
    \}                           { exitAttribute(true);  }
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                   { /* ignore */ }
  {LineTerminator}               { /* ignore */ }
  [^}\n]+                        { /* ignore */ }
}

<SUBTYPEPRIMITIVETYPE> {
  \(                             { primitiveDepth++ ; }
  \)                             { primitiveDepth--; if(primitiveDepth == 0) {subTypePrimitiveType = null; exitAttribute();} }
  {LineTerminator}               { subTypePrimitiveType = null; exitAttribute(); }
  {StringPrimitive}              { subTypePrimitiveType.subType = new StringPrimitiveType(yyline,yycolumn,yychar); subTypePrimitiveType = null;}
  {NumberPrimitive}              { subTypePrimitiveType.subType = new NumberPrimitiveType(yyline,yycolumn,yychar); subTypePrimitiveType = null;}
  {BooleanPrimitive}             { subTypePrimitiveType.subType = new BooleanPrimitiveType(yyline,yycolumn,yychar); subTypePrimitiveType = null;}
  {MapPrimitive}                 { MapPrimitiveType tmpPrimitive = new MapPrimitiveType(null,yyline,yycolumn,yychar); subTypePrimitiveType.subType = tmpPrimitive; subTypePrimitiveType = tmpPrimitive;}
  {ListPrimitive}                 { ListPrimitiveType tmpPrimitive = new ListPrimitiveType(null,yyline,yycolumn,yychar); subTypePrimitiveType.subType = tmpPrimitive; subTypePrimitiveType = tmpPrimitive;}
  {SetPrimitive}                 { SetPrimitiveType tmpPrimitive = new SetPrimitiveType(null,yyline,yycolumn,yychar); subTypePrimitiveType.subType = tmpPrimitive; subTypePrimitiveType = tmpPrimitive;}
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                   { /* ignore */ }
}



/* error fallback */
    [^]                              { throw new HCLParserException("Illegal character <("+
                                                        yytext()+ ") - state: " + yystate()+"> found on line: " + (yyline+1) + " col: " + (yycolumn+1) ); }
