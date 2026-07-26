# SubRadar

SubRadar 现在已经改写为 Kotlin 原生 Android 应用。

它用于记录订阅服务、续费日期、账户余额、备注、图片附件、语言偏好、主题偏好和本地提醒设置。应用没有服务端，数据只保存在设备本机。

## 功能

- Kotlin 原生 Android
- Jetpack Compose 界面，并接入 Miuix
- 使用 Android 本地偏好存储 JSON 数据
- 支持按月、按季、按年和自定义周期
- 支持人民币和美元混合统计
- 支持余额自动扣款和续费日期自动滚动
- 支持搜索、编辑、删除和一键续费
- 支持本地续费提醒
- 支持明亮、深色和跟随系统主题
- 支持中文和英文界面
- 支持图片附件

## 构建

需要：

- JDK 17
- Android SDK
- Gradle 8.11.1 或更新版本

构建调试 APK：

```bash
gradle assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 发布

GitHub Actions 会直接构建原生 Android 项目。推送 `v2.0.0.2` 这样的标签，或手动运行工作流即可。

## 技术栈

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Miuix for Compose
- Android 本地通知

## 许可证

MIT
