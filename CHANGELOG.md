# Change Log

## [1.0.6] - 2025-04-10

### Changed
- 修复了Redission无法自动加载的问题
- 重新设置了默认的key生成
- 优化了一些代码的性能
- 修复了condition为false时，不执行目标方法的问题


## [1.0.5] - 2024-12-30

### Changed
- 优化了一些代码
- 修改了项目的groupId坐标为`io.github.tri5m`

## [1.0.4.RELEASE] - 2024-02-18

### Changed
- 升级Springboot到2.7.15
- 兼容SpringBoot3的自动配置
- 统一管理了项目版本
- 添加了本地缓存TuTreeCache，在非redis环境下也可以使用tucache

### Fixed
- 修复了redis返序列化Long自动转为Integer的问题


## [1.0.3.RELEASE] - 2023-05-30

### Changed
- 添加了枚举类型的方法缓存
### Fixed
- 修复了返序列化的一些bug
- 修复了slf4j版本冲突，规范了文档

## [1.0.2.RELEASE] - 2022-07-01

### Changed
- 新增了多线程清理的特新
- 修复了一些问题

## [1.0.1.RELEASE] - 2022-01-13

### Changed
- 修复了框架设计不够严谨的问题
- 修复了清理缓存注解异步参数语义错误问题


## [1.0.1] - 2021-04-15

### Fixed
- 修复了spring自动配置中过于复杂的问题，导致各种各样的问题
- 修复了类名错误问题


## [1.0.0] - 2021-04-09

### Changed
- 创建项目，编写了核心功能tutu-cache-core
- 完成了tutu-cache的spring-boot-starter模块