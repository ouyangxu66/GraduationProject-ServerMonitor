# 🏗️ GraduationProject-ServerMonitor 架构分析文档

> **文档版本**: 1.0  
> **分析日期**: 2025-12-18  
> **项目类型**: 分布式服务器运维监控平台

---

## 📋 目录

1. [系统概述](#系统概述)
2. [整体架构](#整体架构)
3. [技术栈分析](#技术栈分析)
4. [后端架构详解](#后端架构详解)
5. [前端架构详解](#前端架构详解)
6. [数据流与交互模式](#数据流与交互模式)
7. [核心设计模式](#核心设计模式)
8. [部署架构](#部署架构)
9. [架构优势与特点](#架构优势与特点)
10. [潜在改进方向](#潜在改进方向)

---

## 1. 系统概述

### 1.1 项目定位

**Monitor System** 是一个基于现代化全栈技术构建的**分布式服务器运维监控平台**，提供以下核心能力：

- 🔍 **实时监控**: 基于 OSHI 的深度硬件信息采集
- 📊 **时序存储**: InfluxDB 高性能时序数据库
- 🛡️ **安全鉴权**: Spring Security 6 + JWT + RBAC 权限体系
- 💻 **远程运维**: WebSSH 远程终端功能
- 🎨 **现代化UI**: Vue 3 + Element Plus + 暗黑模式支持

### 1.2 架构风格

- **架构模式**: 前后端分离 (SPA + RESTful API)
- **部署模式**: 分布式 C/S 架构 (Client-Server)
- **数据模式**: 时序数据 + 关系型数据混合存储
- **通信模式**: HTTP/HTTPS + WebSocket
- **认证模式**: 无状态 JWT + 双 Token 机制

---

## 2. 整体架构

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户浏览器 (Browser)                        │
│                  Vue 3 SPA + Xterm.js Terminal                   │
└────────────────┬────────────────────────────────────────────────┘
                 │ HTTPS/WSS
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Monitor Server (核心服务端)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Spring MVC   │  │ Spring       │  │ WebSocket    │          │
│  │ Controllers  │  │ Security     │  │ Handler      │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                  │
│  ┌──────▼──────────────────▼──────────────────▼───────┐         │
│  │            Service Layer (业务逻辑层)               │         │
│  │  AuthService │ MonitorService │ SshService │ ...   │         │
│  └──────┬──────────────┬──────────────┬───────────────┘         │
│         │              │              │                          │
│  ┌──────▼──────┐  ┌───▼────┐  ┌──────▼──────┐                 │
│  │ MyBatis-Plus│  │InfluxDB│  │    JSch     │                 │
│  │   Mapper    │  │ Client │  │   (SSH)     │                 │
│  └──────┬──────┘  └───┬────┘  └──────┬──────┘                 │
└─────────┼─────────────┼───────────────┼─────────────────────────┘
          │             │               │
          ▼             ▼               ▼
    ┌─────────┐   ┌──────────┐   ┌──────────┐
    │  MySQL  │   │ InfluxDB │   │  Target  │
    │ (RBAC+  │   │  (时序   │   │  Linux   │
    │  User)  │   │   数据)  │   │  Server  │
    └─────────┘   └──────────┘   └──────────┘
                                        ▲
                                        │ HTTP POST
                                        │ (每秒上报)
                          ┌─────────────┴──────────────┐
                          │   Monitor Client (探针)    │
                          │  OSHI + 定时任务 + RestAPI  │
                          └────────────────────────────┘
```

### 2.2 三层架构模型

| 层次 | 职责 | 核心技术 |
|------|------|----------|
| **表现层 (Presentation)** | 用户交互、数据展示、权限控制 | Vue 3, Element Plus, ECharts, Xterm.js |
| **业务层 (Business)** | 业务逻辑、安全鉴权、数据处理 | Spring Boot, Spring Security, JWT |
| **数据层 (Data)** | 数据持久化、时序存储、硬件采集 | MySQL, InfluxDB, OSHI |

---

## 3. 技术栈分析

### 3.1 后端技术栈

| 技术 | 版本 | 用途 | 选型理由 |
|------|------|------|----------|
| **Spring Boot** | 3.3.5 | 核心框架 | Jakarta EE 标准，生态成熟 |
| **Spring Security** | 6.x | 安全框架 | 企业级安全标准，支持 RBAC |
| **MyBatis-Plus** | 3.5.x | ORM 框架 | 增强 MyBatis，简化 CRUD |
| **InfluxDB Client** | 6.x | 时序数据库 SDK | 专为高频监控数据设计 |
| **OSHI** | 6.9.1 | 硬件信息采集 | 跨平台底层硬件接口 |
| **JSch** | 0.1.55 | SSH2 协议 | 纯 Java SSH 实现 |
| **Aliyun OSS** | 3.17.x | 对象存储 | 云端头像/文件存储 |
| **JWT (jjwt)** | - | Token 生成 | 无状态认证标准 |

### 3.2 前端技术栈

| 技术 | 版本 | 用途 | 选型理由 |
|------|------|------|----------|
| **Vue.js** | 3.5.25 | 前端框架 | Composition API，性能优越 |
| **Vite** | 7.2.4 | 构建工具 | 极速热更新，ESM 原生支持 |
| **Pinia** | 3.0.4 | 状态管理 | Vue 3 官方推荐 |
| **Element Plus** | 2.11.8 | UI 组件库 | 企业级组件，支持暗黑模式 |
| **ECharts** | 6.0.0 | 数据可视化 | 功能强大，图表丰富 |
| **Xterm.js** | 5.3.0 | 终端模拟器 | WebSSH 核心组件 |
| **Axios** | 1.13.2 | HTTP 客户端 | 拦截器机制，支持请求/响应处理 |
| **Vue Router** | 4.6.3 | 路由管理 | SPA 页面导航 |

### 3.3 数据库选型

| 数据库 | 类型 | 用途 | 特点 |
|--------|------|------|------|
| **MySQL 8.0+** | 关系型 | 用户数据、权限管理、服务器信息 | ACID 保证，适合结构化数据 |
| **InfluxDB 2.7** | 时序 | 监控指标、性能数据 | 高吞吐写入，Flux 查询语言 |

---

## 4. 后端架构详解

### 4.1 模块划分

项目采用 **Maven 多模块** 设计：

```
monitor-project (父工程)
├── monitor-common      # 公共模块：POJO、工具类、统一响应
├── monitor-client      # 探针模块：独立运行，采集上报
├── monitor-server      # 服务端模块：核心业务逻辑
└── sql                 # 数据库脚本
```

#### 4.1.1 monitor-common (公共模块)

**职责**: 定义跨模块共享的数据结构和工具

- **DTO/VO**: `MonitorDataDTO`, `UserInfoVO`, `ServerInfoVO`
- **统一响应**: `Result<T>` 封装 (code, msg, data)
- **工具类**: 日期处理、字符串处理等

#### 4.1.2 monitor-client (探针模块)

**职责**: 部署在目标服务器，采集硬件数据并上报

**核心组件**:
- `SystemMonitorUtil`: OSHI 硬件采集封装
- `MonitorScheduler`: 定时任务 (`@Scheduled`)，每秒采集
- `HttpReporter`: RestTemplate 上报数据

**工作流程**:
```
1. @Scheduled 触发 (1秒间隔)
2. 调用 OSHI API 获取快照 (T1, T2)
3. 计算差值 (CPU%, NetRate, DiskIO)
4. 封装 JSON 数据
5. POST http://server:8080/api/monitor/report
```

#### 4.1.3 monitor-server (服务端模块)

**核心分层架构**:

```
src/main/java/com/xu/monitorserver
├── config/                    # 配置类
│   ├── SecurityConfiguration.java    # Spring Security 配置
│   ├── InfluxDBConfig.java           # InfluxDB 连接配置
│   ├── AliyunOssConfig.java          # OSS 配置
│   └── WebSocketConfig.java          # WebSocket 配置
│
├── controller/                # 控制器层 (RESTful API)
│   ├── AuthController.java           # 登录/注册/刷新
│   ├── MonitorController.java        # 监控数据 API
│   ├── ServerController.java         # 服务器管理
│   ├── SshController.java            # SSH 连接管理
│   └── UserController.java           # 用户信息
│
├── service/                   # 业务逻辑层
│   ├── authservice/
│   │   └── AuthServiceImpl.java      # JWT 双 Token 逻辑
│   ├── monitorservice/
│   │   └── MonitorServiceImpl.java   # 数据写入/查询
│   ├── sshservice/
│   │   └── SshServiceImpl.java       # JSch SSH 连接
│   └── sysuserservice/
│       └── UserServiceImpl.java      # 用户 CRUD
│
├── repository/                # 数据访问层
│   └── InfluxDBRepository.java       # InfluxDB Flux 查询封装
│
├── mapper/                    # MyBatis Mapper
│   ├── SysUserMapper.java
│   ├── SysMenuMapper.java
│   └── ServerInfoMapper.java
│
├── entity/                    # 实体类
│   ├── SysUser.java
│   ├── SysRole.java
│   ├── SysMenu.java
│   └── ServerInfo.java
│
├── filter/                    # 过滤器
│   └── JwtAuthenticationTokenFilter.java  # JWT 校验过滤器
│
├── handler/                   # 处理器
│   ├── WebSshWebSocketHandler.java   # WebSocket 消息处理
│   └── AuthHandshakeInterceptor.java # WebSocket 握手鉴权
│
├── exception/                 # 异常处理
│   ├── GlobalExceptionHandler.java   # 全局异常捕获
│   └── ServiceException.java         # 自定义业务异常
│
└── utils/                     # 工具类
    ├── JwtUtils.java                 # JWT 生成/解析
    └── AliyunOssUtil.java            # OSS 上传工具
```

### 4.2 核心业务模块

#### 4.2.1 安全鉴权模块 (Security Module)

**架构特点**: 双 Token 无感刷新 + RBAC 动态权限

**关键流程**:

1. **登录流程**:
   ```
   POST /api/auth/login
   → UserDetailsService 加载用户 + 权限
   → 生成 Access Token (15分钟) + Refresh Token (7天)
   → 存储 Refresh Token 到 sys_user_token
   → 返回双 Token
   ```

2. **请求鉴权**:
   ```
   Request → JwtAuthenticationTokenFilter
   → 解析 Access Token
   → 填充 SecurityContext
   → 若过期，返回 401 (触发前端刷新)
   ```

3. **Token 刷新**:
   ```
   POST /api/auth/refresh (携带 Refresh Token)
   → 校验数据库记录有效性
   → 轮换生成新双 Token
   → 更新数据库
   → 返回新 Token
   ```

4. **RBAC 权限**:
   ```
   User → Role → Menu (permission 字段)
   前端: v-permission="['server:delete']"
   后端: @PreAuthorize("hasAuthority('server:delete')")
   ```

#### 4.2.2 监控数据模块 (Monitor Module)

**数据流**:

```
Client (OSHI) → POST /api/monitor/report
→ MonitorController.receiveData()
→ MonitorService.saveToInflux()
→ InfluxDBRepository.write(Point)
→ InfluxDB (Bucket: monitor_bucket)
```

**查询流程**:

```
Frontend → GET /api/monitor/history?ip=xxx&range=1h&metric=cpu
→ MonitorService.queryHistory()
→ InfluxDBRepository.query(Flux)
→ 返回时序数据 (时间戳 + 值)
→ ECharts 渲染
```

**Flux 查询示例**:
```flux
from(bucket: "monitor_bucket")
  |> range(start: -1h)
  |> filter(fn: (r) => r["ip"] == "192.168.1.100")
  |> filter(fn: (r) => r["_field"] == "cpu_load")
  |> aggregateWindow(every: 10s, fn: mean)
```

#### 4.2.3 WebSSH 模块 (SSH Module)

**技术栈**: WebSocket + JSch

**连接流程**:

```
1. 前端建立 WebSocket: ws://server/ws/ssh?token=xxx
2. AuthHandshakeInterceptor 校验 Token
3. 连接建立后，前端发送 JSON: { "operate": "connect", "host": "...", "pwd": "..." }
4. WebSshHandler 解析 → SshService.initConnection()
5. JSch 建立 SSH 连接
6. 全双工通信: 前端输入 → SSH OutputStream, SSH InputStream → 前端显示
```

**安全措施**:
- Token 握手鉴权 (防止未授权连接)
- 密码通过 JSON 协议传输 (非 URL 参数)
- 连接保活机制 (Keep-Alive)

---

## 5. 前端架构详解

### 5.1 目录结构

```
monitor-web/src
├── api/                       # API 接口封装
│   ├── auth.js                    # 登录/注册/刷新
│   ├── monitor.js                 # 监控数据
│   ├── server.js                  # 服务器管理
│   └── user.js                    # 用户信息
│
├── components/                # 通用组件
│   ├── ThemeToggle.vue            # 主题切换
│   └── PermissionButton.vue       # 权限按钮
│
├── directive/                 # 自定义指令
│   └── permission.js              # v-permission 指令
│
├── layout/                    # 布局组件
│   ├── MainLayout.vue             # 主框架布局
│   ├── Sidebar.vue                # 侧边栏
│   └── Header.vue                 # 顶部导航
│
├── stores/                    # Pinia 状态管理
│   ├── user.js                    # 用户状态 (userInfo, permissions, token)
│   └── theme.js                   # 主题状态 (isDark)
│
├── router/                    # 路由配置
│   └── index.js                   # Vue Router 路由定义
│
├── utils/                     # 工具函数
│   └── request.js                 # Axios 拦截器 (Token 自动刷新)
│
├── views/                     # 页面组件
│   ├── dashboard/                 # 仪表盘 (ECharts 图表)
│   ├── login/                     # 登录页
│   ├── register/                  # 注册页
│   ├── monitor/                   # SSH 终端页
│   ├── profile/                   # 个人中心
│   └── system/                    # 系统管理 (RBAC)
│
├── App.vue                    # 根组件
└── main.js                    # 入口文件
```

### 5.2 核心技术设计

#### 5.2.1 状态管理 (Pinia)

**user.js Store**:
```javascript
{
  state: {
    userInfo: {},         // 用户基础信息
    permissions: [],      // 权限标识数组 ['server:list', 'server:delete']
    accessToken: '',
    refreshToken: ''
  },
  actions: {
    login(),              // 登录并存储 Token
    logout(),             // 清空状态并跳转
    refreshAccessToken()  // 刷新 Access Token
  }
}
```

**theme.js Store**:
```javascript
{
  state: {
    isDark: false         // 暗黑模式开关
  },
  actions: {
    toggleTheme()         // 切换主题并持久化到 LocalStorage
  }
}
```

#### 5.2.2 自动刷新拦截器 (request.js)

**核心逻辑**:

```javascript
// 响应拦截器
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // 1. 锁定，防止并发请求重复刷新
      if (!isRefreshing) {
        isRefreshing = true;
        try {
          // 2. 调用刷新接口
          const newToken = await refreshAccessToken();
          // 3. 更新 Token
          setToken(newToken);
          // 4. 重发所有挂起的请求
          retryQueue.forEach(cb => cb(newToken));
          retryQueue = [];
          // 5. 重发当前请求
          return axios(error.config);
        } catch {
          // 刷新失败，跳转登录
          router.push('/login');
        } finally {
          isRefreshing = false;
        }
      } else {
        // 6. 挂起当前请求，等待刷新完成
        return new Promise(resolve => {
          retryQueue.push(token => {
            error.config.headers.Authorization = `Bearer ${token}`;
            resolve(axios(error.config));
          });
        });
      }
    }
    return Promise.reject(error);
  }
);
```

#### 5.2.3 权限指令 (v-permission)

**指令定义**:
```javascript
app.directive('permission', {
  mounted(el, binding) {
    const { value } = binding;
    const permissions = useUserStore().permissions;
    
    if (value && value instanceof Array) {
      const hasPermission = permissions.some(perm => value.includes(perm));
      if (!hasPermission) {
        el.parentNode?.removeChild(el);  // 移除无权限的按钮
      }
    }
  }
});
```

**使用示例**:
```vue
<el-button v-permission="['server:delete']" @click="deleteServer">
  删除服务器
</el-button>
```

#### 5.2.4 实时图表 (ECharts)

**组件设计**:
```javascript
// 1. 定时拉取数据
setInterval(async () => {
  const data = await monitorApi.getCpuHistory('1h');
  updateChart(data);
}, 5000);

// 2. 响应式适配
useResizeObserver(chartRef, () => {
  chart.resize();
});

// 3. 暗黑模式适配
watch(() => themeStore.isDark, (isDark) => {
  chart.setOption({ backgroundColor: isDark ? '#1a1a1a' : '#fff' });
});
```

---

## 6. 数据流与交互模式

### 6.1 监控数据采集流

```
┌─────────────┐     每秒采集     ┌──────────────┐
│ 目标服务器   │ ───────────────> │ OSHI Library │
│ (Linux/Win) │                  │  硬件接口层   │
└─────────────┘                  └──────┬───────┘
                                        │ 快照数据
                                        ▼
                                 ┌──────────────┐
                                 │ Monitor      │
                                 │ Client       │
                                 │ (定时任务)   │
                                 └──────┬───────┘
                                        │ HTTP POST
                                        │ JSON Body
                                        ▼
                                 ┌──────────────┐
                                 │ Monitor      │
                                 │ Server       │
                                 │ Controller   │
                                 └──────┬───────┘
                                        │ Service
                                        ▼
                                 ┌──────────────┐
                                 │ InfluxDB     │
                                 │ Repository   │
                                 └──────┬───────┘
                                        │ Flux Write
                                        ▼
                                 ┌──────────────┐
                                 │ InfluxDB     │
                                 │ (时序数据库)  │
                                 └──────────────┘
```

### 6.2 前端查询流

```
┌──────────────┐    GET /history   ┌──────────────┐
│ ECharts      │ ─────────────────> │ Axios        │
│ Component    │                    │ (request.js) │
└──────────────┘                    └──────┬───────┘
      ▲                                    │ + Token
      │                                    ▼
      │ 渲染                        ┌──────────────┐
      │                             │ Monitor      │
      │                             │ Controller   │
      │                             └──────┬───────┘
      │                                    │ Flux Query
      │                                    ▼
      │                             ┌──────────────┐
      │                             │ InfluxDB     │
      │                             │ Repository   │
      │                             └──────┬───────┘
      │                                    │
      │                                    ▼
      │                             ┌──────────────┐
      └─────────────────────────────│ InfluxDB     │
           JSON Response             │ (Bucket)     │
                                     └──────────────┘
```

### 6.3 WebSSH 交互流

```
┌──────────────┐   WebSocket    ┌──────────────┐
│ Xterm.js     │ ──────握手────> │ WebSocket    │
│ (前端终端)   │    ?token=xxx   │ Handler      │
└──────┬───────┘                 └──────┬───────┘
       │                                │
       │ JSON: connect                  │ JSch
       ├───────────────────────────────>│ Session
       │                                ├─────────┐
       │ JSON: command                  │         │
       ├───────────────────────────────>│ Shell   │
       │                                │ Channel │
       │ 输出流 (stdout/stderr)         │         │
       │<───────────────────────────────┤<────────┘
       │                                │
       └────────────────────────────────┘
            全双工实时通信
```

---

## 7. 核心设计模式

### 7.1 分层架构 (Layered Architecture)

```
Presentation Layer (Controller)
         ↓
Business Logic Layer (Service)
         ↓
Data Access Layer (Repository/Mapper)
         ↓
Data Storage (MySQL/InfluxDB)
```

**优势**:
- 关注点分离 (Separation of Concerns)
- 易于维护和测试
- 层间依赖单向

### 7.2 依赖注入 (Dependency Injection)

**Spring 容器管理**:
```java
@RestController
public class MonitorController {
    @Autowired
    private IMonitorService monitorService;  // DI 注入
}
```

### 7.3 策略模式 (Strategy Pattern)

**硬件采集策略**:
```java
interface HardwareCollector {
    MonitorData collect();
}

class LinuxCollector implements HardwareCollector { ... }
class WindowsCollector implements HardwareCollector { ... }
```

### 7.4 单例模式 (Singleton Pattern)

**OSHI 资源复用**:
```java
public class SystemMonitorUtil {
    private static final SystemInfo systemInfo = new SystemInfo();  // 单例
}
```

### 7.5 过滤器模式 (Filter Pattern)

**JWT 过滤链**:
```
Request → JwtAuthenticationTokenFilter
        → Spring Security FilterChain
        → Controller
```

### 7.6 观察者模式 (Observer Pattern)

**Vue 响应式系统**:
```javascript
// Pinia 状态变化自动触发组件更新
const userStore = useUserStore();
watch(() => userStore.permissions, (newVal) => {
  // 自动更新 UI
});
```

---

## 8. 部署架构

### 8.1 组件部署

```
┌─────────────────────────────────────────────────────────┐
│                      Production Server                   │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │   Nginx    │  │   MySQL    │  │  InfluxDB  │        │
│  │   :443     │  │   :3306    │  │   :8086    │        │
│  │  (SSL/WSS) │  └────────────┘  └────────────┘        │
│  └─────┬──────┘                                          │
│        │ Reverse Proxy                                   │
│        ▼                                                  │
│  ┌────────────┐                                          │
│  │ Monitor    │                                          │
│  │ Server     │                                          │
│  │ :8080      │                                          │
│  │ (Spring    │                                          │
│  │  Boot)     │                                          │
│  └────────────┘                                          │
│                                                           │
│  ┌────────────────────────────────────────────┐         │
│  │ Static Files (Nginx serves Vue dist/)      │         │
│  └────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    Target Servers (N)                    │
│  ┌────────────────────────────────────────────┐         │
│  │ Monitor Client (Java -jar client.jar)      │         │
│  │ - OSHI 采集                                │         │
│  │ - 定时上报到 Server:8080/api/monitor/report│         │
│  └────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────┘
```

### 8.2 推荐部署配置

| 组件 | 部署方式 | 配置建议 |
|------|---------|---------|
| **Monitor Server** | jar 包 / Docker | 4C8G, JDK 17+ |
| **Monitor Client** | jar 包 (多实例) | 1C1G 即可 |
| **MySQL** | Docker / 云数据库 | 主从复制，定期备份 |
| **InfluxDB** | Docker | SSD 存储，30天保留策略 |
| **Nginx** | Docker / 宿主机 | SSL 证书，反向代理 |
| **Vue 前端** | 静态资源 | Nginx 托管 dist/ |

---

## 9. 架构优势与特点

### 9.1 技术优势

1. **前后端分离**: 开发解耦，前端可独立迭代
2. **无状态认证**: JWT + 双 Token，易于水平扩展
3. **时序数据专用库**: InfluxDB 高性能写入，降采样查询
4. **微服务友好**: 多模块设计，易于拆分为微服务
5. **现代化技术栈**: Spring Boot 3 + Vue 3，社区活跃

### 9.2 业务特点

1. **轻量级探针**: Client 端低资源占用，适合大规模部署
2. **实时性**: 秒级数据采集，ECharts 5秒刷新
3. **安全性**: RBAC 动态权限，WebSSH Token 鉴权
4. **可扩展性**: 
   - 支持多台服务器监控 (通过 ip 标识区分)
   - 支持新增监控指标 (扩展 OSHI 采集)
   - 支持告警扩展 (预留接口)

### 9.3 用户体验

1. **暗黑模式**: 全站支持，自动适配图表
2. **无感刷新**: Token 过期自动续期，不打断操作
3. **Web 终端**: 浏览器直接 SSH，无需客户端工具

---

## 10. 潜在改进方向

### 10.1 架构层面

| 改进点 | 当前状态 | 建议方案 | 优先级 |
|--------|---------|---------|--------|
| **服务拆分** | 单体应用 | 拆分为监控服务、认证服务、SSH服务 | 中 |
| **缓存层** | 直查数据库 | Redis 缓存热点数据 (如最新监控值) | 高 |
| **消息队列** | HTTP 同步上报 | Kafka/RabbitMQ 异步解耦 | 中 |
| **API 网关** | 直连后端 | Spring Cloud Gateway 统一入口 | 低 |

### 10.2 功能层面

| 改进点 | 描述 | 优先级 |
|--------|------|--------|
| **告警中心** | 自定义阈值告警 (CPU > 90%)，邮件/钉钉通知 | 高 |
| **文件管理** | SFTP 远程文件上传/下载 | 中 |
| **日志收集** | 集成 ELK，统一日志查询 | 中 |
| **配置中心** | Nacos/Apollo 动态配置 | 低 |
| **链路追踪** | SkyWalking/Zipkin 分布式追踪 | 低 |

### 10.3 性能优化

1. **前端优化**:
   - 图表懒加载 (路由级别 Code Splitting)
   - WebWorker 处理大量数据
   - 虚拟滚动 (长列表)

2. **后端优化**:
   - InfluxDB 批量写入 (当前单条)
   - 连接池优化 (HikariCP, InfluxDB Client)
   - 异步处理 (@Async)

3. **数据库优化**:
   - MySQL 索引优化 (sys_user.username)
   - InfluxDB CQ 自动降采样

### 10.4 安全加固

1. **传输加密**: 全站 HTTPS，WSS 加密 WebSocket
2. **SQL 注入**: MyBatis 参数化查询 (已实现)
3. **XSS 防护**: 前端输入校验，后端 HTML 转义
4. **CSRF**: Spring Security CSRF Token
5. **限流**: Sentinel/Resilience4j 接口限流
6. **审计日志**: AOP 记录敏感操作

---

## 📊 架构量化指标

| 维度 | 指标 | 数值 |
|------|------|------|
| **代码规模** | 后端 Java 文件数 | ~40 个 |
| | 前端 Vue/JS 文件数 | ~22 个 |
| **数据库** | MySQL 表数 | 7 张 (用户、角色、菜单、服务器等) |
| | InfluxDB Bucket | 1 个 (monitor_bucket) |
| **API 接口** | RESTful 端点 | ~20 个 |
| **技术层次** | 分层深度 | 3 层 (Controller → Service → Repository) |
| **模块数** | Maven 模块 | 3 个 (common, client, server) |

---

## 📝 总结

**GraduationProject-ServerMonitor** 采用 **现代化全栈分布式架构**，具有以下显著特征：

1. ✅ **前后端分离**: Vue 3 SPA + Spring Boot RESTful API
2. ✅ **分布式设计**: Client-Server 架构，支持多节点监控
3. ✅ **双数据库**: MySQL (关系) + InfluxDB (时序) 混合存储
4. ✅ **企业级安全**: Spring Security + JWT + RBAC 动态权限
5. ✅ **实时通信**: WebSocket + Xterm.js 实现 WebSSH
6. ✅ **高性能**: 时序数据库专用优化，秒级数据刷新
7. ✅ **易扩展**: 多模块设计，支持功能水平扩展

该架构在 **可维护性、安全性、性能** 三方面达到了较高水平，适合作为中小型运维监控平台的技术选型参考。

---

**文档结束** | *Generated: 2025-12-18*
