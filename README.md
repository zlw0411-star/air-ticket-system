# 航空订票管理系统

前后端分离的航空订票课程项目。支持乘客注册登录、航班查询、购票、模拟支付、订单查看、改签和退票；管理员可管理用户、航班与订单。

## 技术栈

- 前端：Vue 3 + Vite + Element Plus + Vue Router + Pinia + Axios
- 后端：Java 17 + Spring Boot 3.2.5 + Spring Security + JWT
- 数据访问：MyBatis-Plus
- 数据库：MySQL 8 + Flyway
- 接口文档：Swagger / Springdoc OpenAPI

## 目录结构

```
air-ticket-system
├── backend      Spring Boot 后端
├── frontend     Vue 3 前端
└── README.md
```

## 环境要求

- JDK 17（本仓库默认路径：`E:\JDK17\jdk-17.0.20.1+1`）
- Maven 3.9+（默认路径：`E:\Maven\apache-maven-3.9.9`）
- MySQL 8（默认路径：`E:\MySQL\mysql-8.0.39-winx64`）
- Node.js 20+

## 启动步骤

### 1. 启动 MySQL

确保 MySQL 已在 `3306` 端口运行。首次启动后端时，Flyway 会自动创建 `airline_ticket` 库并导入演示数据。

可通过环境变量覆盖数据库配置：

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=airline_ticket
DB_USER=root
DB_PASSWORD=
```

### 2. 启动后端

```powershell
.\start-backend.ps1
```

或手动指定 JDK 后启动：

```powershell
$env:JAVA_HOME = "E:\JDK17\jdk-17.0.20.1+1"
cd backend
& "E:\Maven\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
```

后端地址：http://localhost:8080

Swagger 文档：http://localhost:8080/swagger-ui.html

### 3. 启动前端

```powershell
.\start-frontend.ps1
```

或手动启动：

```powershell
cd frontend
npm install
npm run dev
```

前端地址：http://localhost:5173

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | Admin123 |
| 乘客 | user | User123 |

新注册账号默认为乘客角色。

## 核心功能

### 乘客端

- 注册 / 登录 / 修改资料
- 按出发地、目的地、日期查询航班
- 选择舱位并填写乘机人信息
- 创建订单并模拟支付
- 查看订单与机票
- 改签、退票
- 待支付订单超时自动取消并释放余票

### 管理员端

- 用户查询、启用 / 禁用
- 航班新增、修改、停飞
- 订单查询
- 销售统计（订单数、营收、热门航线）

## 业务规则

- 下单时立即锁定余票，支付超时 15 分钟自动释放
- 购票使用条件更新，避免并发超卖
- 起飞前 24 小时以上退票全额退款；2 至 24 小时退 80%；不足 2 小时退 50%
- 起飞前 2 小时以上允许改签，系统自动计算差价并模拟补差或退差
- 已退票、已取消订单不可重复操作

## API 前缀

- `/api/auth` 注册登录
- `/api/users` 当前用户
- `/api/flights` 航班查询
- `/api/orders` 订单、支付、取消
- `/api/tickets` 改签、退票
- `/api/admin` 管理端接口
