# Android 单机游戏合集

两个原生 Android 单机游戏，人机对战模式。AI 经过深度优化，棋力强劲。

## 下载

直接下载 APK 安装到手机：

| 游戏 | 下载 |
|------|------|
| 五子棋 | [gomoku.apk](gomoku.apk) |
| 中国象棋 | [xiangqi.apk](xiangqi.apk) |

## 游戏

### 五子棋 (Gomoku)
- 15×15 标准棋盘
- **AI 搜索深度 8 层**（带置换表优化）
- 置换表 + Zobrist 哈希
- 威胁检测（双活三、冲四必胜）
- 开局库
- 支持悔棋功能
- 最后一步红圈标记

### 中国象棋 (Xiangqi)
- 完整的象棋规则实现
- **AI 搜索深度 8 层**（带置换表优化）
- 置换表 + Zobrist 哈希
- 空步裁剪（Null Move Pruning）
- 静态搜索（Quiescence Search）
- 开局库
- 将军检测
- 上一步橙色标记
- 支持悔棋

## AI 技术细节

| 技术 | 五子棋 | 中国象棋 |
|------|:---:|:---:|
| 搜索深度 | 8层 | 8层 |
| 置换表 | ✅ | ✅ |
| Zobrist 哈希 | ✅ | ✅ |
| 迭代加深 | ✅ | ✅ |
| Alpha-Beta 剪枝 | ✅ | ✅ |
| 杀手启发 | ✅ | ✅ |
| 历史启发 | ✅ | ✅ |
| 静态搜索 | - | ✅ |
| 空步裁剪 | - | ✅ |
| 开局库 | ✅ | ✅ |
| 威胁检测 | ✅ | - |

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
