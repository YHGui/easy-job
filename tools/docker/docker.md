# Docker

## 什么是Docker？

容器化平台

## 为什么Docker

- 部署服务是挑战：复杂多变的技术栈，不同技术之间的版本冲突和依赖

## Docker思想

- 集装箱
- 标准化
  - 运输方式
  - 存储方式
  - API接口
- 隔离（LXC）
- 同时交付并部署程序运行所需的环境
- 仅仅打包必需的内容

## 解决问题

- 本地运行没问题啊！运行环境不一致的问题。
- container之间独立，限定资源，防止影响其他服务
- 快速扩张，弹性伸缩变得简单。

## 走进Docker 

- image：一系列的文件，运行的文件和运行环境的文件，文件系统。镜像除了顶层，其他每一层都是只读的。
- 容器：一个进程。分层只读使得可以建立多个容器运行，不干扰。
- docker仓库：将镜像传到仓库，其他机器要运行需要pull下镜像来建立容器运行。
- 命令：
  - docker images
  - docker run ubuntu:12.10 /bin/bash
  - docker ps
  - docker diff
  - docker commit
  - docker push
  - docker pull