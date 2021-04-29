[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio.svg?style=social&label=Follow&style=for-the-badge)](https://twitter.com/strimziio)


# Proxy-Based Topic-level Encryption at Rest for Kakfka


The goal of this project is to provide proxy-based, topic-level encryption-at-rest for [Apache Kafka](https://kafka.apache.org/).  To learn more about the background and architecture of topic encryption, see our [overview document](doc/README.md).

The next planned milestones in the project are:

## M1, May 14: Foundation
- Technical specification of the project
- First version of the software encryption module in golang 
- Assessment of viable proxy 
  - Envoy vs a custom-developed proxy (in golang or Java)

## M2, June 04: Alpha proxy
- Initial prototype implementation of the proxy
  - stand-alone, not yet integrated

## M3, June 18: Proxy integration evaluation
- Evaluation of proxy integration into Strimzi
  - Familiarize with the Strimzi build environment

## M4, July 02: Alpha Strimzi integration
- Integrate proxy into the Strimzi project
- Integrate encryption module


The goal is to have a working prototype of an encryption proxy integrated into Strimzi by the end of M4, June 25th.
