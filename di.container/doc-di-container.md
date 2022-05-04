# TDD 实战-- RESTful 开发框架：依赖注入容器




## DI container 功能点分解

- 组件的构造
  - 扫描指定目录的所有类，识别出所有带有注解@Inject的类
    - sad path：不同包名下的同名class，通过加上包名区分
  - 解析类的注解元素 @Inject，进行归类，用于后续各个阶段的注入
    - 构造器
      - sad path：多个@Inject 注解的构造器，抛出异常
    - 字段
    - 方法
  - 实例的名称
    - 指定的方式
      - sad path：指定多个同名实例，抛出异常
    - default value
      - 默认自动生成的方式：默认类名（首字母小写）的方式
      - 如果有多个 class#1  class#2 这样的方式累加
  - injected constructor
    - 实例化
      - 查找已存在的依赖
      - 通过构造器实例化并注入
    - sad path：循环依赖
  - injected field
    - 实例化
    - 属性设置
      - 依赖查找对应的实例
      - 通过反射实现字段注入
    - sad path：循环依赖
  - method field
    - 实例化
    - 属性设置
      - 依赖查找对应的实例
      - 通过反射调用方法注入
- dependency selection
  - cycle dependency
    - proxy 实现解决循环依赖问题
    - provider(factory) 延迟加载的循环依赖问题
  - by tag
    - by name
    - by annotation
  - sad path：
    - 找不到对应的实例
      - by name
      - by annotation type
    - 找到符合条件的多个实例
      - 通过 @Primary 来进行优先级划分返回
      - sad path：没有 @Primary，抛出异常
- 生命周期控制
  - @Scope
    - singleton 单例实现，多次获取Class的实例返回一个实例
    - prototype 和默认情况相同
    - default value： prototype

---
专栏中分解的任务

- 无需构造的组件 -- 组件实例
- 如果注册的组件不可实例化，则抛出异常
  - 抽象类
  - 接口
- 构造函数注入
  - 无依赖的组件应该通过默认构造函数生成组件实例
  - 有依赖的组件，通过 Inject 标注的构造函数生成组件实例
  - 如果所依赖的组件也存在依赖，那么需要对所依赖的组件也完成依赖注入
  - 如果组件有多于一个 Inject 标注的构造函数，则抛出异常
  - 如果组件需要的依赖不存在，则抛出异常
  - 如果组件间存在循环依赖，则抛出异常
- 字段注入
  - 通过 Inject 标注将字段声明为依赖组件
  - 如果组件需要的依赖不存在，则抛出异常
  - 如果字段为final则抛出异常
  - 如果组件间存在循环依赖，则抛出异常
- 方法注入
  - 通过 Inject 标注的方法，其参数为依赖组件
  - 通过 Inject 标注的无参数方法，会被调用
  - 按照子类中的规则，覆盖父类中的 inject 方法
  - 如果组件需要的依赖不存在，则抛出异常
  - 如果方法定义类型参数(type parameter)，则抛出异常
  - 如果组件间存在循环依赖，则抛出异常
- 对 Provider 类型的依赖
  - 注入构造函数中可以声明对于 Provider 的依赖
  - 注入字段中可以声明对于 Provider 的依赖
  - 注入方法中可以声明对于 Provider 的依赖
- 自定义 Qualifier 的依赖
  - 注册组件时，可额外指定是否为 Singleton
  - 注册组件时，可从类对象上提取 Singleton 标注
  - 对于包含 Singleton 标注的组件，在容器范围内提供唯一实例
  - 容器组件默认标识 Singleton 生命周期
- 自定义 Scope 标注
  - 可向容器注册自定义 Scope 标注的回调



---
一些注意点
- 首先可以做一些 happy path，最好是冲突比较大的，这样有助于促进重构
- 然后在恰当的时机引入 sad path，作为负向刺激，也有助于重构，尽量早地形成代码结构
  - 如果一味只做 happy path，你可能会因为代码写得太漂亮，后续很难做破坏性重构
