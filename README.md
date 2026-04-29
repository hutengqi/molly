# Molly

分布式 Web 系统脚手架项目，基于 Spring Boot 构建，聚焦权限与认证领域，提供开箱即用的安全基础设施。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.3.2 |
| 安全 | Spring Security 6.x、Spring Authorization Server（OAuth 2.1 / OIDC 1.0） |
| 工具库 | Lombok、Apache Commons Lang3、Apache Commons Collections 4.4 |
| 构建 | Maven 多模块，`flatten-maven-plugin` 统一版本管理 |

## 项目结构

```
molly/                                              # 父 POM (cn.molly)
├── molly-infrastructure/                           # 基础工具与通用模型
│   └── (cn.molly.infrastructure)
├── molly-auth-server-spring-boot-starter/       # OAuth2 认证服务器 Starter（发令牌）
│   └── cn.molly.security.auth
│       ├── config/
│       │   └── MollyAuthServerAutoConfiguration    # 核心自动配置
│       ├── properties/
│       │   └── MollyAuthServerProperties           # 配置属性: molly.security.auth.*
│       └── service/
│           └── MollyUserAccountService             # 用户账户 SPI 扩展
├── molly-authorization-spring-boot-starter/     # 资源服务器 + RBAC 鉴权 Starter（验令牌 + 鉴权）
│   └── cn.molly.security.authorization
│       ├── config/                             # 4 个自动配置：主装配 / 资源服务器 / RBAC / 统一异常
│       ├── properties/                         # 配置属性: molly.security.authorization.*
│       ├── resource/                           # JWT -> Authentication 转换器
│       ├── exception/                          # 401/403 统一 JSON 响应
│       └── rbac/
│           ├── annotation/                     # @MollyPreAuthorize 注解
│           ├── aop/                            # 注解切面
│           ├── core/                           # SPI（MollyPermissionService）+ PermissionEvaluator
│           ├── cache/                          # 权限 TTL 缓存
│           └── url/                            # 动态 URL 鉴权
├── molly-oss-spring-boot-starter/              # 对象存储服务 Starter
│   └── cn.molly.oss
│       ├── config/                             # 自动配置
│       ├── properties/                         # 配置属性: molly.oss.*
│       ├── core/                               # 统一抽象层
│       ├── support/aliyun/                     # 阿里云 OSS 实现
│       ├── support/minio/                      # MinIO 实现
│       ├── endpoint/                           # HTTP 端点
│       └── util/                               # 文件工具
├── molly-cache-spring-boot-starter/            # 声明式缓存 Starter
│   └── cn.molly.cache
│       ├── annotation/                         # 自研缓存注解
│       ├── aop/                                # 切面与 SpEL 上下文
│       ├── config/                             # 自动配置
│       ├── core/                               # SPI 与门面抽象
│       ├── properties/                         # 配置属性: molly.cache.*
│       ├── support/redis/                      # Redisson 默认实现
│       └── sync/                               # 事务后置失效同步器
├── molly-document-spring-boot-starter/         # 文档生成 Starter（Word/Excel/PDF/Email）
│   └── cn.molly.document
│       ├── config/                             # 5 个自动配置（顶层 + Word/Excel/PDF/Email 子能力守门）
│       ├── properties/                         # 配置属性: molly.document.*
│       ├── core/                               # 统一异常 + 模板加载抽象
│       ├── word/                               # poi-tl 封装（模板缓存 + render 门面）
│       ├── excel/                              # POI 封装（注解 + 类型转换 SPI + 流式）
│       │   ├── annotation/                     # @ExcelSheet/@ExcelColumn/@ExcelStyle 等 6 个注解
│       │   ├── converter/                      # CellConverter SPI + 6 个内置转换器
│       │   ├── meta/                           # ExcelClassMeta 反射缓存
│       │   └── style/                          # 样式解析与预设（避开 POI 64000 上限）
│       ├── pdf/                                # PDFBox AcroForm 字段填充
│       └── email/                              # JavaMail + Thymeleaf + 异步 + 重试
└── molly-example/                              # 统一示例应用（单主类 + Spring Profile 切换子示例）
    └── cn.molly.example
        ├── MollyExampleApplication             # 统一 Spring Boot 主类
        ├── auth/config/SecurityConfig          # @Profile("auth") 认证服务器配置  (9000)
        ├── resource/                           # @Profile("resource")                   (9100)
        │   ├── controller/UserController        # @MollyPreAuthorize 示例接口
        │   └── service/DemoPermissionService    # 权限 SPI 示例实现
        └── document/                           # @Profile("document")                   (8081)
            ├── model/OrderRow                   # 注解驱动的 Excel 示例实体
            └── web/DocumentController           # Word/Excel/PDF/Email 四类演示端点
```

---

## molly-auth-server-spring-boot-starter

### 用途

为 Spring Boot 应用提供 **OAuth 2.1 授权服务器** 的开箱即用自动配置。引入此 Starter 后，开发者只需提供少量业务 Bean（客户端存储、用户认证），即可快速搭建一个功能完备的授权服务器，支持：

- 授权码模式（Authorization Code）
- 客户端凭证模式（Client Credentials）
- 刷新令牌（Refresh Token）
- OpenID Connect 1.0 发现端点
- JWT 令牌签名与自定义

### 设计思路

#### 1. Starter 自动配置模式

所有默认 Bean 均使用 `@ConditionalOnMissingBean` 注解，允许使用者在自己的项目中提供同类型 Bean 来覆盖默认行为，实现零侵入的定制化。

#### 2. SPI 驱动 — 不绑定持久化

Starter **不处理数据存储**，使用者必须自行提供以下 Bean：

| Bean | 用途 | 示例实现 |
|------|------|----------|
| `RegisteredClientRepository` | OAuth2 客户端管理 | `InMemoryRegisteredClientRepository`（开发）/ `JdbcRegisteredClientRepository`（生产） |
| `UserDetailsService` | 用户认证 | `InMemoryUserDetailsManager`（开发）/ 自定义数据库实现（生产） |
| `SecurityFilterChain` | 授权服务器安全过滤链 | 参考示例项目 `SecurityConfig` |

> Starter 还提供了 `MollyUserAccountService` 接口（扩展自 `UserDetailsService`），作为统一的用户账户 SPI，  
> 方便未来扩展手机号登录、社交登录等多种认证方式。

#### 3. 令牌定制化

默认的 `OAuth2TokenCustomizer` 会将用户权限（authorities）注入到 Access Token 的 `authorities` 声明中，资源服务器可据此进行细粒度权限控制。可通过提供自定义 `OAuth2TokenCustomizer<JwtEncodingContext>` Bean 来覆盖。

#### 4. JWK 密钥管理

默认在内存中动态生成 2048 位 RSA 密钥对，开发阶段零配置即可运行。**生产环境应覆盖 `JWKSource<SecurityContext>` Bean**，从密钥库、数据库或 HSM 中加载密钥。

### 文件说明

| 文件 | 说明 |
|------|------|
| `MollyAuthServerAutoConfiguration` | 核心自动配置类，提供 `AuthorizationServerSettings`、`JWKSource`、`OAuth2TokenCustomizer` 三个默认 Bean |
| `MollyAuthServerProperties` | 配置属性类，前缀 `molly.security.auth`，当前支持 `issuer-uri` 属性 |
| `MollyUserAccountService` | 用户账户 SPI 接口，扩展自 `UserDetailsService`，为未来多认证方式预留 |
| `AutoConfiguration.imports` | Spring Boot 3.x 自动配置注册文件，声明 `MollyAuthServerAutoConfiguration` |

### 配置属性

| 属性 | 说明 | 示例 |
|------|------|------|
| `molly.security.auth.issuer-uri` | OIDC 签发者地址（必填） | `http://localhost:9000` |

### 使用方法

#### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.molly.auth</groupId>
    <artifactId>molly-auth-server-spring-boot-starter</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

#### 2. 配置 `application.yml`

```yaml
server:
  port: 9000

molly:
  security:
    auth:
      issuer-uri: http://localhost:9000
```

#### 3. 提供必要的 Bean

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 授权服务器协议端点过滤链（处理 OAuth2/OIDC 端点）
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());
        http
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // 默认安全过滤链（处理表单登录等）
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    // OAuth2 客户端存储
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client")
                .clientSecret(passwordEncoder.encode("secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(client);
    }

    // 用户认证服务
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### 4. 验证

启动应用后，访问以下端点确认服务正常：

```bash
# OIDC 发现端点
curl http://localhost:9000/.well-known/openid-configuration

# JWK 公钥端点
curl http://localhost:9000/oauth2/jwks

# 客户端凭证模式获取令牌
curl -X POST -u test-client:secret \
  http://localhost:9000/oauth2/token \
  -d "grant_type=client_credentials&scope=openid"
```

---

## molly-authorization-spring-boot-starter

### 用途

与 `molly-auth-server-spring-boot-starter` 形成互补——后者负责**颁发 JWT**，本 Starter 负责**校验 JWT 并进行权限控制**。一次依赖引入即可为业务服务提供：

- **OAuth2 资源服务器**：基于 `issuer-uri` 自动拉 JWKS 验签，从 JWT claim 构建 `Authentication`
- **方法级权限注解**：`@MollyPreAuthorize` 支持单权限 / anyOf / allOf 三种语义，同时兼容 Spring 原生 `@PreAuthorize`
- **动态 URL 鉴权**：通过 SPI 在运行时注入 `pattern + method + 所需权限` 规则，支持热刷新
- **权限数据 SPI**：`MollyPermissionService` 让使用者自由对接数据库 / 远程权限中心；未提供时默认直接使用 JWT 中的 `authorities`
- **TTL 权限缓存**：本地内存缓存 + 过期时间抖动，降低权限源查询压力，支持主动失效
- **统一 401/403**：手写 JSON 异常响应，结构一致，无需额外依赖 Jackson

### 设计思路

#### 1. 基于标准 OAuth2 Resource Server

底层复用 Spring Security `oauth2ResourceServer().jwt()`，通过 `NimbusJwtDecoder` 远端获取公钥验签；自定义 `MollyJwtAuthenticationConverter` 从可配置的 claim 名（默认 `authorities`）读取权限列表，与认证 Starter 的 Token Customizer 形成契约闭环。

#### 2. SPI 驱动 — 权限数据不绑定持久化

核心接口 `MollyPermissionService#loadPermissions(Authentication)` 交由使用者实现（数据库 / 微服务 / 缓存均可）；未提供任何实现时，由 `DefaultMollyPermissionService` 兜底，直接读取 JWT 中的 authorities，实现零配置可用。

#### 3. 注解双栈 — 共享一套 PermissionEvaluator

`@MollyPreAuthorize`（自定义 AOP）与 Spring `@PreAuthorize` 同时可用，两者底层都经过 `MollyPermissionEvaluator`，语义完全一致。使用方可按团队偏好选择，甚至同一项目内混用。

#### 4. 动态 URL 鉴权（可选）

通过 `MollyPermissionService#loadUrlPermissionRules()` 提供一组 `UrlPermissionRule`（pattern + HttpMethod + 权限集合 + requireAll），由 `DynamicUrlAuthorizationManager` 在请求进入时匹配校验。默认关闭，通过 `rbac.dynamic-url.enabled=true` 启用。

#### 5. 权限缓存与主动失效

默认 `LocalPermissionCache` 使用 `ConcurrentHashMap` + 时间戳 TTL，零额外依赖。需要分布式缓存时实现 `PermissionCache` 接口替换即可。提供 `PermissionRefresher` 供业务在用户角色变更时主动失效指定 principal。

#### 6. 零侵入可覆盖

所有 Bean 默认 `@ConditionalOnMissingBean`，使用方可覆盖 `SecurityFilterChain`、`JwtDecoder`、`PermissionCache`、`AuthenticationEntryPoint`、`AccessDeniedHandler` 等任一组件。

### 核心流程

```
BearerTokenAuthenticationFilter
      ↓ JwtDecoder 验签 + 解析
      ↓ MollyJwtAuthenticationConverter（按 claim 构建 Authentication）
      ↓ DynamicUrlAuthorizationManager（可选，URL 规则匹配）
      ↓ @MollyPreAuthorize AOP / Spring @PreAuthorize
      ↓ MollyPermissionEvaluator（查权限缓存 / MollyPermissionService）
      ↓ 比对通过 → Controller；不通过 → MollyAccessDeniedHandler 返回 403 JSON
```

### 文件说明

| 文件 | 说明 |
|------|------|
| `config/MollyAuthorizationAutoConfiguration` | 顶层自动配置，启用 `MollyAuthorizationProperties` |
| `config/MollyResourceServerConfiguration` | 注册 `JwtDecoder`、`MollyJwtAuthenticationConverter`、默认 `SecurityFilterChain`（`@Order(100)`，stateless，按配置启用白名单与动态 URL 鉴权） |
| `config/MollyRbacConfiguration` | 启用 `@EnableMethodSecurity`，注册 `MollyPermissionEvaluator`、`MethodSecurityExpressionHandler`、`MollyPreAuthorizeAspect`、`PermissionRefresher`、`DynamicUrlAuthorizationManager` |
| `config/MollyAuthorizationExceptionConfiguration` | 注册 `MollyAuthenticationEntryPoint`（401）与 `MollyAccessDeniedHandler`（403） |
| `properties/MollyAuthorizationProperties` | 配置属性类，前缀 `molly.security.authorization`，含 `issuerUri`/`permitAll`/`rbac.*` 等字段 |
| `resource/MollyJwtAuthenticationConverter` | 从 JWT 指定 claim 提取权限列表，拼接前缀后构造 `JwtAuthenticationToken` |
| `exception/MollyAuthenticationEntryPoint` / `MollyAccessDeniedHandler` / `JsonErrorResponseWriter` | 未认证/无权限统一 JSON 响应 |
| `rbac/core/MollyPermissionService` | 权限数据 SPI，`loadPermissions(auth)` + `loadUrlPermissionRules()` |
| `rbac/core/DefaultMollyPermissionService` | 兜底实现：直接取 `Authentication.getAuthorities()` |
| `rbac/core/MollyPermissionEvaluator` | `PermissionEvaluator` 实现，查缓存 → 查 SPI → 比对权限码 |
| `rbac/core/PermissionRefresher` | 主动失效缓存的门面，支持 `refresh(principal)` / `refreshAll()` |
| `rbac/annotation/MollyPreAuthorize` | 自定义注解，字段 `perm` / `anyPerm` / `allPerm`（优先级 perm > allPerm > anyPerm） |
| `rbac/aop/MollyPreAuthorizeAspect` | AspectJ 切面，不通过时抛 `AccessDeniedException` |
| `rbac/cache/PermissionCache` / `LocalPermissionCache` | 权限缓存抽象 + 本地 TTL 实现 |
| `rbac/url/UrlPermissionRule` / `DynamicUrlAuthorizationManager` | 动态 URL 规则模型与运行时匹配器 |
| `AutoConfiguration.imports` | Spring Boot 3.x 自动配置注册文件，声明 4 个自动配置类 |

### 配置属性

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `molly.security.authorization.issuer-uri` | OIDC 签发者地址，与认证服务器对齐（必填） | - |
| `molly.security.authorization.jwk-set-uri` | 显式 JWK Set 地址，不填则由 `issuer-uri` 推导 | - |
| `molly.security.authorization.authorities-claim` | JWT 中承载权限列表的 claim 名 | `authorities` |
| `molly.security.authorization.authority-prefix` | 权限前缀（如 `ROLE_`） | `` |
| `molly.security.authorization.permit-all` | 放行 URL 白名单（Ant 风格） | `[]` |
| `molly.security.authorization.rbac.enabled` | 是否启用 RBAC 相关 Bean | `true` |
| `molly.security.authorization.rbac.dynamic-url.enabled` | 是否启用动态 URL 鉴权 | `false` |
| `molly.security.authorization.rbac.cache.ttl` | 权限缓存过期时间 | `5m` |
| `molly.security.authorization.rbac.cache.type` | 缓存类型：`local` / `redis`（需引入 molly-cache） | `local` |

### 使用方法

#### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.molly.authorization</groupId>
    <artifactId>molly-authorization-spring-boot-starter</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

#### 2. 配置 `application.yml`

```yaml
server:
  port: 9100

molly:
  security:
    authorization:
      # 与 molly-example 的 auth profile (9000) 对齐
      issuer-uri: http://localhost:9000
      permit-all:
        - /actuator/**
      rbac:
        enabled: true
        dynamic-url:
          enabled: false       # 按需开启
        cache:
          ttl: 5m
```

#### 3. 实现权限 SPI（可选）

若只需按 JWT 中已有 `authorities` 鉴权，无需实现；如需接入自有权限数据源：

```java
@Service
public class DbPermissionService implements MollyPermissionService {

    private final UserPermissionRepository repo;

    public DbPermissionService(UserPermissionRepository repo) {
        this.repo = repo;
    }

    @Override
    public Set<String> loadPermissions(Authentication authentication) {
        return repo.findPermCodes(authentication.getName());
    }

    /** 可选：动态 URL 鉴权规则 */
    @Override
    public List<UrlPermissionRule> loadUrlPermissionRules() {
        return List.of(
                new UrlPermissionRule("/api/users/**", "GET",  Set.of("user:read"),  false),
                new UrlPermissionRule("/api/users/**", "POST", Set.of("user:write"), false)
        );
    }
}
```

#### 4. 方法级鉴权（注解）

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 单权限
    @GetMapping
    @MollyPreAuthorize(perm = "user:read")
    public List<User> list() { ... }

    // anyOf：满足任一即可
    @PostMapping
    @MollyPreAuthorize(anyPerm = {"user:write", "admin"})
    public void create(@RequestBody User u) { ... }

    // allOf：同时具备
    @GetMapping("/export")
    @MollyPreAuthorize(allPerm = {"user:read", "user:export"})
    public void export() { ... }
}
```

> 也可使用 Spring 原生写法：`@PreAuthorize("hasPermission(null, 'user:read')")`，底层同样经过 `MollyPermissionEvaluator`。

#### 5. 权限变更主动失效

```java
@Service
public class RoleService {

    private final PermissionRefresher refresher;

    public void assignRole(String principal, String role) {
        // ... 业务写入 ...
        refresher.refresh(principal);   // 立即清除该用户权限缓存
    }
}
```

#### 6. 验证

```bash
# 1) 先从认证服务器 (9000) 获取令牌
TOKEN=$(curl -s -u test-client:secret \
  -X POST http://localhost:9000/oauth2/token \
  -d "grant_type=client_credentials&scope=openid" | jq -r .access_token)

# 2) 携带令牌访问资源服务器 (9100)
curl -H "Authorization: Bearer $TOKEN" http://localhost:9100/api/users

# 3) 缺少权限时将返回 403 JSON
```

---

## molly-oss-spring-boot-starter

### 用途

为 Spring Boot 应用提供 **统一的对象存储服务抽象层**，通过配置切换底层存储实现，支持：

- **阿里云 OSS**（默认）— 云端对象存储
- **MinIO** — 自建 S3 兼容对象存储

核心能力：

| 功能 | 说明 |
|------|------|
| 文件上传 | 单文件/批量上传，支持进度回调 |
| 文件下载 | 流式下载，避免内存溢出 |
| 文件去重 | 基于 MD5 哈希识别重复文件，实现秒传 |
| 断点续传 | 阿里云 SDK 原生支持；MinIO 自动分片 |
| 缩略图 | 阿里云服务端图片处理；MinIO 服务端 ImageIO 生成 |
| 文件安全 | 文件名过滤、大小限制、类型白名单 |
| HTTP 端点 | 开箱即用的 REST API（可关闭） |

### 设计思路

#### 1. 统一抽象层 — OssTemplate

通过 `OssTemplate` 接口封装所有存储操作，业务代码不依赖具体 SDK，切换存储服务只需修改配置。

#### 2. 配置驱动切换

通过 `molly.oss.provider` 属性选择实现，使用 `@ConditionalOnProperty` + `@ConditionalOnClass` 条件注解激活对应 Bean。所有 Bean 均可被 `@ConditionalOnMissingBean` 覆盖。

#### 3. 可选依赖

阿里云 OSS SDK 和 MinIO SDK 均为 `optional` 依赖，使用方根据实际需要引入对应 SDK 即可。

#### 4. 内置 HTTP 端点

提供开箱即用的文件上传/下载 REST API，通过 `molly.oss.endpoint-enabled=false` 可关闭，使用方也可仅注入 `OssTemplate` 自行实现接口。

### 文件说明

| 文件 | 说明 |
|------|------|
| `MollyOssAutoConfiguration` | 主自动配置类，根据 `provider` 注册对应存储客户端和 `OssTemplate` |
| `MollyOssEndpointAutoConfiguration` | HTTP 端点自动配置，注册 `OssEndpointController` |
| `MollyOssProperties` | 配置属性类，前缀 `molly.oss`，含阿里云/MinIO 子配置 |
| `OssTemplate` | 核心接口，定义上传、下载、删除、去重、缩略图等操作 |
| `OssObject` | 对象元信息模型（名称、大小、类型、哈希、URL） |
| `UploadProgress` / `UploadProgressListener` | 上传进度模型和回调接口 |
| `OssException` | 统一异常封装 |
| `AliyunOssTemplate` | 阿里云 OSS 实现（含断点续传、服务端缩略图） |
| `MinioOssTemplate` | MinIO 实现（自动分片、服务端缩略图生成） |
| `OssEndpointController` | REST 控制器，提供文件上传/下载/缩略图/删除接口 |
| `FileUtil` | MD5 计算、对象名生成、缩略图生成、文件名安全过滤 |

### 配置属性

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `molly.oss.provider` | 存储提供商: `aliyun` / `minio` | `aliyun` |
| `molly.oss.bucket` | 默认存储桶名称 | - |
| `molly.oss.endpoint-enabled` | 是否启用内置 HTTP 端点 | `true` |
| `molly.oss.max-file-size` | 最大文件大小（字节） | `104857600` (100MB) |
| `molly.oss.allowed-types` | 允许的 MIME 类型白名单 | 无限制 |
| `molly.oss.thumbnail.enabled` | 是否自动生成缩略图 | `true` |
| `molly.oss.thumbnail.width` | 缩略图宽度 | `200` |
| `molly.oss.thumbnail.height` | 缩略图高度 | `200` |
| `molly.oss.aliyun.endpoint` | 阿里云 OSS Endpoint | - |
| `molly.oss.aliyun.access-key-id` | 阿里云 AccessKey ID | - |
| `molly.oss.aliyun.access-key-secret` | 阿里云 AccessKey Secret | - |
| `molly.oss.minio.endpoint` | MinIO 服务端点 | - |
| `molly.oss.minio.access-key` | MinIO Access Key | - |
| `molly.oss.minio.secret-key` | MinIO Secret Key | - |

### 使用方法

#### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.molly.oss</groupId>
    <artifactId>molly-oss-spring-boot-starter</artifactId>
    <version>${revision}</version>
</dependency>

<!-- 根据存储服务选择引入对应 SDK -->
<!-- 阿里云 OSS -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
</dependency>
<!-- 或 MinIO -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
</dependency>
```

#### 2. 配置 `application.yml`

**阿里云 OSS（默认）:**

```yaml
molly:
  oss:
    provider: aliyun
    bucket: my-bucket
    aliyun:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      access-key-id: your-access-key-id
      access-key-secret: your-access-key-secret
```

**MinIO:**

```yaml
molly:
  oss:
    provider: minio
    bucket: my-bucket
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
```

#### 3. 使用 OssTemplate

```java
@Service
public class MyFileService {

    private final OssTemplate ossTemplate;

    public MyFileService(OssTemplate ossTemplate) {
        this.ossTemplate = ossTemplate;
    }

    public OssObject uploadFile(InputStream input, long size, String filename) {
        String objectName = FileUtil.generateObjectName(filename);
        return ossTemplate.putObject("my-bucket", objectName, input, size,
                "application/octet-stream");
    }
}
```

#### 4. HTTP 端点一览

启动应用后即可使用以下接口：

| 方法 | 端点 | 说明 |
|------|------|------|
| `POST` | `/oss/upload` | 单文件上传（multipart/form-data, 参数名 `file`） |
| `POST` | `/oss/upload/batch` | 批量文件上传（参数名 `files`） |
| `GET` | `/oss/download/{objectName}` | 文件下载（流式响应） |
| `GET` | `/oss/thumbnail/{objectName}` | 获取缩略图 URL |
| `HEAD` | `/oss/exists/{objectName}` | 文件存在性检查（200/404） |
| `DELETE` | `/oss/delete/{objectName}` | 删除文件 |

```bash
# 单文件上传
curl -F "file=@/path/to/file.png" http://localhost:8080/oss/upload

# 批量上传
curl -F "files=@file1.png" -F "files=@file2.jpg" http://localhost:8080/oss/upload/batch

# 下载文件
curl -O http://localhost:8080/oss/download/2026/04/23/uuid.png
```

---

## molly-cache-spring-boot-starter

### 用途

为 Spring Boot 应用提供 **基于自研注解 + SpEL 的声明式缓存能力**，默认采用 Redis（Redisson 客户端）作为底层实现，并借助 Redisson 的 `RBatch` / `RMap` / `RLock` 保障复合操作的原子性。一次依赖引入即可满足以下四类缓存场景：

| 场景 | 代表注解 | 底层结构 |
|------|----------|----------|
| 单 key 读写失效 | `@MollyCacheable` / `@MollyCachePut` / `@MollyCacheEvict` | String（`RBucket`） |
| 多 key 批量读写失效 | `@MollyMultiCacheable` / `@MollyMultiCacheEvict` | `RBatch` + 多 `RBucket` |
| 单 key 单 / 多 subkey | `@MollyHashCacheable` / `@MollyHashCachePut` / `@MollyHashCacheEvict` | `RMap`（Hash） |
| 组合与分布式锁 | `@MollyCaching` / `@MollyCacheLock` | 聚合执行 / `RLock` |

内建三类高频生产防护：

- **缓存穿透**：回源为 `null` 时写入 `NullValue` 占位，短 TTL 自动清除
- **缓存击穿**：`lock = true` 时基于 Redisson 分布式锁 + 双检锁单飞回源
- **缓存雪崩**：全局 TTL 叠加 `ttl-jitter` 随机抖动，打散同时过期时刻

同时与 Spring 事务深度集成：失效/更新类注解默认 `afterCommit = true`，挂入 `TransactionSynchronization` 的提交后阶段执行，回滚不会误删缓存；无事务时自动降级为同步执行。

### 设计思路

#### 1. 自研注解体系，脱离 Spring Cache 抽象

Spring Cache 的 `@Cacheable` 难以表达多 key、Hash subkey 等复合语义。Molly 选择完全自研注解集 + 独立切面，所有注解统一支持 `name` / `key`（SpEL）/ `condition` / `unless` / `ttl` / `cacheNull` / `lock` / `afterCommit` 等字段，语义与扩展点一致。

#### 2. SPI 驱动 — 可替换底层存储

核心抽象 `CacheOperations` 定义所有底层操作，默认提供 `RedissonCacheOperations` 实现。使用方可自行实现该 SPI 接入 Memcached / Tair 等其它存储；使用方也可直接提供自己的 `RedissonClient` Bean 完全接管连接配置。

#### 3. 事务感知的失效策略

`TransactionAwareCacheFlusher` 基于 `TransactionSynchronizationManager`：活动事务中将失效动作挂到 `afterCommit`，避免事务回滚造成缓存与 DB 不一致；无事务时立即执行。该策略对注解与编程式 `CacheTemplate` 统一生效。

#### 4. 与 ORM 解耦

模块本身零 ORM 依赖，推荐在 `@Transactional` 的 application service 层使用注解或 `CacheTemplate`，不侵入 MyBatis / JPA 等 Repository 层；ORM 写操作在事务提交后由切面自动触发对应失效。

#### 5. 统一 SpEL 求值上下文

所有表达式通过 `SpelEvaluator` + `MethodBasedEvaluationContext` 求值，支持 `#argName` / `#root.method` / `#root.args` / `#result`（写回/失效阶段可用）；批量注解的 `idExtractor` 在集合单元素上下文中求值（如 `#this.id`），用于拆分返回集合回填每条缓存。

### 文件说明

| 文件 | 说明 |
|------|------|
| `config/MollyCacheAutoConfiguration` | 主自动配置，按 `provider` 注册 `RedissonClient`、`CacheOperations`、`CacheTemplate`、`TransactionAwareCacheFlusher` 等核心 Bean |
| `config/MollyCacheAopAutoConfiguration` | 切面自动配置，启用 `@EnableAspectJAutoProxy(proxyTargetClass = true)` 并注册主切面与锁切面 |
| `properties/MollyCacheProperties` | 配置属性类，前缀 `molly.cache`，含 `NullValue` / `Lock` / `Redisson` 三个内嵌配置 |
| `annotation/MollyCacheable` | 单 key Cache-Aside 读注解，支持 `condition` / `unless` / `cacheNull` / `lock` |
| `annotation/MollyCachePut` | 单 key 写回注解，始终将方法返回值写入缓存 |
| `annotation/MollyCacheEvict` | 单 key 失效注解，支持 `allEntries` 整命名空间清空与 `beforeInvocation` |
| `annotation/MollyMultiCacheable` | 多 key 批量读注解，`RBatch` 命中分片 + `idExtractor` 拆分回填 |
| `annotation/MollyMultiCacheEvict` | 多 key 批量失效注解 |
| `annotation/MollyHashCacheable` | Hash 读注解，支持单 subkey / 多 subkey / 整表三种模式 |
| `annotation/MollyHashCachePut` | Hash 写注解，支持 `field` / `fields` / 整表写入 |
| `annotation/MollyHashCacheEvict` | Hash 失效注解，支持 `field` / `fields` / `allFields` 三种模式 |
| `annotation/MollyCaching` | 组合注解，可在同方法上声明多个写/失效注解，统一原子调度 |
| `annotation/MollyCacheLock` | 分布式互斥锁注解，基于 Redisson `RLock` 实现 |
| `aop/MollyCacheAspect` | 主切面，统一调度：前置 evict → 读类注解 → proceed → 后置 put / evict / @MollyCaching |
| `aop/MollyCacheLockAspect` | 独立的分布式锁切面，`@Order` 高于事务切面，避免锁持有时间被事务扩展 |
| `aop/CacheOperationContext` | 切面共享上下文，封装方法反射信息、参数、返回值与 SpEL 求值入口 |
| `core/CacheOperations` | 底层缓存 SPI，定义单 key / 批量 / Hash / 锁 / pattern 删除等操作 |
| `core/CacheTemplate` | 编程式缓存门面，负责 key 拼接、TTL 抖动、`NullValue` 透传与事务后置失效封装 |
| `core/CacheKeyGenerator` | Key 拼接器，统一 `keyPrefix + name + separator + key` 规则 |
| `core/SpelEvaluator` | SpEL 求值器，表达式解析结果按字符串缓存，支持 `#this` 根对象求值 |
| `core/NullValue` | 空值占位符（防穿透），命中占位时对外透明返回 `null` |
| `core/CacheException` | 缓存层统一运行时异常 |
| `support/redis/RedissonCacheOperations` | `CacheOperations` 的默认 Redisson 实现，RBucket / RMap / RBatch / RLock 全量适配 |
| `sync/TransactionAwareCacheFlusher` | 事务感知的动作执行器，活动事务中挂入 `afterCommit`，否则立即执行 |
| `AutoConfiguration.imports` | Spring Boot 3.x 自动配置注册文件，声明上述两个自动配置类 |

### 配置属性

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `molly.cache.provider` | 缓存提供商，当前支持 `redis` | `redis` |
| `molly.cache.key-prefix` | 全局 key 前缀 | `molly:` |
| `molly.cache.separator` | 命名空间与 key 的分隔符 | `:` |
| `molly.cache.default-ttl` | 注解未显式声明 TTL 时的默认值 | `PT30M` |
| `molly.cache.ttl-jitter` | TTL 抖动系数（0~1），防雪崩，0 关闭 | `0.1` |
| `molly.cache.after-commit` | 失效/更新类注解默认是否事务后置 | `true` |
| `molly.cache.null-value.enabled` | 是否启用空值占位（防穿透） | `true` |
| `molly.cache.null-value.ttl` | 空值占位 TTL | `PT1M` |
| `molly.cache.lock.enabled` | 是否启用分布式锁能力 | `true` |
| `molly.cache.lock.wait-time` | 锁最大等待时间 | `PT3S` |
| `molly.cache.lock.lease-time` | 锁自动释放时间（租期） | `PT10S` |
| `molly.cache.redisson.address` | Redis 连接地址 | `redis://127.0.0.1:6379` |
| `molly.cache.redisson.database` | 数据库索引 | `0` |
| `molly.cache.redisson.password` | 认证密码 | - |
| `molly.cache.redisson.connection-minimum-idle-size` | 连接池最小空闲连接数 | `8` |
| `molly.cache.redisson.connection-pool-size` | 连接池最大连接数 | `32` |

> 若需要 Sentinel / Cluster 等高级拓扑，请自行提供 `RedissonClient` Bean 覆盖默认配置。

### 使用方法

#### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.molly.cache</groupId>
    <artifactId>molly-cache-spring-boot-starter</artifactId>
    <version>${revision}</version>
</dependency>

<!-- Redisson 为 optional 依赖，需要使用方显式引入 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
</dependency>
```

> 编译时建议开启 `-parameters`（`maven-compiler-plugin` 的 `<parameters>true</parameters>`），否则 SpEL 无法通过参数名引用。

#### 2. 配置 `application.yml`

```yaml
molly:
  cache:
    key-prefix: "app:"
    default-ttl: PT30M
    ttl-jitter: 0.1
    null-value:
      enabled: true
      ttl: PT1M
    lock:
      enabled: true
      wait-time: PT3S
      lease-time: PT10S
    redisson:
      address: redis://127.0.0.1:6379
      database: 0
      password:
```

#### 3. 注解式使用

```java
@Service
public class UserService {

    // 单 key 读：命中直接返回，未命中回源并写入；自动防穿透 + 锁单飞
    @MollyCacheable(name = "user", key = "#id", ttl = "PT30M", lock = true)
    public User findById(Long id) {
        return userRepository.selectById(id);
    }

    // 多 key 批量读：命中分片，未命中回源后按 idExtractor 拆分回填
    @MollyMultiCacheable(name = "user", keys = "#ids", idExtractor = "#this.id")
    public List<User> findByIds(Collection<Long> ids) {
        return userRepository.selectBatchIds(ids);
    }

    // Hash 单 subkey 写：user:{userId} 下的 profile 字段
    @MollyHashCachePut(name = "user", key = "#user.id", field = "'profile'")
    public User updateProfile(User user) {
        userRepository.updateById(user);
        return user;
    }

    // 组合：事务提交后同时失效单 key 与 Hash，回滚则不失效
    @Transactional
    @MollyCaching(
        evict = @MollyCacheEvict(name = "user", key = "#id"),
        hashEvict = @MollyHashCacheEvict(name = "user", key = "#id", allFields = true)
    )
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    // 分布式互斥锁，用于并发防抖 / 幂等
    @MollyCacheLock(name = "order-submit", key = "#userId", waitTime = "PT1S", leaseTime = "PT5S")
    public void submitOrder(Long userId, Order order) {
        // ...
    }
}
```

#### 4. 编程式使用（`CacheTemplate`）

```java
@Service
public class ReportService {

    private final CacheTemplate cache;

    public ReportService(CacheTemplate cache) {
        this.cache = cache;
    }

    public Report load(Long id) {
        // 读取：命中 NullValue 占位时自动还原为 null
        Object hit = cache.get("report", id);
        if (hit != null) {
            return (Report) hit;
        }
        Report report = queryFromDb(id);
        cache.put("report", id, report, Duration.ofMinutes(10));
        return report;
    }

    @Transactional
    public void invalidate(Long id) {
        // 事务提交后失效，回滚不执行
        cache.evictAfterCommit("report", id);
    }
}
```

#### 5. 扩展与覆盖

- **接管 RedissonClient**：声明自己的 `RedissonClient` Bean，自动覆盖 starter 默认实现，适用于 Sentinel / Cluster 场景
- **接管底层存储**：实现 `CacheOperations` 接口并声明为 Bean，即可替换默认 Redisson 实现
- **覆盖 Key 生成规则**：提供自定义 `CacheKeyGenerator` Bean
- **覆盖 SpEL 求值器**：提供自定义 `SpelEvaluator` Bean

---

## molly-document-spring-boot-starter

### 用途

一次引入为业务应用提供 **Word / Excel / PDF / Email 四类文档生成能力** ，子能力可拆可合、按需激活：

| 子能力 | 激活条件 | 门面 Bean | 典型场景 |
|------|----------|-----------|----------|
| Word | classpath 存在 `XWPFTemplate`（引入 `poi-tl`） | `WordTemplate` | 合同 / 报告 / 通知模板渲染 |
| Excel | classpath 存在 POI `Workbook`（引入 `poi-ooxml`） | `ExcelTemplate` | 批量导入导出 + 多级表头 + 流式 |
| PDF | classpath 存在 `PDDocument`（引入 `pdfbox`） | `PdfTemplate` | AcroForm 模板字段填充 + flatten |
| Email | classpath 存在 `JavaMailSender`（引入 `spring-boot-starter-mail`） | `MailService` | 模板渲染 + 附件 + 异步 + 重试 |

核心能力一览：

- **注解驱动 Excel**：6 个专用注解 + CellConverter SPI + 6 个内置转换器，自动处理日期 / 枚举 / 布尔 / 数字等类型
- **多级表头**：支持以 `.` 分隔的层次表头与自动合并单元格（同列前缀一致 ⇒ 横向合并，同值连续段 ⇒ 纵向合并）
- **样式缓存**：`ExcelStyleResolver` 按样式指纹复用 `CellStyle`，避开 POI 64000 样式上限
- **流式导出**： `exportStream` 默认使用 SXSSF，滑窗可配，万级数据恢复内存不超过 100MB
- **AcroForm 填充**：默认 flatten 锁定表单，避免下发给客户后被二次编辑
- **邮件门面**：`MailRequest` 统一承载收件人 / 抄送 / 密送 / 附件 / 内联图片 / 模板变量，`sendAsync` 基于线程池，`send` 支持 backoff 重试

### 设计思路

#### 1. 子能力可拆可合（`@ConditionalOnClass` 守门）

顶层 `MollyDocumentAutoConfiguration` 仅注册 `MollyDocumentProperties`。Word / Excel / PDF / Email 四个子 AutoConfiguration 各自要求相应 SDK 类存在时才激活，实现了按需激活。所有重依赖 `optional=true`，不用就不引。

#### 2. 模板加载统一抽象

`TemplateLoader` 抽象模板来源，支持 `classpath:` / `file:` / 绝对路径三种前缀，内置按路径缓存字节数组。Word 与 PDF 各自拥有独立命名的 Bean（`wordTemplateLoader` / `pdfTemplateLoader`），可分别指定根路径。

#### 3. 注解驱动 + 类型转换 SPI（Excel）

实体类反射一次缓存至 `ExcelClassMeta`（按 `Class + DatePattern` 维度）；单元格值由 `CellConverter<T>` SPI 转换，内置 `String / Date / LocalDateTime / Enum / Boolean(Y/N) / Number` 六类，业务可在字段上用 `@ExcelConverter(xxx.class)` 直接注入自定义转换器。

#### 4. PDF AcroForm 填充后 flatten

默认 `molly.document.pdf.flatten=true`，填充后将表单字段固化为静态内容；如需保留可编辑表单（如内部审批场景）可关闭。中文字体通过 `molly.document.pdf.font.chinese` 指定资源路径。

#### 5. 邮件异步 + 重试

`DefaultMailService#sendAsync` 依赖名为 `mollyMailAsyncExecutor` 的 `ThreadPoolTaskExecutor`（可覆盖）提交 `CompletableFuture`；`send` 含内置 backoff 重试循环，达到 `max-attempts` 仍失败时抑制并抛出最后异常。模板渲染优先使用 Thymeleaf `TemplateEngine`（无模板引擎时降级为朴素 `${var}` 替换）。

### 核心流程

```
Word  : WordTemplate.render(name, model, out)
          ↓ TemplateLoader.readBytes(name)
          ↓ XWPFTemplate.compile(in).render(model).writeAndClose(out)

Excel : ExcelTemplate.export(type, rows, out)
          ↓ ExcelClassMeta.of(type)        // 反射+注解解析，进程级缓存
          ↓ writeHeader(multi-level merge)
          ↓ CellConverter.toCellValue       // 按字段类型分发
          ↓ ExcelStyleResolver             // CellStyle 指纹复用

PDF   : PdfTemplate.render(name, fields, out)
          ↓ Loader.loadPDF(bytes)
          ↓ PDAcroForm.getField(name).setValue(value)
          ↓ form.flatten() → doc.save(out)

Email : MailService.sendAsync(request)
          ↓ executor.submit(() -> send(request))
          ↓ resolveBody(Thymeleaf 或 ${var} 替换)
          ↓ JavaMailSender + MimeMessageHelper
          ↓ backoff retry loop
```

### 文件说明

| 文件 | 说明 |
|------|------|
| `config/MollyDocumentAutoConfiguration` | 顶层自动配置，启用 `MollyDocumentProperties` |
| `config/MollyWordAutoConfiguration` | Word 子能力守门，`@ConditionalOnClass(XWPFTemplate.class)`，注册 `wordTemplateLoader` + `WordTemplate` |
| `config/MollyExcelAutoConfiguration` | Excel 子能力守门，`@ConditionalOnClass(Workbook.class)`，注册 `ConverterRegistry` + `ExcelTemplate` |
| `config/MollyPdfAutoConfiguration` | PDF 子能力守门，`@ConditionalOnClass(PDDocument.class)`，注册 `pdfTemplateLoader` + `PdfTemplate` |
| `config/MollyEmailAutoConfiguration` | Email 子能力守门，`@ConditionalOnClass(JavaMailSender.class)`，注册 `mollyMailAsyncExecutor` + `MailService` |
| `properties/MollyDocumentProperties` | 配置属性根，前缀 `molly.document`，含 `Word/Excel/Pdf/Email/Async/Retry/Font` 嵌套配置 |
| `core/TemplateLoader` | 模板读取抽象，支持 classpath / file / 绝对路径 + 可选缓存 |
| `core/DocumentException` | 文档特定运行时异常 |
| `word/WordTemplate` / `DefaultWordTemplate` | poi-tl 门面与默认实现 |
| `excel/ExcelTemplate` / `DefaultExcelTemplate` | Excel 门面，4 个方法：`export` / `exportStream` / `importFrom` / `importStream` |
| `excel/annotation/*` | `@ExcelSheet` / `@ExcelColumn` / `@ExcelStyle` / `@ExcelConverter` / `@ExcelDateFormat` / `@ExcelEnum` |
| `excel/converter/*` | `CellConverter` SPI + `ConverterRegistry` + 6 个内置转换器 |
| `excel/meta/ExcelClassMeta` / `ExcelColumnMeta` | 实体类元数据（进程级缓存） |
| `excel/style/ExcelStyleResolver` / `DefaultStylePresets` | `CellStyle` 指纹复用（避开 64000 上限）+ 预设样式 |
| `pdf/PdfTemplate` / `DefaultPdfTemplate` | PDFBox AcroForm 填充与 flatten |
| `email/MailService` / `DefaultMailService` | JavaMail 门面，Thymeleaf 可选 + 异步线程池 + backoff 重试 |
| `email/MailRequest` | 邮件请求模型，含 `Attachment` / `Inline` 嵌套类 |
| `AutoConfiguration.imports` | 声明 5 个自动配置类 |

### 配置属性

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `molly.document.word.template-location` | Word 模板根路径 | `classpath:templates/word/` |
| `molly.document.word.cache-templates` | 是否缓存模板字节 | `true` |
| `molly.document.excel.stream-window-size` | SXSSF 流式导出滑窗 | `100` |
| `molly.document.excel.auto-size-column` | 是否自动列宽 | `false` |
| `molly.document.excel.compress-temp-files` | SXSSF 临时文件是否压缩 | `true` |
| `molly.document.excel.date-pattern` | 默认日期格式 | `yyyy-MM-dd HH:mm:ss` |
| `molly.document.excel.header-preset` | 表头预设：`default` / `plain` / `bordered` | `default` |
| `molly.document.excel.header-separator` | 多级表头分隔符 | `.` |
| `molly.document.pdf.template-location` | PDF 模板根路径 | `classpath:templates/pdf/` |
| `molly.document.pdf.flatten` | 填充后是否 flatten（表单不可再编辑） | `true` |
| `molly.document.pdf.font.chinese` | 中文字体资源路径 | - |
| `molly.document.email.from` | 默认发件人 | - |
| `molly.document.email.template-location` | 邮件模板根路径 | `classpath:/templates/mail/` |
| `molly.document.email.template-suffix` | 模板后缀 | `.html` |
| `molly.document.email.async.enabled` | 是否启用异步发送 | `true` |
| `molly.document.email.async.core-pool-size` | 线程池核心线程数 | `4` |
| `molly.document.email.async.max-pool-size` | 线程池最大线程数 | `16` |
| `molly.document.email.async.queue-capacity` | 队列容量 | `200` |
| `molly.document.email.retry.enabled` | 是否启用重试 | `true` |
| `molly.document.email.retry.max-attempts` | 最大尝试次数 | `3` |
| `molly.document.email.retry.back-off` | 重试退避时间 | `PT2S` |

### 使用方法

#### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.molly.document</groupId>
    <artifactId>molly-document-spring-boot-starter</artifactId>
    <version>${revision}</version>
</dependency>

<!-- 按需引入子能力依赖 -->
<!-- Word -->
<dependency>
    <groupId>com.deepoove</groupId>
    <artifactId>poi-tl</artifactId>
</dependency>
<!-- Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
</dependency>
<!-- PDF -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
</dependency>
<!-- Email -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<!-- 可选：Thymeleaf 邮件模板 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

#### 2. 配置 `application.yml`

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 465
    username: noreply@example.com
    password: change-me
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true

molly:
  document:
    word:
      template-location: classpath:/templates/word/
    excel:
      stream-window-size: 200
      header-separator: "."
    pdf:
      template-location: classpath:/templates/pdf/
      flatten: true
    email:
      from: noreply@example.com
      template-location: classpath:/templates/mail/
      async:
        core-pool-size: 4
        max-pool-size: 16
      retry:
        max-attempts: 3
        back-off: 2s
```

#### 3. Word：poi-tl 模板渲染

将 `.docx` 放入 `classpath:/templates/word/`，模板内使用 `{{var}}` / `{{?x}}...{{/x}}` / `{{*list}}` / `{{@img}}` 等 poi-tl 语法。

```java
@Service
@RequiredArgsConstructor
public class ContractService {

    private final WordTemplate wordTemplate;

    public void export(Contract c, OutputStream out) throws Exception {
        Map<String, Object> model = Map.of(
                "name", c.getCustomerName(),
                "amount", c.getAmount(),
                "signDate", c.getSignDate()
        );
        wordTemplate.render("contract.docx", model, out);
    }
}
```

#### 4. Excel：注解驱动导出 / 导入

实体类声明（`.` 分隔多级表头，`@ExcelEnum` 自动把枚举转为 label）：

```java
@Data
@ExcelSheet(name = "订单")
public class OrderRow {

    @ExcelColumn(header = "订单号", order = 1, width = 24)
    private String orderNo;

    @ExcelColumn(header = "客户.姓名", order = 2)       // 属于「客户」父级表头
    private String customerName;

    @ExcelColumn(header = "客户.手机", order = 3)
    private String customerPhone;

    @ExcelColumn(header = "状态", order = 4)
    @ExcelEnum(labelMethod = "getLabel", codeMethod = "getCode")
    private OrderStatus status;

    @ExcelColumn(header = "创建时间", order = 5)
    @ExcelDateFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
```

```java
@RestController
@RequiredArgsConstructor
public class OrderExportController {

    private final ExcelTemplate excelTemplate;
    private final OrderService orderService;

    @GetMapping("/orders/export")
    public void export(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename=orders.xlsx");
        // 大数据集推荐使用 exportStream（SXSSF）
        excelTemplate.exportStream(OrderRow.class, orderService.list(), resp.getOutputStream());
    }
}
```

自定义转换器（如货币金额带单位）：

```java
public class MoneyConverter implements CellConverter<BigDecimal> {
    @Override
    public Object toCellValue(BigDecimal v, ConvertContext ctx) {
        return v == null ? "" : "￥" + v.setScale(2, RoundingMode.HALF_UP);
    }
    @Override
    public BigDecimal fromCellValue(Object cell, ConvertContext ctx) {
        if (cell == null) return null;
        String s = cell.toString().replace("￥", "").trim();
        return s.isEmpty() ? null : new BigDecimal(s);
    }
}

// 在字段上声明
@ExcelColumn(header = "金额", order = 4)
@ExcelConverter(MoneyConverter.class)
private BigDecimal amount;
```

#### 5. PDF：AcroForm 字段填充

用 Acrobat / WPS / LibreOffice 制作带「文本域」的 `.pdf` 模板，记住每个字段的 `Name`，填充时作为 `Map` 的 key：

```java
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final PdfTemplate pdfTemplate;

    public void generate(Invoice inv, OutputStream out) throws Exception {
        Map<String, String> fields = Map.of(
                "invoiceNo", inv.getInvoiceNo(),
                "customer", inv.getCustomerName(),
                "amount", inv.getAmount().toPlainString()
        );
        pdfTemplate.render("invoice.pdf", fields, out);
    }
}
```

#### 6. Email：同步与异步发送

```java
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final MailService mailService;

    /** HTML 模板异步发送 */
    public void welcome(String to, String name) {
        MailRequest req = MailRequest.builder()
                .to(List.of(to))
                .subject("欢迎加入 Molly")
                .template("welcome")                        // classpath:/templates/mail/welcome.html
                .templateVariables(Map.of("name", name))
                .html(true)
                .build();
        mailService.sendAsync(req);                         // 立即返回 CompletableFuture
    }

    /** 带附件的同步发送（含重试） */
    public void sendReport(String to, File report) {
        MailRequest req = MailRequest.builder()
                .to(List.of(to))
                .subject("月度报表")
                .content("请查收附件。")
                .attachments(List.of(
                        new MailRequest.Attachment("report.xlsx", report)
                ))
                .build();
        mailService.send(req);                              // 同步 + backoff 重试
    }
}
```

#### 7. 验证

在统一示例模块 `molly-example` 的 `document` profile 下提供了 4 个演示端点，启动（`mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=document`）：

```bash
# Word：模板渲染（需先将 contract.docx 放入 src/main/resources/templates/word/）
curl -X POST -H "Content-Type: application/json" \
  -d '{"name":"张三","amount":"￥99.00"}' \
  http://localhost:8081/doc/word/contract.docx -o contract.docx

# Excel：注解驱动导出
curl http://localhost:8081/doc/excel/orders -o orders.xlsx

# PDF：表单填充（需先将带 AcroForm 的 invoice.pdf 放入 templates/pdf/）
curl -X POST -H "Content-Type: application/json" \
  -d '{"invoiceNo":"INV-001","customer":"张三","amount":"199.90"}' \
  http://localhost:8081/doc/pdf/invoice.pdf -o invoice.pdf

# Email：异步发送（需调通 spring.mail.*）
curl -X POST -H "Content-Type: application/json" \
  -d '{"to":"user@example.com","name":"张三"}' \
  http://localhost:8081/doc/mail/demo
```

#### 8. 扩展与覆盖

- **定制 TemplateLoader**：依赖远程 / OSS 模板时，声明同名 Bean（`wordTemplateLoader` / `pdfTemplateLoader`）覆盖默认
- **自定义 CellConverter**：在 `ConverterRegistry` Bean 上 `register(type, converter)`，或在字段上用 `@ExcelConverter(xxx.class)` 局部指定
- **替换异步线程池**：提供 `@Bean(name = "mollyMailAsyncExecutor") Executor` 即可覆盖，方便对接业务统一的 `ThreadPoolTaskExecutor`
- **替换模板引擎**：未引入 Thymeleaf 时 `MailService` 自动降级为 `${var}` 替换；引入后默认使用 Thymeleaf

---

## molly-example

### 用途

以单个 Maven 模块 + 单个 Spring Boot 主类 `MollyExampleApplication` 统一演示 **认证服务器 / 资源服务器 / 文档生成** 三类场景，通过 `spring.profiles.active` 切换子示例。避免了为每个 Starter 分别维护独立 example 模块的冗余，同时保留了子示例间的彻底隔离。

| profile | 端口 | 启用能力 | 主要端点 |
|---------|------|---------|----------|
| `auth` | 9000 | OAuth2/OIDC 授权服务器 | `/.well-known/openid-configuration`、`/oauth2/jwks`、`/oauth2/token` |
| `resource` | 9100 | 资源服务器 + RBAC | `/api/users`、`/api/users/{id}`、`/api/users/export` |
| `document` | 8081 | Word / Excel / PDF / Email | `/doc/word/contract.docx`、`/doc/excel/orders`、`/doc/pdf/invoice.pdf`、`/doc/mail/demo` |

### 设计思路

#### 1. 单主类 + @Profile 条件化装配

所有 `@Configuration` / `@Service` / `@RestController` 都加了 `@Profile("auth|resource|document")`，确保三个子示例的 Bean 仅在对应 profile 激活时注入。默认不激活任何 profile 时会因三个 Starter 同时加载而冲突，必须显式指定。

#### 2. AutoConfiguration 精确排除

三个 Starter 的自动配置同时在 classpath，值直接启动会触发多条 `SecurityFilterChain` / `JavaMailSender` 等冲突。各 profile 的 YAML 使用 `spring.autoconfigure.exclude` 显式剔除其它两组 Starter 的自动配置类（用全限定名）。

#### 3. Profile 分级配置布局

公共内容写在 `application.yml`，三个子配置 `application-auth.yml` / `application-resource.yml` / `application-document.yml` 各自持有专属端口、业务属性和 exclude 列表。

### 目录结构

```
molly-example/
└── src/main/
    ├── java/cn/molly/example/
    │   ├── MollyExampleApplication.java     # 统一主类
    │   ├── auth/config/SecurityConfig.java  # @Profile("auth") 授权服务器安全配置
    │   ├── resource/                        # @Profile("resource")
    │   │   ├── controller/UserController.java        # @MollyPreAuthorize 演示
    │   │   └── service/DemoPermissionService.java    # 权限 SPI 实现
    │   └── document/                        # @Profile("document")
    │       ├── TemplateSmokeMain.java                # 模板本地冒烟 main（脱源 Web）
    │       ├── model/OrderRow.java                   # 注解驱动 Excel 实体
    │       └── web/DocumentController.java           # 4 个演示端点
    └── resources/
        ├── application.yml                  # 公共配置
        ├── application-auth.yml             # auth profile
        ├── application-resource.yml         # resource profile
        ├── application-document.yml         # document profile
        └── templates/
            ├── mail/{welcome.html, notice.html}       # 邮件模板（Thymeleaf）
            ├── word/contract.docx                     # Word 演示模板（poi-tl 占位符）
            └── pdf/invoice.pdf                        # PDF 演示模板（AcroForm 字段）
```

### 快速启动

```bash
# 认证服务器（端口 9000）
mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=auth

# 资源服务器（端口 9100，需 auth profile 已启动 —— 用于拉 JWK）
mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=resource

# 文档应用（端口 8081，独立运行）
mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=document
```

### 验证命令

```bash
# ---- auth profile ----
curl http://localhost:9000/.well-known/openid-configuration    # HTTP 200
curl -X POST -u test-client:secret http://localhost:9000/oauth2/token \
     -d "grant_type=client_credentials&scope=openid"

# ---- resource profile（先从上面 auth 拿令牌）----
TOKEN=$(curl -s -u test-client:secret -X POST http://localhost:9000/oauth2/token \
         -d "grant_type=client_credentials&scope=openid" | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" http://localhost:9100/api/users

# ---- document profile ----
curl -X POST -H "Content-Type: application/json" \
     -d '{"contractNo":"C-001","partyA":"张三","partyB":"Molly"}' \
     http://localhost:8081/doc/word/contract.docx -o contract.docx
curl http://localhost:8081/doc/excel/orders -o orders.xlsx
curl -X POST -H "Content-Type: application/json" \
     -d '{"invoiceNo":"INV-001","customer":"张三","total":"199.90"}' \
     http://localhost:8081/doc/pdf/invoice.pdf -o invoice.pdf
curl -X POST -H "Content-Type: application/json" \
     -d '{"to":"user@example.com","name":"张三"}' \
     http://localhost:8081/doc/mail/demo
```

### 扩展与定制

- **新增子示例**：新增包 `cn.molly.example.<name>`，所有装配类打 `@Profile("<name>")`，再新增 `application-<name>.yml` 显式剔除其它 Starter 的 AutoConfiguration 即可
- **切换为多主类**：若业务需要按子模块打包独立可运行 jar，可改为为每个 profile 提供独立 `@SpringBootApplication` 主类，配合 `spring-boot-maven-plugin` 的 `classifier` 多打包
- **模板资产替换**：`templates/word/contract.docx` 与 `templates/pdf/invoice.pdf` 为演示用，实际项目应根据 `molly.document.word.template-location` / `molly.document.pdf.template-location` 指向自有模板

---

## 构建与运行

```bash
# 全量构建
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests

# 构建指定模块
mvn clean install -pl molly-auth-server-spring-boot-starter -am
mvn clean install -pl molly-authorization-spring-boot-starter -am
mvn clean install -pl molly-oss-spring-boot-starter -am
mvn clean install -pl molly-cache-spring-boot-starter -am
mvn clean install -pl molly-document-spring-boot-starter -am

# 运行示例认证服务器（auth profile，端口 9000）
mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=auth

# 运行示例资源服务器（resource profile，端口 9100，需认证服务器先启动）
mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=resource

# 运行示例文档应用（document profile，端口 8081）
mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=document
```

## 许可证

Apache License 2.0
