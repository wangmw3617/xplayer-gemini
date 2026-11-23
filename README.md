# XPlayer

<div align="center">

**XPlayer** · Android 原生双内核高清视频播放器（Media3 + libmpv）

[![Android CI & Release](https://github.com/placeholder/XPlayer/actions/workflows/android-ci.yml/badge.svg)](https://github.com/placeholder/XPlayer/actions)
![Target Android](https://img.shields.io/badge/Android-10%2B%20%28API%2029%2B%29-3DDC84.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-35-blue.svg)
![Language](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg)
![UI](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)
![Dual Kernel](https://img.shields.io/badge/Dual%20Kernel-Media3%20%2B%20libmpv-orange.svg)

</div>

---

## 📌 项目定位与核心特性

XPlayer 是一款专为 Android 打造的高性能、现代设计风格的双内核视频播放器：
* **双内核架构与智能路由**：
  * **Media3（ExoPlayer + FFmpeg 扩展）**：优先调用系统硬件解码器，原生支持 HDR10/HDR10+、Dolby Vision（杜比视界硬件点亮），通过 FFmpeg 扩展音频软解 DTS / TrueHD / EAC3 / AC3。
  * **libmpv 兜底与进阶**：支持 ASS/SSA 复杂特效字幕渲染、外部 GLSL 着色器（如 Anime4K 超分）、高级 `mpv.conf` 配置参数。
  * **智能路由策略**：默认 Media3 优先，遇不支持编码或 ASS 特效字幕时自动静默切换至 libmpv；支持播放页顶部一键热切换并保持播放进度。
* **FlClash 安卓端视觉体系**：
  * 精准复刻 FlClash 安卓端经典 4-Tab 底部导航栏（Pill Indicator 动效、SurfaceContainer 柔和底色）。
  * 深度折叠 `LargeTopAppBar` 与分组卡片式设置体系，设置项实时副标题回显。
  * 完整支持 Material You (Monet) 动态壁纸取色与 **纯黑 AMOLED 模式**。
* **全自定义专业级手势系统**：
  * 左侧垂直滑动调节亮度、右侧垂直滑动调节音量（支持 200% 软件音量增益）。
  * 水平滑动 Seek 跳转（支持精准帧定位与自定义灵敏度）。
  * 双击分区快进快退（自定义步长 5s/10s/15s/30s）。
  * 长按倍速播放（1.5x - 4.0x）与长按左右滑动无级调速。
  * 双指无级缩放（50% - 400%）与拖拽平移，边缘防误触死区。
* **进阶播放能力**：
  * 当前帧高清截图（保存至系统相册）。
  * 画中画（PiP）模式自适应宽高比。
  * 后台纯音频播放与系统通知栏媒体卡片。
  * 本机硬件解码体检面板（芯片能力探测与杜比视界/HDR 支持一览）。
* **零本地编译与 CI 自动发包**：
  * 无需本地配置 Android SDK/NDK，全流程由 GitHub Actions CI 自动编译并分发 `arm64-v8a` APK。

---

## 🏗️ 架构分层

```text
app/src/main/java/com/xplayer/app/
├── data/
│   ├── local/          # Room 数据库（视频元数据、播放历史、屏蔽目录、着色器、直链历史）
│   ├── datastore/      # Preferences DataStore（主题、内核策略、手势配置、字幕样式）
│   ├── scanner/        # 本地媒体扫描器、MediaMetadataRetriever、外挂字幕匹配器
│   └── source/         # MediaSource 抽象层（LocalFileSource, NetworkUrlSource；预留 V2）
├── player/
│   ├── engine/         # PlayerEngine 统一接口、Media3EngineImpl、MpvEngineImpl、EngineRouter
│   ├── codec/          # MediaCodecList 本机硬件解码能力探测工具
│   ├── service/        # 后台音频播放 MediaSessionService 与通知栏控制
│   └── pip/            # 画中画（PiP）状态控制器
├── ui/
│   ├── theme/          # Material 3 主题、莫奈动态取色、AMOLED 纯黑配色方案
│   ├── navigation/     # 4-Tab 底部导航栏（复刻 FlClash）与路由表
│   ├── home/           # 首页（继续观看、最近播放、网络直链入口）
│   ├── videos/         # 全部视频页
│   ├── folders/        # 文件夹视图页
│   ├── settings/       # 设置体系（复刻 FlClash 风格，含二级设置页与 mpv 实验室）
│   └── player/         # 统一播放页 UI、手势检测层、底部弹窗面板
└── XPlayerApp.kt
```

---

## 🚀 云端构建与安装

1. 将本仓库推送至 GitHub。
2. 触发 Actions 流水线（Push/PR 或手动在 Actions 页面点击 `Run workflow`）。
3. 构建完成后，在 **Artifacts** 中下载 `XPlayer-arm64-v8a-debug.apk` 安装到手机。
4. 推送 `v*` Tag（如 `git tag v1.0.0 && git push origin v1.0.0`）会自动发布 GitHub Release。
