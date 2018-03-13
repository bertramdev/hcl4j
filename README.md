HCL4j
=====

HCL4j is a Parser for the Hashicorp Configuration Language on the JVM. This provides a mechanism for converting HCL syntax into an Object Map that can be used for further inspection. 

Features:

* Support for Syntax parsing
* Nested Array and Map support


## Installation

Using gradle one can include the hcl4j dependency like so:

```groovy
dependencies {
	compile "com.bertramlabs.plugins:hcl4j:0.1.0"
}
```

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

This plugin does not yet handle processing of the interpolated string syntax. While it does generate it into the result map, Parsing the values of the interpolation syntax needs to be done in a follow up step using some type of HCL runtime engine