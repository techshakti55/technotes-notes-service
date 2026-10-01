# Notes image: first-live preparation

This branch adds packaging only. Endpoints, validation, Java 21, and Spring
configuration are unchanged. It does not deploy AWS resources.

## Build and inspect

From the repository root, with Docker Desktop's Linux engine running:

```powershell
docker build --platform linux/amd64 -t technotes-notes:local .
docker image inspect technotes-notes:local --format '{{.Os}}/{{.Architecture}} user={{.Config.User}}'
docker run --rm --entrypoint java technotes-notes:local -version
```

Expected: linux/amd64, user 10001:10001, Java 21. The java-version command does
not start Spring or connect to a database.

The build compiles the project and packages the executable JAR. Tests are
deliberately a separate release gate, not executed by this Dockerfile. Run
the project's appropriate Maven tests before merging/releasing.

## Runtime configuration still required

Supply configuration at runtime, not inside this image:

- MONGODB_URI: reachable MongoDB URI; publishing requires the replica set.
- OAUTH_ISSUER_URI: exact issuer used in the access token.
- OAUTH_JWK_SET_URI: JWKS URL reachable from the container.
- EUREKA_CLIENT_ENABLED: false for a deployment using static Gateway routes;
  otherwise supply the discovery configuration.
- JAVA_TOOL_OPTIONS: memory settings chosen during the full-stack trial.

Inside a container, localhost is that container. Do not reuse laptop
localhost database or JWKS URLs without adapting connectivity.

EXPOSE does not publish a host port. The final Compose configuration will
define private service networking, resource limits and health checks.

## Pending before production

- Full Compose integration, including OAuth keys and database persistence.
- Representative full-stack memory test; 2 GiB suitability is unconfirmed.
- Database backup/restore and restart tests.
- Pin base images/artifacts to verified digests for the production release.

No passwords, environment files, laptop build output or private keys are
included in the build context. The final image contains a JRE and application
JAR; Maven is only in the build stage.
