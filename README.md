# SpringBoot + MyBatis 动态数据管理系统：新手入门指南

这份文档旨在帮助初学者理解本项目的设计思路、架构组成以及核心技术实现。本项目是一个典型的 **SpringBoot 2.x + MyBatis + Thymeleaf** 单体 Web 应用，涵盖了权限校验、动态表管理、CRUD 操作等核心业务场景。

---

## 1. 技术栈概览

| 组件 | 技术 | 作用 |
| :--- | :--- | :--- |
| **核心框架** | Spring Boot 2.7.18 | 提供容器管理、自动配置、内嵌服务器。 |
| **持久层** | MyBatis 2.3.1 (注解版) | 负责 Java 对象与 MySQL 数据库的交互，使用 SQL 直接操作。 |
| **数据库** | MySQL 8.0 | 存储账号信息、系统数据及动态业务表。 |
| **模板引擎** | Thymeleaf | 在服务端将数据渲染进 HTML 页面，实现前后端不分离的单页交互。 |
| **连接池** | Alibaba Druid | 高性能数据库连接池，支持监控。 |
| **UI 框架** | Bootstrap 5 | 提供现代化的 GUI 样式、弹窗 (Modal) 和响应式布局。 |

---

## 2. 项目目录结构说明

```text
src/main/java/com/SpringBoot_Test/
├── Config/           # 配置类 (如 WebMvc 拦截器注册)
├── Controller/       # 控制器层 (处理 HTTP 请求，调度业务逻辑)
├── Interceptor/      # 拦截器 (如登录状态校验)
├── Mapper/           # 持久层接口 (MyBatis SQL 定义)
├── Model/            # 实体类 (POJO，对应数据库表结构)
├── Util/             # 工具类 (权限校验、Session 获取)
└── SpringBootIn.java # 项目启动入口类

src/main/resources/
├── templates/        # HTML 页面模板
├── application.yml   # 核心配置文件 (端口、数据库连接、MyBatis 配置)
└── static/           # 静态资源 (CSS/JS/图片，本项目多使用 CDN)
```

---

## 3. 核心业务逻辑解析

### 3.1 角色与权限体系 (RBAC)
项目内置了三类角色，其权限边界如下：
- **Admin (超级管理员)**：拥有最高权限。可管理所有账号（增删改密码）、查看并操作所有数据库表（系统表+业务表）。
- **Teacher (教师)**：业务操作者。可创建、删除、修改任何业务动态表。具备查看所有业务表的权限，并能修改自己的登录密码。
- **Student (学生)**：受限观察者。仅能查看（只读）所有业务表的数据，无法执行任何增删改操作。

### 3.2 动态表管理
这是本项目最具特色的功能。系统允许用户在网页端直接输入表名，后端会执行：
1. **DDL 建表**：使用 `CREATE TABLE` 动态创建包含 `id`, `name`, `score`, `create_user` 字段的物理表。
2. **初始化数据**：建表成功后自动预生成 ID 为 1-60 的初始行，方便用户直接在现有行上修改数据。
3. **数据隔离**：通过 `isBusinessTable` 逻辑将系统账号表（admin/teacher/student）与普通业务表隔离，防止误删系统核心数据。

---

## 4. 关键代码解析 (面向初学者)

### 4.1 控制器层：[TestController.java](src/main/java/com/SpringBoot_Test/Controller/TestController.java)
它是整个系统的“大脑”。
- **统一数据更新**：`handleDataUpdate` 方法是一个很好的重构范例。它集成了权限校验、ID 存在性检查、数据校验，并根据操作类型（新增/修改/删除）调用不同的 Mapper 方法，避免了代码冗余。
- **数据适配映射**：`mapToUser` 方法将不同的账号实体（Admin/Teacher/Student）统一转换为 `User` 视图实体，确保前端 `index.html` 能够用同一套逻辑渲染不同的表。

### 4.2 持久层：[SearchMapper.java](src/main/java/com/SpringBoot_Test/Mapper/SearchMapper.java)
展示了 MyBatis 注解的高级用法。
- **动态 SQL**：使用 `${tableName}` 实现动态表名切换。注意：这里使用了反引号 `` `${tableName}` `` 来保护表名，防止与数据库关键字冲突。
- **脚本插入**：`insertInitialIds` 使用了 `<script>` 和 `<foreach>` 标签，演示了如何在注解中编写复杂的动态批量插入逻辑。

### 4.3 拦截器：[LoginInterceptor.java](src/main/java/com/SpringBoot_Test/Interceptor/LoginInterceptor.java)
- **安全第一**：在请求到达 Controller 之前拦截，校验 Session 中是否存在登录用户信息。未登录者会被强制重定向至 `/login`。

### 4.4 前端交互：[index.html](src/main/resources/templates/index.html)
- **Thymeleaf 逻辑判断**：利用 `th:if` 和 `th:text` 根据当前登录的角色动态隐藏或显示按钮。
- **Modal 交互**：通过 `data-bs-*` 属性和简单的 JavaScript，实现了不跳转页面即可弹出表单进行数据录入的流畅体验。

---

## 5. 如何运行与测试

### 环境要求
- JDK 8
- Maven 3.6+
- MySQL 8.0 (需先创建名为 `testdb` 的数据库)

### 运行步骤
1. 修改 `src/main/resources/application.yml` 中的数据库账号密码。
2. 运行 `SpringBootIn.java` 中的 `main` 方法。
3. 访问浏览器：`http://localhost:8080`。

### 测试用例
1. **登录**：使用默认账号 `admin / 123456` 登录。
2. **账号管理**：进入“账号管理”，创建一个老师账号。
3. **动态建表**：以老师身份登录，点击“新建表格”，输入 `test_table`。
4. **数据操作**：在 `test_table` 中修改 ID 为 1 的行，将分数设为 `98.5`。
5. **搜索测试**：在顶部搜索框输入关键字，验证模糊查询是否生效。

---

## 6. 给初级工程师的建议
- **关注安全性**：理解本项目中如何通过拦截器和后端双重校验（Controller 逻辑）来防止非法操作。
- **学习解耦**：观察 `AuthUtil` 是如何将权限判断逻辑从 Controller 中抽离出来的。
- **SQL 意识**：注意动态 SQL 中的 `${}` 与 `#{}` 的区别，以及在动态表名场景下为何必须使用 `${}`。

希望这份文档能帮助你快速上手本项目！如有疑问，请随时查阅源码中的详细注释。
