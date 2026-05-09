# Molly - Agent Guidelines

## Project Overview

Molly 是一个面向分布式 Web 系统的通用 Spring Boot 脚手架项目，提供认证授权、资源鉴权、对象存储、声明式缓存、文档生成与消息队列等可组合的基础设施 Starter。

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.3.2 (via `spring-boot-dependencies` BOM)
- **Security**: Spring Security 6.x, Spring Authorization Server (OAuth 2.1 / OIDC 1.0)
- **Libraries**: Lombok (provided scope), Apache Commons Lang3, Apache Commons Collections 4.4
- **Storage/Cache**: Aliyun OSS, MinIO, Redisson
- **Document**: Apache POI, poi-tl, PDFBox, Spring Mail, Thymeleaf
- **MQ**: RocketMQ, Kafka, Pulsar, RabbitMQ, Micrometer, OpenTelemetry
- **Build**: Maven multi-module with `flatten-maven-plugin` for `${revision}` version management

## Project Structure

```text
molly/                                              # Parent POM (groupId: cn.molly)
├── molly-infrastructure/                           # Base dependencies/utilities module (currently dependency aggregator)
│   └── groupId: cn.molly.infrastructure
├── molly-auth-server-spring-boot-starter/          # OAuth2 / OIDC Authorization Server Starter
│   └── cn.molly.security.auth
│       ├── config/                                 # MollyAuthServerAutoConfiguration
│       ├── properties/                             # molly.security.auth.*
│       └── service/                                # MollyUserAccountService SPI
├── molly-authorization-spring-boot-starter/        # Resource Server + RBAC Authorization Starter
│   └── cn.molly.security.authorization
│       ├── config/                                 # main/resource-server/RBAC/exception auto-config
│       ├── properties/                             # molly.security.authorization.*
│       ├── resource/                               # JWT -> Authentication converter
│       ├── exception/                              # JSON 401/403 handlers
│       └── rbac/                                   # @MollyPreAuthorize, cache, SPI, dynamic URL rules
├── molly-oss-spring-boot-starter/                  # Object Storage Starter
│   └── cn.molly.oss
│       ├── config/                                 # OSS + endpoint auto-config
│       ├── properties/                             # molly.oss.*
│       ├── core/                                   # OssTemplate abstraction
│       ├── support/aliyun/                         # Aliyun OSS implementation
│       ├── support/minio/                          # MinIO implementation
│       └── endpoint/                               # optional upload/download HTTP endpoint
├── molly-cache-spring-boot-starter/                # Declarative Cache Starter
│   └── cn.molly.cache
│       ├── annotation/                             # @MollyCacheable, @MollyCachePut, locks, hash/multi ops
│       ├── aop/                                    # cache aspects and SpEL operation context
│       ├── config/                                 # cache + AOP auto-config
│       ├── core/                                   # CacheOperations, CacheTemplate, key generation
│       ├── properties/                             # molly.cache.*
│       ├── support/redis/                          # Redisson default implementation
│       └── sync/                                   # transaction-aware cache flush
├── molly-document-spring-boot-starter/             # Word / Excel / PDF / Email Document Starter
│   └── cn.molly.document
│       ├── config/                                 # top-level + Word/Excel/PDF/Email gated auto-config
│       ├── properties/                             # molly.document.*
│       ├── core/                                   # exception + template loader
│       ├── word/                                   # poi-tl facade
│       ├── excel/                                  # POI facade, annotations, converters, styles, metadata
│       ├── pdf/                                    # PDFBox AcroForm filling
│       └── email/                                  # JavaMail + Thymeleaf + async/retry
├── molly-mq-spring-boot-starter/                   # Message Queue Starter
│   └── cn.molly.mq
│       ├── config/                                 # core/consumer/reliability/observability auto-config
│       ├── config/provider/                        # RocketMQ/Kafka/Pulsar/Rabbit provider auto-config
│       ├── consumer/                               # listener annotation, containers, retry, idempotency, DLQ
│       ├── core/                                   # Message, converter, ProducerOperations, SendResult
│       ├── observability/                          # health, metrics, actuator endpoint
│       ├── properties/                             # molly.mq.*
│       ├── provider/                               # provider-specific producer/container implementations
│       └── reliability/                            # transactional outbox
└── molly-example/                                  # Unified example app, switched by Spring Profile
    └── cn.molly.example
        ├── MollyExampleApplication                 # main class
        ├── auth/                                   # profile: auth, port 9000
        ├── resource/                               # profile: resource, port 9100
        ├── document/                               # profile: document, port 8081
        └── mq/                                     # profile: mq, port 8082
```

## Build Commands

```bash
# Full build
./mvnw clean install

# Build without tests
./mvnw clean install -DskipTests

# Build a specific module with required upstream modules
./mvnw clean install -pl molly-auth-server-spring-boot-starter -am
./mvnw clean install -pl molly-authorization-spring-boot-starter -am
./mvnw clean install -pl molly-cache-spring-boot-starter -am
./mvnw clean install -pl molly-document-spring-boot-starter -am
./mvnw clean install -pl molly-mq-spring-boot-starter -am
./mvnw clean install -pl molly-oss-spring-boot-starter -am

# Run example profiles
./mvnw spring-boot:run -pl molly-example -Dspring-boot.run.profiles=auth
./mvnw spring-boot:run -pl molly-example -Dspring-boot.run.profiles=resource
./mvnw spring-boot:run -pl molly-example -Dspring-boot.run.profiles=document
./mvnw spring-boot:run -pl molly-example -Dspring-boot.run.profiles=mq
```

## Key Configuration Prefixes

| Prefix | Module | Notes |
|--------|--------|-------|
| `molly.security.auth` | auth server | `issuer-uri` is used for OAuth2/OIDC issuer settings. |
| `molly.security.authorization` | resource authorization | JWT issuer/JWK settings, authorities claim, permit-all paths, RBAC, dynamic URL auth, permission cache. |
| `molly.oss` | OSS | provider (`aliyun`/`minio`), bucket, endpoint switch, file limits, credentials, thumbnail settings. |
| `molly.cache` | cache | provider (`redis`), key prefix, TTL/jitter, null-value cache, lock, Redisson single-node settings. |
| `molly.document` | document | Word/Excel/PDF/Email template paths, export options, PDF font/flatten, email async/retry. |
| `molly.mq` | MQ | enabled switch, provider (`rocketmq`/`kafka`/`pulsar`/`rabbit`), producer/consumer, outbox, observability, provider connection settings. |

## Auto-Configuration Rules

1. Use Spring Boot 3.x `@AutoConfiguration` and register classes in `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
2. Do not use `spring.factories` for auto-configuration registration.
3. Bind configuration via `@ConfigurationProperties` and enable it with `@EnableConfigurationProperties` from auto-configuration classes; do not rely on `@Component` scanning for properties classes.
4. Default beans in starters should use `@ConditionalOnMissingBean` where consumers are expected to override behavior.
5. Optional integrations should be guarded with `@ConditionalOnClass`, `@ConditionalOnProperty`, `@ConditionalOnBean`, and optional Maven dependencies.
6. Provider-style starters must keep single-provider semantics clear: activate one provider from the relevant `provider` property and make provider SDK dependencies optional unless the module intentionally supplies a default.

## Architecture & Design Principles

1. **Starter Pattern**: Starter modules provide auto-configuration and sane defaults, while application code can override beans without modifying starter internals.
2. **SPI-Driven Boundaries**: Starters expose small SPI interfaces (`MollyUserAccountService`, `MollyPermissionService`, `CacheOperations`, `OssTemplate`, `ProducerOperations`, etc.) and avoid owning business persistence.
3. **Auth Server**: `molly-auth-server-spring-boot-starter` supplies Authorization Server settings, development JWK generation, and token customization. Consumers still provide security filter chains, users, and registered clients in real applications.
4. **Resource Authorization**: `molly-authorization-spring-boot-starter` reads JWT authorities from configurable claims, supports `@MollyPreAuthorize`, optional dynamic URL rules, local/Redis permission cache, and JSON 401/403 responses.
5. **Cache**: `molly-cache-spring-boot-starter` uses annotation-driven AOP and defaults to Redisson/Redis. Keep key generation, null-value caching, TTL jitter, locking, and after-commit invalidation behavior consistent.
6. **Document**: `molly-document-spring-boot-starter` gates Word/Excel/PDF/Email sub-capabilities by classpath. Keep document APIs template-centric and reusable; avoid coupling to example-only files.
7. **MQ**: `molly-mq-spring-boot-starter` abstracts provider send/listen behavior, supports retry, idempotency, DLQ, optional transactional outbox, and observability. Provider implementations must respect common `Message` and `ProducerOperations` contracts.
8. **OSS**: `molly-oss-spring-boot-starter` abstracts object storage providers behind `OssTemplate`; HTTP endpoints are optional and controlled by configuration.
9. **Example App**: `molly-example` is one app with `auth`, `resource`, `document`, and `mq` profiles. Always run it with an explicit profile to avoid unrelated starter auto-config conflicts.

## Coding Conventions

- **Language**: All code comments and Javadoc in Chinese; class/method/variable names in English.
- **Author Tag**: `@author Ht7_Sincerity`.
- **Javadoc**: All public classes and public methods require Javadoc with `@since` tag.
- **File Header**: Java files should start with a Chinese Javadoc file/class description, author, and since tag.
- **Lombok**: Use Lombok annotations such as `@Data` where they match existing style; do not hand-write boilerplate without a reason.
- **Package Names**: Keep module packages aligned with existing roots (`cn.molly.security.auth`, `cn.molly.security.authorization`, `cn.molly.cache`, `cn.molly.document`, `cn.molly.mq`, `cn.molly.oss`).
- **Dates**: Use `yyyy/MM/dd` in Javadoc `@since` tags.
- **Encoding**: Source is UTF-8. Prefer ASCII for non-source generated artifacts unless Chinese text is required by conventions.

## Code Rule

### File Header

```java
/**
 * #{文件说明}
 *
 * @author Ht7_Sincerity
 * @since yyyy/MM/dd
 */
```

## Maven & Dependency Rules

- Root POM owns shared versions through properties and dependency management.
- Module POMs should inherit from root and use `${project.version}` or `${revision}` consistently for internal Molly dependencies following existing module style.
- Do not edit `.flattened-pom.xml` directly; it is generated by `flatten-maven-plugin`.
- Keep third-party provider SDK dependencies optional in starters when activation is classpath/property-driven.
- `molly-infrastructure` currently has no Java source; do not invent shared utilities there unless multiple modules genuinely need them.

## Example Profiles

| Profile | Port | Purpose | Config File |
|---------|------|---------|-------------|
| `auth` | 9000 | OAuth2 / OIDC Authorization Server | `molly-example/src/main/resources/application-auth.yml` |
| `resource` | 9100 | JWT Resource Server + RBAC demo | `molly-example/src/main/resources/application-resource.yml` |
| `document` | 8081 | Word / Excel / PDF / Email demo | `molly-example/src/main/resources/application-document.yml` |
| `mq` | 8082 | MQ producer/listener demo, RocketMQ by default | `molly-example/src/main/resources/application-mq.yml` |

When changing example behavior, update the matching profile YAML and avoid enabling unrelated starter auto-configurations in the same profile.

## Testing & Verification Guidance

- Prefer targeted Maven builds with `-pl <module> -am` after module changes.
- Run the full `./mvnw clean install` when touching shared contracts, dependency management, or multiple starters.
- For auto-configuration changes, verify the relevant `AutoConfiguration.imports` file and conditions.
- For example changes, run the affected profile command when feasible.
- If external services are required (Redis, OSS, RocketMQ/Kafka/Pulsar/Rabbit, SMTP), document the dependency and use unit/compile verification when the service is unavailable.

## Git Conventions

- Commit messages follow Conventional Commits: `type(scope): description` in Chinese.
- Suggested scopes: `auth`, `auth-server`, `authorization`, `security`, `cache`, `document`, `mq`, `oss`, `infrastructure`, `example`, `build`, `docs`.
- Examples:
  - `feat(auth-server): 添加认证授权服务器自动配置`
  - `feat(cache): 支持事务提交后缓存失效`
  - `docs(mq): 更新消息队列示例说明`

## Agent Workflow Notes

- Read the relevant module code before editing; this repository has several independent starters with similar patterns but different activation conditions.
- Keep edits scoped to the requested module and avoid broad refactors unless required.
- Preserve user or generated changes already present in the working tree.
- Do not remove optional dependency guards or `@ConditionalOnMissingBean` customizability without a concrete reason.
