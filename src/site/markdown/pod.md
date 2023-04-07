

# Pod installation

SUMARiS use a database engine to store data.


It can exist many SUMARiS databases instances on the web.
Each database instance is accessible through a [server software](https://en.wikipedia.org/wiki/Server_(computing)), called **SUMARiS Pod**.

A Pod manage only one database instance.

## Main features

The SUMARiS Pod has several features:

 - Create a database instance, then manage schema updates.
    * The Pod is compliant with many database engines: [HSQLDB](http://hsqldb.org/), PostgreSQL and Oracle;

 - Allow data access (read/write) to a database instance. Such API is used by client software (like the [SUMARiS App](./app.md)):
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

## Installation of the database

### HSQLDB engine (default)

- Copy the file [sumaris-db-hsqldb.sh](https://github.com/sumaris-net/sumaris-pod/blob/master/sumaris-server/src/main/assembly/bin/sumaris-db-hsqldb.sh) locally

```bash
wget -kL https://raw.githubusercontent.com/sumaris-net/sumaris-pod/master/sumaris-server/src/main/assembly/bin/sumaris-db-hsqldb.sh
# Or using curl: 
# curl https://raw.githubusercontent.com/sumaris-net/sumaris-pod/master/sumaris-server/src/main/assembly/bin/sumaris-db-hsqldb.sh > sumaris-db-hsqldb.sh  

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
  
  Your database is ready, and should be accessible (e.g. through a JDBC client software).
 
  To make sure everything is running well, check logs at: `<SUMARIS_HOME>/logs/` 

### Oracle

Download locally oracle properties file from [isi-sih-sumaris](https://gitlab.ifremer.fr/dev_ops/shared_docker_image_factory/isi-sih-sumaris) project (you can find properties files under `sumaris-server` directory).

Run next command with the right path to oracle tns_name and propertie files :
```bash
spring-boot:run -Dspring-boot.run.fork=false -Doracle.net.tns_admin=\\brest\tnsnames -Dspring.config.location=path-to-file\application-oracle.properties
```

### PostgreSQL

`TODO: write this part`

## Installation of the Pod

### On Linux systems (Debian, Ubuntu)

 1. Install Pod's dependencies: 
    * Install LibSodium (Unix only) : https://download.libsodium.org/doc/installation/
    * Install Java SDK 11
    
 2. Download the latest WAR file at: https://github.com/sumaris-net/sumaris-pod/releases

 3. Copy the WAR file anywhere;
 
 4. In a terminal, start the pod using the command:
    ```bash
    java -jar sumaris-pod-x.y.z.war
    ``` 

  5. Congratulations ! 
  
     Your Pod should now be running.
     
     A welcome page should also be visible at the address [http://localhost:8080](http://localhost:8080):
     
     ![](./images/pod-screenshot-api.png)

<u>Note for IT developers:</u> 

Your running Pod give access to useful dev tools : 
  - A GraphQL live query editor, at `<server_url>/graphiql` (WARN some query will need authorization) 
  - A GraphQL subscription query editor (GraphQL + websocket), at `<server_url>/subscription/test`
    
### On MS Windows

`TODO: write this part`

### Configuration

To change the Pod's configuration, follow this steps:

 1. Create a directory for your configuration (e.g. `/home/<USER>/.config/SUMARiS/`): 
    ```bash
    mkdir -p /home/<USER>/.config/SUMARiS/
    ```
 
 2. Create a file `application.properties` inside this directory;
 
 3. Edit the file, and add options you want to override (see the [list of available options](./config-report.html)):
 
    A basic example:
    ```properties
    server.address= 127.0.0.1
    server.port=8080
    sumaris.basedir= /home/<USER>/.config/SUMARiS
    ```

 4. In a terminal, start the pod with the command:
    ```bash
    java -server -Xms512m -Xmx1024m -Dspring.config.additional-location=/home/<USER>/.config/sumaris/ -jar sumaris-pod-x.y.z.war
    ``` 

 5. That's it !
 
    Your configuration file should have been processed.


## Build from source

 1. Install project dependencies:
    * Install build tools (Make, GCC, Git)
      ```bash 
      sudo apt-get install build-essential
      ```
   * Install LibSodium (Unix only):
     https://download.libsodium.org/doc/installation/

   * Install Java SDK 11

   * Install Apache Maven 3

 2. Get the source code
    ```bash 
    git clone git@github.com:sumaris-net/sumaris-pod.git
    cd sumaris-pod
    ```

 3. Run the compilation:
    ```bash
    cd sumaris-pod
    mvn install -DskipTests
    ```

 4. The final WAR file should have been created inside the directory: `<PROJECT_DIR>/sumaris-server/target/`  
