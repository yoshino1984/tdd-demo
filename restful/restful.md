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





---
一些注意点
- 首先可以做一些 happy path，最好是冲突比较大的，这样有助于促进重构
- 然后在恰当的时机引入 sad path，作为负向刺激，也有助于重构，尽量早地形成代码结构
  - 如果一味只做 happy path，你可能会因为代码写得太漂亮，后续很难做破坏性重构
