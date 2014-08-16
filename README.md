raml-validation-proxy
=====================

Creates http proxy to record requests and then validates with raml specification.
Generates json report.

It is built on the base of vert.x.

How to start
-----------

#### Starting proxy server.

```sh
mvn exec:java
```

#### Running your tests

By default proxy server listens on http://localhost:8081 and redirect calls to http://localhost:8080.
Remember to set URL location to your RAML file (Check config section for details).

#### Showing report

Open in your browser:

* [http://localhost:8081/raml-validation-proxy/raml_report.json][1]
    Report with discrepancies with RAML and tests. Currently checked: NOT_CALLED_IN_TESTS, NOT_DEFINED_IN_RAML, NOT_USED_IN_TEST
* [http://localhost:8081/raml-validation-proxy/proxy_log.json][2] Recorded requests report.
* [http://localhost:8081/raml-validation-proxy/raml_log.json][3] Parsed RAML report.
* [http://localhost:8081/raml-validation-proxy/restart][4] Restarting testing session.
 
Configuration
-----------

Application is looking for ramlvalidation_config.json file on the classpath.

Example configuration with default values:

```json
{
   "target.host" : "localhost",
   "target.port" : "8080",
   "proxy.port" : "8081",
   "raml.resource" : "http://localhost:8080/service.raml",
   "ignored.resources" : [ "request.header.Host", "response.header.Date", "request.header.Accept", "request.header.Connection", "response.header.Server", "request.header.Content-Length", "response.header.Content-Length"
   ]
}
```

Sample Report
-----------
```json
{
  "/{service}/resource": 
    {
    "POST" : {
      "request.header.Count" : {
        "orderService" : "NOT_USED_IN_TEST"
      },
      "request.header.Accept" : "NOT_CALLED_IN_TESTS",
      "response.status" : {
        "409" : "NOT_USED_IN_TEST"
      },
      "response.header.Content-Type" : "NOT_DEFINED_IN_RAML"
    }
  }
}
```

  [1]: http://localhost:8081/raml-validation-proxy/raml_report.json
  [2]: http://localhost:8081/raml-validation-proxy/proxy_log.json
  [3]: http://localhost:8081/raml-validation-proxy/raml_log.json
  [4]: http://localhost:8081/raml-validation-proxy/restart
