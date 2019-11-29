

# Database and Pod

SUMARiS use a database engine to store data.


It can exists many SUMARiS databases instances on the web.
Each databases instance is accessible through a [server software](https://en.wikipedia.org/wiki/Server_(computing)), called **SUMARiS Pod**.

A Pod manage only one database instance.

## Main features of the Pod

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
    * [HQL named queries](./sumaris-core/hibernate/queries/index.html) declared in the source code.
      We also use JPA Criteria API to build queries dynamically (see source code for more details).


## Installation of the Pod

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

## Installation of the database

### HSQLDB

- Copy the file [sumaris-db-hsqldb.sh](https://github.com/sumaris-net/sumaris-pod/blob/master/sumaris-server/src/main/assembly/bin/sumaris-db-hsqldb.sh) locally

```bash
wget -kL https://github.com/sumaris-net/sumaris-pod/blob/master/sumaris-server/src/main/assembly/bin/sumaris-db-hsqldb.sh
# Or using curl: 
# curl https://github.com/sumaris-net/sumaris-pod/blob/master/sumaris-server/src/main/assembly/bin/sumaris-db-hsqldb.sh > sumaris-db-hsqldb.sh  

# Give execution rights
chmod u+x sumaris-db-hsqldb.sh
```

- Edit this file, to set the `SUMARIS_HOME` variable :
```bash
#!/bin/bash
# --- User variables (can be redefined): ---------------------------------------
#SUMARIS_HOME=/path/to/sumaris/home
SERVICE_NAME=sumaris-db
DB_NAME=sumaris
DB_PORT=9000
(...)
```

- Start the database, using the command (in a terminal): 
```
./sumaris-db-hsqldb.sh start
```  

- That's it !
  
  Your database is ready, and should be accessible.  
 
  To make sure everything is OK, please check logs at: `<SUMARIS_HOME>/logs/` 
