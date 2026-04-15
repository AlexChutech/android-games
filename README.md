# Android 单机游戏合集

两个原生 Android 单机游戏，人机对战模式。

## 下载

直接下载 APK 安装到手机：

| 游戏 | 下载 |
|------|------|
| 五子棋 | [gomoku.apk](gomoku.apk) |
| 中国象棋 | [xiangqi.apk](xiangqi.apk) |

## 游戏

### 五子棋 (Gomoku)
- 15×15 标准棋盘
- AI 使用极大极小搜索 + Alpha-Beta 剪枝
- 支持悔棋功能
- 最后一步红圈标记

### 中国象棋 (Xiangqi)
- 完整的象棋规则实现
- AI 6层搜索深度 + 静态搜索
- 杀手启发 + 历史启发优化
- 将军检测
- 上一步橙色标记
- 支持悔棋

## 截图

| 五子棋 | 中国象棋 |
|:---:|:---:|
| ![五子棋](screenshots/gomoku.png?raw=true) | ![中国象棋](screenshots/xiangqi.png?raw=true) |

## 技术栈
- Kotlin
- Android SDK 34
- 自定义 View 绘制棋盘
- Coroutines 异步 AI 计算

## 从源码构建
用 Android Studio 打开对应项目目录即可编译运行。

```bash
# 或使用 Gradle 命令行
./gradlew assembleDebug
```

---

Made with ❤️ by Halo AI
