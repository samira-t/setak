h1. Setak Testing Framework

h2. Overview

*Setak* is a testing framework for testing "Akka":http://akka.io actors. It facilitates writing unit and integration tests for actor applications by allowing the programmers to 
# control the non-determinism in the concurrent actor systems; and 

# manage the asynchrony by checking the assertions when the system is stable

It supports writing test cases in both forms of "JUnit":http://junit.sourceforge.net/ and "ScalaTest":http://www.scalatest.org.


h2. Requirements

Setak depends on:

# "Scala 2.9.1":http://www.scala-lang.org/
# "Akka 1.2":http://akka.io/
# "JUnit 4.5":http://junit.sourceforge.net, for writing tests in JUnit format
# "ScalaTest 1.6.1":http://www.scalatest.org, for writing tests in ScalaTest format
