# Molly
## 项目说明
1. 首先，这是一个支持分布式 web 项目开发脚手架项目，以 spring-boot-dependencies 等依赖为基础，封装了常用功能，如：认证授权，定时任务，支付，短信，文件管理，RPC，注册中心，配置中心，ORM，缓存，消息队列，搜索引擎，监控等。
2. 业务系统项目有选择的加入本项目中的模块已完成某项功能，业务系统可以通过 ymal 配置文件定制模块功能，减少重复开发，提高开发效率。

## 模块说明（持续更新中）
1. molly-infrastructure: 基础工具， 公共模型类。
2. spring-security-oauth2-authorization-server： 认证授权服务器核心逻辑，支持多方式登录及社交平台登录，不负责数据库交互，只定义认证授权需要的数据交互接口，具体实现由真正的认证服务器项目实现。
3. molly-resource-server-spring-boot-starter： 为分布式系统中的业务服务提供自动配置，使其轻松成为OAuth2资源服务器和客户端，为服务间RPC接口调用提供认证保护。

# 认证授权
## spring-security-oauth2-authorization-server：认证授权服务器核心
1. 负责所有复杂的、通用的、与业务无关的 “安全流程控制”，使用 Spring Security 6.x 实现。
2. 完全遵循 OAuth 2.1 ， OIDC 1.0 ， JSON Web Tokens，SAML，LDAP标准，管理客户端和服务，为它们颁发不同权限（Scopes）的 Token。
3. 支持验证码，短信等多因子认证，支持微信，支付宝等多平台登录。

### 开发计划

第一阶段：搭建核心骨架 (MVP)

此阶段的目标是让 Starter 能够以最少配置运行起来，提供一个功能完备但最简化的 OAuth2 认证服务器。

1. 定义配置属性 (`@ConfigurationProperties`):
    * 创建 MollyAuthServerProperties 类。
    * 提供最基础的配置项，例如 issuer-uri (签发者地址)，这是 OIDC 的必需配置。

2. 实现核心自动配置 (`MollyAuthServerAutoConfiguration`):
    * 配置 `AuthorizationServerSettings`: 读取 MollyAuthServerProperties 中的 issuer-uri 并将其配置到这个 Bean 中。
    * 提供默认的 `JWKSource`: 为了开箱即用，自动配置一个基于内存的 JWKSource Bean，用于生成和管理 JWT 签名密钥。这让使用者在开发阶段无需关心密钥存储。同时，使用 @ConditionalOnMissingBean                                         
      注解，允许使用者覆盖它，以便在生产环境中使用自己的密钥源（如从数据库或文件中加载）。
    * 应用核心安全过滤器链: 自动配置 Spring Authorization Server 的标准安全过滤器链，它会保护 /oauth2/authorize, /oauth2/token 等核心端点。

3. 定义必须由使用者实现的接口:
    * 这是本 Starter 设计的关键。我们不提供数据访问的实现，而是要求使用者提供。
    * 在自动配置类中，明确要求 Spring 容器中必须存在以下两个 Bean：
        * UserDetailsService: 用于根据用户名加载用户核心数据。
        * RegisteredClientRepository: 用于管理和查询已注册的 OAuth2 客户端信息。
    * 如果使用者没有提供这两个 Bean 的实现，Spring Boot 应用启动时会因为缺少依赖而失败。这强制使用者必须实现自己的用户和客户端存储逻辑。

第二阶段：增强扩展能力

在核心骨架之上，增加更高级和可定制的功能。

1. 令牌定制 (`OAuth2TokenCustomizer`):
    * 提供一个默认的 OAuth2TokenCustomizer Bean。
    * 它可以在 JWT (Access Token) 中加入一些额外的声明 (claims)，例如用户 ID、角色等。
    * 同样使用 @ConditionalOnMissingBean，允许使用者提供自己的 OAuth2TokenCustomizer 来完全控制令牌内容。

2. 定义多因子认证 (MFA) 和社交登录的扩展点:
    * 定义接口: 创建 MollyUserAccountService 接口，继承 Spring Security 的 UserDetailsService，并额外增加 loadUserByPhone(String phone) 或 loadUserBySocial(String provider, String openId) 等方法。
    * 定义抽象: 自动配置类将依赖我们自己的 MollyUserAccountService 接口，而不是直接依赖 UserDetailsService。
    * 流程切入: 为后续实现验证码登录、短信登录等流程打下基础。此时只定义接口和依赖，暂不实现具体流程。

第三阶段：实现高级认证流程

实现 GEMINI.md 中提到的多因子和社交登录功能。

1. 实现验证码/短信登录:
    * 创建新的认证提供者 (AuthenticationProvider) 和认证过滤器 (AuthenticationFilter)。
    * 这些组件将处理携带验证码或手机号的登录请求，并调用 MollyUserAccountService 中相应的方法来验证用户。
    * 通过配置属性，让使用者可以选择性地开启这些登录方式。

2. 集成社交登录:
    * 利用 Spring Security 内置的 oauth2Login() 功能。
    * 在 MollyAuthServerProperties 中增加社交登录的配置项，让使用者可以方便地填入各平台（如微信、GitHub）的 client-id 和 client-secret。
    * 自动配置会根据这些属性，动态地注册 ClientRegistrationRepository，从而启用对应的社交登录按钮。

第四阶段：完善与文档

1. 编写清晰的文档:
    * README.md: 详细说明此 Starter 的作用、如何引入。
    * 使用指南: 提供一个清晰的教程，指导开发者如何实现 MollyUserAccountService 和 RegisteredClientRepository 接口（可以提供一个基于内存或 JDBC 的简单示例）。
    * 配置项大全: 列出所有可在 application.yml 中配置的属性及其说明。
2. 编写单元测试和集成测试: 确保自动配置在各种场景下都能正确工作。

## molly-resource-server-spring-boot-starter：OAuth2资源服务器和客户端