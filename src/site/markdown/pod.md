


# SUMARiS Pod

## Introduction

**SUMARiS Pod** is a [server software](https://en.wikipedia.org/wiki/Server_(computing)) used to manage a SUMARiS database:

 - Create the SUMARiS database and manage schema updates:
    * Compatible with [HSQLDB](http://hsqldb.org/), PostgreSQL and Oracle;

 - Allow data access (read/write) to the SUMARiS database, from client software (like [SUMARiS App](./app.md)):
    * Publish a GraphQL API;
    * Publish a RDF API for semantic web, including OWL (Ontoligy Web Language);
    * CSV files input/output (using the ICES RDB exchange data format)

## Documentation

 - Documentation on the database:
    * Database conceptual model:
      * [Simplified conceptual model](./doc/model/index.md);
      * [Full entities specification](./sumaris-core/hibernate/entities/index.html) (Hibernate mapping);
    * Database physical model:
      * [Full tables specification](./sumaris-core/hibernate/tables/index.html);
  
 - Other technical documentation: 
    * [Built-in queries](./sumaris-core/hibernate/queries/index.html) (HQL and SQL) used in the source code.

## Installation

- Install LibSodium (Unix only) : https://download.libsodium.org/doc/installation/

- Install Java SDK 8

- Download the latest JAR file at : https://github.com/sumaris-net/sumaris-pod/releases

- In a terminal, start the pod:
```bash
java -jar sumaris-pod-x.y.z.jar
``` 

### How to change configuration options ?

- Create a directory for your configuration (e.g. `/home/<USER>/.config/sumaris/`): 
```bash
mkdir -p /home/<USER>/.config/sumaris/
```
 
- Create a file `application.yml` inside this directory;
- Define all options to override, in this file (see [all available options](./config-report.html)):
  ```yml
  server.address: 127.0.0.1
  server.port: 8080  
  sumaris.basedir: /home/<USER>/.config/sumaris
  ```

 - In a terminal, start the pod:
```bash
java -server -Xms512m -Xmx1024m -Dspring.config.additional-location=/home/<USER>/.config/sumaris/ -jar sumaris-pod-x.y.z.jar
``` 

## Compile from source

- Install Build tools (Make, GCC, Git)

``` 
sudo apt-get install build-essential
```

- Install LibSodium (Unix only) : https://download.libsodium.org/doc/installation/

- Install Java SDK 8

- Install Apache Maven 3


- Get the source code

``` 
git clone git@github.com:sumaris-net/sumaris-pod.git
cd sumaris-pod
```

- Compile the source code :

``` 
mvn install -DskipTests
```
