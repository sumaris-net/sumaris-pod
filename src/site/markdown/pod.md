

# Database and Pod

SUMARiS use a database engine to store data. It can exists various SUMARiS database instances on the web.
Those are accessible throw a [server software](https://en.wikipedia.org/wiki/Server_(computing)) called **SUMARiS Pod**,
that manage one database instance.

## Main features

The SUMARiS Pod has several features:

 - Create a database instance, then manage schema updates.
 
    * The Pod is compliant with many database engines: [HSQLDB](http://hsqldb.org/), PostgreSQL and Oracle;

 - Allow data access (read/write) to a database instance. Such API is used by client software, like the [SUMARiS App](./app.md)):
 
    * Publish data as GraphQL API;
    * Publish data as RDF API for semantic web, including OWL (Ontoligy Web Language);
    * CSV files input/output (using the ICES RDB exchange data format)
    * Peer-to-peer synchronization, to share data between Pods (coming soon) 

## Database model

 - [Conceptual model](doc/model/index.md) of the database:
 - [Tables list](./sumaris-core/hibernate/tables/index.html) with all associated columns;

 - For IT developers: 
 
    * [Entities](./sumaris-core/hibernate/entities/index.html) (Hibernate mapping);
    * [Built-in queries](./sumaris-core/hibernate/queries/index.html) (HQL and SQL) used in the source code.

## Installation (Database and Pod)

### On Linux systems (Debian, Ubuntu)

- Install LibSodium (Unix only) : https://download.libsodium.org/doc/installation/

- Install Java SDK 8

- Download the latest JAR file at : https://github.com/sumaris-net/sumaris-pod/releases

- In a terminal, start the pod:
```bash
java -jar sumaris-pod-x.y.z.jar
``` 
### On MS Windows

TODO: complete this installation guide for windows

### Configuration

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

## Build from source

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
