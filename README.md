[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio.svg?style=social&label=Follow&style=for-the-badge)](https://twitter.com/strimziio)


# Topic-level Encryption at Rest for Apache Kafka®


The goal of this project is to provide proxy-based, topic-level encryption-at-rest for [Apache Kafka®](https://kafka.apache.org/).  
To learn more about the background and architecture of topic encryption, see our [overview document](doc/README.md).

The next planned milestones in the project are:

## M1, May 14: Foundation
- Technical specification of the project
- Assessment of viable proxy 
  - Envoy vs. a custom-developed proxy (in golang or Java)

## M2, June 04: Alpha proxy
- Initial implementation of selected proxy architecture
  - stand-alone, not yet integrated

## M3, June 18: Proxy integration evaluation
- First version of the software encryption module
- Integration of encryption module with proxy
- Evaluation of proxy integration into Strimzi and build environment

## M4, July 02: Alpha Strimzi integration
- Integrate proxy with the Strimzi project
- Integrate encryption module


