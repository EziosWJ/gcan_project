# 后端 Docker 部署说明

本文说明 `system-api` 和 `gcan-api` 使用 Docker Compose 部署到 Linux 服务器的方式，适用于通过 1Panel 的容器编排功能运行。

本文采用以下方案：

- 在开发机或 CI 环境使用 Maven 打包 JAR；
- 1Panel 使用 Java 21 JRE 镜像运行 JAR；
- 数据库连接信息通过 Docker Compose 的 `environment` 注入；
- system-api 的上传目录映射到宿主机，避免容器重建后文件丢失；
- 前端和 HTTP API 由 Nginx 统一对外提供；
- GCAN 盒子直接连接 `gcan-api` 的 TCP 端口 `8000`。

接口鉴权不在本文范围内。

## 一、服务和端口

| 服务 | 容器端口 | 当前用途 | 对外暴露建议 |
|---|---:|---|---|
| system-api | 8080 | 登录、系统管理、文件管理、字典接口 | 仅允许 Nginx 访问 |
| gcan-api HTTP | 8081 | GCAN 管理接口、对外监控 API | 仅允许 Nginx 访问 |
| gcan-api TCP | 8000 | 接收 GCAN 盒子数据 | 只对设备网络开放 |
| MySQL | 3306 | 两个 API 共享的业务数据库 | 不直接暴露公网 |

容器内部服务通过 Compose 服务名互相访问。例如 `gcan-api` 访问 `system-api` 时使用：

```text
http://system-api:8080
```

不能在容器中使用 `http://localhost:8080`，因为容器内的 `localhost` 指向当前容器自身。

## 二、打包 JAR

要求构建环境安装 Java 21。进入后端目录：

```bash
cd base-api
```

打包全部模块：

```bash
./mvnw clean package -DskipTests
```

也可以只打包两个可运行服务：

```bash
./mvnw -pl system-api -am package -DskipTests
./mvnw -pl gcan-api -am package -DskipTests
```

构建结果位于：

```text
system-api/target/*.jar
gcan-api/target/*.jar
```

建议将两个 JAR 复制为固定文件名，方便 Compose 更新：

```bash
mkdir -p deploy/artifacts
cp system-api/target/system-api-*.jar deploy/artifacts/system-api.jar
cp gcan-api/target/gcan-api-*.jar deploy/artifacts/gcan-api.jar
```

如果 `target` 目录中存在多个 JAR，请确认复制的是 Spring Boot 可执行 JAR，而不是测试或源码附件。

## 三、部署目录

建议在服务器上建立类似目录：

```text
/opt/gcan-api/
├── docker-compose.yml
├── artifacts/
│   ├── system-api.jar
│   └── gcan-api.jar
└── data/
    └── uploads/
```

将两个 JAR、Compose 文件和 `.env` 上传到服务器。

创建上传目录：

```bash
cd /opt/gcan-api
mkdir -p artifacts data/uploads
```

`data/uploads` 是持久化目录，需要纳入服务器备份。

## 四、Docker Compose 配置

在 `/opt/gcan-api/docker-compose.yml` 中保存：

```yaml
services:
  system-api:
    image: eclipse-temurin:21-jre
    container_name: gcan-system-api
    restart: unless-stopped
    working_dir: /app
    command: ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/system-api.jar"]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      TZ: Asia/Shanghai
      DB_URL: ${DB_URL:?DB_URL is required}
      DB_USERNAME: ${DB_USERNAME:?DB_USERNAME is required}
      DB_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}
      SYSTEM_FILE_UPLOAD_ROOT: /data/uploads
    volumes:
      - /opt/application/vehicleGcanProject/system-api.jar:/app/system-api.jar:ro
      - /opt/application/vehicleGcanProject/uploads:/data/uploads
    ports:
      - "127.0.0.1:8080:8080"
    networks:
      - 1panel-network

  gcan-api:
    image: eclipse-temurin:21-jre
    container_name: gcan-api
    restart: unless-stopped
    working_dir: /app
    command: ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/gcan-api.jar"]
    depends_on:
      - system-api
    environment:
      SPRING_PROFILES_ACTIVE: prod
      TZ: Asia/Shanghai
      DB_URL: ${DB_URL:?DB_URL is required}
      DB_USERNAME: ${DB_USERNAME:?DB_USERNAME is required}
      DB_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}
      GCAN_SYSTEM_API_URL: http://system-api:8080
    volumes:
      - /opt/application/vehicleGcanProject/gcan-api.jar:/app/gcan-api.jar:ro
    ports:
      - "127.0.0.1:8081:8081"
      - "8000:8000"
    networks:
      - 1panel-network

networks:
  1panel-network:
    external: true
```

说明：

- `SPRING_PROFILES_ACTIVE=prod` 启用生产配置；
- `TZ=Asia/Shanghai` 设置容器时区，`-Duser.timezone=Asia/Shanghai` 确保 JVM 使用东八区；
- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 由 Compose 运行时注入；
- `SYSTEM_FILE_UPLOAD_ROOT` 覆盖默认的相对路径 `uploads`；
- `gcan-api` 通过 Compose 服务名访问 `system-api`；
- 8080、8081 只绑定到宿主机本地，交给 Nginx 反向代理；
- 8000 需要让 GCAN 盒子所在网络能够访问；
- `depends_on` 只保证启动顺序，不代表 system-api 已经完成数据库连接和 HTTP 就绪。
- 两个服务都加入外部 Docker 网络 `1panel-network`，便于与 1Panel 中的 Nginx 或其他容器通信。

如果 Nginx 运行在另一个 Docker 容器中，而不是宿主机上，需要将 Nginx 和这两个服务加入同一个 Docker 网络，并按实际网络结构调整 `ports` 和上游地址。

### 外部网络准备

`external: true` 表示 Compose 不会自动创建该网络。部署前确认 1Panel 已经创建：

```bash
docker network inspect 1panel-network
```

如果网络不存在，需要先创建，或在 1Panel 中创建同名网络：

```bash
docker network create 1panel-network
```

## 五、环境变量配置

推荐在 Compose 文件旁边创建 `.env`，不要把真实密码直接写进 `docker-compose.yml`：

```env
DB_URL=jdbc:mysql://10.0.0.10:3306/base_project?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=baseapi
DB_PASSWORD=请替换为强密码
```

### 开发环境读取线上当前原始 CAN 帧

开发环境可以通过 gcan-api 的配置镜像线上开放接口的当前原始 CAN 帧。该能力默认关闭，只应在开发环境启用；开启后会停用本地 GCAN TCP 监听，并要求配置至少一个目标盒子，避免误读取线上全部数据。

例如线上 gcan-api 地址为 `http://47.96.10.182:8081` 时，在开发环境的运行环境中配置：

```env
GCAN_MIRROR_ENABLED=true
GCAN_MIRROR_BASE_URL=http://47.96.10.182:8081
GCAN_MIRROR_BOX_IDS=请替换为目标盒子HEX编号
# 可选；为空表示目标盒子的全部当前CAN ID
GCAN_MIRROR_CAN_IDS=
GCAN_MIRROR_POLL_INTERVAL_MS=1000
GCAN_MIRROR_CONNECT_TIMEOUT_MS=2000
GCAN_MIRROR_READ_TIMEOUT_MS=2000
```

盒子编号支持项目既有的 HEX 写法，例如 `33`、`0x21`；多个盒子用逗号分隔。开发环境必须提前维护与线上目标盒子相同的盒子绑定、启用车辆和车型协议配置，镜像不会自动导入线上车辆档案。镜像使用线上原始帧的 `receivedAt`，线上接口短暂不可用时保留旧帧并让其自然过期。

开发环境前端继续访问开发环境自己的监控页面和 API，例如 `http://localhost:6001/gcan/monitor`；不需要让浏览器直接请求线上开放接口。

如果 MySQL 也是同一个 Compose 应用中的服务，连接地址中的主机名应改为 MySQL 服务名，例如：

```env
DB_URL=jdbc:mysql://mysql:3306/base_project?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
```

如果 MySQL 由 1Panel 单独管理，使用 MySQL 所在服务器的内网 IP 或可解析的服务地址，不要默认填写 `localhost`。

`.env` 包含数据库密码，应设置合适的文件权限，并避免提交到 Git：

```bash
chmod 600 .env
```

当前 `application.yml` 中的配置：

```yaml
spring:
  datasource:
    url: ${DB_URL:...}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

会在容器启动时读取环境变量。`DB_USERNAME` 和 `DB_PASSWORD` 没有默认值，未注入时服务会启动失败，这是预期行为。

## 六、使用 1Panel 部署

1. 在服务器创建 `/opt/gcan-api` 部署目录。
2. 上传 `docker-compose.yml`、两个 JAR 和 `.env`。
3. 确认 `data/uploads` 目录存在且可写。
4. 在 1Panel 的容器编排/Compose 应用功能中，选择该目录和 Compose 文件创建应用。
5. 启动或重新部署应用。
6. 查看 `gcan-system-api` 和 `gcan-api` 两个容器的日志，确认没有数据库连接错误。

不同版本的 1Panel 菜单名称可能略有差异，但核心是让 1Panel 使用上述 Compose 文件，并将 `.env` 中的变量注入 Compose。

也可以在服务器命令行执行：

```bash
cd /opt/gcan-api
docker compose up -d
docker compose ps
docker compose logs -f system-api gcan-api
```

## 七、数据库初始化

当前项目没有使用 Flyway 或 Liquibase，Spring Boot 不会自动按版本执行所有 SQL。首次部署前需要手工初始化数据库，并先做好备份。

建议执行顺序：

1. `system-api/src/main/resources/sql/schema.sql`
2. `system-api/src/main/resources/sql/data.sql`
3. `system-api/src/main/resources/sql/migration/` 下按日期顺序执行迁移脚本
4. `gcan-api/src/main/resources/sql/schema.sql`

已有数据库只执行尚未执行过的迁移脚本和 GCAN 表结构脚本，不要重复执行会产生重复数据的初始化脚本。

可以使用 1Panel 的数据库管理工具执行，也可以使用 MySQL 客户端：

```bash
mysql -h <数据库地址> -P 3306 -u <用户名> -p base_project < system-api/src/main/resources/sql/schema.sql
mysql -h <数据库地址> -P 3306 -u <用户名> -p base_project < system-api/src/main/resources/sql/data.sql
mysql -h <数据库地址> -P 3306 -u <用户名> -p base_project < gcan-api/src/main/resources/sql/schema.sql
```

迁移脚本需要单独按实际部署版本执行。

## 八、部署后的检查

### 1. 检查容器状态

```bash
docker compose ps
```

两个容器应处于 `running` 状态。

### 2. 检查 HTTP 端口

```bash
curl http://127.0.0.1:8080/api/open/gcan/v1/dictionaries
curl http://127.0.0.1:8081/api/open/gcan/v1/monitor/overview
```

返回业务响应或鉴权响应都说明 HTTP 服务已经能够接收请求；如果是连接失败或 502，需要先检查容器日志。

### 3. 检查 TCP 端口

```bash
ss -lntp | grep -E ':8000|:8080|:8081'
```

确认宿主机的 `8000`、`8080`、`8081` 监听情况符合预期。

### 4. 检查文件持久化

上传一个测试文件后确认宿主机出现文件：

```bash
find /opt/gcan-api/data/uploads -type f -print
```

重建 `system-api` 容器后文件仍应存在。

## 九、发布新版本

在构建机重新打包：

```bash
cd base-api
./mvnw clean package -DskipTests
cp system-api/target/system-api-*.jar deploy/artifacts/system-api.jar
cp gcan-api/target/gcan-api-*.jar deploy/artifacts/gcan-api.jar
```

将两个新 JAR 上传并覆盖服务器上的 `artifacts` 文件，然后重新创建容器：

```bash
cd /opt/gcan-api
docker compose up -d --force-recreate system-api gcan-api
```

如果只更新一个服务，只重建对应服务。更新前建议保留上一版本 JAR，便于回滚。

## 十、与前端 Nginx 的连接

前端部署文档中的 Nginx 代理地址应对应本 Compose 的宿主机映射：

```nginx
location ^~ /api/open/gcan {
    proxy_pass http://127.0.0.1:8081;
}

location ^~ /api/gcan {
    proxy_pass http://127.0.0.1:8081;
}

location ^~ /api {
    proxy_pass http://127.0.0.1:8080;
}
```

`proxy_pass` 后不要额外添加 `/`，以保留完整的 `/api/...` 请求路径。

## 十一、常见问题

### 容器不断重启

先查看日志：

```bash
docker compose logs --tail=200 system-api
docker compose logs --tail=200 gcan-api
```

常见原因包括：

- `DB_URL`、`DB_USERNAME` 或 `DB_PASSWORD` 未注入；
- MySQL 地址在容器内不可访问；
- 数据库表结构未初始化；
- `gcan-api` 无法访问 `http://system-api:8080`；
- `8000` 端口已被其他进程占用。

### gcan-api 启动但监控没有数据

检查以下项目：

- GCAN 盒子是否能访问宿主机 TCP `8000`；
- 服务器防火墙和云安全组是否放行设备网络；
- `gcan-api` 日志中是否出现 TCP 服务启动信息；
- 车辆档案是否已配置为启用；
- `gcan-api` 是否能访问 system-api 的字典接口。

### 上传后文件丢失

检查 Compose 是否包含：

```yaml
volumes:
  - ./data/uploads:/data/uploads
```

并确认环境变量：

```yaml
SYSTEM_FILE_UPLOAD_ROOT: /data/uploads
```

### 使用容器内的 localhost 导致连接失败

容器之间不能通过对方的 `localhost` 访问。Compose 内部服务必须使用服务名，例如：

```yaml
GCAN_SYSTEM_API_URL: http://system-api:8080
```
