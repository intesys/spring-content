# SKILL: Spring Content Module Development

Guidelines for writing, modifying, and reviewing code across the **Spring Content** multi-module Java/Maven repository.

**Project coordinates**
- **GroupId:** `it.intesys`
- **Current version:** `3.1.0-SNAPSHOT`
- **Java:** 17
- **Maven wrapper:** `./mvnw` (bundled Maven 3.6.3)
- **Spring Boot:** 3.5.0
- **Spring Cloud:** 2024.0.1
- **Jakarta Persistence:** 3.1.0

## When to Use

Apply this skill whenever the user requests:
- Implementing a new feature in any spring-content module.
- Adding or fixing content store backends (JPA, FS, Mongo, S3, etc.).
- Modifying REST endpoints, auto-configuration, or starter modules.
- Writing or updating tests.
- Refactoring public APIs or internal implementations.

## Repository Layout

**Core libraries**
- `spring-content-commons` — public API (annotations, store interfaces, events, utilities).
- `spring-content-fs` — filesystem-backed stores.
- `spring-content-jpa` — database BLOB-backed stores.
- `spring-content-mongo` — MongoDB GridFS-backed stores.
- `spring-content-s3` — AWS S3-backed stores.
- `spring-content-azure-storage` — Azure Blob Storage.
- `spring-content-gcs` — Google Cloud Storage.
- `spring-content-rest` — REST controllers and hypermedia for content.
- `spring-content-renditions` — content rendition services.
- `spring-content-metadata-extraction` — metadata extraction.
- `spring-content-solr` / `spring-content-elasticsearch` — full-text search.
- `spring-content-encryption` — encryption support.
- `spring-content-autoconfigure` — Spring Boot auto-configuration for all modules.
- `spring-versions-commons` / `spring-versions-jpa` — versioning/locking APIs.

**Starters** (use the modern names, but legacy `content-*-spring-boot-starter` names also exist)
- `spring-content-fs-boot-starter`
- `spring-content-jpa-boot-starter`
- `spring-content-mongo-boot-starter`
- `spring-content-rest-boot-starter`
- `spring-content-s3-boot-starter`
- `spring-content-renditions-boot-starter`
- `spring-content-metadata-extraction-boot-starter`
- `spring-content-solr-boot-starter`
- `spring-content-elasticsearch-boot-starter`

## Core Concepts

### 1. Content Entity Annotations (`org.springframework.content.commons.annotations`)
Annotate entity fields to let Spring Content manage content metadata:
- `@ContentId` — stores the content identifier (e.g., UUID, path).
- `@MimeType` — stores the MIME type string.
- `@OriginalFileName` — stores the original uploaded file name.
- `@ContentLength` — stores the content size in bytes.

### 2. Store Interfaces (`org.springframework.content.commons.store` / `.repository`)
- `Store<CID>` — base store.
- `AssociativeStore<T, CID>` — associate content with an entity domain object.
- `ContentStore<T, CID extends Serializable>` — full CRUD for content (`setContent`, `getContent`, `unsetContent`, `getResource`).
- `ReactiveContentStore<T, CID>` — reactive variant.

Backend-specific extensions (examples):
- `JpaContentStore<T, CID>` — in `spring-content-jpa`.
- `FilesystemStore<CID>` / `FilesystemContentStore<T, CID>` — in `spring-content-fs`.
- Analogous interfaces exist for Mongo, S3, etc.

### 3. Store Events
Events are fired around store operations. Key annotations for listeners:
- `@StoreEventHandler` on the listener class.
- `@HandleBeforeSetContent`, `@HandleAfterSetContent`, `@HandleBeforeUnsetContent`, `@HandleAfterUnsetContent`, `@HandleBeforeGetContent`, `@HandleAfterGetContent`, etc.
- Event classes live in `org.springframework.content.commons.store.events` and `org.springframework.content.commons.repository.events`.

### 4. Package Conventions
- **Public API:** `org.springframework.content.<module>.*` (e.g., `org.springframework.content.jpa.store`, `org.springframework.content.fs.config`).
- **Internal implementations:** `internal.org.springframework.content.<module>.*` (e.g., `internal.org.springframework.content.jpa.store.DefaultJpaStoreImpl`).
  - **Never** leak internal classes into public API signatures.

## Implementation Patterns

### Adding a new backend store capability
1. If the change touches the **public contract** (new annotation, new method on `ContentStore` or `AssociativeStore`), modify `spring-content-commons`.
2. Provide the **backend implementation** in the target module under `internal.org.springframework.content.<module>.store.Default*StoreImpl`.
3. Register the store via a `*StoreFactoryBean` and a `*StoreRegistrar` / `*ContentRepositoriesRegistrar` in the backend module.
4. If non-Boot, provide an `@Enable*Stores` annotation (e.g., `@EnableJpaStores`, `@EnableFilesystemStores`) in `org.springframework.content.<module>.config`.
5. If Boot, add/update the auto-configuration class in `spring-content-autoconfigure` under `internal.org.springframework.content.<module>.boot.autoconfigure.*AutoConfiguration`.
6. Ensure the auto-configuration is listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### Configuration classes
- Backend-specific configurers: `*StoreConfigurer`, `*ContentRestConfigurer`.
- Properties beans: `*Properties` or `*StoreProperties` (e.g., `JpaStoreProperties`, `ContentJpaProperties`).

## Build & Test Commands

**Quick build (units only)**
```bash
AWS_REGION=us-west-1 ./mvnw clean install
```

**Full build with integration tests**
```bash
AWS_REGION=us-west-1 ./mvnw -P tests clean install
```

**Single module / single test class**
```bash
./mvnw -pl <module> test -Dtest=ClassName
```

**CI shortcut**
```bash
mvn -B -P tests -Dmaven.javadoc.skip=true install
```

> **Note:** `AWS_REGION=us-west-1` is required because `spring-content-s3` integration tests use LocalStack.

## Testing Conventions

- **Framework:** [ginkgo4j](https://github.com/paulcwarren/ginkgo4j) BDD (`com.github.paulcwarren:ginkgo4j:1.0.15`).
- **Unit tests:** classes named `*Test.java` — run by default.
- **Integration tests:** classes named `*IT.java` or `*Tests.java` — **only run with `-P tests`**.
- Some modules exclude JUnit Jupiter from `spring-boot-starter-test` to avoid conflicts with ginkgo4j; follow the existing `pom.xml` pattern.
- Docker availability matters for ITs (Testcontainers, LocalStack, embedded dbs).

## Code Style Requirements

**`CONTRIBUTING.md` → *Code Conventions and Housekeeping* is the authoritative checklist and must be satisfied on every change.** The points below mirror it:

- Format with `eclipse/eclipse-code-formatter.xml`.
- **New `.java` files** must include:
  - GPLv3 license header (copy from `HEADER.txt`). Pre-existing files with the ASF header keep it unchanged.
  - Javadoc class comment with at least `@author`.
- Add `@author` to files you modify substantially, and add some Javadocs.
- Add unit tests for new behavior; rebase your branch on the current target branch before merge.
- Commit messages: follow standard git conventions; append `Fixes gh-XXXX` when applicable.
- Non-trivial contributions require signing the [CLA](CLA.md).

## Dependency Cheat Sheet

When writing code in a module, reference these typical dependencies:

**Public API (always safe)**
```xml
<dependency>
    <groupId>it.intesys</groupId>
    <artifactId>spring-content-commons</artifactId>
    <version>${project.version}</version>
</dependency>
```

**Backend-specific (examples)**
- `spring-content-jpa` depends on `spring-data-jpa`, `hibernate-core`, `commons-io`.
- `spring-content-fs` depends on `spring-content-commons`, `spring-data-commons`, `spring-tx`, `jakarta.persistence-api`.
- `spring-content-autoconfigure` pulls in all other modules as `<optional>true</optional>`.

## OpenSpec: maintainer workflow, not a contribution requirement

`openspec/` (driven by the `.claude/`/`.opencode/` `opsx` commands) is a spec-driven workflow the **maintainers use internally** to plan larger architectural changes. It is an internal process, **not** a prerequisite for contributing. When working on behalf of an external contributor, do **not** require or auto-generate an OpenSpec change to open an issue or PR — lightweight changes go straight to a PR with no spec. For a substantial architectural change (new module/starter, public-contract change, breaking change), the only ask is to open an alignment issue first; whether a spec is written, and writing it, is a maintainer decision. See `CONTRIBUTING.md` → *Working with OpenSpec*.

## Checks Before Submitting

Run through **`CONTRIBUTING.md` → *Code Conventions and Housekeeping*** and confirm each item is met, then:

- Does the code compile with **Java 21**?
- Do unit tests pass (`./mvnw test -pl <module>`)?
- Do integration tests pass (`./mvnw -P tests -pl <module> verify`)?
- Is the code formatted with the Eclipse formatter?
- Do new files carry the **GPLv3 license header** (from `HEADER.txt`) and an `@author` Javadoc tag?
- Are public APIs free of `internal.*` package imports?
- Are starter/auto-config changes mirrored in both modern (`spring-content-*-boot-starter`) and legacy (`content-*-spring-boot-starter`) starter modules when applicable?
