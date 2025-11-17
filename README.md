# SkyPilot 无人机控制应用

## 项目概述

SkyPilot 是一个基于 Android 平台的无人机控制应用，使用 Kotlin 语言开发。该应用集成了 DJI Mobile SDK V5，提供航点任务规划等无人机控制功能。

## 功能特性

- **航点任务**：支持航点任务规划与执行
- **地图集成**：集成 Mapbox 地图，提供直观的飞行路线展示
- **DJI 设备支持**：全面支持 DJI 无人机设备
- **现代化 UI**：采用 Material Design 设计语言

## 技术栈

- **开发语言**：Kotlin
- **最小 SDK 版本**：24 (Android 7.0)
- **目标 SDK 版本**：33 (Android 13)
- **构建工具**：Gradle
- **主要依赖**：
  - DJI Mobile SDK V5
  - Mapbox Android SDK 9.6.1
  - AndroidX 核心组件
  - Kotlin 标准库

## 项目结构

```
app/
├── src/
│   └── main/
│       ├── java/com/brainai/skypilot/
│       │   ├── MainActivity.kt      # 主活动，应用入口
│       │   ├── MyApplication.kt     # 应用类，负责全局初始化
│       │   └── pages/
│       │       └── WayPointV3.kt    # 航点任务功能
│       └── res/                     # 资源文件
```

## 快速开始

### 环境要求

- Android Studio Flamingo 或更高版本
- JDK 11 或更高版本
- Android SDK 33
- 一部支持 OTG 的 Android 设备（推荐）
- 一部 DJI 无人机（如 Mavic 3 系列）

### 安装步骤

1. 克隆仓库到本地：
   ```bash
   git clone https://github.com/yourusername/SkyPilot.git
   ```

2. 使用 Android Studio 打开项目

3. 同步 Gradle 项目

4. 连接 Android 设备或启动模拟器

5. 运行应用

### 配置说明

1. **DJI SDK 配置**：
   - 确保已正确配置 DJI 开发者账号和 App Key
   - 在 `AndroidManifest.xml` 中配置必要的权限和元数据

2. **Mapbox 配置**：
   - 在 `res/values/strings.xml` 中添加 Mapbox 访问令牌

## 使用说明

1. 启动应用后，主界面将显示可用功能入口
2. 点击"航点任务"按钮进入航点规划界面
3. 在地图上添加航点并设置任务参数
4. 连接无人机后执行任务

## 依赖项

- DJI Mobile SDK V5.3.0
- Mapbox Android SDK 9.6.1
- AndroidX 核心组件
- Kotlin 标准库 1.7.21
- Lottie 动画库 3.3.1

## 构建与发布

### 构建调试版本

```bash
./gradlew assembleDebug
```

### 构建发布版本

1. 配置签名信息
2. 运行：
   ```bash
   ./gradlew assembleRelease
   ```

## 贡献指南

欢迎提交 Issue 和 Pull Request。在提交代码前，请确保：

1. 代码符合 Kotlin 编码规范
2. 添加适当的注释和文档
3. 通过所有测试

## 许可证

```
Copyright 2023 BrainAI

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
