# JSON Validation Service
Snowplow technical test submission

The service uses MongoDB to persist schemas, so Mongo must be running on port 27017 for the service to function (e.g. `docker run -d -p 27017:27017 mongo`)

**To run**
- `sbt compile`
- `sbt run` (or `sbt start` to run the service in the background)

**To run the tests**
- `sbt test`

**To test the use case from the instructions**
- Run the service as described above
- `curl http://localhost:9000/schema/config-schema -d @use-case/config-schema.json -H "Content-Type: application/json"`
- `curl http://localhost:9000/validate/config-schema -X POST -d @use-case/config.json -H "Content-Type: application/json"`