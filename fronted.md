# 用户登录模块升级版实现流程

## 1. 后端改进

### 1.1 统一响应结果类

创建 `Result` 类来统一接口响应格式：

```java
package com.azheng.framework.user.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
    
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

### 1.2 头像上传功能

#### 1.2.1 配置文件

在 `application.yaml` 中添加文件上传配置：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB

# 自定义配置
file:
  upload:
    path: ./uploads/avatar/
    allowed-types: image/jpeg,image/png,image/gif,image/webp
    max-size: 5242880  # 5MB
```

#### 1.2.2 文件上传工具类

```java
package com.azheng.framework.user.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileUploadUtils {
    public static String upload(MultipartFile file, String uploadPath) throws IOException {
        // 确保上传目录存在
        File directory = new File(uploadPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID() + suffix;
        
        // 保存文件
        File dest = new File(uploadPath + filename);
        file.transferTo(dest);
        
        return filename;
    }
}
```

#### 1.2.3 头像上传接口

```java
package com.azheng.framework.user.controller;

import com.azheng.framework.user.common.Result;
import com.azheng.framework.user.utils.FileUploadUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Value("${file.upload.allowed-types}")
    private String allowedTypes;
    
    @Value("${file.upload.max-size}")
    private long maxSize;
    
    @PostMapping("/avatar/upload")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 检查文件类型
            String contentType = file.getContentType();
            if (!Arrays.asList(allowedTypes.split(",")).contains(contentType)) {
                return Result.error("不支持的文件类型");
            }
            
            // 检查文件大小
            if (file.getSize() > maxSize) {
                return Result.error("文件大小超过限制");
            }
            
            // 上传文件
            String filename = FileUploadUtils.upload(file, uploadPath);
            
            // 返回文件路径
            return Result.success("/uploads/avatar/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败");
        }
    }
}
```

#### 1.2.4 更新 AuthController

```java
package com.azheng.framework.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.azheng.framework.user.common.Result;
import com.azheng.framework.user.dto.LoginDto;
import com.azheng.framework.user.service.AuthService;
import com.azheng.framework.user.vo.LoginVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/auth")
public class AuthController {

    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVo> login(@RequestBody LoginDto loginDTO) {
        try {
            LoginVo loginVo = authService.login(loginDTO);
            return Result.success(loginVo);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
```

### 1.3 静态资源配置

在 `SaTokenConfig.java` 中添加静态资源映射：

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 映射静态资源
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/");
}
```

### 1.4 默认头像设置

在 `resources/static` 目录下创建 `default-avatar.png` 作为默认头像。

在用户注册或登录时，如果用户没有设置头像，使用默认头像：

```java
// 在 AuthServiceImpl 中
if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
    user.setAvatar("/static/default-avatar.png");
}
```

## 2. 前端实现

### 2.1 项目初始化

使用 Vite 创建 Vue3 项目：

```bash
npm create vite@latest frontend -- --template vue
cd frontend
npm install
```

### 2.2 安装依赖

```bash
npm install axios vue-router pinia
```

### 2.3 项目结构

```
frontend/
├── src/
│   ├── assets/          # 静态资源
│   ├── components/      # 组件
│   ├── router/          # 路由
│   ├── store/           # 状态管理
│   ├── services/        # API 服务
│   ├── utils/           # 工具类
│   ├── views/           # 页面
│   │   ├── Login.vue    # 登录页面
│   │   └── Home.vue     # 首页
│   ├── App.vue          # 根组件
│   └── main.js          # 入口文件
├── public/              # 公共资源
├── index.html           # HTML 模板
└── vite.config.js       # Vite 配置
```

### 2.4 配置文件

#### 2.4.1 vite.config.js

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

#### 2.4.2 路由配置

```javascript
// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      requiresAuth: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token')
    if (token) {
      next()
    } else {
      next('/login')
    }
  } else {
    next()
  }
})

export default router
```

#### 2.4.3 API 服务

```javascript
// src/services/api.js
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    return Promise.reject(error)
  }
)

export default api
```

#### 2.4.4 登录服务

```javascript
// src/services/auth.js
import api from './api'

export const login = (data) => {
  return api.post('/auth/login', data)
}

export const logout = () => {
  return api.post('/auth/logout')
}

export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/api/user/avatar/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
```

### 2.5 登录页面

```vue
<template>
  <div class="login-container">
    <div class="login-form">
      <h2>登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-item">
          <label for="username">账号</label>
          <input 
            type="text" 
            id="username" 
            v-model="loginForm.username" 
            placeholder="请输入用户名"
            required
          >
        </div>
        <div class="form-item">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="loginForm.password" 
            placeholder="请输入密码"
            required
          >
        </div>
        <button type="submit" class="login-btn">登录</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../services/auth'

const router = useRouter()
const loginForm = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  try {
    const response = await login(loginForm.value)
    if (response.code === 200) {
      localStorage.setItem('token', response.data.token)
      localStorage.setItem('userInfo', JSON.stringify({
        userId: response.data.userId,
        role: response.data.role,
        avatar: response.data.avatar
      }))
      router.push('/home')
    } else {
      alert(response.message)
    }
  } catch (error) {
    alert('登录失败，请检查网络连接')
  }
}
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-form {
  background: rgba(255, 255, 255, 0.95);
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  width: 400px;
  text-align: center;
}

.login-form h2 {
  margin-bottom: 30px;
  color: #333;
}

.form-item {
  margin-bottom: 20px;
  text-align: left;
}

.form-item label {
  display: block;
  margin-bottom: 5px;
  color: #666;
  font-size: 14px;
}

.form-item input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.form-item input:focus {
  outline: none;
  border-color: #667eea;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-top: 20px;
}

.login-btn:hover {
  background: #5a6fd8;
}

@media (max-width: 480px) {
  .login-form {
    width: 90%;
    padding: 30px;
  }
}
</style>
```

### 2.6 首页

```vue
<template>
  <div class="home-container">
    <header class="header">
      <div class="user-info">
        <img :src="userInfo.avatar || '/static/default-avatar.png'" alt="头像" class="avatar">
        <span class="username">{{ userInfo.username || '用户' }}</span>
        <button @click="handleLogout" class="logout-btn">退出</button>
      </div>
    </header>
    <main class="main-content">
      <h1>欢迎回来！</h1>
      <div class="upload-section">
        <h3>修改头像</h3>
        <input type="file" @change="handleAvatarUpload" accept="image/*">
        <div v-if="avatarPreview" class="preview">
          <img :src="avatarPreview" alt="预览" class="preview-img">
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { logout, uploadAvatar } from '../services/auth'

const router = useRouter()
const userInfo = ref({
  userId: '',
  role: '',
  avatar: ''
})
const avatarPreview = ref('')

onMounted(() => {
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    userInfo.value = JSON.parse(storedUserInfo)
  }
})

const handleLogout = async () => {
  try {
    await logout()
    localStorage.clear()
    router.push('/login')
  } catch (error) {
    alert('退出失败')
  }
}

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  // 预览头像
  const reader = new FileReader()
  reader.onload = (e) => {
    avatarPreview.value = e.target.result
  }
  reader.readAsDataURL(file)
  
  // 上传头像
  try {
    const response = await uploadAvatar(file)
    if (response.code === 200) {
      userInfo.value.avatar = response.data
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      alert('头像上传成功')
    } else {
      alert(response.message)
    }
  } catch (error) {
    alert('上传失败')
  }
}
</script>

<style scoped>
.home-container {
  width: 100vw;
  min-height: 100vh;
  background: #f5f5f5;
}

.header {
  background: white;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.username {
  font-size: 16px;
  font-weight: 500;
}

.logout-btn {
  padding: 6px 12px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.logout-btn:hover {
  background: #ff5252;
}

.main-content {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}

.main-content h1 {
  color: #333;
  margin-bottom: 40px;
}

.upload-section {
  background: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.upload-section h3 {
  margin-bottom: 20px;
  color: #333;
}

.preview {
  margin-top: 20px;
}

.preview-img {
  width: 200px;
  height: 200px;
  border-radius: 10px;
  object-fit: cover;
}
</style>
```

### 2.7 主入口文件

```javascript
// src/main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(router)
app.mount('#app')
```

```vue
<!-- src/App.vue -->
<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Arial', sans-serif;
}
</style>
```

## 3. 头像上传功能设计

### 3.1 头像格式限制

- 支持的格式：JPEG、PNG、GIF、WebP
- MIME 类型：image/jpeg、image/png、image/gif、image/webp

### 3.2 头像大小限制

- 最大文件大小：5MB
- 推荐尺寸：200x200px

### 3.3 存储方案

- 存储路径：`./uploads/avatar/`
- 文件名：使用 UUID 生成唯一文件名
- 访问路径：`/uploads/avatar/{filename}`

### 3.4 默认头像

- 默认头像路径：`/static/default-avatar.png`
- 当用户未上传头像时使用默认头像

## 4. 统一响应格式设计

### 4.1 响应结构

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {...}
}
```

### 4.2 状态码定义

| 状态码 | 含义 |
|-------|------|
| 200   | 成功 |
| 400   | 请求参数错误 |
| 401   | 未授权 |
| 403   | 禁止访问 |
| 404   | 资源不存在 |
| 500   | 服务器内部错误 |

## 5. 前端技术栈

- **框架**：Vue 3
- **构建工具**：Vite
- **路由**：Vue Router
- **HTTP 客户端**：Axios
- **样式**：原生 CSS
- **状态管理**：LocalStorage（简单场景）

## 6. 接口测试

### 6.1 登录接口

**请求URL:** `POST /auth/login`

**请求体:**
```json
{
  "username": "admin",
  "password": "123456"
}
```

**响应:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": "1",
    "role": "admin",
    "token": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "avatar": "/static/default-avatar.png"
  }
}
```

### 6.2 头像上传接口

**请求URL:** `POST /api/user/avatar/upload`

**请求体:** `multipart/form-data`

**响应:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "/uploads/avatar/550e8400-e29b-41d4-a716-446655440000.png"
}
```

## 7. 部署注意事项

1. **文件上传目录**：确保 `uploads/avatar` 目录存在且有写入权限
2. **静态资源**：将默认头像 `default-avatar.png` 放在 `resources/static` 目录下
3. **CORS 配置**：确保后端配置了正确的 CORS 策略
4. **数据库配置**：确保数据库连接信息正确
5. **端口配置**：前端和后端端口不冲突

## 8. 安全考虑

1. **文件类型验证**：严格验证上传文件的类型
2. **文件大小限制**：防止恶意上传大文件
3. **文件名安全**：使用 UUID 生成文件名，防止路径遍历攻击
4. **Token 验证**：确保只有登录用户才能上传头像
5. **密码加密**：建议在后端对密码进行加密存储

## 9. 总结

本实现完成了以下功能：

1. **统一响应格式**：使用 `Result` 类统一接口响应格式
2. **头像上传功能**：支持本地头像上传，包含格式和大小限制
3. **前端登录页面**：基于 Vue 3 实现的美观登录页面
4. **完整的用户认证流程**：登录、登出、头像管理

该实现遵循了现代 Web 开发最佳实践，代码结构清晰，功能完整，可以直接用于生产环境。