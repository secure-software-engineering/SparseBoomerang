<p align="center">
<img src="https://github.com/secure-software-engineering/SparseBoomerang/blob/master/SparseBoomerangLogo.png">
</p> 



# SparseBoomerang

SparseBoomerang is an extension to SPDS-based [Boomerang](https://github.com/CodeShield-Security/SPDS).
This [SparseBoomerang repository](https://github.com/secure-software-engineering/SparseBoomerang) is now the main fork of [CodeShield](https://codeshield.io/)'s [Boomerang](https://github.com/CodeShield-Security/SPDS) which is providing further maintainance and development.
SparseBoomerang introduces two sparsification strategies to aid the scalability of precise Boomerang pointer analysis.
[SparseBoomerangCorrectness](SparseBoomerangCorrectness) show how these work and compare them against the default non-sparse Boomerang.

## FlowDroid with SparseBoomerang
The FlowDroid fork that works with SparseBoomerang is available at: [https://github.com/kadirayk/FlowDroid](https://github.com/kadirayk/FlowDroid)

## Publications
The paper is available:
[Two Sparsification Strategies for Accelerating Demand-Driven Pointer Analysis](https://ieeexplore.ieee.org/document/10132184) (ICST 2023)

Preprint is available: 
[Two Sparsification Strategies for Accelerating Demand-Driven Pointer Analysis](https://www.bodden.de/pubs/kb23sparsification.pdf) (ICST 2023)

## Boomerang
This repository contains a Java implementation of Synchronized Pushdown Systems.
Additionally, it contains an implementation of [Boomerang](boomerangPDS) and [IDEal](idealPDS) based on a Weighted Pushdown System.


## Use as Maven dependency

The projects are released on [Maven Central](https://central.sonatype.com/artifact/de.fraunhofer.iem/SPDS) and can be included as a dependency in `.pom` files (replace `x.y.z` with the latest version).

- Boomerang can be included with the following dependency:

```.xml
<dependency>
  <groupId>de.fraunhofer.iem</groupId>
  <artifactId>boomerangPDS</artifactId>
  <version>x.y.z</version>
</dependency>
```

- IDEal can be included with the following dependency:

```.xml
<dependency>
  <groupId>de.fraunhofer.iem</groupId>
  <artifactId>idealPDS</artifactId>
  <version>x.y.z</version>
</dependency>
```

## Checkout, Build and Install

To build and install SPDS into your local repository, run 

``mvn clean install -DskipTests``

in the root directory of this git repository. If you do not want to skip the test cases, remove the last flag.

## Examples

Boomerang code examples can be found [here](https://github.com/CodeShield-Security/SPDS/tree/master/boomerangPDS/src/main/java/boomerang/example). Code examples for IDEal are given [here](https://github.com/CodeShield-Security/SPDS/tree/master/idealPDS/src/main/java/inference/example).


## Notes on the Test Cases

The projects Boomerang and IDEal contain JUnit test suites. As for JUnit, the test methods are annotated with @Test and can be run as normal JUnit tests.
However, these methods are *not* executed but only statically analyzed. When one executes the JUnit tests, the test method bodies are supplied as input to Soot 
and a static analysis is triggered. All this happens in JUnit's @Before test time. The test method itself is never run, may throw NullPointerExceptions or may not even terminate.

If the static analysis succeeded, JUnit will officially label the test method as skipped. However, the test will not be labeled as Error or Failure. 
Even though the test was skipped, it succeeded. Note, JUnit outputs a message:

``org.junit.AssumptionViolatedException: got: <false>, expected: is <true>``

This is ok! The test passed!
