# Rawdata Master
TIM reference implementation to collect raw data based on spring boot and kotlin.

# Usage
1. Run `./mvnw compile`, it should generate some classes from the schema files in `./OPB3.0_Schemadateien_R300_ab01.10.2019`.
2. Start the postgres database container from `./docker-compose.yml`.
3. Start the registration service as docker container or locally via spring boot

## Probes
The health and readiness of this service can be obtained at the following endpoints:

- `GET :9021/actuator/health/liveness`
- `GET :9021/actuator/health/readiness`

`liveness` includes health states of the following components:

- `web-server`

`readiness` includes health states of the following components:

- `database`

# License Report
Create a license report for the local version by submitting the following command:
```bash
$ ./mvnw license:third-party-report -f rdm-backend/pom.xml
```
This creates an HTML file plus assets in `$PROJECT_ROOT/rdm-backend/target/site/third-party-report.html` which contains the license report. Use your favorite browser to view the report.
