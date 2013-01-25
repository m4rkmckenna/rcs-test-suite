rcs-test-suite
==============

Suite of Integration Tests for the rcs.oneapi-gw.gsma.com platform which can be run as a suite of JUnit Tests.

To run the tests from the command line, try the following:

```
mvn test
```

Or if you wish to run a specific test, then the following example shows how to run all the File Transfer Tests:

```
mvn -Dtest=com.aepona.rcs.test.fileTransfer.* test
```

The tests can also be run inside eclipse by importing this project as an existing maven project into your workspace and then right clicking on the **src/test/java** folder and selecting **Run As -> JUnit Test** 