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
├── molly-authorization-server-spring-boot-starter/ # OAuth2 授权服务器 Starter
│   └── cn.molly.security.auth
│       ├── config/
│       │   └── MollyAuthServerAutoConfiguration    # 核心自动配置
│       ├── properties/
│       │   └── MollyAuthServerProperties           # 配置属性: molly.security.auth.*
│       └── service/
│           └── MollyUserAccountService             # 用户账户 SPI 扩展
└── molly-auth-server-example/                      # 示例认证服务器应用
    └── cn.molly.example.auth
        ├── AuthServerApplication                   # Spring Boot 主类 (端口 9000)
        └── config/SecurityConfig                   # 安全配置示例
```

---

## molly-authorization-server-spring-boot-starter

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
    <artifactId>molly-authorization-server-spring-boot-starter</artifactId>
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

## 构建与运行

```bash
# 全量构建
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests

# 构建指定模块
mvn clean install -pl molly-authorization-server-spring-boot-starter -am

# 运行示例认证服务器
mvn spring-boot:run -pl molly-auth-server-example
```

## 许可证

Apache License 2.0
