raml-validation-proxy
=====================

A lot of RESTful services using [RESTful API Modeling Language][8] to define their APIs.
This service utilizes RESTful services test harness to verify if implementation follows API contract.

It creates http proxy to record requests and then generates json report.

It is built on [Vert.x][7] embedded server with Java 8 and [Maven](http://maven.apache.org/).

Roadmap
-----------

Following features are planned:

* Integration with [Maven Failsafe][9].
* Integration with [JUnit][10].
* [Gradle][11] build.
* Request and response body verification.

How to start
-----------

#### Starting proxy server.

```sh
mvn exec:java
```

#### Running your tests

By default proxy server listens on [http://localhost:8081][5] and redirect calls to [http://localhost:8080][6].
Remember to set URL location to your RAML file (Check config section for details).

#### Showing report

Open in your browser:

* [http://localhost:8081/raml-validation-proxy/raml_report.json][1] Report with discrepancies with RAML and tests. See report section for detais.
* [http://localhost:8081/raml-validation-proxy/proxy_log.json][2] Recorded requests report.
* [http://localhost:8081/raml-validation-proxy/raml_log.json][3] Parsed RAML file report.
* [http://localhost:8081/raml-validation-proxy/restart][4] Restarts testing session. Reports are cleared.
 
Configuration
-----------

Application is looking for *ramlvalidation_config.json* file on the classpath.

Example configuration with default values:

```json
{
   "target.host" : "localhost",
   "target.port" : 8080,
   "proxy.port" : 8081,
   "raml.resource" : "http://localhost:8080/service.raml",
   "ignored.resources" : [ "request.header.Host", "response.header.Date", "request.header.Accept", "request.header.Connection", "response.header.Server", "request.header.Content-Length", "response.header.Content-Length"
   ]
}
```

Report
-----------

What is checked:

* URLs,
* http methods,
* status codes,
* header names,
* header values (examples and defaults defined in RAML),
* query parameter names,
* query parameters values (examples and defaults defined in RAML).

In case checked value verification failed following messages are added to report:

* **NOT_USED_IN_TEST** : Defined in RAML file but no found in any request or response.
* **NOT_DEFINED_IN_RAML** : Found in request or response but not dfined in RAML file. 

#### Example

```json
{
  "/cars/{id}/resource": {
    "POST" : {
      "request.header.Count" : {
        "orderService" : "NOT_USED_IN_TEST"
      },
      "request.header.Accept" : "NOT_USED_IN_TEST",
      "response.status" : {
        "409" : "NOT_USED_IN_TEST"
      },
      "response.header.Content-Type" : "NOT_DEFINED_IN_RAML"
    }
  },
  "/cars" : "NOT_USED_IN_TEST",
  "/cars/123" : "NOT_DEFINED_IN_RAML"
}
```

  [1]: http://localhost:8081/raml-validation-proxy/raml_report.json
  [2]: http://localhost:8081/raml-validation-proxy/proxy_log.json
  [3]: http://localhost:8081/raml-validation-proxy/raml_log.json
  [4]: http://localhost:8081/raml-validation-proxy/restart
  [5]: http://localhost:8080
  [6]: http://localhost:8081 
  [7]: http://vertx.io/embedding_manual.html
  [8]: http://raml.org/
  [9]: http://maven.apache.org/surefire/maven-failsafe-plugin/
  [10]: http://junit.org/
  [11]: http://www.gradle.org/
