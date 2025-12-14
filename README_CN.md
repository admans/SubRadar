# SubRadar 📡

> 一个极简的订阅管理工具，采用 Material You 设计风格，帮助您轻松管理周期性支出和续费日期。

SubRadar 旨在以简单、隐私优先的方式帮助您追踪订阅服务（如 Netflix, Spotify, iCloud 等）。它拥有清晰的界面，核心目标是让您一眼看清“接下来需要支付什么”。

[English Documentation](./README.md)

## ✨ 功能特点

- **隐私优先**：所有数据仅存储在您的设备本地。无服务器，无追踪。
- **Material You 设计**：现代、清爽的视觉风格。
- **智能排序**：自动按“下次扣费日期”排序，紧急事项置顶。
- **状态可视化**：
  - 🟡 **黄色**：今天到期（需立即付款）
  - 🔴 **红色**：已逾期
  - ⚪ **简洁**：即将到来
- **多币种支持**：支持人民币 (¥) 和 美元 ($)。
- **灵活记录**：支持按月/按年计费，提供备注功能。

## 📱 下载安卓 App

本项目通过 GitHub Actions 自动构建安卓 APK 安装包。

1. 访问本仓库的 **[Releases (发布页面)](../../releases)**。
2. 下载最新版本下的 `app-debug.apk` 文件。
3. 传输到手机并安装。
   * *注意：由于这是测试版签名 (Debug Build)，安装时手机可能会提示“未知来源风险”，这是正常现象，允许安装即可。*

## 🚀 本地开发

### 环境要求
- Node.js (v18 或更高)
- npm

### Web 开发
```bash
npm install
npm run dev
```

### 安卓开发 (可选)
如果您想在本地 Android Studio 中调试：

1. 构建 Web 资源：`npm run build`
2. 初始化安卓平台：`npx cap add android`
3. 同步资源：`npx cap sync`
4. 打开编辑器：`npx cap open android`

## 🛠 技术栈

- **React 18** + **Vite**
- **Tailwind CSS**
- **Capacitor** (用于打包安卓应用)
- **Lucide React**

## 📄 许可证

MIT