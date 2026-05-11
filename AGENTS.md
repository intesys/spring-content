# AGENTS.md — Spring Content

High-signal facts to avoid guessing wrong in this multi-module Java/Maven repo.

## Project Basics

- **Group**: `it.intesys` | **Java**: 17 | **Maven**: wrapper (`./mvnw`) uses 3.6.3
- **Boot**: 3.5.0 | **Spring Cloud**: 2024.0.1 | **Jakarta Persistence**: 3.1.0
- **Maintained by Intesys**; forked from `paulcwarren/spring-content`.

## Build Commands

- Quick build (units only, no ITs):  
  `AWS_REGION=us-west-1 ./mvnw clean install`
- Full build with integration tests:  
  `AWS_REGION=us-west-1 ./mvnw -P tests clean install`
- With reference docs:  
  `AWS_REGION=us-west-1 ./mvnw -P docs clean install`
- CI shortcut (no javadoc):  
  `mvn -B -P tests -Dmaven.javadoc.skip=true install`

> **Why `AWS_REGION`:** Required for `spring-content-s3` (LocalStack tests). Already set in the devcontainer; omitting it can cause S3 IT failures.

## Monorepo Layout

Key module families:

- **Core libs**: `spring-content-commons`, `spring-content-fs`, `spring-content-jpa`, `spring-content-mongo`, `spring-content-s3`, `spring-content-rest`, `spring-content-solr`, `spring-content-elasticsearch`, `spring-content-renditions`, `spring-content-metadata-extraction`, `spring-content-encryption`, `spring-content-azure-storage`, `spring-content-gcs`
- **Starters**: `spring-content-*-boot-starter` (current naming) and legacy `content-*-spring-boot-starter`
- **Versions**: `spring-versions-commons`, `spring-versions-jpa`, `spring-versions-jpa-boot-starter`
- **BOM**: `spring-content-bom`
- **Autoconfigure**: `spring-content-autoconfigure`
- `spring-content-docx4j` is currently **commented out** in the root `pom.xml` modules list.

## Testing

- **Framework**: [ginkgo4j](https://github.com/paulcwarren/ginkgo4j) BDD (not plain JUnit).  
  Tests run via a custom `JUnitRunListener` (`com.github.paulcwarren.ginkgo4j.maven.JUnitRunListener`).
- **Unit tests**: `*Test.java` — run by default.
- **Integration tests**: `*IT.java` and `*Tests.java` — **only run with `-P tests`**.
- Many ITs use Testcontainers / LocalStack / embedded DBs; Docker availability matters for a full `-P tests` pass.
- To run a single test class:  
  `./mvnw -pl <module> test -Dtest=ClassName`

## Style & Conventions

- Use **Spring Framework code format** via `eclipse/eclipse-code-formatter.xml`.
- New `.java` files need:
  - ASF license header (copy from an existing file)
  - Javadoc class comment with at least `@author`
- Add `@author` to files you modify substantially.
- Commit messages: follow [standard git conventions](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html); append `Fixes gh-XXXX` when applicable.

## CI / Release Notes

- PRs run `CLAAssistant` (sign the CLA) + build with `-P tests` + validation against the `spring-content-gettingstarted` repo.
- Tags trigger release build (`-P ci,docs deploy`), GPG signing, Maven Central publish, and docs publish to `gh-pages`.
- Docs output path: `target/generated-docs/refs/dev/`

## Local Dev Tips

- Devcontainer is configured with `AWS_REGION=us-west-1` and Docker-in-Docker.
- If importing into Eclipse, use the `.setup` file or import the root `pom.xml` with M2Eclipse.
