# 🚀 Monitor System | 分布式服务器运维监控平台

<p align="center">
  <img src="https://img.shields.io/badge/Java-JDK17-b07219?style=flat-square&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/SpringBoot-3.3.x-6db33f?style=flat-square&logo=springboot" alt="SpringBoot">
  <img src="https://img.shields.io/badge/Vue.js-3.x-4fc08d?style=flat-square&logo=vue.js" alt="Vue">
  <img src="https://img.shields.io/badge/Security-RBAC-red?style=flat-square&logo=guard" alt="Security">
  <img src="https://img.shields.io/badge/InfluxDB-2.7-22adf6?style=flat-square&logo=influxdb" alt="InfluxDB">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License">
</p>

> **Monitor System** 是一个基于 **Spring Boot 3** 和 **Vue 3** 的全栈分布式运维平台。它不仅具备 **OSHI** 硬件采集与 **InfluxDB** 时序存储能力，还集成了商业级的 **RBAC 权限管理体系**、**WebSSH 远程终端**以及现代化的 **扁平化/暗黑模式** UI 设计，旨在为开发者提供一个开箱即用的轻量级运维解决方案。

---

## 🖼️ 界面预览 (Preview)

| 📊 实时监控仪表盘 (Dark Mode) | 💻 Web SSH 远程终端 |
| :---: | :---: |
| <img width="2558" height="1236" alt="image" src="https://github.com/user-attachments/assets/00f2681e-83ac-4cd7-a86b-a61e29c66ecd" /> | <img width="2555" height="1233" alt="image" src="https://github.com/user-attachments/assets/ad34ad8a-f4a6-4d83-b59a-bf6f0e3777b9" />


| 🛡️ 登录与注册 | 👤 个人中心 & 权限管理 |
| :---: | :---: |
| <img width="2551" height="1239" alt="image" src="https://github.com/user-attachments/assets/61f2c3c3-9d59-4edf-85e9-3cb93f6434d8" /> | <img width="2559" height="1238" alt="image" src="https://github.com/user-attachments/assets/31ca3319-03aa-4105-87c5-604557bcc1c8" /> |

---

## ✨ 核心特性 (Features)

### 1. 🛡️ 企业级安全鉴权
*   **JWT 无状态认证**：基于 Spring Security 6 + JWT 实现前后端分离的安全认证。
*   **RBAC 权限控制**：细粒度的权限管理（用户 -> 角色 -> 菜单/按钮），支持动态指令 `v-permission` 控制按钮显隐。
*   **账号生命周期**：支持用户自助注册、密码修改（分步验证）、账号注销（逻辑删除）等完整流程。

### 2. 🖥️ 深度硬件监控
*   **多维度采集**：基于 **OSHI (v6.x)** 深入底层，精准采集 CPU、内存、JVM 堆内存等核心指标。
*   **分布式探针**：轻量级 Client 端设计，支持多台服务器同时上报，毫秒级数据刷新。

### 3. 🔌 增强型 Web SSH
*   **浏览器即终端**：集成 **Xterm.js** + **xterm-addon-fit**，提供接近原生 Shell 的操作体验。
*   **全双工通信**：后端使用 **JSch** 建立 SSH 连接，通过 WebSocket 实现标准输入输出流的实时透传。
*   **连接保持**：支持页面切换后的 **Keep-Alive** 连接保持，防止誤触导致连接断开。

### 4. 🎨 现代化 UI/UX
*   **扁平化设计 (Flat Design)**：去繁就简，采用高对比度、纯色块的视觉风格。
*   **暗黑模式 (Dark Mode)**：全站支持一键切换深色主题，自动适配图表与终端配色。
*   **云端存储**：集成 **阿里云 OSS**，实现用户头像等静态资源的云端托管。

### 5. 💾 高性能时序存储
*   **InfluxDB 2.x 驱动**：利用时序数据库处理高并发监控数据写入与降采样查询 (Flux)。

---

## 🛠 技术架构 (Tech Stack)

### 后端 (Backend)
| 技术 | 版本 | 说明 |
| :--- | :--- | :--- |
| **Spring Boot** | `3.3.5` | 核心框架 (Jakarta EE) |
| **Spring Security** | `6.x` | 安全鉴权与访问控制 |
| **MyBatis-Plus** | `3.5.x` | ORM 框架 (MySQL) |
| **InfluxDB Client** | `6.x` | 时序数据库 SDK |
| **Aliyun OSS** | `3.17.x` | 对象存储服务 |
| **JSch** | `0.1.55` | SSH2 协议实现 |
| **OSHI** | `6.4.x` | 硬件信息采集 |

### 前端 (Frontend)
| 技术 | 说明 |
| :--- | :--- |
| **Vue 3** | Composition API |
| **Vite** | 极速构建工具 |
| **Pinia** | 状态管理 (User, Theme) |
| **Element Plus** | UI 组件库 (支持 Dark Mode) |
| **ECharts 5** | 数据可视化 |
| **Xterm.js** | Web 终端组件 |

---

## 📂 项目结构 (Structure)

```text
GraduationProject-ServerMonitor (Root)
├── 📂 monitor-project              # [后端] Maven 父工程
│   ├── 📂 monitor-common           # [公共模块] Pojo, Utils, Result封装
│   ├── 📂 monitor-client           # [探针端] 运行在目标服务器，采集上报
│   ├── 📂 monitor-server           # [服务端] 核心业务
│   │   ├── config                  # Security, OSS, WebConfig
│   │   ├── controller              # API 接口 (Auth, User, Server, Report)
│   │   ├── entity                  # 实体类 (SysUser, SysRole...)
│   │   ├── filter                  # JWT 过滤器
│   │   ├── handler                 # WebSocket 处理器 (WebSSH)
│   │   ├── service                 # 业务逻辑 (SshService, UserService...)
│   │   └── utils                   # JwtUtils, AliyunOssUtil
│   └── 📄 pom.xml
│
├── 📂 monitor-web                  # [前端] Vue 3 + Vite 工程
│   ├── 📂 src
│   │   ├── 📂 api                  # Axios 接口封装
│   │   ├── 📂 directive            # 自定义指令 (v-permission)
│   │   ├── 📂 layout               # 布局组件 (MainLayout)
│   │   ├── 📂 stores               # Pinia (user.js, theme.js)
│   │   ├── 📂 views                # 页面 (Dashboard, Login, Profile, SSH)
│   │   └── 📂 utils                # 工具类 (request.js 拦截器)
│   └── 📄 vite.config.js
│
└── 📂 sql                          # [数据库脚本]
    ├── 01_init_structure.sql       # 初始化表结构
    ├── 02_add_user_profile.sql     # 用户画像扩展
    └── 03_rbac_upgrade.sql         # RBAC 权限体系升级
```

---

## 🚀 快速开始 (Getting Started)

### 1. 环境准备
*   **JDK**: 17+
*   **MySQL**: 8.0+
*   **InfluxDB**: 2.7.x
*   **Node.js**: 16+

### 2. 数据库初始化
请在 MySQL 中创建数据库 `monitor_db`，并**依次执行** `sql/` 目录下的脚本：
1.  `01_init_structure.sql` (基础表)
2.  `02_add_user_profile.sql` (用户扩展)
3.  `03_rbac_upgrade.sql` (权限体系)

### 3. 后端配置
修改 `monitor-server/src/main/resources/application.yml`：

<details>
<summary>📄 点击查看关键配置示例</summary>

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/monitor_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_mysql_password

# InfluxDB 配置
influx:
  url: http://localhost:8086
  token: YOUR_INFLUX_TOKEN
  bucket: monitor_bucket
  org: my_org

# 阿里云 OSS 配置 (用于头像上传)
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.coe
    access-key-id: ${ALIYUN_ACCESS_KEY}      # 建议使用环境变量，不要硬编码
    access-key-secret: ${ALIYUN_SECRET_KEY}
    bucket-name: your-bucket-name
    url-prefix: https://your-bucket.oss-cn-hangzhou.aliyuncs.com/
```
</details>

### 4. 启动服务
1.  **Server**: 运行 `MonitorServerApplication`。
2.  **Client**: 修改 `monitor-client` 中的 `server-url` 为 `http://localhost:8080/api/monitor/report`，然后启动。
3.  **Web**:
    ```bash
    cd monitor-web
    npm install
    npm run dev
    ```

访问 `http://localhost:5173`，默认管理员账号：`admin` / `123456`。

---

## 🔮 路线图 (Roadmap)

- [x] **基础监控**: CPU、内存数据采集与时序存储
- [x] **Web SSH**: 基于 WebSocket 的远程终端 (支持 Keep-Alive)
- [x] **安全鉴权**: Spring Security + JWT + RBAC 动态权限
- [x] **用户中心**: 头像云存储 (OSS)、密码修改、账号注销
- [x] **UI 设计**: 扁平化设计 + 暗黑模式 (Dark Mode)
- [ ] **告警中心**: 自定义阈值（如 CPU > 90%），支持邮件/钉钉/飞书通知
- [ ] **文件管理**: 类似 SFTP 的远程文件上传/下载功能
- [ ] **Docker 部署**: 提供 docker-compose 一键拉起所有服务

---

## 🤝 贡献 (Contribution)

欢迎提交 Issue 和 Pull Request！

## 📄 开源协议 (License)

[MIT License](LICENSE) © 2024 ouyangxu66@github.com