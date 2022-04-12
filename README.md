[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio.svg?style=social&label=Follow&style=for-the-badge)](https://twitter.com/strimziio)


# Topic-level Encryption at Rest for Apache Kafka®


The goal of this project is to provide proxy-based, topic-level encryption-at-rest for [Apache Kafka®](https://kafka.apache.org/).  
To learn more about the background and architecture of topic encryption, see our [overview document](doc/README.md).

Consists of two nested projects:
- `encmod`,  The topic encryption module
- `vertx-proxy`, An experimental Kafka proxy programmed with vert.x. This proxy currently serves to provide a runtime context for development and testing of the encryption module. The ultimate implementation of the proxy is subject to change.




