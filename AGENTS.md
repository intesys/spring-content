# AGENTS.md — Spring Content

High-signal facts to avoid guessing wrong in this multi-module Java/Maven repo.

## Project Basics

- **Group**: `it.intesys` | **Java**: 25 | **Maven**: wrapper (`./mvnw`) uses 3.6.3
- **Boot**: 4.0.5 | **Spring Cloud**: 2025.1.1
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

- **Framework**: JUnit 5 / Mockito / Spring Boot Test (ginkgo4j was removed by the archived change `2026-05-15-migrate-tests-from-ginkgo4j-to-junit`; write new tests in plain JUnit 5, using `@Nested` / `@DisplayName` for BDD-style grouping).
- **Unit tests**: `*Test.java` — run by default.
- **Integration tests**: `*IT.java` and `*Tests.java` — **only run with `-P tests`**.
- Many ITs use Testcontainers / LocalStack / embedded DBs; Docker availability matters for a full `-P tests` pass.
- To run a single test class:  
  `./mvnw -pl <module> test -Dtest=ClassName`

## Style & Conventions

- Use **Spring Framework code format** via `eclipse/eclipse-code-formatter.xml`.
- New `.java` files need:
  - GPLv3 license header (copy from `HEADER.txt`); pre-existing ASF-headered files keep their original header
  - Javadoc class comment with at least `@author`
- Add `@author` to files you modify substantially.
- Commit messages: follow [standard git conventions](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html); append `Fixes gh-XXXX` when applicable.

## CI / Release Notes

- **Two active lines.** `main` = Spring Boot 4.x; `support/spring-boot-3` = Spring Boot 3.x, maintained alongside `main`. CI (Dependabot, Main Build, PR Validation) runs on both branches; the two lines must use **distinct artifact versions** (e.g. `4.x-SNAPSHOT` vs `3.x-SNAPSHOT`) or their snapshots collide on Maven Central. Dependabot config for **both** lines lives in this branch's (`main`'s) `.github/dependabot.yml` — Dependabot only reads it from the default branch.
- PRs run `CLAAssistant` (sign the CLA) + build with `-P tests` + validation against the `spring-content-gettingstarted` repo. Validation follows the target line: PRs to `main` (4.x) use the getting-started default branch; PRs to `support/spring-boot-3` (3.x) use that repo's `support/spring-boot-3` branch. PR Validation runs for PRs to **both** `main` and `support/spring-boot-3` (`prs.yml` → `pull_request.branches`), so every Dependabot update proposal is built.
- **Dependabot auto-merge (`dependabot-auto-merge.yaml`).** Only **patch** updates (`version-update:semver-patch`) are auto-approved and auto-merged (squash); minor/major PRs are built but left open for manual review. A patch is merged only after **both** PR Validation checks are green — the `build` check (`-P tests`) and the `validate-with-gettingstarteds` check — which the workflow waits for (build first, fail-fast) before enabling auto-merge.
- Tags trigger release build (`-P ci,docs deploy`), GPG signing, and Maven Central publish. Pushes to `main`/`support/spring-boot-3` publish snapshots the same way.
- **Docs are published from both lines** — the Main Build runs on `main`, `support/spring-boot-3` and tags, via the official `actions/upload-pages-artifact` + `actions/deploy-pages` flow (Pages Source must be set to "GitHub Actions"). The landing-page version table is **deterministic**: every build computes all four coordinates from fixed sources — 4.x GA = latest `4.x` tag, 4.x SNAPSHOT = `main` pom, 3.x GA = latest `3.x` tag, 3.x SNAPSHOT = `support/spring-boot-3` pom — so either line regenerates an identical landing page and can't clobber the other. Reference docs are published per line — `main` → `refs/snapshot/main`, `support/spring-boot-3` → `refs/snapshot/support/spring-boot-3`, tags → `refs/release/<tag>` — and accumulate on the `gh-pages` branch (kept as persistent storage; no longer the deployment source). The job builds on JDK 25 to cover `main`'s Java 25 (Spring Boot 4) baseline (a 25 toolchain also builds the `support/spring-boot-3` Java 17 target).
- Docs output path: `target/generated-docs/refs/dev/`

## Local Dev Tips

- Devcontainer is configured with `AWS_REGION=us-west-1` and Docker-in-Docker.
- If importing into Eclipse, use the `.setup` file or import the root `pom.xml` with M2Eclipse.
