# Guman 开发规约

本文件适用于整个仓库。修改代码时，应保持实现、测试和文档同步。

## 技术基线

- 使用 Java 21、Spring Boot 4、Spring WebFlux 和 AgentScope Java 2。
- 优先使用 Java 21 已稳定且团队熟悉的语言特性，但不要使用 `record`。
- DTO 和事件对象使用普通 Java 类，并提供明确的 Getter/Setter；不要声明为 `record`。
- HTTP 接口保持非阻塞，响应式调用链中禁止使用 `block()`、`subscribe()` 或阻塞式 I/O。
- `HarnessAgent` 作为单例 Bean 使用；每次请求通过 `RuntimeContext` 隔离 `userId` 和 `sessionId`。

## DDD 模块边界

- `guman-domain` 是纯领域层，仅包含领域对象、值对象、领域事件与领域规则；不得依赖 Spring、AgentScope、WebFlux 或其他项目模块。
- `guman-application` 是应用层，仅负责编排用例并声明输入、输出端口；只能依赖 `guman-domain` 和必要的抽象接口库。
- `guman-infrastructure` 是基础设施层，负责实现应用层输出端口以及集成 AgentScope 等外部能力；可以依赖 `guman-application` 和 `guman-domain`。
- `guman-interfaces` 是接口层，负责 HTTP、SSE、请求校验和接口 DTO；可以依赖 `guman-application` 与 `guman-domain`，不得直接调用基础设施实现。
- `guman-bootstrap` 是启动装配层，仅放置应用入口、运行配置和模块装配依赖，不承载领域或业务逻辑。
- 新业务先定义领域模型和应用端口，再在外层实现适配器；禁止 Controller 直接依赖 AgentScope SDK。
- 跨模块依赖必须遵循由外向内的方向，不得形成循环依赖或绕过应用层调用基础设施。

## Spring 依赖注入

- 统一使用字段注入，不使用构造器注入。
- 所有由 Spring 注入的依赖字段必须显式添加 `@Autowired`，不能依赖隐式注入。
- 注入字段声明为 `private`；由于需要由 Spring 写入，不添加 `final`。
- 配置值字段使用 `@Value` 或 `@ConfigurationProperties` 注入，不要通过构造器传递。
- 配置项使用 `application.yml`、`@ConfigurationProperties` 或 `@Value`；API Key 等敏感信息只能来自环境变量，不能写入源码或提交到 Git。

## 注释与命名

- 类、方法和变量使用含义明确的英文名称，避免无意义缩写。
- 所有 Java 方法和构造函数都必须编写 Javadoc，并介绍用途、全部入参和出参。
- 每个入参都必须有对应的 `@param`；有返回值的函数必须有 `@return`，无返回值时应在用途描述中说明。
- 构造函数的 Javadoc 同样要介绍全部入参，并说明它创建的对象。
- 注释重点解释设计意图、业务约束和“为什么”，不要逐行复述代码行为。
- 简单且自解释的代码不添加冗余注释；代码变更后必须同步更新失效注释。
- 注释默认使用中文；框架名、协议名、事件名和无法准确翻译的术语保留英文。
- 变量或字段需要注释时，必须使用 `/* ... */` 多行块注释；禁止使用 `//` 单行注释或行尾注释说明变量。
- 待办事项使用 `TODO(owner): 原因或后续动作` 格式，并尽可能附关联 Issue。

## API 与错误处理

- REST 路径统一使用小写名词和短横线；流式响应使用 `text/event-stream`。
- SSE 事件名保持稳定，新增或修改事件时同步更新前端消费逻辑与 README。
- 请求参数必须在系统边界校验；不要把堆栈、密钥或内部实现细节返回给客户端。
- 不吞掉异常。面向客户端返回安全错误信息，同时在服务端保留可诊断日志。

## 前端

- 页面保持无构建工具即可运行，避免无必要的第三方 CDN 依赖。
- 使用语义化 HTML，确保键盘操作、焦点状态和移动端布局可用。
- 用户或模型生成的内容必须通过 `textContent` 或等价安全方式渲染，禁止直接拼接到 `innerHTML`。

## 测试与交付

- 新增行为或修复缺陷时补充对应测试。
- 提交前至少运行 `mvn clean verify` 和 `git diff --check`。
- Web 页面变更需检查桌面端和移动端；接口变更需验证正常请求及参数错误场景。
- 不提交 `target/`、AgentScope 运行状态、本地缓存、IDE 文件或任何密钥。
