# ADropX

High-Speed Offline Local File Transfer Platform for Windows and Android.

ADropX is a high-performance, zero-cloud local file transfer platform created by Aman Vishwakarma. It enables bidirectional file sharing, folder archiving, 4K media streaming, and real-time clipboard synchronization between Windows computers and mobile devices at maximum physical network speeds without consuming internet bandwidth.

Official Website: https://beginneraman.github.io/ADropX/  
GitHub Releases: https://github.com/BeginnerAman/ADropX/releases/latest

---

## Table of Contents

- 1. Executive Summary
- 2. Performance Benchmarks
- 3. Core Platform Features
- 4. Windows Desktop Power Suite
- 5. Competitive Matrix: ADropX vs Alternatives
- 6. Official Packages and Downloads
- 7. Connection Methods and Setup Guide
- 8. Android Companion Client vs Web Mode
- 9. Smart Clipboard and OTP Engine
- 10. Technical Architecture and Protocols
- 11. Security, Privacy, and Network Isolation
- 12. Troubleshooting and Network Diagnostics
- 13. System Requirements
- 14. Developer Attribution and Terms of Use

---

## 1. Executive Summary

Moving files between a Windows PC and mobile devices has traditionally suffered from friction, slow transfer speeds, and unnecessary privacy risks:

- Messaging applications (such as WhatsApp or Telegram) re-encode photos and videos with lossy compression, degrade audio bitrates, and burn mobile data quotas.
- Cloud storage providers (such as Google Drive, OneDrive, or Dropbox) require two complete network cycles: first uploading to a remote datacenter, then downloading back to the second device. A 5 GB file uses 10 GB of internet data.
- Bluetooth transfers remain bottlenecked between 1 MB/s and 2.5 MB/s, making video transfers impractical.
- USB cables require physical tethering, specific OEM drivers, and tedious file-manager navigation.
- Ecosystem-locked solutions (such as Apple AirDrop) do not bridge Windows and Android hardware.

ADropX transforms your computer into a localized, high-throughput network host. Using your local WiFi network, mobile phone hotspot, or USB tethering, ADropX streams data directly between device network interface cards (NICs) at physical link speeds up to 150+ MB/s.

No internet connection is required. No account registration is needed. No telemetry data is collected.

---

## 2. Performance Benchmarks

ADropX is engineered to saturate local hardware bandwidth. Transfer speed is bounded only by the physical capability of your network router, wireless frequency band, and storage drive write speeds.

### Real-World Transfer Speed Comparison

| Network Mode / Medium | ADropX Transfer Speed | Time for 1 GB File | Time for 10 GB 4K Video |
| :--- | :--- | :--- | :--- |
| **5 GHz WiFi 6 (802.11ax)** | 120 - 165 MB/s | ~6.5 seconds | ~65 seconds |
| **5 GHz WiFi 5 (802.11ac)** | 70 - 110 MB/s | ~10.5 seconds | ~1.7 minutes |
| **Mobile Hotspot (5 GHz band)** | 45 - 75 MB/s | ~16 seconds | ~2.5 minutes |
| **USB Tethering (USB 3.0)** | 50 - 85 MB/s | ~14 seconds | ~2.2 minutes |
| **2.4 GHz WiFi (802.11n)** | 22 - 32 MB/s | ~38 seconds | ~5.8 minutes |
| **Bluetooth 5.0 (Standard)** | 1.2 - 2.1 MB/s | ~8.5 minutes | ~85 minutes |
| **Cloud Storage (100 Mbps ISP)** | 10 - 12 MB/s (x2 cycles) | ~2.8 minutes | ~28 minutes |

All benchmarks were measured in real-world local environments transferring uncompressed video archives and raw image batches.

---

## 3. Core Platform Features

### 1. High-Throughput Socket Pipeline
ADropX utilizes an asynchronous multi-threaded streaming pipeline built on non-blocking I/O loops. File chunks are dispatched across persistent TCP sockets with dynamic buffer allocation, enabling sustained high-speed transfers reaching 150+ MB/s without throughput stutter.

### 2. Native Hardware-Accelerated C# WPF Installer
The Windows setup program (`ADropX-Setup.exe`) is compiled with Microsoft .NET DirectWrite hardware acceleration:
- Starts in 0.05 seconds with zero white flash.
- Operates without administrator (UAC) prompts, installing to the local user profile directory (`%LOCALAPPDATA%\Programs\ADropX`).
- Dual Desktop Shortcut resolution automatically handles standard Desktop and OneDrive Cloud redirected Desktop directories.
- Registers a clean uninstaller in Windows Settings under Installed Apps with developer attribution and version tags.

### 3. Dedicated Android Companion App (v3.3.1)
The official native Android client (`ADropX.apk`) provides convenience features for power users:
- Viewfinder Camera Scanner: Launches immediately into a camera viewfinder with 50ms haptic vibration upon pairing.
- Background WakeLock Engine: Keeps CPU and WiFi chipsets awake during multi-gigabyte transfers, preventing Android from pausing transfers when the phone screen turns off.
- System Download Folder Routing: Automatically routes saved files into the native Android public `Download/` folder.
- Gallery Multi-Picker: Select hundreds of photos or videos simultaneously from Android storage.

### 4. Zero-Install Progressive Web App (Universal Mode)
Devices without the native companion app connect through their web browser. The transfer interface runs as an offline PWA compatible with:
- Apple iPhone and iPad (Safari on iOS / iPadOS)
- Android devices (Chrome, Samsung Internet, Firefox, Brave)
- Mac, Linux, and secondary Windows PCs (Chrome, Firefox, Safari, Edge)

### 5. Smart Clipboard with Automated Entity Detection
ADropX includes an intelligent two-way clipboard synchronization engine. When text is shared between devices, the parser automatically identifies content types and provides dedicated one-tap action chips:
- One-Time Passwords (OTPs): Detects 4-digit, 6-digit, and 8-digit verification codes and surfaces a one-tap "Copy Code" chip.
- Web URLs: Detects links and surfaces a one-tap "Open Link" button.
- Phone Numbers: Detects telephone numbers and surfaces a one-tap "Dial" button on mobile devices.
- Email Addresses: Detects emails and surfaces a one-tap "Compose" button.

### 6. In-Browser 4K Video and Audio Streaming
ADropX implements HTTP Range Request headers (`bytes=start-end`), allowing phones and connected computers to stream 4K MKV, MP4, WebM, and FLAC media directly in the browser. Users can scrub forward and backward on the playback timeline instantly without waiting for the full file to download first.

### 7. Resumable Chunk Transfers
Network fluctuations and momentary signal drops do not require restarting file transfers from scratch. The client tracks chunk byte offsets; if a connection pauses, transferring resumes from the exact byte where it left off.

### 8. RAM-Safe Fixed Buffer Architecture
Many file transfer utilities buffer entire files in system RAM before writing to disk, leading to memory exhaustion or browser crashes on large files. ADropX enforces fixed 2 MB to 4 MB streaming buffers. Memory consumption remains completely flat (under 15 MB) whether moving a 50 KB image or an 80 GB Blu-ray disk image.

### 9. Batch Folder and Multi-File ZIP Creation
Users can select individual files or entire directory trees on the desktop. The recipient can download all selected items in a single package as an uncompressed or standard ZIP archive.

### 10. PIN Code Access Protection
When operating on public or shared networks (such as coffee shops, shared student dorms, or corporate offices), the desktop host can activate PIN Security in Settings. Any connecting phone or computer must enter the matching 4-digit PIN code before viewing or uploading files.

### 11. Dark OLED Glassmorphic User Interface
Designed with deep `#0a0a1a` OLED black surfaces, frosted glass translucent cards, indigo `#7c83ff` accent highlights, and crisp Lucide vector icons. The interface reduces battery consumption on OLED mobile displays and prevents eye fatigue during evening use.

### 12. 100% Private and Local Network Isolation
All file streams travel strictly between your PC and phone hardware interfaces. Zero cloud servers are involved, zero user accounts are required, and zero telemetry or analytics are collected.

---

## 4. Windows Desktop Power Suite

The desktop executable (`ADropX.exe` and `ADropX-Setup.exe`) incorporates a dedicated suite of desktop features engineered specifically for Windows productivity:

### 1. Multiple Watched Folders Engine
ADropX can monitor multiple local directories simultaneously (such as your `Downloads`, `Photos`, or work folders). When files are added or modified in any watched directory, the background filesystem hooks automatically detect the changes and broadcast the new files to connected devices in real time.
- Configuration: Open the Folders Drawer from the top bar to add or remove monitored directories.
- Default Behavior: Automatically monitors the user Downloads directory upon initial launch.

### 2. System Tray Background Mode
When the main window close button is pressed, ADropX does not abruptly terminate ongoing transfers. Instead, it minimizes quietly into the Windows system notification tray.
- Context Menu: Right-click the system tray icon to view active status, open the dashboard, or cleanly exit the application.
- Notifications: Displays a balloon toast notification when minimized to confirm transfers continue in the background.

### 3. Windows Explorer Right-Click Menu Integration
Users can share files and folders directly from the Windows operating system shell.
- How It Works: Right-click any file or directory in Windows File Explorer and select "Share with ADropX".
- Action: The selected items are immediately staged into the ADropX transfer registry and made accessible to connected devices without manual file picking.

### 4. Windows Startup Auto-Launch
A single toggle in Desktop Settings allows ADropX to launch automatically whenever you log into Windows (`HKCU\Software\Microsoft\Windows\CurrentVersion\Run`). This ensures your local sharing hub is perpetually accessible without manual execution.

### 5. Custom Frameless OLED Title Bar
Replaces the standard Windows operating system title bar with an integrated, dark glassmorphism title bar matching the ADropX OLED design:
- Full Draggable Region: Move the window naturally across multiple monitors.
- Window Controls: Custom Minimize (`_`), Maximize/Restore (`□`), and Close (`✕` with red hover accent) buttons.
- Browser Adaptive: Hides itself automatically when accessed through a web browser, rendering only inside the desktop application shell.

### 6. Connected Devices Inspector
A dedicated sliding drawer displays all active clients on your local network:
- Details: Shows client IP addresses, device hostnames, user agent details, active connection state, and last-seen timestamps.
- Live Top Bar Pill: Displays a real-time badge showing the exact number of actively connected devices.

### 7. Recursive Directory Drag-and-Drop Traversal
Drop complete nested folders onto the desktop dropzone. The internal recursive engine traverses all subdirectories, collects individual files, and preserves the relative directory structure for batch packaging.

### 8. Wildcard and Regex File Exclusion Filter
Prevents cluttering by automatically filtering out system artifacts, temporary caches, and development directories:
- Excluded Patterns: Automatically ignores `.tmp`, `.crdownload`, `Thumbs.db`, `desktop.ini`, `.git`, `node_modules`, and hidden operating system files.

### 9. 5 Sliding Frosted Glass Drawers
The user interface organizes advanced capabilities into five sliding frosted glass drawers with smooth animations and backdrop blur:
- Shared Clipboard Drawer
- Watched Folders Drawer
- Connected Devices Drawer
- Transfer History Drawer
- Desktop Settings Drawer

### 10. Window State and Geometry Persistence
The application saves its exact desktop window dimensions (width and height), screen coordinates (X and Y), and maximized state (`window_state.json`). When reopened, it restores its exact previous desktop position.

### 11. Smart File Source Attribution Engine
The server tracks which device uploaded which file (`.file_sources.json`). When a mobile phone uploads a file to the PC, the mobile interface automatically suppresses redundant "Download" buttons on the sender device while showing clear received status on the PC.

### 12. Local Direct LAN APK Distribution
The desktop dock includes a direct "Share APK" option that serves the official `ADropX.apk` binary directly to connecting mobile devices over the local network, allowing first-time phone pairing without requiring external internet downloads.

### 13. Interactive Click-to-Zoom QR Dock
The bottom dock features a mini QR thumbnail with hover glow. Clicking the thumbnail opens a high-resolution connection modal displaying the auto-detected local IP address, port, and a one-click "Copy Link" utility.

---

## 5. Competitive Matrix: ADropX vs Alternatives

| Feature / Metric | ADropX | Apple AirDrop | Quick Share (Google) | LocalSend | WhatsApp / Telegram | Google Drive / OneDrive |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Windows Support** | Native (.exe) | None | Limited app | Yes | Web / App | Web / App |
| **Android Support** | Native (.apk) + Web | None | Native | Yes | App | App |
| **iOS / iPadOS Support** | Universal Web PWA | Native | None | Yes | App | App |
| **App Required on Phone?** | **No** (Optional) | Yes (iOS only) | Yes | Yes (Mandatory) | Yes (Mandatory) | Yes (Mandatory) |
| **Internet Required?** | **No** (100% Offline) | No | No | No | **Yes** (Double data) | **Yes** (Double data) |
| **Transfer Speed** | **Up to 165 MB/s** | Up to 80 MB/s | Up to 60 MB/s | Up to 80 MB/s | 5 - 15 MB/s | 10 - 20 MB/s |
| **File Compression** | **Zero Loss** | Zero Loss | Zero Loss | Zero Loss | **Heavy Loss** | Zero Loss |
| **File Size Limit** | **No Limit** | No Limit | No Limit | No Limit | 2 GB limit | Cloud storage quota |
| **Setup Launch Time** | **0.05 seconds** | Built-in | Variable | Variable | Variable | Variable |
| **Watched Folders** | **Yes** (Auto-share) | No | No | No | No | Sync client |
| **Explorer Context Menu** | **Yes** (Right-click) | No | Limited | No | No | Yes |
| **OTP & Smart Clipboard** | **Yes** (Automated) | Basic text | Basic text | Basic text | Manual copy | Manual note |
| **Direct 4K Streaming** | **Yes** (Range bytes) | No | No | No | Transcoded | Transcoded |
| **Cloud Telemetry / Ads** | **Zero** | Apple telemetry | Google telemetry | Zero | Meta / Telegram | Cloud tracking |

---

## 6. Official Packages and Downloads

All binaries are built for Windows 10/11 and Android 8.0+. Download the latest verified releases from:
https://github.com/BeginnerAman/ADropX/releases/latest

### Available Formats

#### 1. Windows Installer (`ADropX-Setup.exe`)
- Format: Native Windows Executable (.exe)
- File Size: ~44.5 MB
- Target: Recommended for personal computers.
- Characteristics: Starts in 0.05 seconds. Installs to local user directory (`%LOCALAPPDATA%\Programs\ADropX`). Creates Desktop shortcuts (handling OneDrive Desktop folder redirection) and Start Menu shortcuts. Registers in Windows Settings with a clean uninstaller. No UAC administrator elevation required.

#### 2. Standalone Binary (`ADropX.exe`)
- Format: Single-File Executable (.exe)
- File Size: ~43.9 MB
- Target: Users who prefer zero system modification.
- Characteristics: Self-contained binary containing all required runtime components. Run directly from any folder or desktop.

#### 3. Portable Archive (`ADropX-Portable.zip`)
- Format: ZIP Archive (.zip)
- File Size: ~44.1 MB
- Target: USB thumb drives, external SSDs, and restricted workstations.
- Characteristics: Pre-extracted directory structure. Leaves zero registry footprints on host computers.

#### 4. Android Companion Client (`ADropX.apk`)
- Format: Native Android Package (.apk)
- File Size: ~7.3 MB
- Target: Android 8.0 through Android 15+.
- Characteristics: Built-in viewfinder camera QR scanner, haptic feedback, background WakeLock service, and direct system download folder integration.

---

## 7. Connection Methods and Setup Guide

Connecting your devices takes less than five seconds. Ensure your PC and mobile device share a common local network route through any of the following setups:

### Method A: Home or Office WiFi Router (Most Common)
1. Connect your PC and phone to the same WiFi router network.
2. Launch `ADropX-Setup.exe` (or `ADropX.exe`) on your PC.
3. The desktop dashboard will display an on-screen connection QR code.
4. Point your phone camera or the ADropX companion app at the QR code.
5. The sharing dashboard opens immediately on your phone.

### Method B: Mobile Phone Hotspot (No Internet Required)
1. Turn on the Personal Hotspot on your mobile phone.
2. In your phone hotspot settings, ensure the AP Band is set to 5 GHz for maximum throughput.
3. Connect your PC to your phone's hotspot. Note: You do not need an active mobile data pack or cellular internet; local network routing functions independently.
4. Launch ADropX on PC, scan the QR code, and transfer files.

### Method C: PC Mobile Hotspot
1. On your Windows PC, open Windows Settings > Network & Internet > Mobile Hotspot, and turn it on.
2. Connect your phone to your PC's hotspot WiFi.
3. Launch ADropX on PC, scan the QR code, and transfer files.

### Method D: USB Cable Tethering (Zero Wireless Interference)
1. Connect your phone to your PC using a USB cable.
2. On your phone, go to Settings > Network / Hotspot > Enable USB Tethering.
3. Launch ADropX on PC. The system detects the local RNDIS/USB network adapter.
4. Scan the QR code or type the displayed IP address in your phone browser.

---

## 8. Android Companion Client vs Web Mode

ADropX provides complete flexibility: you can use the web interface without installing anything, or install the native APK for convenience.

| Operational Feature | Web Client (Browser PWA) | Native Android Companion App |
| :--- | :--- | :--- |
| **Installation Requirement** | Zero installation | Single APK install (~7.3 MB) |
| **QR Code Pairing** | System Camera / Browser QR | Fast built-in viewfinder camera |
| **Haptic Feedback** | Device dependent | Instant vibration on QR lock |
| **Long Transfers with Screen Off** | Browser may sleep connection | Background WakeLock keeps WiFi active |
| **File Storage Location** | Browser download prompt | Native Android `Download/` folder |
| **System Compatibility** | iOS, Android, macOS, Linux | Android 8.0+ |

---

## 9. Smart Clipboard and OTP Engine

ADropX bridges the text barrier between PC and mobile devices.

### How It Works
1. Navigate to the Clipboard tab on either your PC or phone.
2. Type or paste any snippet of text, notes, links, or verification codes.
3. Click or tap Send.
4. The text appears on the connected device in real time.

### Automated Entity Parsing
The built-in parser scans clipboard text for actionable patterns:
- Verification Codes: Regex matches 4, 6, and 8 digit security tokens (such as bank OTPs and 2FA credentials) and surfaces a dedicated "Copy OTP" chip.
- Web Hyperlinks: Identifies `http://` and `https://` URLs, formatting them as clickable "Open Link" buttons.
- Telephone Numbers: Identifies standard national and international phone numbers, providing a "Dial" button on phones.
- Email Addresses: Identifies standard email addresses, providing a "Compose Mail" action.

---

## 10. Technical Architecture and Protocols

ADropX is built on a modern, decoupled client-server architecture:

```
[ Windows Desktop Host ]
       │
       ├── Native Launcher: DirectWrite C# WPF Shell (0.05s Startup)
       ├── Desktop Shell: Frameless Microsoft Edge Chromium WebView2
       ├── Backend Engine: Python 3 with FastAPI and Asyncio Event Loop
       ├── Discovery Service: Zeroconf Multicast DNS (mDNS) Daemon
       ├── Tray Service: Background Pystray Icon with Notifications
       └── Filesystem Monitor: Watchdog Multi-Directory Polling Engine
       │
  (Local WiFi / Hotspot TCP Socket Stream: 120-165 MB/s)
       │
[ Connected Client ]
       │
       ├── Native Android APK (Viewfinder + WakeLock + DownloadManager)
       └── Universal Browser PWA (Vanilla JS + Web Workers + Range Stream)
```

### Key Technical Specifications
- Network Layer: Asynchronous TCP socket streaming over standard HTTP/1.1 and HTTP/2 transport layers.
- Storage I/O: Non-blocking zero-copy disk streaming using 2 MB to 4 MB buffer chunks.
- Media Streaming: RFC 7233 standard HTTP Range Requests for instant random-access audio and video seeking.
- Local Discovery: Zeroconf mDNS advertising service for automated LAN peer detection.
- Memory Consumption: Flat 10 MB to 20 MB RAM footprint during active high-speed transfers.

---

## 11. Security, Privacy, and Network Isolation

ADropX adheres to strict zero-trust data privacy standards:

1. 100% Local Network Isolation: Files, clipboard text, and media streams travel exclusively over your physical local network. Zero bytes are uploaded to remote cloud datacenters.
2. Zero Cloud Telemetry: ADropX contains no analytics trackers, no third-party tracking scripts, and no advertising frameworks.
3. Zero User Registration: No user accounts, phone numbers, or email credentials are required.
4. Public Network PIN Verification: An optional 4-digit security PIN can be activated in Settings to prevent unauthorized connections on public or shared networks.
5. Ephemeral Sessions: Authentication tokens and temporary transfer sessions expire automatically when the application is closed.

---

## 12. Troubleshooting and Network Diagnostics

### Issue 1: Phone browser cannot load the connection page
- Cause: The PC and phone are on different network subnets, or Windows Firewall is blocking inbound connections.
- Resolution:
  1. Verify both devices are connected to the same WiFi router or phone hotspot.
  2. In Windows Firewall settings, verify that ADropX has permissions enabled for Private networks.
  3. If your WiFi router has "AP Isolation" or "Client Isolation" enabled in router admin settings, disable it so connected devices can communicate with each other. Alternatively, switch to phone hotspot mode.

### Issue 2: Large transfer pauses when phone screen turns off
- Cause: Android aggressive battery saver puts WiFi to sleep when the screen locks.
- Resolution:
  1. Use the official `ADropX.apk` companion app, which includes an automated WakeLock to keep WiFi active during transfers.
  2. If using a web browser, keep the browser tab active or adjust your phone screen timeout settings.

### Issue 3: Windows Defender or SmartScreen notice on first launch
- Cause: ADropX is a newly released independent executable without an expensive corporate enterprise certificate.
- Resolution: Click "More info" and select "Run anyway". ADropX contains zero malware, zero adware, and zero trackers.

### Issue 4: Slow transfer speeds (under 15 MB/s)
- Cause: The router or hotspot is operating on the crowded 2.4 GHz WiFi frequency band.
- Resolution: Configure your router or phone hotspot to the 5 GHz WiFi band. On 5 GHz WiFi, speeds increase up to 120+ MB/s.

---

## 13. System Requirements

### Desktop Host (Personal Computer)
- Operating System: Microsoft Windows 10 (Build 1809 or higher) or Windows 11 (64-bit architecture).
- Memory: Minimum 512 MB available RAM (1 GB recommended).
- Network: WiFi adapter (5 GHz recommended for maximum throughput), Ethernet port, or USB tethering support.
- Disk Space: 100 MB available storage for application binaries.

### Mobile Client (Phone or Tablet)
- Android: Android 8.0 (Oreo) or higher for native APK; any modern browser for Web mode.
- iOS / iPadOS: iOS 13.0 or higher (Apple Safari, Google Chrome).
- Other Devices: Any modern web browser supporting HTML5 and Web Workers.

---

## 14. Developer Attribution and Terms of Use

ADropX is created, engineered, and maintained by:

**Aman Vishwakarma**  
Developer GitHub: https://github.com/BeginnerAman  
Repository: https://github.com/BeginnerAman/ADropX  
Official Landing Page: https://beginneraman.github.io/ADropX/

Terms: ADropX is 100% free for personal, commercial, and educational use. Developed by Aman Vishwakarma.
