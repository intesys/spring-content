## Context

The project historically used ginkgo4j, a custom BDD framework for Java, to write unit and integration tests. Spring Boot already provides JUnit 5 (Jupiter) and Mockito out of the box. The ginkgo4j library references have already been removed from the Maven POMs, and some test classes were partially converted, but a large number of tests still import ginkgo4j classes or are left in an uncompilable intermediate state (e.g., commented-out code, mixed JUnit 4/5 annotations, invalid initializer blocks).

There are ~289 remaining references to ginkgo4j across the test source tree. The project uses a custom `JUnitRunListener` for ginkgo4j, but with JUnit 5 this is no longer needed. The goal is to finish the migration so that the entire test suite compiles and passes without any ginkgo4j classes on the classpath.

## Goals / Non-Goals

**Goals:**
- Convert every test class that still references ginkgo4j to standard JUnit 5 / Mockito / Spring Boot Test.
- Restore partially migrated or broken test files to a compilable, passing state.
- Preserve existing test coverage and behavior; do not alter the intent of any test.
- Ensure `./mvnw clean install` (unit tests) and `./mvnw -P tests clean install` (integration tests) succeed.
- Remove all imports and usages of `Ginkgo4jDSL`, `Ginkgo4jSpringRunner`, `Ginkgo4jRunner`, `Ginkgo4jConfiguration`, and `Ginkgo4jMatchers`.

**Non-Goals:**
- Changing production (non-test) source code.
- Introducing new testing libraries beyond what Spring Boot already provides (JUnit 5, Mockito, AssertJ if already present).
- Rewriting tests to a different style (e.g., plain JUnit without nested classes) when the existing BDD structure can be mapped cleanly to `@Nested`.
- Expanding or reducing test coverage.

## Decisions

1. **Preserve BDD readability with `@Nested` / `@DisplayName`**
   - *Rationale*: ginkgo4j tests use `Describe`, `Context`, `It` blocks that provide rich nesting and readable output. JUnit 5 `@Nested` classes with `@DisplayName` map directly to this structure, keeping test names human-friendly and preserving the original hierarchy.
   - *Alternative considered*: Flattening all tests into single-level `@Test` methods. Rejected because it would destroy readability and make large test classes unmanageable.

2. **Map `JustBeforeEach` to `@BeforeEach` in the innermost `@Nested` class or inline in the `@Test`**
   - *Rationale*: ginkgo4j `JustBeforeEach` runs immediately before each test, after all other setup. In JUnit 5, `@BeforeEach` runs before every test in the scope where it is declared. Placing the action inside the same `@Nested` class as the `@Test` replicates the execution order. If a `JustBeforeEach` is shared across sibling contexts, it can be extracted to a helper method called by each `@Test`.

3. **Replace `Ginkgo4jSpringRunner` with `@SpringBootTest` / `@ExtendWith(SpringExtension.class)`**
   - *Rationale*: Spring Boot 3.x / Spring Framework 6.x uses the JUnit 5 `SpringExtension`. `@SpringBootTest` already includes it. For plain Spring integration tests that do not need the full web environment, `@ExtendWith(SpringExtension.class)` + `@ContextConfiguration` is the direct replacement.

4. **Keep existing assertion libraries (Hamcrest, AssertJ) where already used**
   - *Rationale*: Minimizing churn. The migration focuses on framework structure, not assertion style. New or rewritten assertions should prefer AssertJ or standard JUnit 5 assertions for consistency with modern Spring Boot practices.

5. **Migrate module by module, starting with core libraries (`spring-content-commons`) and then outward**
   - *Rationale*: Core modules have fewer external dependencies, making it easier to verify compilation and unit test behavior first. Integration tests in storage modules (S3, Mongo, etc.) often require Docker/Testcontainers and can be validated after the unit tests are stable.

## Risks / Trade-offs

- **[Risk] Large surface area → missed imports or broken tests**  
  *Mitigation*: Use automated search (`grep -r "ginkgo4j"`) after each module to verify zero remaining references. Run the full build (`-P tests`) before declaring the change complete.

- **[Risk] Partially migrated files have invalid Java syntax**  
  *Mitigation*: Prioritize "fixup" tasks for any file that does not compile. Use the compiler error output as a guide. In some cases it may be faster to revert the file to the original ginkgo4j version and re-migrate cleanly.

- **[Risk] Integration tests rely on ginkgo4j-specific `JUnitRunListener` behavior**  
  *Mitigation*: The listener is no longer configured in POMs (ginkgo4j is already removed), so tests must run with standard Maven Surefire/Failsafe. Verify that Surefire/Failsafe are correctly picking up `*Test.java` (units) and Failsafe is picking up `*IT.java` / `*Tests.java` (ITs). The project already uses standard naming, so no configuration change is expected.

- **[Trade-off] Preserving nested structure increases verbosity**  
  *Acceptance*: `@Nested` classes add boilerplate compared to flat ginkgo4j closures, but this is the standard JUnit 5 idiom and is well supported by IDEs and build tools.
