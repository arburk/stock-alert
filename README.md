# stock-alert

> Track securities and raise defined alerts

Using the (Free Stock-Market API)[https://fcsapi.com/document/stock-api#stock-report]
to monitor stocks and send alerts when defined thresholds are exceeded or falling below.

## Configuration

TODO: configure securities and threshold

## Runtime

Define the following mandatory environment parameter:

| Parameter name | Description                                                                                                                  | Default value         |
|----------------|------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| FCS-API-KEY    | the API key used to query data via https://fcsapi.com/                                                                       | none                  |
| UPDATE-CRON    | cron expression to schedule updates. consider __rate limits__ here.<br/> Default is once per hour between 9:16AM and 9:16 PM | 0 16 9-21 * * MON-FRI |


## Build and Development
