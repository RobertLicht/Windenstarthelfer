# Firebase Cloud Messaging — Architecture, Message Flow & Latency

**Communication via Firebase Cloud Messaging (FCM) for data exchange between a Windenstarthelfer Android Smartphone, a Laptop Web Client and/or another Android Smartphone.**

---

## 1. System Architecture Overview

```mermaid
flowchart LR
    subgraph Devices ["Client Devices"]
        PhoneA[Phone A<br/>Android]
        PhoneB[Phone B<br/>Android]
        Laptop[Laptop<br/>Web Client]
    end

    subgraph Server ["Trusted Server Layer"]
        API[Firebase Cloud Function /<br/>Custom Node.js API]
        DB[(Firestore Database<br/>tokens, messages, users)]
    end

    subgraph FCM ["Google FCM Infrastructure"]
        GW[FCM HTTP v1 API Gateway<br/>fcm.googleapis.com]
        APL[Android Transport Layer<br/>Google Play Services]
        WP[Web Push Protocol<br/>Chrome/Edge browser]
    end

    PhoneA -->|HTTP POST + token| Server
    Laptop -->|WebSocket live sync| DB
    Server --> DB
    Server --> GW
    GW --> APL
    GW --> WP
    APL --> PhoneA
    APL --> PhoneB
    WP --> Laptop
    DB -.->|realtime updates| Laptop

    classDef device fill:#e8f5e9,stroke:#66bb6a,stroke-width:2px;
    classDef server fill:#fff3e0,stroke:#ffa726,stroke-width:2px;
    classDef fcm fill:#e3f2fd,stroke:#42a5f5,stroke-width:2px;

    class PhoneA,PhoneB,Laptop device;
    class API,DB server;
    class GW,APL,WP fcm;
```

---

## 2. Device Registration & Token Exchange

Before any messaging can happen, every client device must register with FCM and exchange its token (unique identifier) with the App Server.

```mermaid
sequenceDiagram
    autonumber
    participant S as Smartphone<br>(Windenstarthelfer app)
    participant A as Your App Server
    participant F as Firebase Cloud Messaging

    Note over S,F: Phase 1 -- Device Registration (app install / first launch)
    S->>F: Request registration token via FCM SDK
    F-->>S: Return unique registration token
    S->>A: Send token + device info / user profile
    A->>A: Store in Firestore users/{uid}/fcmToken

    Note over S,F: Phase 2 -- Authentication with your server
    A-->>S: Respond with user account state (if signed in)
    A->>A: Link FCM token to user profile
```

**What this achieves:** The App Server now has the unique address (token) that Firebase needs to deliver messages back to this specific device. The same registration process runs on any second smartphone or a web browser laptop app -- each obtains its own distinct FCM token.

---

## 3. Message Delivery Sequence -- All Scenarios

### 3A. Scenario: Smartphone A -> Laptop (Phone Sends a Message)

```mermaid
sequenceDiagram
    autonumber
    participant Phone as Smartphone<br>(Sender)
    participant Server as App Server + Firestore
    participant FCM as Firebase Cloud Messaging API
    participant WebPush as Web Push Protocol / Browser Runtime
    participant Laptop as Laptop (Web Client -- Receiver)

    Note over Phone,Laptop: Scenario: Smartphone sends a message to the laptop user

    rect rgba(100,200,100,0.15)
        title 1. User action on sender device
        Phone->>Phone: User types message and hits send
        Phone->>Server: HTTP POST /api/messages { text: "...", to: "laptop_user_42" }
    end

    rect rgba(100,150,200,0.15)
        title 2. Server stores message and looks up recipients
        Server->>Server: Write message document in Firestore<br>messages collection (source + target + timestamp + text)
        Server->>Server: Query FCM tokens of laptop_user_42
        Server-->>Server: Token received -- "APxv...kL9" (laptop browser token)
    end

    rect rgba(255,180,140,0.2)
        title 3. Message queued for delivery via FCM API
        Server->>FCM: POST https://fcm.googleapis.com/v1/projects/.../messages:send
        Note right of FCM: Content-Type: application/json
        Note over FCM: Authorization: Bearer <access_token><br>{ "token": "APxv...kL9",<br>"data": { "id": "...", "text": "..." },<br>"notification": { "title": "New message from Windenstarthelfer" } }
        FCM-->>Server: 200 OK -- {"name": "projects/.../messages/abc123"}
    end

    rect rgba(255,230,140,0.2)
        title 4. FCM pushes message to the laptop browser
        FCM->>WebPush: Push message over persistent connection<br>(device must be online -- Chrome/Edge -- Push API enabled)
        WebPush->>Laptop: Deliver payload via Notification Service Worker
    end

    rect rgba(100,255,180,0.2)
        title 5. Laptop displays message to user
        Laptop->>Server: Real-time listener fires (Firestore watch)<br>(receives the same data -- no manual polling)
        Server-->>Laptop: { text: "...", sender: "sarah@gmx.de", timestamp: ... }
        Note over Laptop: Browser may show system notification even if tab is hidden<br>(requires notification permission -- only once at install time)
    end

    rect rgba(100,200,255,0.15)
        title 6. Acknowledgment (optional)
        Server-->>Phone: Optional receipt callback / message status update in Firestore
    end
```

### 3B. Scenario: Smartphone -> Another Smartphone (Device-to-Device)

```mermaid
sequenceDiagram
    autonumber
    participant PhoneA as Smartphone (Sender - Robert)
    participant PhoneB as Smartphone (Recipient - Sarah)
    participant Server as App Server + Firestore
    participant FCM as Firebase Cloud Messaging API

    Note over PhoneA,PhoneB: Scenario: Robert sends a message to Sarah's phone

    rect rgba(100,200,100,0.15)
        PhoneA->>Server: HTTP POST /api/messages { text: "...", targetPhoneId: "sarah_phone_7" }
        Server->>Server: Write to Firestore messages collection
    end

    rect rgba(100,150,200,0.15)
        Server->>Server: Query Firestore for Sarah's FCM tokens
        Server-->>Server: Token = "fBk9x...mNq3R"
    end

    rect rgba(255,180,140,0.2)
        Server->>FCM: POST fcm.googleapis.com/v1/messages:send { token, data, notification, android.priority: high }
        FCM-->>Server: 200 OK
    end

    rect rgba(155,160,255,0.2)
        Note over FCM,PhoneB: High priority wakes device / normal priority batches
        FCM->>PhoneB: Push via Android Transport Layer (Google Play Services)
        PhoneB-->>FCM: Acknowledge
    end

    rect rgba(100,255,180,0.2)
        PhoneB->>PhoneB: onMessageReceived() fires - notification + UI update
    end
```

### 3C. Scenario: Laptop -> Smartphone (Reverse Direction)

```mermaid
sequenceDiagram
    autonumber
    participant Laptop as Laptop (web user -- Sarah)
    participant Server as App Server + Firestore
    participant FCM as Firebase Cloud Messaging API
    participant PhoneB as Smartphone (Robert's app)

    Note over Laptop,PhoneB: Scenario: Sarah on her laptop replies to Robert's message

    rect rgba(100,200,100,0.15)
        title 1. User action on laptop
        Laptop->>Server: WebSocket -- send reply: "Danke! Die Kraftwerte waren super." { targetPhoneId: "robert_phone_3" }
    end

    rect rgba(100,150,200,0.15)
        title 2. Server looks up recipient token and pushes message
        Server->>Server: Lookup Robert's FCM token in Firestore -> "mNq3R...vBk9x"
        Server->>FCM: POST https://fcm.googleapis.com/v1/projects/.../messages:send { "token": "...", "data": { ... }, "notification": { ... } }
        FCM-->>Server: 200 OK
    end

    rect rgba(155,160,255,0.2)
        title 3. Smartphone receives via Android Transport Layer
        FCM->>PhoneB: Push message -- onMessageReceived() fires
        PhoneB-->>FCM: Acknowledge receipt
        PhoneB->>PhoneB: Display notification + update chat UI in Windenstarthelfer app
    end

    rect rgba(100,255,180,0.2)
        title 4. Laptop receives confirmation (via Firestore realtime listener -- no manual polling needed)
        Server-->>Laptop: Firestore listener fires -- "delivered" status + read timestamp
    end
```

---

## 4. Timing / Latency Breakdown by Device State

| **Device state** | **Normal priority** | **High priority** |
|---|---|---|
| Screen on / app active | Near-instant (~1-3 s end-to-end) | Near-instant (~1-3 s end-to-end) |
| App backgrounded | Seconds to minutes (may be batched) | Near-instant (~1-3 s end-to-end) |
| Device in doze mode | Can be delayed significantly (minutes -- delivery is opportunistic) | Delivered immediately, device can be woken up |
| Device offline | Queued for up to 28 days (TTL), delivered when back online | Same behavior -- FCM attempts immediate delivery once the device reconnects |

### End-to-end latency composition

| **Step** | **Description** | **Typical latency** |
|---|---|---|
| Sender -> your server | HTTP request over mobile/wifi network | ~50 ms - 3 s (network dependent) |
| Server processing & database write | Firestore document write + query | ~10 ms - 500 ms (hosted service) |
| Server -> FCM API | HTTP v1 API call to fcm.googleapis.com | Usually < 1 s |
| FCM delivery to target device | Transport layer delivers over open connection | Seconds if online; hours if offline and queued |
| Device receives & displays payload | OS/browser processes and shows notification | Near-instant once arrived via transport |

**Total round-trip latency:** roughly **1-3 seconds** when the entire chain is active and online.

### Factors that add delay

- **Doze mode** (Android) -- major cause of delayed delivery for normal-priority messages; device enters battery-saving sleep when screen is off
- **Message priority** -- `normal` vs `high`; always use `high` for time-sensitive communication
- **Collapsing** (`collapseKey`) -- newer identical messages replace older ones in the queue (can appear as a delay)
- **TTL (time-to-live)** -- message age at which FCM discards it; default 28 days, set to `0` for "now or never"
- **Transport differences** -- Android devices use Google Play Services' ATP; web browsers use the Push API (different protocols but comparable performance)

---

## 5. Code Reference Snippets

### Sending a high-priority message from your server

```javascript
// Node.js + Firebase Admin SDK
import admin from 'firebase-admin';

async function sendMessage({ recipientToken, text }) {
  const response = await admin.messaging().send({
    token: recipientToken,
    data: { text, type: 'message' },
    notification: {
      title: 'Neue Nachricht',
      body: text.substring(0, 80),
      icon: '/icons/icon.png',
      badge: '/icons/badge.png',
    },
    android: { priority: 'high' },
    apns: { payload: { aps: { sound: 'default' } } },
    webpush: { headers: { Urgency: 'high' } },
  });
  console.log('Delivered:', response);
}
```

### Receiving messages on Android (in the Windenstarthelfer app)

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Show system notification
        remoteMessage.notification?.let { n ->
            showNotification(n.title ?: "Neue Nachricht", n.body ?: "")
        }
        // Store message text in app-local UI state
        val text = remoteMessage.data["text"] ?: ""
        EventBus.getDefault().post(MessageReceivedEvent(text))
    }
}
```

### Web browser client receiving messages on laptop

```javascript
// In your web app entry point
import { getMessaging, getToken, onMessage } from 'firebase/messaging';
const messaging = getMessaging(app);

onMessage(messaging, (payload) => {
  document.getElementById('chat').innerHTML += `<p>${payload.data.text}</p>`;
});

getToken(messaging, { vapidKey: 'YOUR_VAPID_KEY' }).then((token) => {
  fetch('/api/register-token', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token }),
  });
});
```

---

## Summary

| Aspect | Key takeaway |
|---|---|
| **Architecture** | All messaging flows through your server -> FCM API -> Android Transport Layer / Web Push Protocol. There is no direct peer-to-peer path. |
| **Latency when online & high priority** | ~1-3 seconds end-to-end. |
| **Doze mode effect** | The single largest source of latency -- use `high` priority for time-sensitive communication. |
| **Payload limit** | Data messages carry up to 4 KB (32 key-value pairs, 8 KB per value). |
| **Message expiration** | Set `ttl: 0` for "now or never" delivery; default is 28 days of queuing while offline. |
