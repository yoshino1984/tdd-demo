# TDD 实战-- RESTful 开发框架




### Spike
> POC 验证架构愿景，快速测试寻找代码结构 
- Jetty 
- ResourceServlet
- Application
- MessageBodyWriter 
- Providers
- di container
  - application scope
  - request scope

### 分解任务
#### 功能点关联到功能上下文(架构愿景中的组件)
- 将请求派分给对应的资源（Resource），并根据返回的状态、超媒体类型、内容，响应 Http 请求
  - ResourceServlet
  - ResourceRouter 
- 在处理请求派分时，可以支持多级子资源（Sub-Resource）
  - ResourceRouter
- 在处理请求派分时，可以根据客户端提供的超媒体类型，选择对应的资源方法（Resource Method）
  - ResourceRouter
- 在处理请求派分时，可以根据客户端提供的 Http 方法，选择对应的资源方法
  - ResourceRouter
- 资源方法可以返回 Java 对象，由 Runtime 自行推断正确的返回状态
  - ResourceRouter
- 资源方法可以不明确指定返回的超媒体类型，由 Runtime 自行推断，比如，资源方法标注了 Produces，那么就使用标注提供的超媒体类型等
  - ResourceRouter
- 可通过扩展点 MessageBodyWriter 处理不同类型的返回内容
  - Providers
- 当资源方法抛出异常时，根据异常影响 Http 请求
  - ResourceServlet
- 可通过扩展点 ExceptionMapper 处理不同类型的异常
  - Providers
- 资源方法可按找期望的类型，访问 Http 请求的内容
  - ResourceRouter
- 可通过扩展点 MessageBodyReader 处理不同类型的请求内容
  - Providers
- 资源对象和资源方法可接受环境组件的注入
  - ResourceContext
  - ResourceRouter
#### 按照技术组件重组功能列表
- ResourceServlet
  - 将请求派分给对应的资源（Resource），并根据返回的状态、超媒体类型、内容，响应 Http 请求
  - 当资源方法抛出异常时，根据异常影响 Http 请求
- ResourceRouter
  - 将请求派分给对应的资源（Resource），并根据返回的状态、超媒体类型、内容，响应 Http 请求
  - 在处理请求派分时，可以支持多级子资源（Sub-Resource）
  - 在处理请求派分时，可以根据客户端提供的超媒体类型，选择对应的资源方法（Resource Method）
  - 在处理请求派分时，可以根据客户端提供的 Http 方法，选择对应的资源方法
  - 资源方法可以返回 Java 对象，由 Runtime 自行推断正确的返回状态
  - 资源方法可以不明确指定返回的超媒体类型，由 Runtime 自行推断，比如，资源方法标注了 Produces，那么就使用标注提供的超媒体类型等
- Providers
  - 可通过扩展点 MessageBodyReader 处理不同类型的请求内容
  - 可通过扩展点 MessageBodyWriter 处理不同类型的返回内容
  - 可通过扩展点 ExceptionMapper 处理不同类型的异常
- ResourceContext
  - 资源对象和资源方法可接受环境组件的注入
#### 细化任务列表
- ResourceServlet
  - 将请求派分给对应的资源（Resource），并根据返回的状态、超媒体类型、内容，响应 Http 请求使用 OutboundResponse 的 status 作为 Http Response 的状态；
  - 使用 OutboundResponse 的 headers 作为 Http Response 的 Http Headers；
  - 通过 MessageBodyWriter 将 OutboundResponse 的 GenericEntity 写回为 Body；
  - 如果找不到对应的 MessageBodyWriter，则返回 500 族错误
  - 当资源方法抛出异常时，根据异常影响 Http 
  - 请求如果抛出 WebApplicationException，且 response 不为 null，则使用 response 响应 Http
  - 如果抛出 WebApplicationException，而 response 为 null，则通过异常的具体类型查找 ExceptionMapper，生产 response 响应 Http 请求
  - 如果抛出的不是 WebApplicationException，则通过异常的具体类型查找 ExceptionMapper，生产 response 响应 Http 请求



---
一些注意点
- 首先可以做一些 happy path，最好是冲突比较大的，这样有助于促进重构
- 然后在恰当的时机引入 sad path，作为负向刺激，也有助于重构，尽量早地形成代码结构
  - 如果一味只做 happy path，你可能会因为代码写得太漂亮，后续很难做破坏性重构
