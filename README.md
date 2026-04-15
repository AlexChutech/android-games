# Android 单机游戏合集

两个原生 Android 单机游戏，人机对战模式。

## 游戏

### 五子棋 (Gomoku)
- 15×15 标准棋盘
- AI 使用极大极小搜索 + Alpha-Beta 剪枝
- 支持悔棋功能
- 最后一步红圈标记

### 中国象棋 (Xiangqi)
- 完整的象棋规则实现
- AI 4层搜索深度
- 将军检测
- 上一步橙色标记
- 支持悔棋

## 技术栈
- Kotlin
- Android SDK 34
- 自定义 View 绘制棋盘
- Coroutines 异步 AI 计算

## 构建
用 Android Studio 打开对应项目目录即可编译运行。

```bash
# 或使用 Gradle 命令行
./gradlew assembleDebug
```

## 截图

游戏运行在 Android 模拟器上的效果良好。

---

Made with ❤️ by Halo AI
