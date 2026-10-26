#!/bin/sh

# Check if Docker is installed and running
# 检查 Docker 是否已安装并正在运行
if ! command -v docker &> /dev/null; then
   echo "Error: Docker is not installed"
   echo "错误：Docker 未安装"
   exit 1
fi

# Check if Docker daemon is running
# 检查 Docker 守护进程是否运行
if ! docker info &> /dev/null; then
   echo "Error: Docker daemon is not running"
   echo "错误：Docker 守护进程未运行"
   exit 1
fi

# Check if Docker buildx is available
# 检查 Docker buildx 是否可用
if ! docker buildx version &> /dev/null; then
   echo "Error: Docker buildx is not available"
   echo "错误：Docker buildx 不可用"
   exit 1
fi

# Get Maven project version
# 获取 Maven 项目版本号
APP_VERSION=`./mvnw -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN -Dorg.slf4j.simpleLogger.log.org.apache.maven.plugins.help=INFO | tail -1`

# Check if version was successfully retrieved
# 检查版本号是否成功获取
if [ -z "$APP_VERSION" ]; then
   echo "Error: Failed to get application version"
   echo "错误：获取应用版本失败"
   exit 1
fi

# Print the version that will be used
# 打印将要使用的版本号
echo "Building version: ${APP_VERSION}"
echo "构建版本：${APP_VERSION}"

# Create and use a new builder instance
# 创建并使用新的构建器实例
echo "Setting up Docker buildx..."
echo "设置 Docker buildx..."
docker buildx create --use

# Check if Dockerfile exists
# 检查 Dockerfile 是否存在
if [ ! -f "Dockerfile" ]; then
   echo "Error: Dockerfile not found"
   echo "错误：未找到 Dockerfile"
   exit 1
fi

# Build multi-architecture images and push to Docker Hub
# - Platform: linux/amd64 (Intel/AMD) and linux/arm64 (ARM)
# - Tags: version specific and latest
# 构建多架构镜像并推送到 Docker Hub
# - 平台: linux/amd64 (Intel/AMD) 和 linux/arm64 (ARM)
# - 标签: 特定版本和最新版
echo "Building and pushing Docker images..."
echo "构建并推送 Docker 镜像..."
if docker buildx build --platform linux/amd64,linux/arm64 \
 -t devliveorg/metaforge:${APP_VERSION} \
 -t devliveorg/metaforge:latest \
 . --push; then
   # Print success message
   # 打印成功信息
   echo "Successfully built and pushed images:"
   echo "成功构建并推送以下镜像："
   echo "- devliveorg/metaforge:${APP_VERSION}"
   echo "- devliveorg/metaforge:latest"
else
   echo "Error: Failed to build and push Docker images"
   echo "错误：构建并推送 Docker 镜像失败"
   exit 1
fi