# NAT_Traversal_Service
[![Build Status](https://travis-ci.org/MaoJianwei/NAT_Traversal_Service.svg?branch=master)](https://travis-ci.org/MaoJianwei/NAT_Traversal_Service)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/MaoJianwei/NAT_Traversal_Service/blob/master/LICENSE)

NAT traversal server, client and libraries.

.

## Usage

### Compile from source code
```shell
$ git clone https://github.com/MaoJianwei/NAT_Traversal_Service.git
$ cd NAT_Traversal_Service
$ mvn clean package
```

### Or, download pre-build binary jar file:
https://github.com/MaoJianwei/NAT_Traversal_Service/releases


### Run nat-traversal server/client
1. copy **application.properties** from *nat-traversal-server/src/main/resources/* or *nat-traversal-client/src/main/resources/* to **the same directory as** nat-traversal-client-1.0.jar / nat-traversal-server-1.0.jar

2. modify **application.properties** on your demand.

3. run "**java -jar nat-traversal-client-1.0.jar / nat-traversal-server-1.0.jar**"


## UML Sequence chart

![UML_Sequence_Chart_v1.0](https://raw.githubusercontent.com/MaoJianwei/NAT_Traversal_Service/master/docs/UML_Sequence_Chart_v1.0.png)
