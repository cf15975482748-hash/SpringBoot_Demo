# SpringBoot 数据库管理系统 (GUI)

本项目是一个基于 SpringBoot + MyBatis + Thymeleaf 构建的轻量级 Web 数据库管理系统。它支持动态表格管理、严格的数据校验以及基于 Bootstrap 的现代化 GUI 交互。

## 🚀 核心功能

### 1. 动态表格管理
- **选择表格**：通过下拉框实时切换当前操作的数据库表。
- **新建表格**：支持弹出式窗口新建表，创建时自动预生成 **ID 1-60** 的初始行，姓名和年龄默认为空。
- **删除表格**：支持下拉选择并带有**二次确认模态框**，防止误删操作。
- **重复校验**：新建表时会自动检测表名是否已存在，若重名则通过页面下方淡出的红色漂浮窗提示。

### 2. 数据操作 (CRUD)
- **添加/更新数据**：通过弹出式窗口录入数据。由于 ID 已预生成，添加数据本质上是更新对应 ID 行的内容。
- **数据清空**：列表中的“清空”按钮带有二次确认逻辑，点击后将对应行的姓名和年龄置空，保留 ID。
- **模糊查询**：支持在当前表中按姓名关键词进行模糊搜索（Like 匹配）。
- **实时刷新**：支持一键刷新数据视图。

### 3. 严格的数据校验逻辑
- **ID 范围**：仅允许操作 ID 在 **1-60** 之间的行。
- **年龄限制**：年龄必须在 **1-30** 岁之间。
- **非法字符检测**：姓名禁止包含 `！！@#￥%……&*——+` 等特殊符号。
- **错误反馈**：所有校验失败的信息均通过页面下方淡出的**红色漂浮窗 (Toast)** 提示。

---

## 🛠️ 技术栈
- **后端**：Spring Boot 2.7.18, MyBatis
- **数据库**：MySQL (Druid 连接池)
- **前端**：Thymeleaf, Bootstrap 5.3, Bootstrap Icons
- **校验**：正则表达式 + 后端逻辑校验

---

## 📂 核心代码说明

### 1. 控制层 [TestController.java](src/main/java/com/SpringBoot_Test/Controller/TestController.java)
负责路由分发、业务逻辑处理及数据校验。集成了模态框状态控制（如校验失败时保持模态框开启）。

### 2. 持久层 (Mapper)
- [SearchMapper.java](src/main/java/com/SpringBoot_Test/Mapper/SearchMapper.java)：负责动态表名查询、表结构管理（创建/删除/查询表名）以及初始 ID 生成。
- [Mappers.java](src/main/java/com/SpringBoot_Test/Mapper/Mappers.java)：负责数据增删改及模糊查询。

### 3. 模型层 [User.java](src/main/java/com/SpringBoot_Test/Model/User.java)
基础实体类，包含 `id`, `name`, `age` 字段。

### 4. 前端界面 [index.html](src/main/resources/templates/index.html)
单页面 GUI 交互：
- 采用 Bootstrap 5 响应式布局。
- 使用 Thymeleaf 内联 JavaScript 处理后端传回的错误和模态框状态。
- 集成了 `event.preventDefault()` 机制确保删除操作必须经过模态框确认。

---

## 📝 运行说明
1. 确保本地 MySQL 环境已启动。
2. 在 `application.yml` 中配置正确的数据库连接信息。
3. 运行 `SpringBootIn.java` 中的 `main` 方法启动项目。
4. 访问 `http://localhost:8080` 即可进入管理系统。


#注意！本项目还在开发中，暂时使用AI的MD文档，后续会完善。