# BFTB

Highly Dependable Systems 2021-2022, 2nd semester project

## Authors

**Group G41**

### Team members

| Number | Name              | User                                | Email                                                |
| ------ | ----------------- | ----------------------------------- | ---------------------------------------------------- |
| 93575  | António Salgueiro | <https://github.com/Salg-0>         | <mailto:antonio.bastos.salgueiro@tecnico.ulisboa.pt> |
| 93612  | Rodrigo Pinto     | <https://github.com/rodrigoappinto> | <mailto:rodrigo.pinto@tecnico.ulisboa.pt>            |
| 93614  | Sérgio Pinto      | <https://github.com/seeeerju>       | <mailto:sergio.g.pinto@tecnico.ulisboa.pt>           |

<img src=images/antonio.png height=200 width=200> <img src=images/rodrigo.png height=200 width=200> <img src=images/sergio.png height=200 width=200>

## Getting Started

The overall system is composed of multiple modules.
The main server is the bftb-server.
The clients are the bftb-client and the bftb-library which is the API.

See the [project statement](https://fenix.tecnico.ulisboa.pt/downloadFile/563568428850476/SEC-2122%20project%20-%20stage%201_v2.pdf) for a full description of the requirements.

### Prerequisites

Java Developer Kit 11 or higher is required running on Linux, Windows or Mac.
Maven 3 is also required. For the database, MySQL 8.0.28 version is required.

To install MySQL in Mac type:

```
brew install mysql
brew services start mysql
```

To install in Windows follow this guide:

https://dev.mysql.com/doc/refman/8.0/en/windows-installation.html

Download MySQL here:

https://dev.mysql.com/downloads/installer/

and follow the executable prompts using default parameters,
After that you may need to add MySQL to variable path, as shown here:

https://dev.mysql.com/doc/mysql-windows-excerpt/5.7/en/mysql-installation-windows-path.html

in case you may have this error:

```
only whitespace content allowed before start tag and not \u0 (position: START_DOCUMENT seen \u0... @1:1) -> [Help 1]
```

Follow this link:

https://stackoverflow.com/questions/13929633/maven-install-error-only-whitespace-content-allowed-before-start-tag-and-not

To confirm that you have them installed, open a terminal and type:

```
javac -version

mvn -version

mysql -V
```

### Database

To create the database for this project type the following commands:

```
mysql -uroot
CREATE DATABASE bftbServer;
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';
```

To create the schemas in the database type in the root directory:

```
mvn flyway:migrate
```

To clear and reuse the database after each run type:

```
mvn flyway:clean

mvn flyway:migrate
```

### Installing

To compile and install all modules type in the root directory(/BFTB-SEC):

```
mvn clean install -DskipTests
```

The integration tests are skipped because they require the servers to be running.

### Running

To run the server type:

```
mvn compile exec:java
```

To run the client type:

```
mvn compile exec:java
```

### Testing

to be done

### Demos

to be done

## Built With

- [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
- [gRPC](https://grpc.io/) - RPC framework
- [MySQL](https://www.mysql.com) - Database Management System
