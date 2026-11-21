# ADropX

High-Speed Offline Local File Sharing System for Windows and Mobile Devices.

ADropX is a free, zero-cloud offline local file transfer platform designed to move photos, 4K videos, documents, music, and complete directories between personal computers and mobile devices at maximum network speeds with zero internet consumption.

---

## Table of Contents

- Overview
- Why ADropX
- Download Links
- How to Connect
- Comprehensive Feature Guide
- Android Companion App
- System Requirements
- Security and Privacy Guarantee
- Project Architecture
- Developer Information and License

---

## Overview

Transferring files between a Windows computer and a mobile device has historically required USB cables, cloud uploads, or messaging apps. These approaches suffer from severe bandwidth limitations, compression artifacts, privacy exposure, or slow pairing processes.

ADropX solves this problem by turning your computer into a high-speed local streaming node. By leveraging your existing local WiFi connection, phone hotspot, or USB tethering, ADropX transfers data directly from device to device at hardware speeds reaching up to 150+ MB/s.

Everything happens strictly within your local environment. Zero bytes are uploaded to external servers, and no internet connection is required.

---

## Why ADropX

Traditional file transfer methods have clear drawbacks:

1. Messaging Apps: Services such as WhatsApp and Telegram re-encode images and videos, causing noticeable quality loss and consuming your cellular data plan.
2. Cloud Storage: Services such as Google Drive, OneDrive, and Dropbox require uploading files first and then downloading them again on the target device. A 5 GB video requires 10 GB of total internet bandwidth.
3. Bluetooth: Maximum transfer speeds hover between 1 MB/s and 2 MB/s, making large video transfers impractically slow.
4. USB Cables: Cables require physical connection, driver recognition, and file system navigation.
5. Proprietary Protocols: Solutions like Apple AirDrop do not provide native support across Windows and Android devices.

ADropX delivers an open, platform-agnostic alternative with zero data compression, zero internet dependency, and immediate zero-setup connectivity.

---

## Download Links

Choose the package that best fits your workflow:

| Package | Format | File Size | Description |
| :--- | :--- | :--- | :--- |
| **ADropX Setup** (Recommended) | `.exe` | ~44.5 MB | Native high-speed Windows installer. Starts in 0.05 seconds, creates Desktop and Start Menu shortcuts, registers clean Windows uninstaller, installs to local user profile with no administrator prompt. |
| **ADropX Standalone** | `.exe` | ~43.9 MB | Single standalone executable file. No installation required. Double-click to run immediately from any folder. |
| **ADropX Portable** | `.zip` | ~44.1 MB | Pre-extracted portable archive. Ideal for USB flash drives, external SSDs, and restricted work computers. |
| **ADropX Android Client** | `.apk` | ~7.3 MB | Native Android companion application with built-in camera QR scanner, background transfer locks, and direct download folder integration. |

All downloads are accessible from the official release repository:
https://github.com/BeginnerAman/ADropX/releases/latest

---

## How to Connect

Connecting your phone to your computer takes less than five seconds.

### Step 1: Network Connection
Ensure both devices are on the same local network. Any of the following configurations work:
- Home or Office WiFi: Connect your PC and your phone to the same WiFi router.
- Mobile Hotspot: Turn on your phone's personal hotspot and connect your PC to it. No active internet or data pack is required.
- PC Hotspot: Turn on Windows Mobile Hotspot on your PC and connect your phone to it.
- USB Tethering: Connect your phone to your PC via USB cable and enable USB Tethering in phone settings.

### Step 2: Launch ADropX on PC
Double-click `ADropX-Setup.exe` (or `ADropX.exe`). The desktop dashboard opens automatically in a dedicated dark window, displaying a unique connection QR code.

### Step 3: Scan and Transfer
- Using Camera: Open your phone camera or web browser, point it at the QR code on your PC screen, and tap the link.
- Using Android App: Open the ADropX companion app, tap Scan QR, and pair instantly.

The transfer interface appears immediately in your phone browser or native app. You can now drag and drop files from your PC or select media on your phone to transfer.

---

## Comprehensive Feature Guide

### Maximum LAN Speed Transfer Engine
ADropX utilizes an optimized multi-threaded chunked socket streaming pipeline. Because data travels strictly over local network hardware, transfer speeds can exceed 150+ MB/s on 5 GHz WiFi 6 and Gigabit networks, limited only by your router and device storage speed.

### Zero Mobile App Requirement (Universal PWA)
Recipients do not need to install any application. The transfer dashboard operates as a Progressive Web App (PWA) compatible with:
- Android (Chrome, Firefox, Samsung Internet, Brave)
- iOS / iPadOS (Safari)
- macOS and Linux (Chrome, Firefox, Safari, Edge)

### In-Browser Instant Media Streaming
Large 4K videos, audio files, and PDFs can be streamed directly inside the browser using HTTP Range Request headers. You can scrub through video timelines immediately without waiting for the full file to download first.

### Smart Clipboard Synchronization
Share text, web links, passwords, and notes between devices instantly. ADropX features an intelligent text engine that recognizes:
- Web URLs: Provides a one-tap Open Link button.
- One-Time Passwords (OTPs): Highlights 4-digit, 6-digit, and 8-digit verification codes with a one-tap Copy Code button.
- Phone Numbers: Provides a one-tap Dial chip on mobile devices.
- Email Addresses: Provides a one-tap Compose action.

### Resumable File Transfers
If a transfer is interrupted by a brief WiFi signal fluctuation or accidental screen lock, ADropX automatically records transfer chunk offsets and resumes transferring from the exact byte where it paused, avoiding repeated data transfers.

### RAM-Safe Streaming Architecture
Unlike naive file transfer tools that load entire files into system memory (RAM), ADropX processes data through fixed 2 MB to 4 MB buffer pipelines. System RAM consumption remains completely flat whether transferring a 200 KB image or a 60 GB Blu-ray video file.

### Batch Folder and Multiple File Archive
Select dozens of individual files or complete directory hierarchies on PC. Recipients can download all items in a single click as an uncompressed or standard ZIP archive.

### PIN Code Security Layer
For shared public networks (such as coffee shops, universities, or co-working offices), enable the PIN protection toggle in Settings. Connected devices will be required to input a matching 4-digit security PIN before access is granted.

### Dark OLED Glassmorphic Interface
Crafted with high-contrast OLED black surfaces (`#0a0a1a`), subtle frosted glass borders, and custom typography to minimize eye strain and screen battery consumption.

### Native Windows Integration
- Starts with zero white flash on both Windows 10 and Windows 11.
- Automatically handles OneDrive Desktop and standard desktop path redirection.
- Registers cleanly in Windows Settings under Installed Applications with versioning, developer attribution, and a one-click uninstaller.

---

## Android Companion App

While the browser interface requires zero setup, the native Android companion APK provides enhanced convenience for daily power users:

- Built-In Fast QR Scanner: Opens directly to a camera viewfinder with haptic vibration upon connection.
- Background WakeLock: Prevents Android battery optimization services from sleeping WiFi connections during multi-gigabyte background transfers.
- Native System Download Manager: Saves received photos, videos, and archives directly into the Android public `Download/` folder.
- Native File Chooser: Supports multi-selection from Android Gallery, Google Photos, and system storage documents.

Download `ADropX.apk` directly from the release page or through the desktop dashboard by clicking the mobile app banner.

---

## System Requirements

### Personal Computer (Desktop Host)
- Operating System: Microsoft Windows 10 or Windows 11 (64-bit architecture).
- Memory: Minimum 512 MB available RAM.
- Network Interface: WiFi adapter, Ethernet connection, or USB tethering capability.
- Storage: 100 MB available disk space for application files.

### Mobile Device (Client)
- Android: Android 8.0 or higher (for native companion APK), or any modern web browser.
- iOS: iOS 13.0 or higher (Safari, Chrome).
- macOS / Linux: Any standards-compliant modern browser.

---

## Security and Privacy Guarantee

ADropX is engineered from the ground up on zero-trust privacy principles:

1. 100% Local Processing: All file streaming occurs directly between your PC and phone network interface cards.
2. Zero Cloud Telemetry: No analytics trackers, no advertising SDKs, and no external metrics collection.
3. No Account Creation: No sign-ups, no email addresses, and no passwords required.
4. Temporary Session Memory: Network tokens and temporary sessions expire automatically upon closing the application.

---

## Project Architecture

- Desktop Server Engine: Python 3 with FastAPI and Uvicorn asynchronous event loops.
- Local Network Discovery: Zeroconf multicast DNS (mDNS) daemon.
- Desktop Native Shell: Frameless Microsoft Edge Chromium WebView2 window.
- Desktop Native Setup: Hardware-accelerated C# WPF installer utilizing DirectWrite rendering.
- Frontend Client: HTML5, CSS3 Custom Properties with frosted glassmorphism tokens, and Vanilla JavaScript Web Workers.
- Android Client: Native Android Studio project with WebChromeClient hardware acceleration.

---

## Developer Information and License

ADropX is developed and maintained by:

**Aman Vishwakarma**  
GitHub: https://github.com/BeginnerAman

License: Free for personal, commercial, and educational use. Developed by Aman Vishwakarma.
