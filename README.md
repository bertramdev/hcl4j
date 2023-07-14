HCL4j
=====

HCL4j is a Parser for the Hashicorp Configuration Language on the JVM. This provides a mechanism for converting HCL syntax into an Object Map that can be used for further inspection. 

Features:

* Support for Syntax parsing
* Nested Array and Map support
* Runtime Parsing Evaluation (Variable Traversal, Function calls, mathematical operations, conditional expressions, for loop objects and tuples)


## Installation

Using gradle one can include the hcl4j dependency like so:

```groovy
dependencies {
	compile "com.bertramlabs.plugins:hcl4j:0.7.3"
}
```

## What's New

* **0.7.3** Fixing Multiline Formatting Issue
* **0.7.2** Handling nested Block types in a List as a non null value
* **0.7.1** Adding some parser exception safety and added slf4j dependency for logging errors
* **0.7.0** Adding some null safety on some of the base functions
* **0.6.9** Fixed some null pointer issues if content on base64encode was null and some exceptions on array traversal
* **0.6.8** Fixed ! (not) operator and added base64encode, base64decode, textdecodebase64, textencodebase64
* **0.6.7** Fixed operators called on variables without spaces
* **0.6.6** Fixed nested strings inside an interpolation syntax as well as some add conditional checks
* **0.6.5** Fixed issue with same line comments sometimes causing a parser error as well as Maps with new line terminators causing issues
* **0.6.4** Improved toString() behavior of VariableTree when serializing to JSON
* **0.6.3** Updated Gradle Dependencies to most recent versions. Fixed issue with accessing local vars from multiple `locals{}` blocks in same context. Function calls with non evaluated arguments are no longer processed for safety.
* **0.6.2** Handling class cast exception on contains in some scenarios and alias subType on primitive types to check children
* **0.6.1** Handling (any) object sub type primitive. Handling non quote enclosed block attributes. Added jsonencode and jsondecode function implementations.
* **0.6.0** Runtime parsing of Conditional Expressions, Mathematical Operators, String Interpolation now supported. Added additional Terraform Base Functions. Improved parser syntax to handle some outliar formats. Nested Type Primitives now work more completely (subType no longer used, check children of the Primitive Symbol)
* **0.5.0** Runtime Parsing is now supported and for loops. There are huge improvements to support actually evaluating complex runtime operations during the parsing of the hcl. variables can even be processed via the parseVars method.
* **0.4.0** Primitive Types are now appended into the Map.  These are of an extended `PrimitiveType` class. These Types include `StringPrimitiveType`, `NumberPrimitiveType`, `BooleanPrimitiveType`, `MapPrimitiveType`, and lastly `ListPrimitiveType` with a `subType` capable property.

## Usage

Using the HCL Parser is fairly straightfoward. Most calls are still limited to use of the `HCLParser` class itself. There are several `parse` method helpers supporting both `File`, `InputStream`, `String`, and `Reader` as inputs.


```java
import com.bertramlabs.plugins.hcl4j.HCLParser;

File terraformFile = new File("terraform.tf");
Map results = new HCLParser().parse(terraformFile, "UTF-8");
```

For More Information on the HCL Syntax Please see the project page:

[https://github.com/hashicorp/hcl](https://github.com/hashicorp/hcl)


## Things to be Done

* for tuples and objects are processed but not evaluated
* add more method definitions for base terraform functions
* add handlers for data lookup modules
