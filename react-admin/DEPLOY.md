# 前端部署说明

本文说明 `react-admin` 前端的构建和 Nginx 部署方式。后端 `system-api`、`gcan-api` 的安装、启动和进程管理不在本文范围内。

## 一、部署结构

推荐让浏览器只访问一个域名：

```text
浏览器
  └── Nginx
       ├── 静态文件：react-admin/dist
       ├── /api/open/gcan/** ──> gcan-api   :8081
       ├── /api/gcan/**      ──> gcan-api   :8081
       └── 其他 /api/**      ──> system-api :8080
```

前端请求使用同源路径 `/api/...`，因此生产环境不需要把 API 地址写入前端，也不需要额外处理跨域。

注意：`vite.config.ts` 中的 `server.proxy` 只在开发服务器运行时生效，执行 `npm run build` 后不会参与生产请求转发。生产环境必须由 Nginx 配置 API 代理。

## 二、构建前端

在项目的 `react-admin` 目录执行：

```bash
cd react-admin
npm ci
npm run build
```

构建成功后会生成：

```text
react-admin/dist/
```

将 `dist` 目录中的内容部署到服务器上的静态目录，例如：

```bash
sudo mkdir -p /var/www/gcan-admin
sudo cp -r dist/. /var/www/gcan-admin/
```

如果服务器没有 Node.js，也可以在本地或 CI 环境完成构建，只需要把构建后的 `dist` 内容复制到服务器。

## 三、Nginx 配置

以下配置假设：

- 前端静态文件目录为 `/var/www/gcan-admin`；
- `system-api` 监听 `127.0.0.1:8080`；
- `gcan-api` 监听 `127.0.0.1:8081`；
- 前端访问域名为 `example.com`。

将域名和目录替换为实际值：

```nginx
server {
    listen 80;
    server_name example.com;

    root /var/www/gcan-admin;
    index index.html;

    # GCAN 对外只读 API，以及 GCAN 管理 API
    location ^~ /api/open/gcan {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ^~ /api/gcan {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # system-api 接口，包括登录、用户、菜单、文件上传等
    location ^~ /api {
        client_max_body_size 100m;
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # React Router 的 history fallback
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

`proxy_pass` 后面故意没有添加 `/`，这样可以保留完整请求路径。例如：

```text
/api/open/gcan/v1/monitor/overview
```

转发到 `gcan-api` 后仍然是这个路径。不要随意改成 `proxy_pass http://127.0.0.1:8081/`，否则可能导致后端收到被截断的 URI。

配置检查并重新加载 Nginx：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 四、发布更新

每次发布前端时，在构建环境执行：

```bash
cd react-admin
npm ci
npm run build
```

然后将新的 `dist` 内容同步到 Nginx 静态目录：

```bash
sudo cp -r dist/. /var/www/gcan-admin/
sudo nginx -t
sudo systemctl reload nginx
```

前端是静态文件，更新前端通常不需要重启 Java 服务。

## 五、验收检查

部署后至少检查以下内容：

1. 访问 `http://example.com` 能打开登录页。
2. 直接刷新 `/login`、`/gcan/monitor` 等前端路由不会返回 Nginx 404。
3. 登录请求 `/api/auth/login` 能到达 `system-api:8080`。
4. 监控总览 `/api/open/gcan/v1/monitor/overview` 能到达 `gcan-api:8081`。
5. GCAN 管理请求，例如 `/api/gcan/vehicle/page`，能到达 `gcan-api:8081`。
6. 文件上传请求 `/api/system/file/upload` 不返回 `413 Request Entity Too Large`。

可以在浏览器开发者工具的 Network 面板中确认请求地址仍然是当前域名下的 `/api/...`，不应出现 `localhost:8080` 或 `localhost:8081`。

## 六、常见问题

### 刷新页面返回 404

检查 Nginx 是否包含：

```nginx
try_files $uri $uri/ /index.html;
```

React Router 的前端路由需要回退到 `index.html`。

### API 返回 502 Bad Gateway

检查对应 Java 服务是否启动、监听端口是否正确，以及 Nginx 所在环境能否访问：

```bash
curl http://127.0.0.1:8080/api/auth/me
curl http://127.0.0.1:8081/api/open/gcan/v1/monitor/overview
```

如果 Nginx 和 Java 服务运行在不同容器中，`127.0.0.1` 不能作为后端地址，应改成容器服务名或内网地址。

### 上传返回 413

增大 Nginx 的：

```nginx
client_max_body_size 100m;
```

该值需要不小于后端允许的请求大小。

### 页面请求了 localhost

生产环境不要把 `VITE_API_BASE_URL` 设置为 `http://localhost:8080` 或 `http://localhost:8081`。默认留空即可，让请求使用当前域名和 Nginx 代理。

如果确实要把 API 部署到独立域名，需要在构建前设置 `VITE_API_BASE_URL`，同时配置后端 CORS；当前推荐的同域 Nginx 方案不需要这样做。

