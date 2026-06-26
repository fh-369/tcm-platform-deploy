# 中医问诊与日常养生平台 - 轻量部署仓库

这个仓库只保留 Docker 部署必需文件，不包含开发文档、Git 历史、node_modules、dist、target 或本机真实 `.env`。

## 目录说明

```text
.
├─ docker-compose.yml          # 一键启动 MySQL、后端、前端
├─ .env.example                # 环境变量示例，复制为 .env 后填写真实值
├─ backend/                    # Spring Boot 后端源码与 Dockerfile
└─ frontend/                   # Vite 前端源码、Nginx 配置与 Dockerfile
```

## 本地或服务器首次部署

1. 安装 Docker 和 Docker Compose。
2. 克隆本仓库。
3. 复制环境变量文件：

```bash
cp .env.example .env
```

Windows PowerShell 可以使用：

```powershell
Copy-Item .env.example .env
```

4. 编辑 `.env`，填写服务器自己的数据库密码、JWT_SECRET、DASHSCOPE_API_KEY。

5. 启动：

```bash
docker compose up -d --build
```

6. 查看容器状态：

```bash
docker compose ps
```

默认访问地址：

```text
前端：http://localhost:8081
后端：http://localhost:8080
MySQL：localhost:3307
```

## 日常更新代码后的部署

```bash
git pull
docker compose up -d --build
```

## 修改 API Key 或数据库密码

只需要修改服务器上的 `.env`，然后重启相关容器：

```bash
docker compose up -d --force-recreate backend
```

如果修改了 MySQL 用户、密码或初始化脚本，注意 Docker 数据卷已有旧数据库时不会自动重新初始化。

## 数据说明

- `backend/src/main/resources/schema.sql`：数据库表结构。
- `backend/src/main/resources/migration/knowledge_content_seed.sql`：演示养生文章数据。
- `backend/src/main/resources/demo-data.sql`：演示药膳数据。
- Docker 首次创建 MySQL 数据卷时，会自动导入这些初始化 SQL。
- 用户注册、AI 测试、后台新增内容属于运行数据，保存在 Docker MySQL 数据卷中，不会因为提交 GitHub 自动同步。

## 常用命令

启动：

```bash
docker compose up -d --build
```

停止：

```bash
docker compose down
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

进入 MySQL：

```bash
docker exec -it tcm-mysql mysql -utcm_user -p tcm_platform
```

检查环境变量是否传入后端，不要打印真实密钥：

```bash
docker exec tcm-backend sh -c 'if [ -n "$DASHSCOPE_API_KEY" ]; then echo DASHSCOPE_API_KEY_SET; else echo DASHSCOPE_API_KEY_EMPTY; fi'
```

## 不要提交的文件

```text
.env
node_modules/
dist/
target/
.idea/
.vscode/
```
