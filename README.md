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

| Parameter name     | Description                                                                                                                                                                                                                                           | Default value         |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| FCS-API-KEY        | the API key used to query data via https://fcsapi.com/                                                                                                                                                                                                | n/a                   |
| UPDATE-CRON        | cron expression to schedule updates. consider __rate limits__ here.<br/> Default is once per hour between 9:16AM and 9:16 PM                                                                                                                          | 0 16 9-21 * * MON-FRI |
| UPDATE-ON-STARTUP  | perform update when app starts independent of configured UPDATE-CRON                                                                                                                                                                                  | false                 |
| CONFIG-URL         | URL pointing to config.json defining stocks and thresholds  <br/> This can either be file or url reference. <br/> Examples: <br/> file:///C:/github/stock-alert/config-example.json <br/> https://mydomain.com/gitops/stock-alert/config-example.json | n/a                   |
| STORAGE            | Storage provider to be used. Chose one of the following: <br>___default___ : for local file system<br/>___google_drive___: see [Google Cloud Storage](#google-cloud-storage) for further configuration                                                | default               |
| __GATEWAYS__       |                                                                                                                                                                                                                                                       |                       |
|                    | __E-Mail__                                                                                                                                                                                                                                            |                       | 
| GATEWAY-EMAIL-HOST | Host to send email notification (IP address or domain name)                                                                                                                                                                                           | localhost             |
| GATEWAY-EMAIL-PORT | TCP Port to be used                                                                                                                                                                                                                                   | 587                   |
| GATEWAY-EMAIL-AUTH | true/false defining if authentication should be used (see also GATEWAY-EMAIL-USER and GATEWAY-EMAIL-PWD )                                                                                                                                             | true                  |
| GATEWAY-EMAIL-USER | user for authentication                                                                                                                                                                                                                               | n/a                   |
| GATEWAY-EMAIL-PWD  | password for authentication                                                                                                                                                                                                                           | n/a                   |
| GATEWAY-EMAIL-TLS  | true/false defining if TLS should be activated (default port is 587)                                                                                                                                                                                  | true                  |
| GATEWAY-EMAIL-SSL  | true/false defining if SSL should be activated (default port is 465)                                                                                                                                                                                  | false                 |

### Storage
To monitor changes it is necessary to store latest values. Without further configuration, data is stored in the user 
directory. Following storage provider are supported.

#### Google Cloud Storage

Use Google Drive as storage provider using a service account. Complete following steps:

1. Open [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project (if one does not already exist)<br/>
   Top left: Select project → “New project.”
3. Activate Drive API<br/>
   Left: “APIs & Services” → “Library.”<br/>
   Search for “Google Drive API” → “Enable.”
4. Create a service account:<br/>
   “APIs & Services” → ‘Credentials’ → “Create credentials” → “Service account.”<br/>
   Assign a name.<br/>
   Optional: Role “Project → Editor” (the Drive permission is more important, will come later).<br/>
   Finish.
5. Download JSON key<br/>
   In the service account overview: Click on the account → “Keys” → “Add key” → “JSON.”
   This JSON contains client_email and private key data, among other things.
6. Encode the JSON file as BASE64 String<br/>
   Using Linux Terminal /WSL ``cat download.json | BASE64 -w 0``
7. Copy output and apply it as Environment variable ``GOOGLE_SERVICE_ACCOUNT``, e.g.<br/>
   ```
   GOOGLE_SERVICE_ACCOUNT=ewogICJ0eXBlIjogInNlcnZpY2VfYWNjb3VudCIsCiAgInByb2plY3RfaWQiOiAic3RvY2stYWxlcnQtNDY4NTIwIiwKICAicHJpdmF0ZV9rZXlfaWQiOiAiYWYxOWFkMzRiMjZjYWNlZWI1NGJiMDBmZTFkNjk2MjAiLAogICJwcml2YXRlX2tleSI6ICItLS0tLUJFR0lOIFBSSVZBVEUgS0VZLS0tLS1cbk1JSUV2Z0lCQURBTkJnZHNhaGNQdnA5dUVcbkxUOGZZVThQSTdjVFlvTGpwTmUvVkI0ZWprZGFzV3piMG5LVFBWc0Znd01cbitrbmdoY2lNRENFbTUrSHFHNVxuLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLVxuIiwKICAiY2xpZW50X2VtYWlsIjogInN0b2NrLWFsZXJ0LXN0b3JhZ2UtcHJvdmlkZXJAaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLAogICJjbGllbnRfaWQiOiAiODU5NDMwODUzMjkwODQyMzkwIiwKICAiYXV0aF91cmkiOiAiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tL28vb2F1dGgyL2F1dGgiLAogICJ0b2tlbl91cmkiOiAiaHR0cHM6Ly9vYXV0aDIuZ29vZ2xlYXBpcy5jb20vdG9rZW4iLAogICJhdXRoX3Byb3ZpZGVyX3g1MDlfY2VydF91cmwiOiAiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vb2F1dGgyL3YxL2NlcnRzIiwKICAiY2xpZW50X3g1MDlfY2VydF91cmwiOiAiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vcm9ib3QvdjEvbWV0YWRhdGEveDUwOS9zdG9jay1hbGVydC1zdG9yYWdlLXByb3ZpZGVyJTQwc3RvY2stYWxlcnQtNDY4NTIwLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwKICAidW5pdmVyc2VfZG9tYWluIjogImdvb2dsZWFwaXMuY29tIgp9Cg==
   ```
8. In Google Drive, share any desired folder → “Share” → enter the email address found in downloaded json file → grant appropriate permissions (edit).

> [Google Cloud Console](https://console.cloud.google.com/welcome?inv=1&invt=Ab5C6Q&project=stock-alert-468520)

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
           arburk/stock-alert:stock-alert-0.1.0
```
- execute container with mounted config file, assuming, the config file is ``/home/user/my-config/my-config.json``
```
docker run -e FCS-API-KEY=your-api-key \
           -e GATEWAY-EMAIL-HOST=smtp.provider.com \
           -e GATEWAY-EMAIL-USER=you@provider.com \
           -e GATEWAY-EMAIL-PWD=<your-secret-password> \
           -v /home/user/my-config:/config \
           -e CONFIG-URL=/config/my-config.json
           arburk/stock-alert:stock-alert-0.1.0
```

## Build and Development

[![CI](https://github.com/arburk/stock-alert/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/arburk/stock-alert/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=arburk_stock-alert&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=arburk_stock-alert)

- build the source by ``mvn -B clean verify --file pom.xml``
- build the Docker image by ``docker build -t stock-alert:0.1.1-SNAPSHOT . ``
- run the docker image using this very version as described in [Docker](#docker)

