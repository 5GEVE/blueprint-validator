# blueprint-validator
A simple validator for blueprints used in 5G EVE Portal

### Command line usage

The jar file produced by `mvn package` is executable and you can use it to validate blueprints.
The jar file is also available for download from
[releases](https://github.com/TheWall89/blueprint-validator/releases).

Usage example:

```
# print help and exit
java -jar validator-VERSION.jar --help

# validate a VSB
java -jar validator-VERSION.jar --type vsb ./vsb_asti_agv.yaml
```

### Blueprint examples in YAML

Some blueprint examples are provided in [blueprint-yaml](link-coming-soon).

