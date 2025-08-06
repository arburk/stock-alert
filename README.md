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

| Parameter name | Description                                                                                                                                                                                                                                           | Default value         |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| FCS-API-KEY    | the API key used to query data via https://fcsapi.com/                                                                                                                                                                                                | n/a                   |
| UPDATE-CRON    | cron expression to schedule updates. consider __rate limits__ here.<br/> Default is once per hour between 9:16AM and 9:16 PM                                                                                                                          | 0 16 9-21 * * MON-FRI |
| GATEWAY-EMAIL  | Host to send email notification (IP address or domain name)                                                                                                                                                                                           | localhost             |
| CONFIG-URL     | URL pointing to config.json defining stocks and thresholds  <br/> This can either be file or url reference. <br/> Examples: <br/> file:///C:/github/stock-alert/config-example.json <br/> https://mydomain.com/gitops/stock-alert/config-example.json | n/a                   |



## Build and Development

[![CI](https://github.com/arburk/stock-alert/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/arburk/stock-alert/actions/workflows/ci.yml)


