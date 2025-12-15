# SubRadar 📡

> 一个极简、隐私优先的订阅管理工具，采用 Material You 设计风格。

<p align="left">
  <img src="https://img.shields.io/github/v/release/yourusername/subradar?label=最新版本&style=flat-square&color=6366f1" />
  <img src="https://img.shields.io/github/actions/workflow/status/yourusername/subradar/android-release.yml?label=构建状态&style=flat-square" />
  <img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" />
</p>

SubRadar 旨在帮助您轻松管理 Netflix, Spotify, iCloud 等周期性支出。应用完全在本地运行，您的财务数据永远不会上传到任何服务器。

[English Documentation](./README.md)

## 📸 应用截图

| 亮色模式 | 深色模式 |
|:---:|:---:|
| <!-- 在此处添加截图链接 --> *Coming Soon* | <!-- 在此处添加截图链接 --> *Coming Soon* |

## ✨ 核心功能

*   **🔒 隐私至上**：无服务器、无追踪。所有数据仅存储在您设备的 LocalStorage 中。
*   **🎨 Material Design**：现代清爽的 UI 设计，完美支持 **深色模式 (Dark Mode)**。
*   **🔔 本地通知**：在续费当天发送本地推送通知，不再错过付款。
*   **📅 灵活周期**：支持按月、按季、按年，以及 **自定义周期**（例如：每 3 天、每 2 周）。
*   **💰 多币种支持**：支持 **人民币 (¥)** 和 **美元 ($)** 混合记账。
*   **📊 状态可视化**：
    *   **黄色**：今天到期（需立即付款）
    *   **红色**：已逾期
    *   **简洁**：即将到来
*   **📝 详细记录**：支持添加备注、记录账户余额以及上传附件图片（如发票截图）。

## 🤖 自动化双语发布流

本项目包含一个为独立开发者设计的 **GitHub Actions** 特色工作流：

1.  **零配置翻译**：构建时自动安装 `translate-shell`。
2.  **无需 API Key**：利用命令行工具调用 Google Translate 接口，无需申请付费 Key。
3.  **智能日志生成**：自动识别 Git 提交记录（Commit Log），将其从英文翻译为中文，并生成中英对照的 Release Note。

欢迎查看 `.github/workflows/android-release.yml` 参考实现！

## 📱 下载安卓 App

1.  访问本仓库的 **[Releases (发布页面)](../../releases)**。
2.  下载最新版本的 `SubRadar-vX.X.X.apk` 文件。
3.  传输到手机并安装。
    *   *注意：由于这是测试版签名 (Debug Build)，安装时手机可能会提示“未知来源风险”，允许安装即可。*

## 🚀 本地开发

### 环境要求
*   Node.js (v20+)
*   npm

### Web 开发
```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

### 安卓开发
如果您想在本地 Android Studio 中调试：

```bash
# 构建 Web 资源
npm run build

# 同步资源到安卓项目
npx cap sync

# 打开 Android Studio
npx cap open android
```

## 🛠 技术栈

*   **核心框架**: React 18, TypeScript, Vite
*   **样式库**: Tailwind CSS
*   **移动端容器**: Capacitor 6
*   **图标库**: Lucide React
*   **数据存储**: LocalStorage

## 📄 许可证

MIT © 2024
