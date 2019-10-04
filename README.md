# blueprint-validator
A simple validator for blueprints used in 5G EVE Portal

# Old readme. TODO update

# 5g-blueprint-libs
Definitions and POJO mappings of Vertical Service Blueprints for 5G EVE

## Developer guide

POJO classes for all blueprints and sub-components are included in
[blueprint](src/test/java/eu/_5geve/blueprint) package.

## User Guide

### Blueprint examples in YAML

Some blueprint examples are included in this project. You can find them in
[blueprint-examples](src/main/resources/blueprint-examples/) folder.

### Importing and using POJOs in your Java project

To understand how to use the generated Java classes, please refer to the test
classes in [test](src/test/java/eu/_5geve/blueprint)

To parse YAML files you can use jackson's `ObjectMapper`.
The `ObjectMapper` behavior can be configured with various options and an
example of this is shown in the `testSetup()` method in the test classes.

To validate the generated object you can use
[Hibernate Validator](https://hibernate.org/validator/) provided as a
dependency, or any other validator that you like.
Please refer again to the `testSetup()` method for how to initialize it.

### Command line

The jar file produced by `mvn package` is executable and you can use it to validate blueprints.
The jar file is also available for download from
[releases](https://github.com/5GEVE/5geve-blueprint-libs/releases).

Usage example:

```
# print help and exit
java -jar 5geve-blueprint-libs-VERSION.jar --help

# validate a VSB
java -jar 5geve-blueprint-libs-VERSION.jar --type vsb ./vsb_asti_agv.yaml
```
