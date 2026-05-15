## Why

The project currently uses ginkgo4j, a custom BDD testing framework, while Spring Boot already ships with JUnit 5 and Mockito. A partial migration has already been started (ginkgo4j dependencies were removed from the POMs and some tests already use JUnit 5), but many test classes still import ginkgo4j or are left in a broken intermediate state. Completing the migration removes an unnecessary external dependency, aligns the codebase with standard Spring ecosystem tooling, improves IDE support, and simplifies maintenance.

## What Changes

- Convert all remaining test classes that reference `ginkgo4j` to pure JUnit 5 / Mockito / Spring Boot Test.
- Remove all imports and usages of `Ginkgo4jDSL`, `Ginkgo4jSpringRunner`, `Ginkgo4jRunner`, `Ginkgo4jConfiguration`, and `Ginkgo4jMatchers`.
- Rewrite BDD-style blocks (`Describe`, `Context`, `It`, `BeforeEach`, `JustBeforeEach`, `AfterEach`) into standard JUnit 5 structures: `@Nested`, `@DisplayName`, `@Test`, `@BeforeEach`, `@AfterEach`.
- Fix partially migrated test files that are currently broken (e.g., commented-out code, invalid syntax, mixed JUnit 4 runners with JUnit 5 annotations).
- Ensure the full test suite (unit and integration tests) compiles and passes after the migration.

## Capabilities

### New Capabilities
- `unit-test-migration`: Migrate unit test classes (`*Test.java`) from ginkgo4j to JUnit 5 / Mockito.
- `integration-test-migration`: Migrate integration test classes (`*IT.java`, `*Tests.java`) from ginkgo4j to JUnit 5 / Spring Boot Test / Testcontainers.
- `partial-migration-fixup`: Repair test files that were partially converted and are currently uncompilable or incomplete.

### Modified Capabilities
<!-- No existing user-facing capabilities are changing; this is an internal test-only refactoring. -->
- None

## Impact

- Affects test source code in all modules (`spring-content-commons`, `spring-content-fs`, `spring-content-jpa`, `spring-content-mongo`, `spring-content-s3`, `spring-content-rest`, `spring-content-solr`, `spring-content-gcs`, `spring-content-azure-storage`, `spring-versions-jpa`, `spring-content-autoconfigure`, etc.).
- No changes to production APIs, runtime behavior, or public contracts.
- Build commands remain unchanged; the goal is to make existing tests pass without ginkgo4j.
