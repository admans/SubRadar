# SubRadar 📡

> A minimalist, privacy-first subscription tracker with Material You aesthetics.

<p align="left">
  <img src="https://img.shields.io/github/v/release/yourusername/subradar?label=Latest%20Version&style=flat-square&color=6366f1" />
  <img src="https://img.shields.io/github/actions/workflow/status/yourusername/subradar/android-release.yml?label=Build&style=flat-square" />
  <img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" />
</p>

SubRadar helps you manage your recurring expenses (Netflix, Spotify, SaaS, etc.) efficiently. It runs entirely in your browser or on your Android device, keeping your financial data 100% local.

[中文文档](./README_CN.md)

## 📸 Screenshots

| Light Mode | Dark Mode |
|:---:|:---:|
| <!-- Add screenshot here e.g. ./docs/light.png --> *Coming Soon* | <!-- Add screenshot here --> *Coming Soon* |

## ✨ Key Features

*   **🔒 Privacy First**: No servers, no tracking. All data lives in your device's LocalStorage.
*   **🎨 Material You Design**: Adaptive UI that feels native, with full **Dark Mode** support.
*   **🔔 Smart Notifications**: Get local alerts on your device when a bill is due today.
*   **📅 Flexible Cycles**: Supports Monthly, Quarterly, Yearly, and **Custom** cycles (e.g., every 3 weeks).
*   **💰 Multi-Currency**: Seamlessly track expenses in **USD ($)** and **CNY (¥)**.
*   **📊 Visual Dashboard**:
    *   **Yellow**: Due Today (Action required)
    *   **Red**: Overdue
    *   **Clean**: Upcoming
*   **📝 Rich Details**: Add notes, account balances, and attach images (e.g., invoices) to subscriptions.

## 🤖 Automated Bilingual Releases

This project features a unique **GitHub Actions** workflow designed for solo developers:

1.  **Zero-Config Translation**: Uses `translate-shell` to automatically translate Git commit messages from English to Chinese.
2.  **No API Keys Required**: Leverages Google Translate via CLI without needing a paid API token.
3.  **Smart Changelog**: Automatically identifies the range between the last tag and current HEAD to generate clean, relevant release notes.

Check out `.github/workflows/android-release.yml` to see how it works!

## 📱 Download Android APK

1.  Go to the **[Releases](../../releases)** page.
2.  Download the latest `SubRadar-vX.X.X.apk`.
3.  Install on your Android device.
    *   *Note: Since this is a debug build, your phone may ask for permission to install from unknown sources.*

## 🚀 Local Development

### Prerequisites
*   Node.js (v20+)
*   npm

### Web
```bash
# Install dependencies
npm install

# Start dev server
npm run dev
```

### Android
To build and debug in Android Studio:

```bash
# Build web assets
npm run build

# Sync with Capacitor
npx cap sync

# Open Android Studio
npx cap open android
```

## 🛠 Tech Stack

*   **Core**: React 18, TypeScript, Vite
*   **Styling**: Tailwind CSS (with custom font intergration)
*   **Mobile Engine**: Capacitor 6
*   **Icons**: Lucide React
*   **State/Storage**: React Hooks + LocalStorage

## 📄 License

MIT © 2024
