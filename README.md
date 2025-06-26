# Project Overview

This project consists of two modules:

1. **Avro Schema Generator from POJO Classes**: Automatically generates Avro schemas from Java POJO classes.
2. **Avro Schema Generator from Database Views**: Converts database views into Avro schema definitions.

---

## Module 1: Avro Schema Generator from POJO Classes

### Description
This module generates Avro schemas directly from Java POJO classes using reflection. It ensures compatibility with Apache Kafka and schema registries.

### Features
- Automatic schema generation from POJO classes.
- Support for custom Avro properties like `avro.java.string`.
- Handles logical types such as timestamps and UUIDs.

### Getting Started
Refer to the `README.md` file in the module directory for detailed setup instructions.

### Usage
1. Add your POJO classes to the module.
2. Run the tool to generate `.avsc` files in the specified output directory.

---

## Module 2: Avro Schema Generator from Database Views

### Description
This module generates Avro schemas from database views, bridging the gap between relational databases and Avro-based systems.

### Features
- Converts database views into Avro schemas.
- Maintains field order from the database view.
- Supports Kafka Connect logical types.

### Getting Started
Refer to the `README.md` file in the module directory for detailed setup instructions.

### Usage
1. Configure the `application-local.yml` file with database connection details.
2. Run the tool to generate `.avsc` files based on the database views.

---

## Common Notes
- Both modules require Java and Maven to build and run.
- Ensure proper configuration of the `application-local.yml` file for database-related operations.
- Generated schemas are compatible with Apache Kafka and schema registries.

For more information, consult the respective `HELP.md` files in each module directory.