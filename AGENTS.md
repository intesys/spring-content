# Agent Instructions for Spring Content

## Build Commands

```bash
# Basic build (unit tests only)
./mvnw clean install

# With integration tests (includes IT.java and Tests.java)
./mvnw -P tests clean install

# With reference docs generation
./mvnw -P docs clean install
```

**Important**: Integration tests require `AWS_REGION=us-west-1` (for S3/localstack).

## Environment

- **JDK**: 17 required
- **Devcontainer**: Already sets `AWS_REGION=us-west-1` (required for S3 tests)
- **Windows**: Git clone needs `git clone -c core.longPaths=true ...` due to long file paths

## Test Conventions

| Type | Pattern | Run with |
|------|---------|----------|
| Unit tests | `*Test.java` | Default (no profile) |
| Integration tests | `*IT.java`, `*Tests.java` | `-P tests` |

Tests use **ginkgo4j** BDD framework + JUnit. Test packages:
- `org/` - unit tests
- `it/` - integration tests
- `internal/` - internal implementation tests

## Module Structure

- `spring-content-commons` - Base module (interfaces, core logic)
- `spring-content-jpa/mongo/fs/s3/gcs/azure-storage` - Storage backends
- `spring-content-rest` - REST API layer
- `spring-content-solr/elasticsearch` - Search integration
- `spring-content-renditions` - Content rendering/transform
- `spring-content-encryption` - Content encryption
- `spring-content-metadata-extraction` - Metadata extraction
- `*-boot-starter` modules - Spring Boot auto-configuration
- `spring-versions-*` - Versioning/locking for JPA

Each storage module depends on `spring-content-commons`. REST depends on commons + specific storage.

## Running Single Module Tests

```bash
cd spring-content-commons && ../mvnw test                    # unit tests only
cd spring-content-rest && ../mvnw -P tests test           # includes integration tests
```

## Code Formatting

Import `eclipse/eclipse-code-formatter.xml` into IntelliJ via Eclipse Code Formatter plugin, or use in Eclipse directly.

## Dependencies

- Spring Boot 4.0.5
- Spring Cloud 2025.1.1
- Hibernate ORM 6.1.7
- Testcontainers for database containers in ITs

## Architecture Notes

- Stores use `ContentStore<T, ID>` interface pattern
- JPA uses `Blob`/`GenericBlobResource` for binary content
- MongoDB uses GridFS
- S3 uses AWS SDK v2
- Integration tests in `spring-content-rest` run against multiple DBs (H2, MySQL, PostgreSQL, Oracle, SQL Server) via testcontainers
