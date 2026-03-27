# EeveeAgent 前端开发文档

## 项目概述

EeveeAgent 前端项目是一个基于 Vue 3 的单页应用，主要实现了登录门户和会话平台的前端界面。项目使用现代前端技术栈，包括 Vue 3、Vue Router、Pinia 和 Axios 等。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **路由**: Vue Router 5.x
- **状态管理**: Pinia 3.x
- **HTTP 客户端**: Axios 1.x
- **构建工具**: Vite 8.x
- **样式**: 原生 CSS (Scoped)

## 项目结构

```
frontend/
├── src/
│   ├── assets/           # 静态资源
│   ├── components/       # 通用组件
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── views/            # 页面组件
│   │   ├── Login.vue     # 登录页面
│   │   └── Home.vue      # 会话平台首页
│   ├── App.vue           # 应用根组件
│   ├── main.js           # 应用入口
│   └── style.css         # 全局样式
├── public/               # 公共静态资源
├── index.html            # HTML 模板
├── package.json          # 项目配置
└── vite.config.js        # Vite 配置
```

## 核心功能

### 1. 登录系统

- **登录页面** (`views/Login.vue`)
  - 实现了用户名和密码输入表单
  - 支持记住密码功能
  - 登录状态管理和错误提示
  - 响应式设计，适配不同屏幕尺寸

- **登录流程**
  1. 前端发送 POST 请求到 `/api/auth/login` 接口
  2. 后端验证用户凭据并返回 token
  3. 前端将用户信息和 token 存储到 Pinia store 和 localStorage
  4. 登录成功后跳转到会话平台首页

### 2. 会话平台

- **首页** (`views/Home.vue`)
  - 左侧边栏：包含 logo、搜索框、历史对话列表和用户信息
  - 主内容区：包含欢迎信息、聊天区域和消息输入框
  - 支持新建对话、上传图片和文档功能
  - 响应式布局，适配不同屏幕尺寸

## 状态管理

使用 Pinia 进行状态管理，主要管理用户登录状态：

- **用户状态** (`stores/user.js`)
  - `isLoggedIn`: 登录状态
  - `userInfo`: 用户信息（userId、role、token、avatar）
  - `login()`: 登录方法，存储用户信息到 localStorage
  - `logout()`: 登出方法，清除用户信息
  - `loadUserInfo()`: 从 localStorage 加载用户信息

## 路由配置

使用 Vue Router 配置路由：

- `/`: 登录页面
- `/home`: 会话平台首页

## API 接口

### 登录接口

- **URL**: `http://localhost:8080/api/auth/login`
- **方法**: POST
- **请求体**:
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **响应**:
  ```json
  {
    "success": true,
    "data": {
      "userId": "用户ID",
      "role": "用户角色",
      "token": "登录令牌",
      "avatar": "头像URL"
    },
    "message": "登录成功"
  }
  ```

### 登出接口

- **URL**: `http://localhost:8080/api/auth/logout`
- **方法**: POST
- **响应**:
  ```json
  {
    "success": true,
    "data": null,
    "message": "登出成功"
  }
  ```

## 开发流程

1. **安装依赖**:
   ```bash
   cd frontend
   npm install
   ```

2. **启动开发服务器**:
   ```bash
   npm run dev
   ```

3. **构建生产版本**:
   ```bash
   npm run build
   ```

4. **预览生产构建**:
   ```bash
   npm run preview
   ```

## 设计说明

### 登录页面

- **设计风格**: 现代化、简洁、专业
- **配色方案**: 渐变紫色背景 (#667eea 到 #764ba2)
- **布局**: 居中登录表单，响应式设计
- **交互**: 表单验证、加载状态、错误提示

### 会话平台

- **设计风格**: 简洁、高效、用户友好
- **配色方案**: 白色背景，紫色主题色
- **布局**: 左侧边栏 + 右侧主内容区
- **交互**: 平滑过渡、悬停效果、实时反馈

## 响应式设计

- **桌面端** (> 768px): 完整布局，左侧边栏固定
- **移动端** (<= 768px): 紧凑布局，适配小屏幕

## 安全性考虑

- **密码安全**: 密码在传输过程中加密
- **token 管理**: 使用 localStorage 存储 token，并在会话中验证
- **XSS 防护**: 使用 Vue 的模板系统自动转义
- **CSRF 防护**: 依赖后端实现

## 未来优化方向

1. **添加表单验证**：使用 VeeValidate 等库增强表单验证
2. **实现主题切换**：支持浅色/深色模式
3. **添加动画效果**：使用 Vue Transition 增强用户体验
4. **优化性能**：使用懒加载、代码分割等技术
5. **添加国际化支持**：使用 i18n 实现多语言

## 注意事项

- 本项目仅实现前端代码，后端功能由其他团队负责开发
- 登录接口的基础 URL 配置为 `http://localhost:8080`，实际部署时需要根据后端服务地址进行调整
- 前端开发过程中，若后端接口尚未就绪，可使用 Mock 数据进行开发和测试
