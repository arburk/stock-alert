# stock-alert

> Track securities and raise defined alerts

Using the (unofficial) [Yahoo Finance chart API](https://query1.finance.yahoo.com/v8/finance/chart/NESN.SW?interval=1d&range=1d)
to monitor stocks and send alerts when defined thresholds are exceeded or falling below.
No API key is required; quotes are delayed by roughly 15 minutes. Since the API is
unofficial, it may change or be rate limited at any time.




## Configuration

Stocks, thresholds and alerting is configured via config.json. The file itself can be managed either via git repository,
any other accessible URL or local file system. The path to will be specified by the environment parameter ``CONFIG-URL``
(see section [Runtime](#runtime)).

See following [config-example.json](src/main/resources/config-example.json)

The ``symbol`` of each security must be the __Yahoo Finance ticker__, e.g. ``NESN.SW`` (SIX),
``ALV.DE`` (Xetra), ``CS.PA`` (Paris), ``INGA.AS`` (Amsterdam), or plain ``MMM`` for US exchanges.
The ``exchange`` value is a free label that is echoed in notifications.



## Runtime

Define the following mandatory environment parameter:

| Parameter name                | Description                                                                                                                                                                                                                                           | Default value                 |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| UPDATE-CRON                   | cron expression to schedule updates. consider __rate limits__ here.<br/> Default is once per hour between 9:16AM and 9:16 PM                                                                                                                          | 0 16 9-21 * * MON-FRI         |
| UPDATE-ON-STARTUP             | perform update when app starts independent of configured UPDATE-CRON                                                                                                                                                                                  | false                         |
| CONFIG-URL                    | URL pointing to config.json defining stocks and thresholds  <br/> This can either be file or url reference. <br/> Examples: <br/> file:///C:/github/stock-alert/config-example.json <br/> https://mydomain.com/gitops/stock-alert/config-example.json | n/a                           |
| STORAGE                       | Storage provider to be used. Chose one of the following: <br>___default___ : for local file system<br/>___s3___: for S3 kompatible bucket.                                                                                                            | default                       |
|                               | __S3__                                                                                                                                                                                                                                                |                               |
| S3-ENDPOINT                   | Endpoint URL of S3 provider                                                                                                                                                                                                                           | n/a                           |
| S3-ACCESS-KEY                 |                                                                                                                                                                                                                                                       | n/a                           |
| S3-SECRET-KEY                 |                                                                                                                                                                                                                                                       | n/a                           |
| S3-BUCKET                     | Name of bucket to be used for                                                                                                                                                                                                                         | stock-alert                   |
| S3-REGION                     | Region name if supported by provider                                                                                                                                                                                                                  | n/a                           |
| S3-ENDPOINT-FORCE-PATH-STYLE  | __true__: use endpoint.tld/bucket <br/> __false__: use bucket.endpoint.tld                                                                                                                                                                            | true                          |
||||
| __GATEWAYS__                  |                                                                                                                                                                                                                                                       |                               |
|                               | __E-Mail__                                                                                                                                                                                                                                            |                               | 
| GATEWAY-EMAIL-HOST            | Host to send email notification (IP address or domain name)                                                                                                                                                                                           | localhost                     |
| GATEWAY-EMAIL-PORT            | TCP Port to be used                                                                                                                                                                                                                                   | 587                           |
| GATEWAY-EMAIL-AUTH            | true/false defining if authentication should be used (see also GATEWAY-EMAIL-USER and GATEWAY-EMAIL-PWD )                                                                                                                                             | true                          |
| GATEWAY-EMAIL-USER            | user for authentication                                                                                                                                                                                                                               | n/a                           |
| GATEWAY-EMAIL-PWD             | password for authentication                                                                                                                                                                                                                           | n/a                           |
| GATEWAY-EMAIL-TLS             | true/false defining if TLS should be activated (default port is 587)                                                                                                                                                                                  | true                          |
| GATEWAY-EMAIL-SSL             | true/false defining if SSL should be activated (default port is 465)                                                                                                                                                                                  | false                         |
| GATEWAY-EMAIL-SENDER-ADDRESS  | set the FROM address to be used as sender email                                                                                                                                                                                                       | stock-alert@arburk.github.com |
| GATEWAY-EMAIL-DEBUG           | set to __true__ to output detailed mail logs if something is not working to figure out the root cause                                                                                                                                                 | false                         |

### Docker
Either build the source on your own as described in  [Build and Development](#build-and-development) or operate a pre-build
image hosted on [docker hub](https://hub.docker.com/r/arburk/stock-alert).
Required parameter from the table above are added by ``-e param=value`` syntax. 

Examples:
- execute container with git hosted config
```
docker run -e CONFIG-URL=https://raw.githubusercontent.com/arburk/stock-alert/refs/heads/main/src/main/resources/config-example.json \
           -e GATEWAY-EMAIL-HOST=smtp.provider.com \
           -e GATEWAY-EMAIL-USER=you@provider.com \
           -e GATEWAY-EMAIL-PWD=<your-secret-password> \
           arburk/stock-alert:0.5.0
```
- execute container with mounted config file, assuming, the config file is ``/home/user/my-config/my-config.json``
```
docker run -e GATEWAY-EMAIL-HOST=smtp.provider.com \
           -e GATEWAY-EMAIL-USER=you@provider.com \
           -e GATEWAY-EMAIL-PWD=<your-secret-password> \
           -v /home/user/my-config:/config \
           -e CONFIG-URL=/config/my-config.json
           arburk/stock-alert:0.5.0
```

## Build and Development

[![CI](https://github.com/arburk/stock-alert/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/arburk/stock-alert/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=arburk_stock-alert&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=arburk_stock-alert)

- build the source by ``mvn -B clean verify --file pom.xml``
- build the Docker image by ``docker build -t stock-alert:0.5.1-SNAPSHOT . ``
- run the docker image using this very version as described in [Docker](#docker)

## Change Log

- 1.0.0 - __!Breaking!__ - switch to Yahoo Finance chart API (formerly FCS API)
- 0.5.1 - dependency updates
- 0.5.0 - __!Breaking!__ - upgrade to Java 25 and Spring Boot 4
- 0.4.1 - dependency updates
- 0.4.0 - removed deprecated methods
