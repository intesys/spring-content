## ADDED Requirements

### Requirement: Integration tests no longer reference ginkgo4j
All integration test classes (`*IT.java`, `*Tests.java`) SHALL compile and run without any import or usage of ginkgo4j classes.

#### Scenario: MongoStoreIT compiles and runs
- **WHEN** the build compiles `spring-content-mongo` integration test sources with `-P tests`
- **THEN** `MongoStoreIT.java` contains zero ginkgo4j imports and passes its integration tests

#### Scenario: S3StoreWithEntityConverterIT compiles and runs
- **WHEN** the build compiles `spring-content-s3` integration test sources with `-P tests`
- **THEN** `S3StoreWithEntityConverterIT.java` contains zero ginkgo4j imports and passes its integration tests

### Requirement: Spring runner replaced by SpringExtension
Test classes that previously used `Ginkgo4jSpringRunner` or `Ginkgo4jRunner` SHALL use the standard JUnit 5 Spring test support (`@SpringBootTest`, `@ExtendWith(SpringExtension.class)`, or `@ContextConfiguration` as appropriate).

#### Scenario: Spring Boot IT uses @SpringBootTest
- **WHEN** an integration test previously annotated with `@RunWith(Ginkgo4jSpringRunner.class)` is migrated
- **THEN** the converted test uses `@SpringBootTest` or `@ExtendWith(SpringExtension.class)` and retains the same Spring context configuration

### Requirement: Testcontainers and LocalStack tests remain functional
Integration tests that rely on Testcontainers or LocalStack SHALL continue to start the required containers and execute successfully after the ginkgo4j removal.

#### Scenario: S3 integration tests pass with LocalStack
- **WHEN** the `spring-content-s3` integration tests are executed
- **THEN** LocalStack container starts, tests run against it, and all assertions pass without ginkgo4j
