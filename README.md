# 🚨 EmergencyMeshApp

An **offline emergency communication app** for Android that uses **Bluetooth mesh networking** to send messages, SOS alerts, and location data — even without internet or cellular signal.

> Built as a college project to demonstrate peer-to-peer multi-hop mesh communication for disaster/emergency scenarios.

---

## 📱 Features

| Feature | Description |
|---------|-------------|
| 📡 **Bluetooth Mesh** | Devices relay messages to each other via multi-hop routing |
| 💬 **Send Messages** | Text, location, and emergency alert messages |
| 🚨 **SOS Alert** | One-tap SOS broadcast to all nearby mesh devices |
| 📥 **Message Inbox** | View received messages with RECEIVED/PENDING/Delivered status |
| 📍 **Location Share** | Share your GPS coordinates over the mesh |
| 👥 **Emergency Contacts** | Save and broadcast to emergency contacts |
| 📶 **Nearby Devices** | Scan, discover, and connect to nearby Bluetooth devices |
| 🔁 **Reply** | Reply directly to received messages |

---

## 🏗️ Project Structure

```
app/src/main/java/com/emergencymesh/app/
│
├── MainActivity.java               # Home screen, mesh status, SOS
├── MessageInboxActivity.java       # Inbox — view all messages
├── SendMessageActivity.java        # Compose & send messages
├── NearbyDevicesActivity.java      # Scan & connect Bluetooth devices
├── EmergencyContactsActivity.java  # Manage emergency contacts
├── ProfileSetupActivity.java       # First-time user setup
├── SplashActivity.java             # Splash screen
│
├── adapters/
│   ├── MessageAdapter.java         # RecyclerView adapter for messages
│   ├── DeviceListAdapter.java      # RecyclerView adapter for devices
│   └── EmergencyContactAdapter.java
│
├── models/
│   ├── Message.java                # Message data model
│   └── EmergencyContact.java       # Contact data model
│
├── services/
│   ├── BluetoothMeshService.java   # Core Bluetooth mesh engine
│   └── GlobalMeshService.java      # Singleton service manager
│
└── utils/
    ├── MessageStorage.java         # SharedPreferences message store
    ├── ContactStorage.java         # SharedPreferences contact store
    ├── MessageCache.java           # Duplicate message prevention cache
    └── SharedPrefsHelper.java      # User profile preferences
```

---

## 🔧 Tech Stack

- **Language:** Java
- **Platform:** Android (API 21+)
- **Networking:** Bluetooth Classic (RFCOMM sockets)
- **Storage:** SharedPreferences (JSON via Gson)
- **UI:** XML layouts, RecyclerView, Material-style dark theme
- **Libraries:** Gson, AndroidX, LocalBroadcastManager

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Hedgehog or newer)
- Android device with Bluetooth (API 21+)
- For Android 12+: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE` permissions

### Build & Run
1. Clone or open the project in Android Studio
2. Sync Gradle (`Ctrl + Alt + Y`)
3. Build (`Ctrl + F9`)
4. Run on a physical Android device (Bluetooth required — emulator won't work)

---

## 📋 Permissions Required

```xml
BLUETOOTH
BLUETOOTH_ADMIN
BLUETOOTH_CONNECT       <!-- Android 12+ -->
BLUETOOTH_SCAN          <!-- Android 12+ -->
BLUETOOTH_ADVERTISE     <!-- Android 12+ -->
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
```

---

## 📡 How the Mesh Works

```
Device A  ──BT──►  Device B  ──BT──►  Device C
  (sender)          (relay)            (receiver)
```

1. Device A sends a message over Bluetooth to Device B
2. Device B **relays** the message to Device C (multi-hop)
3. Each message has a **hop count** and **route path** tracked
4. A **message cache** prevents duplicate forwarding (seen message IDs are cached)
5. Messages are stored locally on each device using SharedPreferences

---

## 💬 Message Types

| Type | Icon | Description |
|------|------|-------------|
| `text` | 💬 | Regular text message |
| `location` | 📍 | GPS coordinates share |
| `alert` | 🚨 | Emergency broadcast alert |

---

## 📲 Navigation

The app has a **bottom navigation bar** on every screen:

| Tab | Action |
|-----|--------|
| 🏠 HOME | Go to main screen |
| ✉ INBOX | View received messages |
| ⛙ MESH | View nearby devices |
| ✕ ON | Show mesh connection status |

---

## 🐛 Known Limitations

- Bluetooth range is limited (~10–30m per hop)
- No encryption on messages (plain text over RFCOMM)
- Contact import from phone contacts not yet implemented
- Works best with 2+ physical Android devices

---

## 👨‍💻 Author

**Saubhagay Goyal**
College Project — Emergency Mesh Communication System

---

## 📄 License

This project is for educational/demonstration purposes.
