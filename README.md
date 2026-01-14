# PDP_MWF

---
A simulation for testing data preservation of base station-less networks (BSNs) according to packet priority.

## Table of Contents

---
- [About](#about)
- [Setup](#setup)
- [Example](#example)
  - [Terminal Output](#terminal-output)
- [Authors](#authors)

## About

---
This data preservation simulation generates a suite of base station-less networks (BSNs) to apply ILP-based solutions.
It uses Google-OR Tools

## Setup

---

### Dependencies

- JDK 25 or newer ([Latest JDK from Oracle](https://www.oracle.com/java/technologies/downloads/))
- Maven ([Installation Guide from Apache](https://maven.apache.org/install.html))
    - Or manually install (and add to classpath):
        - [Google OR-Tools](developers.google.com/optimization)
        - [Google Guava](https://github.com/google/guava)
        - [Google Protocol Buffers](https://github.com/protocolbuffers/protobuf)

### 1. Clone the Repository

Open a command line or terminal instance and enter the following command:
```sh
git clone https://github.com/grivera64/PDP_MWF.git
```

You can also download the repository as a zip file directly
from GitHub [here](https://github.com/grivera64/PDP_MWF/archive/refs/heads/main.zip) and unzip it.

### 2. Change directories into the source folder.

```sh
cd PDP_MWF
```

### 3. Setup/Compile/Run using maven

```console
mvn -q clean install exec:java
```

## Authors

---
- Giovanni Rivera ([@grivera64](https://github.com/grivera64))

