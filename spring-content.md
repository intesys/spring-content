# Spring Content (incl. Spring Content Rest)

Cloud-Native Headless Content Management Services (CMS) for Spring. Integrates with Spring Data, Spring Data REST and Apache Solr.

This is done through a central "content store" abstraction with content store implementations for:
- **Filesystem** (stores content on disk)
- **JPA** (stores content as LOBs)
- **MongoDB** (stores content in GridFS)
- **Amazon S3** (stores content in an S3 bucket)
- **Google Cloud Storage** (GCS)
- **Azure Storage**

## Projects

- spring-content-commons; common core
- spring-content-fs; Filesystem implementation
- spring-content-jpa; JPA implementation
- spring-content-mongo; MongoDB implementation
- spring-content-s3; S3 implementation
- spring-content-gcs; Google Cloud Storage implementation
- spring-content-azure-storage; Azure Storage implementation
- spring-content-rest; REST layer that adds content links to Spring Data REST
- spring-content-solr; Solr integration for fulltext search
- spring-content-elasticsearch; Elasticsearch integration for fulltext search
- spring-content-encryption; support for encrypted content
- spring-content-renditions; support for content transformations (e.g. doc -> pdf)
- spring-content-metadata-extraction; support for metadata extraction

- spring-boot-starter-content-fs; Spring Boot starter for spring-content-fs
- spring-boot-starter-content-jpa; Spring Boot starter for spring-content-jpa
- spring-boot-starter-content-mongo; Spring Boot starter for spring-content-mongo
- spring-boot-starter-content-s3; Spring Boot starter for spring-content-s3
- spring-boot-starter-content-rest; Spring Boot starter for spring-content-rest
- spring-content-solr-boot-starter; Spring Boot starter for spring-content-solr
- spring-content-elasticsearch-boot-starter; Spring Boot starter for spring-content-elasticsearch
- spring-content-renditions-boot-starter; Spring Boot starter for spring-content-renditions
- spring-content-metadata-extraction-boot-starter; Spring Boot starter for spring-content-metadata-extraction

## Todo
- Add a CDN implementation (TBD: which CDN?)
- Add ALPs support
- Add support for byte range content handling for clients like Adobe's PDF Reader
- Possibly add a Spring Content-based Webdav library implementation?
- Possibly add a Spring Content-based CMIS implementation
