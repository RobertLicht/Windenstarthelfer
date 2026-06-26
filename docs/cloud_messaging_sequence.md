# Cloud Messaging Sequence Diagram

Near real-time communication and data flow between an **Android Smartphone** and an **Android Tablet** via Firebase Cloud Messaging (FCM) HTTP v1 API, Firestore listeners, and device-to-device sync.

```mermaid
sequenceDiagram
    participant S as Android Smartphone
    participant C as App Server<br/>(Admin SDK / HTTP v1)
    participant F as Firebase Backend
    participant FS as Cloud Firestore
    participant T as Android Tablet

    rect rgba(80, 160, 255, 0.12)
        Note over S,T: ── 1. Device Registration & Token Exchange ──
        S->>F: Install app → FCM SDK initializes
        F-->>S: FCM assigns registration token<br/>(refreshed on install / opt-in)
        S->>C: POST register device + FCM token + userId
        C->>FS: Store token under /devices/{userId}

        T->>F: Install app → FCM SDK initializes
        F-->>T: FCM assigns registration token
        T->>C: POST register device + FCM token + userId
        C->>FS: Store token under /devices/{userId}
    end

    rect rgba(100, 220, 140, 0.12)
        Note over S,T: ── 2. Cloud Firestore Listener Setup ──
        S->>FS: subscribe to /messages/{chatId} collection
        T->>FS: subscribe to /messages/{chatId} collection
    end

    rect rgba(255, 200, 80, 0.14)
        Note over S,T: ── 3. Smartphone → Tablet (Message Push) ──
        S->>C: POST /api/messages<br/>payload + recipient FCM token
        C->>FS: Write message to /messages/{chatId}
        FS-->>T: Firestore listener fires → render in-app
        C->>F: POST fcm.googleapis.com/v1/PROJECT/messages:send<br/>target = tablet FCM token, data payload
        F->>T: Deliver push notification (data msg)<br/>(foreground: onMessageReceived; background: system tray)
        T->>T: Foreground app → show message bubble via listener<br/>Background app → show NotificationCompat from payload
    end

    rect rgba(255, 140, 80, 0.14)
        Note over S,T: ── 4. Tablet → Smartphone (Message Push) ──
        T->>C: POST /api/messages<br/>payload + recipient FCM token
        C->>FS: Write message to /messages/{chatId}
        FS-->>S: Firestore listener fires → render in-app
        C->>F: POST fcm.googleapis.com/v1/PROJECT/messages:send<br/>target = smartphone FCM token, data payload
        F->>S: Deliver push notification (data msg)<br/>(foreground: onMessageReceived; background: system tray)
        S->>S: Foreground app → show message bubble via listener<br/>Background app → show NotificationCompat from payload
    end

    rect rgba(180, 255, 180, 0.14)
        Note over S,T: ── 5. Real-time Bidirectional Sync (Firestore) ──
        FS-->>S: Snapshot updates for all messages<br/>ordered by server timestamp
        FS-->>T: Snapshot updates for all messages<br/>ordered by server timestamp
    end

    rect rgba(200, 180, 255, 0.14)
        Note over S,T: ── 6. Presence & Read Receipts ──
        S->>FS: Write /presence/{userId}: {online:true, lastSeen:…}
        T->>FS: Write /presence/{userId}: {online:true, lastSeen:…}
        FS-->>S: Listen → show "tablet online"
        FS-->>T: Listen → show "phone online"

        S->>FS: Message read → write read receipt<br/>/messages/{chatId}/{msgId}/readBy/{userId}: true
        FS-->>T: Read receipt visible (double-check mark)
    end
```

## Overview

| Phase | Description |
|-------|-------------|
| **1. Device Registration** | Each Android device initializes the FCM SDK, receives a unique registration token, and registers that token with the App Server which stores it in Firestore. Tokens are refreshed on app install or re-opt-in. |
| **2. Firestore Listener Setup** | Both devices subscribe to the same Firestore collection (`/messages/{chatId}`), creating persistent WebSocket connections for near real-time document change events without polling. |
| **3. Smartphone → Tablet** | The smartphone sends the message to the App Server, which (a) writes it to Firestore and (b) triggers a data-only FCM push via the HTTP v1 API to the tablet's token. If the tablet app is in the foreground, the Firestore listener delivers the message directly; if in the background, the system notification bar wakes the user. |
| **4. Tablet → Smartphone** | The symmetric reverse path — same dual-write (Firestore + FCM push) ensures delivery regardless of the recipient's app state. |
| **5. Real-time Sync** | Firestore snapshot listeners on both devices keep all message bubbles synchronized with server timestamps as the source of truth, providing consistent ordering across devices. |
| **6. Presence & Read Receipts** | Each device publishes its online status to a `/presence` document and writes read receipts into individual message documents. The counterpart listener shows up-to-date availability indicators and delivery/read confirmations. |

## Architecture Notes

- **FCM HTTP v1 API** (`POST fcm.googleapis.com/v1/PROJECT/messages:send`) is the current standard — legacy Server Key API was deprecated in June 2024.
- **Data-only messages** are preferred for chat payloads; notification messages can be used when a visual system banner is desired on its own.
- When the app is in the **foreground**, `FirebaseMessagingService.onMessageReceived()` fires with the full data payload. In the **background**, Android routes the message to the system notification tray (NotificationCompat). Tapping the notification opens the app, which then syncs from Firestore.
- A dual-write pattern (Firestore + FCM push) guarantees delivery: Firestore provides ordered, queryable history; FCM provides instant wake-up for background devices. |

## Key Sequence (Textual)

```
Smartphone → App Server → Firestore (persist) + FCM (push)
                               ↓                        ↓
                          Tablet listener        Tablet notification
                    (foreground: render         (background: system tray)
                       message immediately)
```
