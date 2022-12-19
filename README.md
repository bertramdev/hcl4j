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
	compile "com.bertramlabs.plugins:hcl4j:0.5.0"
}
```

## Whats New

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
