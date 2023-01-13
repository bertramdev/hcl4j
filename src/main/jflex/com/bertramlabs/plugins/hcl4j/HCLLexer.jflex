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
  Integer previousState = null;

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

  private void startAttribute(StringInterpolatedExpression expression) {
          //System.out.println("Starting Attribute");

      HCLAttribute currentAttribute = new HCLAttribute(null,yyline,yycolumn,yychar);
      currentAttribute.runtimeName = expression;
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

  private void startInterpolatedString() {
    StringInterpolatedExpression currentAttribute;
    if(currentBlock != null && currentBlock instanceof StringInterpolatedExpression) {
     //we behave different since already in one
        currentAttribute = (StringInterpolatedExpression)currentBlock;
    } else {
        currentAttribute = new StringInterpolatedExpression(yyline,yycolumn,yychar);
        if(currentBlock == null) {
            elementStack.add(currentAttribute);
          } else {
            currentBlock.appendChild(currentAttribute);
          }
    }
    currentAttribute.appendChild(new HCLValue("string",string.toString(),yyline,yycolumn,yychar));
    string.setLength(0);
    currentAttribute.appendChild(new Operator("+",yyline,yycolumn,yychar));
    StringInterpolatedExpression expr = new StringInterpolatedExpression(yyline,yycolumn,yychar);
    currentAttribute.appendChild(expr);


      currentBlock = expr;
      yybegin(HCLATTRIBUTEVALUE);
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


  private void startGroupedExpression() {
          GroupedExpression currentAttribute = new GroupedExpression(yyline,yycolumn,yychar);
              if(currentBlock == null) {
                elementStack.add(currentAttribute);
              } else {
                currentBlock.appendChild(currentAttribute);
              }
              currentBlock = currentAttribute;
              yybegin(HCLATTRIBUTEVALUE);
    }


  private void startVariableTree() {
          VariableTree currentAttribute = new VariableTree(yyline,yycolumn,yychar);
              if(currentBlock == null) {
                elementStack.add(currentAttribute);
              } else {
                currentBlock.appendChild(currentAttribute);
              }
              currentBlock = currentAttribute;
              yypushback(yylength());
              yybegin(VARIABLETREE);
    }


  private void startComputedTuple() {
          ComputedTuple currentAttribute = new ComputedTuple(yytext(),yyline,yycolumn,yychar);
              if(currentBlock == null) {
                elementStack.add(currentAttribute);
              } else {
                currentBlock.appendChild(currentAttribute);
              }
              currentBlock = currentAttribute;
              yybegin(FORTUPLEVARIABLES);
    }

    private void startForConditional() {
        ForConditional currentAttribute = new ForConditional(yyline,yycolumn,yychar);
          if(currentBlock == null) {
            elementStack.add(currentAttribute);
          } else {
            currentBlock.appendChild(currentAttribute);
          }
          currentBlock = currentAttribute;
          yybegin(HCLATTRIBUTEVALUE);
    }

    private void startComputedObject() {
              ComputedObject currentAttribute = new ComputedObject(yytext(),yyline,yycolumn,yychar);
              if(currentBlock == null) {
                elementStack.add(currentAttribute);
              } else {
                currentBlock.appendChild(currentAttribute);
              }
              currentBlock = currentAttribute;
              yybegin(FOROBJVARIABLES);
    }

    private void startFunction() {
            String functionName = yytext();
            if(functionName.endsWith("(")) {
                functionName = functionName.substring(0,functionName.length() - 1);
            }
            Function currentAttribute = new Function(functionName,yyline,yycolumn,yychar);
                if(currentBlock == null) {
                  elementStack.add(currentAttribute);
                } else {
                  currentBlock.appendChild(currentAttribute);
                }
                currentBlock = currentAttribute;
                yybegin(FUNCTIONCALL);
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
        if((!(currentBlock instanceof HCLArray) && !(currentBlock instanceof HCLMap) && !(currentBlock instanceof GroupedExpression) && !(currentBlock instanceof StringInterpolatedExpression) && !(currentBlock instanceof VariableTree) && !(currentBlock instanceof Variable) && !(currentBlock instanceof ComputedTuple) && !(currentBlock instanceof ComputedObject) && !(currentBlock instanceof Function)) || force == true) {
          exitBlock();

        }

      if(currentBlock instanceof Variable || currentBlock instanceof VariableTree) {
        yybegin(VARIABLETREE);
      }  else if(currentBlock instanceof SubTypePrimitiveType) {
         yybegin(SUBTYPEPRIMITIVETYPE);
      } else if(currentBlock instanceof StringInterpolatedExpression) {
                yybegin(HCLATTRIBUTEVALUE);
      } else if(currentBlock instanceof GroupedExpression) {
         yybegin(HCLATTRIBUTEVALUE);
      } else if(currentBlock instanceof ForConditional) {
         yybegin(HCLATTRIBUTEVALUE);
      } else if(currentBlock instanceof ComputedTuple) {
        yybegin(FORTUPLEEXPRESSION);
      } else if(currentBlock instanceof ComputedObject) {
        yybegin(FOROBJECTEXPRESSION);
      } else if(currentBlock instanceof Function) {
         yybegin(FUNCTIONCALL);
      } else if(currentBlock instanceof HCLBlock) {
        yybegin(HCLINBLOCK);
      } else if(currentBlock instanceof HCLArray) {
        yybegin(HCLARRAY);
      } else if(currentBlock instanceof HCLMap) {
        yybegin(HCLMAP);
      } else if(currentBlock instanceof HCLAttribute) {
        yybegin(HCLATTRIBUTEVALUE);
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
WhiteSpaceNLOpt = [ \t\f\r\n]+?

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment} | {EndOfLineCommentHash} | {DocumentationComment}


TraditionalComment   = "/*" [^*]+ ~"*/" | "/*" "*"+ "/"
// Comment can be the last line of the file, without line terminator.
EndOfLineComment     = "//" {InputCharacter}*
EndOfLineCommentHash     = "#" {InputCharacter}*
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )* 

AnyChar = [^]


Identifier = [:jletter:] [a-zA-Z0-9\-\_]*
VariableBracketAccessor = \[ [a-zA-Z\"\'0-9\.\_\-\[\]\(\)]+ \]
IdentifierTree = [:jletter:] [a-zA-Z0-9\-\_\.]* | [:jletter] [a-zA-Z0-9\-\_\.]* {VariableBracketAccessor}+

GetAttr = "." {Identifier}
Function = [:jletter:] [a-zA-Z0-9\-\_]*\(
Arguments = ({Expression} ("," {Expression})* ("," | "...")?)
FunctionCall = {Identifier} "("

ArrayModifier = [:jletter:] [a-zA-Z0-9\-\_]*\[
Property = [:jletter:] [a-zA-Z0-9\-\_]*\.
EvaluatedExpression = [\(] | {IdentifierTree} | {ArrayModifier} | {Function} | {Identifier}

True = true
False = false
Null = null

StringPrimitive = string
NumberPrimitive = number
BooleanPrimitive = bool
AnyPrimitive = any
IfPrimitive = if
ListPrimitive = list | list*\( | tuple | tuple*\(
MapPrimitive = map | map*\( | object | object*\(
SetPrimitive = set | set*\(
ForInExpression = in

DigitValue = [0-9\.\-]+

HCLAttributeName = [:jletter:] [a-zA-Z0-9\-\_]*
HCLQuotedPropertyName = [\"] [^\r\n]+ [\"]

HCLBlock = {HCLAttributeName} {HCLBlockAttribute}* "{" [^]* "}" | {HCLAttributeName} {WhiteSpaceOpt} "{" [^]* "}"

HCLBlockAttribute = {WhiteSpaceOpt} "\"" {HCLDoubleStringCharacters} "\"" {WhiteSpaceOpt} | {WhiteSpace} "\'" {HCLSingleStringCharacters} "\'" {WhiteSpaceOpt} | {WhiteSpace} {HCLAttributeName}  {WhiteSpaceOpt}

HCLAttribute = {HCLAttributeName} {WhiteSpaceOpt} "=" | {HCLQuotedPropertyName} {WhiteSpaceOpt} "="

MapKeyDef = {MapKey} ":"
MapKey = {HCLAttributeName} | "\"" {HCLDoubleStringCharacters} "\""
MapBlockStart = "{" {WhiteSpaceNLOpt} {MapKeyDef}

HCLDoubleStringCharacters = {HCLDoubleStringCharacter}*
HCLSingleStringCharacters = {HCLSingleStringCharacter}*
HCLDoubleStringCharacter = [^\r\n]
HCLSingleStringCharacter = [^\']
EscapedInterpolation = [\$] [\$]
InterpolationSyntax = [\$] "{"
MLineModifierStart = [\<] [\<] [\-\~] {HCLAttributeName}
MLineStart = [\<] [\<] [\ ]? {HCLAttributeName}


ExprTerm = {True} | {False} | {Null} | {DigitValue} | {Identifier} | {FunctionCall}
Conditional = \|\| | \&\& | \> [=]* | \< [=]* | == | \!= | [?] | \: | if
Operation = [\+\-\/\*\%]
Expression = {ExprTerm} | {Operation} | {Conditional}
/*For Expression*/
ForObjExpr = \{ [\n\t\f\r ]* {ForIntro}
ForTupleExpr = \[ [\n\t\f\r ]* {ForIntro}
ForExpr =  {ForObjExpr} | {ForTupleExpr}


ForIntro = "for" {WhiteSpaceSL}
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
%state FOROBJVARIABLES
%state FOROBJECTEXPRESSION
%state FORTUPLEVARIABLES
%state FORTUPLESOURCE
%state FOROBJSOURCE
%state FUNCTIONCALL
%state SUBTYPEPRIMITIVETYPE
%state VARIABLETREE

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

  \"                             { if(blockNames != null) { blockNames.add(string.toString());  yybegin(HCLBLOCKATTRIBUTES); } else if(currentBlock != null && currentBlock instanceof HCLMap && currentMapKey == null) { currentMapKey = string.toString() ; yybegin(HCLMAPKEYDEF); } else if (stringAttributeName) { stringAttributeName = false ; yybegin(HCLATTRIBUTE); if(currentBlock instanceof StringInterpolatedExpression) {StringInterpolatedExpression expr = (StringInterpolatedExpression)currentBlock; exitAttribute(true);startAttribute(expr);yybegin(HCLATTRIBUTE);} else { startAttribute(string.toString());}} else if(currentBlock != null) { currentBlock.appendChild(new HCLValue("string",string.toString(),yyline,yycolumn,yychar));if(currentBlock instanceof StringInterpolatedExpression){exitAttribute(true);}  yybegin(HCLATTRIBUTEVALUE); } else { throw new HCLParserException("String block found outside of block or attribute assignment."); } }
  \\\"                           { string.append('\"'); }
  {EscapedInterpolation}         { string.append( yytext() );}
  {InterpolationSyntax}          { startInterpolatedString(); }
  \$[^\{\$\"]                      { string.append( yytext() ); }
  \$\"                            { string.append( "$" ); yypushback(yylength()-1); }
  [^\$\n\r\"\\]+                 { string.append( yytext() ); }
}

<STRINGSINGLE> {
  [^\n\r\'\\]+                   { string.append( yytext() ); }
  \'                             { if(blockNames != null) { blockNames.add(string.toString());  yybegin(HCLBLOCKATTRIBUTES); } else if(currentBlock != null && currentBlock instanceof HCLMap && currentMapKey == null) { currentMapKey = string.toString() ; yybegin(HCLMAPKEYDEF); } else if (stringAttributeName) { stringAttributeName = false ; yybegin(HCLATTRIBUTE); startAttribute(string.toString());} else if(currentBlock != null) { currentBlock.appendChild(new HCLValue("string",string.toString(),yyline,yycolumn,yychar)); yybegin(HCLATTRIBUTEVALUE); } else { throw new HCLParserException("String block found outside of block or attribute assignment."); } }
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
  {HCLAttributeName}             { blockNames.add(yytext());}
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
  {True}                  { currentBlock.appendChild(new HCLValue("boolean","true",yyline,yycolumn,yychar)) ;  }
  {False}                 { currentBlock.appendChild(new HCLValue("boolean","false",yyline,yycolumn,yychar)) ; }
  {Null}                  { currentBlock.appendChild(new HCLValue("null",null,yyline,yycolumn,yychar)) ;  }
  {DigitValue}            { currentBlock.appendChild(new HCLValue("number",yytext(),yyline,yycolumn,yychar)) ;  }
  {StringPrimitive}       { currentBlock.appendChild(new StringPrimitiveType(yyline,yycolumn,yychar)); }
  {NumberPrimitive}       { currentBlock.appendChild(new NumberPrimitiveType(yyline,yycolumn,yychar)); }
  {BooleanPrimitive}      { currentBlock.appendChild(new BooleanPrimitiveType(yyline,yycolumn,yychar)); }
  {AnyPrimitive}          { currentBlock.appendChild(new AnyPrimitiveType(yyline,yycolumn,yychar)); }
  {ListPrimitive}         { subTypePrimitiveType = new ListPrimitiveType(null,yyline,yycolumn,yychar); if(yytext().endsWith("(")) { yypushback(1);} currentBlock.appendChild(subTypePrimitiveType); yybegin(SUBTYPEPRIMITIVETYPE); }
  {SetPrimitive}         { subTypePrimitiveType = new SetPrimitiveType(null,yyline,yycolumn,yychar); if(yytext().endsWith("(")) { yypushback(1);} currentBlock.appendChild(subTypePrimitiveType); yybegin(SUBTYPEPRIMITIVETYPE); }
  {MapPrimitive}         { subTypePrimitiveType = new MapPrimitiveType(null,yyline,yycolumn,yychar); if(yytext().endsWith("(")) { yypushback(1);} currentBlock.appendChild(subTypePrimitiveType); yybegin(SUBTYPEPRIMITIVETYPE); }
  {Conditional}                  { currentBlock.appendChild(new Operator(yytext(),yyline,yycolumn,yychar)); }
  {Operation}                    { currentBlock.appendChild(new Operator(yytext(),yyline,yycolumn,yychar)); }
  {EvaluatedExpression}          { startEvalExpression(); }
  {Comment}                      { /* ignore */ }
  {LineTerminator}               { if(currentBlock instanceof HCLAttribute) {exitAttribute(true); }  }
  \,                             { exitAttribute();}
  \]                             { if(currentBlock instanceof HCLArray) {exitAttribute(true); } else if(currentBlock instanceof ForConditional) { exitAttribute(true); yypushback(yylength());}  }
  {WhiteSpace}                   { /* ignore */ }
  \)                             { exitAttribute(true); }

  \}                             { if(currentBlock instanceof StringInterpolatedExpression) {exitAttribute(true);currentBlock.appendChild(new Operator("+",yyline,yycolumn,yychar));yybegin(STRINGDOUBLE);string.setLength(0);} else {yypushback(yylength()); exitAttribute(true); }  }

}

<EVALUATEDEXPRESSION> {

  {FunctionCall}             {startFunction(); }
  {IdentifierTree}           { startVariableTree(); }
  \}                             { yypushback(yylength()); exitAttribute(true);  }
  {LineTerminator}               { if(currentBlock instanceof HCLAttribute) {exitAttribute(true); }}
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                   { /* ignore */ }
  {Conditional}                  { currentBlock.appendChild(new Operator(yytext(),yyline,yycolumn,yychar)); }
  {Operation}                    { currentBlock.appendChild(new Operator(yytext(),yyline,yycolumn,yychar));  }
  \(                             { startGroupedExpression();/* ignore */ }
  \)                             { exitAttribute(true); }
}

<VARIABLETREE> {
    {FunctionCall} {startFunction(); }
    {Identifier}   { currentBlock.appendChild(new Variable(yytext(),yyline,yycolumn,yychar)); }
    \[             { startArray(); }
    \]             { yypushback(yylength()); exitAttribute(true); }
    \(             {startFunction(); }
    \)             { yypushback(yylength()); exitAttribute(true);}
    \,             { yypushback(yylength()); exitAttribute(true);}
    :              { yypushback(yylength()); exitAttribute(true); }
    \.             { /*ignore*/ }
    \}                             { yypushback(yylength()); exitAttribute(true);  }
    {LineTerminator}               { yypushback(yylength()); exitAttribute(true);   }
    {Comment}                      { exitAttribute(true); }
    {WhiteSpace}                   { exitAttribute(true); }
}

<FUNCTIONCALL> {

  ,                              {yybegin(HCLATTRIBUTEVALUE); }
  \)                             { exitAttribute(true); }
  {LineTerminator}               { /* ignore */ }
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                   { /* ignore */ }
  [^)\n\, ]+                        { yypushback(yylength()); yybegin(HCLATTRIBUTEVALUE); }
}

<FORLOOPEXPRESSION> {
  {ForObjExpr}     { startComputedObject();}
  {ForTupleExpr}   {   startComputedTuple();}
}

<FORTUPLEEXPRESSION> {
  :                 { yybegin(HCLATTRIBUTEVALUE);  }
  [\]]              { exitAttribute(true);  }
  {IfPrimitive}                { startForConditional(); }
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                 { /* ignore */ }
  {LineTerminator}             { /* ignore */ }

}

<FORTUPLEVARIABLES> {
  {ForInExpression}          {  yybegin(HCLATTRIBUTEVALUE);  }
  {Identifier}           { if(yytext() == "in") {yybegin(HCLATTRIBUTEVALUE);} else {((ComputedTuple)currentBlock).getVariables().add(new Variable(yytext(),yyline,yycolumn,yychar));}}
  [,] {/*ignore*/}
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                 { /* ignore */ }
  {LineTerminator}             { /* ignore */ }
}


<FOROBJVARIABLES> {
  {ForInExpression}          {  yybegin(HCLATTRIBUTEVALUE);  }
  {Identifier}           { if(yytext() == "in") {yybegin(HCLATTRIBUTEVALUE);} else {((ComputedObject)currentBlock).getVariables().add(new Variable(yytext(),yyline,yycolumn,yychar));}}
  [,] {/*ignore*/}
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                 { /* ignore */ }
  {LineTerminator}             { /* ignore */ }
}


<FORTUPLESOURCE> {
  {EvaluatedExpression}           { ((ComputedTuple)currentBlock).setSourceExpression( new Variable(yytext(),yyline,yycolumn,yychar));}
  :                 { yybegin(HCLATTRIBUTEVALUE); }

  {Comment}                      { /* ignore */ }
  {WhiteSpace}                 { /* ignore */ }
  {LineTerminator}             { /* ignore */ }
}

<FOROBJSOURCE> {
  {EvaluatedExpression}           { ((ComputedObject)currentBlock).setSourceExpression( new Variable(yytext(),yyline,yycolumn,yychar));}
  :                 { yybegin(HCLATTRIBUTEVALUE); }

  {Comment}                      { /* ignore */ }
  {WhiteSpace}                 { /* ignore */ }
  {LineTerminator}             { /* ignore */ }
}


<FOROBJECTEXPRESSION> {
  :                 { yybegin(HCLATTRIBUTEVALUE); }
  \=\>                           { yybegin(HCLATTRIBUTEVALUE); }
  \}                             { exitAttribute(true);  }
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                   { /* ignore */ }
  {LineTerminator}               { /* ignore */ }
  [^}\n]+                        { /* ignore */ }
}

<SUBTYPEPRIMITIVETYPE> {
  \(                             { currentBlock = subTypePrimitiveType ; yybegin(HCLATTRIBUTEVALUE); }
  \)                             { exitAttribute(true); }
  {LineTerminator}               { exitAttribute(true); }
  {Comment}                      { /* ignore */ }
  {WhiteSpace}                   { /* ignore */ }
}



/* error fallback */
    [^]                              { throw new HCLParserException("Illegal character <("+
                                                        yytext()+ ") - state: " + yystate()+"> found on line: " + (yyline+1) + " col: " + (yycolumn+1) ); }
