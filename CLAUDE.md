# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Companion docs

Two detailed guides already exist and are the source of truth — read them before non-trivial work; keep this file for the big picture:
- **`AGENTS.md`** — build commands, monorepo layout, CI/release, local dev tips.
- **`SKILL.md`** — module-development playbook: core concepts (annotations, store interfaces, events), package conventions, step-by-step patterns for adding a backend, dependency cheat sheet, pre-submit checklist.

## What this is

Spring Content is a multi-module Java 21 / Maven library (group `it.intesys`, Spring Boot 4.x) for managing binary content and associating it with Spring Data entities across pluggable storage backends. It is a **fork** of `paulcwarren/spring-content`, maintained by Intesys.

**Two parallel release lines.** `main` targets Spring Boot 4.x (`4.x`/`4.x-SNAPSHOT`); `support/spring-boot-3` targets Spring Boot 3.x (`3.x`/`3.x-SNAPSHOT`). The two lines must keep **distinct artifact versions** or their snapshots collide on Maven Central. When changing anything version- or CI-related, remember both lines exist. Docs are published from both lines: the landing-page version table is computed deterministically (4.x GA/SNAPSHOT from `main`, 3.x GA/SNAPSHOT from `support/spring-boot-3`) so neither line clobbers the other, and each line publishes its own reference docs under `refs/snapshot/<branch>`. See `AGENTS.md` → *CI / Release Notes* for details.

## Build & test essentials

```bash
AWS_REGION=us-west-1 ./mvnw clean install            # units only
AWS_REGION=us-west-1 ./mvnw -P tests clean install   # + integration tests
./mvnw -pl <module> test -Dtest=ClassName            # single test class
```

- **`AWS_REGION` is required** even for non-S3 builds — `spring-content-s3` LocalStack tests fail without it (the devcontainer sets it).
- Tests use **ginkgo4j** (BDD), not plain JUnit, run through a custom `JUnitRunListener`.
- **`*Test.java`** = unit (default). **`*IT.java` / `*Tests.java`** = integration, run **only with `-P tests`** and need Docker (Testcontainers / LocalStack / embedded DBs).

## Architecture (the part that spans multiple modules)

The design is a **contract-in-commons, implementation-per-backend, wiring-in-autoconfigure** layering. To change how content is stored you almost always touch three places:

1. **`spring-content-commons`** — the public contract: content annotations (`@ContentId`, `@MimeType`, `@OriginalFileName`, `@ContentLength`), the store interface hierarchy (`Store` → `AssociativeStore` → `ContentStore` / `ReactiveContentStore`), and store events.
2. **A backend module** (`spring-content-fs`, `-jpa`, `-mongo`, `-s3`, `-azure-storage`, `-gcs`, …) — implements the contract as `Default*StoreImpl`, exposes a `*StoreFactoryBean` + `*Registrar`, and (for non-Boot use) an `@Enable*Stores` annotation.
3. **`spring-content-autoconfigure`** — Boot auto-configuration that wires each backend, listed in `META-INF/spring/...AutoConfiguration.imports`. It depends on every other module as `<optional>true</optional>`.

Cross-cutting rules that aren't obvious from a single file:
- **Public vs internal packages are enforced by convention.** Public API lives in `org.springframework.content.<module>.*`; implementations live in `internal.org.springframework.content.<module>.*`. **Never** let an `internal.*` type appear in a public API signature.
- **Starters are duplicated.** Each backend has a modern `spring-content-<x>-boot-starter` **and** a legacy `content-<x>-spring-boot-starter`. When touching starter/auto-config wiring, mirror the change in both.
- Full-text search (`spring-content-solr`, `-elasticsearch`), `-renditions`, `-metadata-extraction`, and versioning (`spring-versions-*`) are cross-cutting add-ons layered on the same commons contract.
- `spring-content-docx4j` is currently **commented out** in the root `pom.xml` module list.

## OpenSpec: internal maintainer workflow, not a contribution gate

`openspec/` (and the `.claude/`/`.opencode/` `opsx` commands) is a spec-driven workflow the **maintainers use internally** to plan larger architectural changes. It is **not** a requirement for contributing. When helping an external contributor, do **not** force or auto-generate an OpenSpec change to open an issue or PR: lightweight changes go straight to a PR with no spec, and for substantial architectural changes the contributor is only asked to open an alignment issue first — deciding whether a spec is needed, and writing it, is a maintainer's job. See `CONTRIBUTING.md` → *Working with OpenSpec*. Keep `openspec/` public and in-repo — it exists to make architectural decisions transparent.

## License & file headers

The project is **GPL v3** (see `LICENSE`, `NOTICE`). New `.java` files must carry the **GPLv3 header from `HEADER.txt`**. Pre-existing files with the **ASF header are Apache-2.0 third-party/derived code and must keep their original header unchanged** — do not relicense them; `NOTICE` documents them.

## Conventions

- Format with `eclipse/eclipse-code-formatter.xml`.
- New/substantially-changed `.java` files get an `@author` Javadoc tag.
- Commit messages follow standard git conventions; append `Fixes gh-XXXX` when applicable.
- PRs require signing the CLA (`CLA.md`) and pass a build against the `spring-content-gettingstarted` repo.
