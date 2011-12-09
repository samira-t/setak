Setak Testing Framework
========================

Overview
---------

__Setak__ is a testing framework for testing [Akka](http://akka.io) actors. 
It facilitates writing unit and integration tests for actor applications by allowing the programmers to 

- control the non-determinism in the concurrent actor systems; and 

- manage the asynchrony of events by checking the assertions when the system is stable

It supports writing test cases in both forms of [JUnit](http://junit.sourceforge.net/) and [BDD (FlatSpec and WordSpec)](http://www.scalatest.org).


Requirements
--------------

Setak depends on:

- [Scala 2.9.1](http://www.scala-lang.org/)
- [Akka 1.2](http://akka.io/)
- [JUnit 4.5](http://junit.sourceforge.net), for writing tests in JUnit format
- [ScalaTest 1.6.1](http://www.scalatest.org), for writing tests in FlatSpec or WordSpec format
- [sbt 0.11](https://github.com/harrah/xsbt/wiki), for compiling the source code and running tests and examples (examples are in the test folder)


How to Use
--------------
Snapshots are located at (http://mir.cs.illinois.edu/setak/snapshots/), 
which can be used in sbt by adding the following lines:

    resolvers += "setak" at "http://mir.cs.illinois.edu/setak/snapshots/"
    
    libraryDependencies += "edu.illinois" %% "setak" % "1.0-SNAPSHOT"

In order to compile from the source code and run examples and tests: 

    $ cd setak
    $ sbt
    > update
    > test (run the tests and the examples)
