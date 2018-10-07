


# Server API


## Introduction

**SUMARiS :: Server** is a software used connecting to a SUMARiS database  :

- Read/Write in the SUMARiS database: schema update, data queries;


## Documentation

Available documentation on server database:

- [Tables](./sumaris-core/hibernate/tables/index.html) of the server database schema;
- [Entities`](./sumaris-core/hibernate/entities/index.html) used by the conceptual model (Hibernate entities);
- [Queries](./sumaris-core/hibernate/queries/index.html) (HQL format) used by source code.


## Installation

- Install Build tools (Make, GCC)

``` 
sudo apt-get install build-essential
```

- Install LibSodium (Unix only) : https://download.libsodium.org/doc/installation/

- Install Java SDK 8

- Install Apache Maven 3

- Compile the source code :

``` 
mvn install
```
