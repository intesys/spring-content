## ADDED Requirements

### Requirement: Broken partially migrated tests are restored to compilable state
Any test file that was left in an intermediate, uncompilable, or commented-out state during earlier partial migration efforts SHALL be fully converted to valid JUnit 5 / Mockito code.

#### Scenario: DefaultFilesystemStoresImplTest compiles and passes
- **WHEN** the build compiles `spring-content-fs` test sources
- **THEN** `DefaultFilesystemStoresImplTest.java` is not commented out, has valid Java syntax, and passes its unit tests

#### Scenario: GridFSResourceTest compiles and passes
- **WHEN** the build compiles `spring-content-mongo` test sources
- **THEN** `GridFSResourceTest.java` is not commented out, has valid Java syntax, and passes its unit tests

### Requirement: No remaining ginkgo4j references in test source tree
After the migration, a full-text search of the entire `src/test/java` tree SHALL return zero occurrences of the string `ginkgo4j`.

#### Scenario: grep returns zero matches
- **WHEN** a search for `ginkgo4j` is executed across all `src/test/java` directories
- **THEN** the result set is empty (excluding legitimate references in non-test documentation, if any)
