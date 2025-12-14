# SubRadar 📡

> A minimalist subscription tracker designed to help you manage recurring expenses and payment dates with a Material You aesthetic.

SubRadar helps you track your subscriptions (Netflix, Spotify, iCloud, etc.) in a simple, privacy-focused way.

[中文文档](./README_CN.md)

## ✨ Features

- **Privacy First**: All data is stored locally in your browser/device. No servers.
- **Material You Design**: Modern, clean aesthetic.
- **Smart Sorting**: Automatically sorts subscriptions by the next billing date.
- **Visual Status**:
  - 🟡 **Yellow**: Due Today (Pay now!)
  - 🔴 **Red**: Overdue
  - ⚪ **Clean**: Upcoming
- **Multi-Currency**: Supports USD ($) and CNY (¥).
- **Flexible Billing**: Monthly/Yearly cycles with Notes support.

## 📱 Download Android App

This project automatically generates an Android APK using GitHub Actions.

1. Go to the **[Releases](../../releases)** page.
2. Download the `app-debug.apk` file from the latest version.
3. Install it on your Android phone.
   * *Note: You may receive a security warning because this is a "Debug" build not from the Play Store. This is normal.*

## 🚀 Development

### Prerequisites
- Node.js (v18+)
- npm

### Web Development
```bash
npm install
npm run dev
```

### Android Development (Optional)
To run the app in Android Studio locally:

1. Build the web app: `npm run build`
2. Add Android platform: `npx cap add android`
3. Sync assets: `npx cap sync`
4. Open Android Studio: `npx cap open android`

## 🛠 Tech Stack

- **React 18** + **Vite**
- **Tailwind CSS**
- **Capacitor** (For Android packaging)
- **Lucide React**

## 📄 License

MIT