## ADDED Requirements

### Requirement: Unit tests no longer reference ginkgo4j
All unit test classes (`*Test.java`) SHALL compile and run without any import or usage of ginkgo4j classes.

#### Scenario: DefaultS3StoreImplTest compiles
- **WHEN** the build compiles `spring-content-s3` test sources
- **THEN** `DefaultS3StoreImplTest.java` contains zero ginkgo4j imports and passes its unit tests

#### Scenario: EnableMongoStoresTest compiles
- **WHEN** the build compiles `spring-content-mongo` test sources
- **THEN** `EnableMongoStoresTest.java` contains zero ginkgo4j imports and passes its unit tests

### Requirement: BDD structure mapped to JUnit 5 nested classes
Test classes that previously used `Describe`, `Context`, or `It` blocks SHALL be rewritten using JUnit 5 `@Nested` classes with `@DisplayName`, and individual assertions SHALL use `@Test` methods.

#### Scenario: Nested describe block becomes @Nested class
- **WHEN** a test class contains a ginkgo4j `Describe` block
- **THEN** the converted test uses a `@Nested` inner class annotated with `@DisplayName` matching the original description

#### Scenario: It block becomes @Test method
- **WHEN** a test class contains a ginkgo4j `It` block
- **THEN** the converted test uses a `@Test` method annotated with `@DisplayName` matching the original description

### Requirement: Mockito remains the mocking framework
All mock setup and verification previously performed with ginkgo4j DSL SHALL continue to use Mockito (`mock()`, `when()`, `verify()`, etc.) without behavioral changes.

#### Scenario: Mocked resource loader still verified
- **WHEN** a converted test sets up a mocked `FileSystemResourceLoader`
- **THEN** the test verifies the same interactions using Mockito as the original ginkgo4j test did
