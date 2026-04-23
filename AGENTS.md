# Molly - Agent Guidelines

## Project Overview

Molly 是一个面向分布式 Web 系统的通用脚手架项目，基于 Spring Boot 构建，聚焦权限与认证领域，提供开箱即用的安全基础设施。

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.3.2 (via spring-boot-dependencies BOM)
- **Security**: Spring Security 6.x, Spring Authorization Server (OAuth 2.1 / OIDC 1.0)
- **Libraries**: Lombok (provided scope), Apache Commons Lang3, Apache Commons Collections 4.4
- **Build**: Maven multi-module with `flatten-maven-plugin` for `${revision}` version management

## Project Structure

```
molly/                                          # Parent POM (groupId: cn.molly)
├── molly-infrastructure/                       # Base utilities and common models
│   └── (groupId: cn.molly.infrastructure)
├── molly-authorization-server-spring-boot-starter/  # OAuth2 Authorization Server Starter
│   └── cn.molly.security.auth
│       ├── config/
│       │   └── MollyAuthServerAutoConfiguration  # Core auto-configuration
│       ├── properties/
│       │   └── MollyAuthServerProperties          # Config: molly.security.auth.*
│       └── service/
│           └── MollyUserAccountService            # User account SPI (extends UserDetailsService)
└── molly-auth-server-example/                  # Example auth server application
    └── cn.molly.example.auth
        ├── AuthServerApplication                  # Spring Boot main class (port 9000)
        └── config/SecurityConfig                  # In-memory client & user for testing
```

## Build Commands

```bash
# Full build
./mvnw clean install

# Build without tests
./mvnw clean install -DskipTests

# Build a specific module
./mvnw clean install -pl molly-authorization-server-spring-boot-starter -am

# Run the example auth server
./mvnw spring-boot:run -pl molly-auth-server-example
```

## Key Configuration

Application config prefix: `molly.security.auth`

| Property                          | Description         | Example                  |
|-----------------------------------|---------------------|--------------------------|
| `molly.security.auth.issuer-uri`  | OIDC issuer URI     | `http://localhost:9000`  |

## Architecture & Design Principles

1. **Starter Pattern**: `molly-authorization-server-spring-boot-starter` provides auto-configuration with `@ConditionalOnMissingBean` defaults, allowing consumers to override any bean.
2. **SPI-Driven**: The starter does NOT handle data persistence. Consumers MUST provide:
   - `RegisteredClientRepository` - for OAuth2 client management
   - `UserDetailsService` (or `MollyUserAccountService`) - for user authentication
3. **Token Customization**: Default `OAuth2TokenCustomizer` adds user authorities to access tokens. Override by providing your own bean.
4. **JWK Management**: Default in-memory RSA key generation for development. Production should override `JWKSource<SecurityContext>` bean.

## Coding Conventions

- **Language**: All code comments and Javadoc in Chinese; class/method/variable names in English.
- **Author Tag**: `@author Ht7_Sincerity`
- **Javadoc**: All public classes and methods require Javadoc with `@since` tag.
- **Lombok**: Use `@Data` and other Lombok annotations for boilerplate reduction.
- **Config Properties**: Use `@ConfigurationProperties` with `@EnableConfigurationProperties` (not `@Component` scanning).
- **Auto Configuration**: Use `@AutoConfiguration` annotation (Spring Boot 3.x style), not `@Configuration` with spring.factories.

## Git Conventions

- Commit messages follow Conventional Commits: `type(scope): description` in Chinese.
  - Examples: `feat(auth-server): 添加认证授权服务器自动配置`, `docs(auth): 更新认证授权服务器支持的标准`
- Scopes: `auth`, `auth-server`, `security`, `infrastructure`

## Code Rule
### File Header
```java
/**
 * #{File Description}
 * 
 * @author Ht7_Sincerity
 * @since yyyy/MM/dd
 */
```
