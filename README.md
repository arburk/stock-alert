# stock-alert

> Track securities and raise defined alerts

Using the [Free Stock-Market API](https://fcsapi.com/document/stock-api#stock-report)
to monitor stocks and send alerts when defined thresholds are exceeded or falling below.




## Configuration

Stocks, thresholds and alerting is configured via config.json. The file itself can be managed either via git repository,
any other accessible URL or local file system. The path to will be specified by the environment parameter ``CONFIG-URL``
(see section [Runtime](#runtime)).

See following [config-example.json](src/main/resources/config-example.json)



## Runtime

Define the following mandatory environment parameter:

| Parameter name      | Description                                                                                                                                                                                                                                           | Default value         |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| FCS-API-KEY         | the API key used to query data via https://fcsapi.com/                                                                                                                                                                                                | n/a                   |
| UPDATE-CRON         | cron expression to schedule updates. consider __rate limits__ here.<br/> Default is once per hour between 9:16AM and 9:16 PM                                                                                                                          | 0 16 9-21 * * MON-FRI |
| CONFIG-URL          | URL pointing to config.json defining stocks and thresholds  <br/> This can either be file or url reference. <br/> Examples: <br/> file:///C:/github/stock-alert/config-example.json <br/> https://mydomain.com/gitops/stock-alert/config-example.json | n/a                   |
| __GATEWAYS__        |                                                                                                                                                                                                                                                       |
|                     | __E-Mail__                                                                                                                                                                                                                                            |                       | 
| GATEWAY-EMAIL-HOST  | Host to send email notification (IP address or domain name)                                                                                                                                                                                           | localhost             |
| GATEWAY-EMAIL-PORT  | TCP Port to be used                                                                                                                                                                                                                                   | 587                   |
| GATEWAY-EMAIL-AUTH  | true/false defining if authentication should be used (see also GATEWAY-EMAIL-USER and GATEWAY-EMAIL-PWD )                                                                                                                                             | true                  |
| GATEWAY-EMAIL-USER  | user for authentication                                                                                                                                                                                                                               | n/a                   |
| GATEWAY-EMAIL-PWD   | password for authentication                                                                                                                                                                                                                           | n/a                   |
| GATEWAY-EMAIL-TLS   | true/false defining if TLS should be activated (default port is 587)                                                                                                                                                                                  | true                  |
| GATEWAY-EMAIL-SSL   | true/false defining if SSL should be activated (default port is 465)                                                                                                                                                                                  | false                 |

### Docker
Either build the source on your own as described in  [Build and Development](#build-and-development) or operate a pre-build
image hosted on [docker hub](https://hub.docker.com/r/arburk/stock-alert).
Required parameter from the table above are added by ``-e param=value`` syntax. 

Examples:
- execute container with git hosted config
```
docker run -e FCS-API-KEY=your-api-key \
           -e CONFIG-URL=https://raw.githubusercontent.com/arburk/stock-alert/refs/heads/main/src/main/resources/config-example.json \
           -e GATEWAY-EMAIL-HOST=smtp.provider.com \
           -e GATEWAY-EMAIL-USER=you@provider.com \
           -e GATEWAY-EMAIL-PWD=<your-secret-password> \
           arburk/stock-alert:0.0.1-SNAPSHOT
```
- execute container with mounted config file, assuming, the config file is ``/home/user/my-config/my-config.json``
```
docker run -e FCS-API-KEY=your-api-key \
           -e GATEWAY-EMAIL-HOST=smtp.provider.com \
           -e GATEWAY-EMAIL-USER=you@provider.com \
           -e GATEWAY-EMAIL-PWD=<your-secret-password> \
           -v /home/user/my-config:/config \
           -e CONFIG-URL=/config/my-config.json
           arburk/stock-alert:0.0.1-SNAPSHOT
```

## Build and Development

[![CI](https://github.com/arburk/stock-alert/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/arburk/stock-alert/actions/workflows/ci.yml)

- build the source by ``mvn -B clean verify --file pom.xml``
- build the Docker image by ``docker build -t stock-alert:0.0.1-SNAPSHOT . ``
- run the docker image using this very version as described in [Docker](#docker)

