# BFTB

Highly Dependable Systems 2021-2022, 2nd semester project


## Authors

**Group GXX**


### Team members

*(fill-in table below with team members; and then delete this line)*

| Number | Name          | User                             | Email                               |
|--------|---------------|----------------------------------| ------------------------------------|
| 93575  | Sérgio Pinto  | <https://github.com/Salg-0>     | <>     |
| 93612  | Rodrigo Pinto | <https://github.com/rodrigoappinto>   | <mailto:rodrigo.pinto@tecnico.ulisboa.pt>   |
| 93614  | Sérgio Pinto  | <https://github.com/seeeerju>     | <mailto:sergio.g.pinto@tecnico.ulisboa.pt>     |



### Task leaders


| Task set | To-Do                         | Leader              |
| ---------|-------------------------------| --------------------|
| core     | protocol buffers, silo-client | _(whole team)_      |
| T1       | cam_join, cam_info, eye       | Patrícia Vilão      |
| T2       | report, spotter               | Sérgio Pinto        |




## Getting Started

The overall system is composed of multiple modules.
The main server is the _dgs_.
The clients are the _sniffer_, the _journalist_ and the _researcher_.

See the [project statement](https://github.com/tecnico-distsys/StaySafe/blob/main/part1.md) for a full description of the domain and the system.

### Prerequisites

Java Developer Kit 11 is required running on Linux, Windows or Mac.
Maven 3 is also required.

To confirm that you have them installed, open a terminal and type:

```
javac -version

mvn -version
```

### Installing

To compile and install all modules:

```
mvn clean install -DskipTests
```

The integration tests are skipped because they require the servers to be running.


## Built With

* [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
* [gRPC](https://grpc.io/) - RPC framework


## Versioning

We use [SemVer](http://semver.org/) for versioning. 
