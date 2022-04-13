[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio.svg?style=social&label=Follow&style=for-the-badge)](https://twitter.com/strimziio)


# Topic-level Encryption at Rest for Apache Kafka®


The goal of this project is to provide proxy-based, topic-level encryption-at-rest for [Apache Kafka®](https://kafka.apache.org/).  

### Documentation
To learn more about the background and architecture of topic encryption, see our [overview document](doc/README.md). 

The [getting started guide](doc/getting-started.md) explains how to compile and run the encrypting proxy for testing and evaluation.

### Project structure
The project consists of two nested projects:
- [encmod](encmod/),  the topic encryption module
- [vertx-proxy](vertx-proxy/), an experimental Kafka proxy for developing and testing the encryption module. 




