# Cloud Messaging Sequence Diagram

Communication flow between Smartphone and Laptop via Firebase Cloud Messaging (FCM).

```mermaid
sequenceDiagram
    participant S as Smartphone
    participant C as App Server
    participant F as Firebase (Cloud)
    participant L as Laptop

    rect rgba(100,200,100,0.15)
        Note over S,F: 1. Device Registration
        S->>C: Install app & sign in
        C-->>S: Assign user account
        S->>F: Register device -> get FCM token
        F-->>S: Return FCM token
        S->>C: Send FCM token + user info
    end

    rect rgba(100,200,255,0.15)
        Note over L,F: 2. Cloud Messaging Setup
        L->>L: User logs into app on Laptop
        L->>F: Authenticate device with FCM
        F-->>L: Device registered as client
    end

    rect rgba(255,230,140,0.2)
        Note over S,L: 3. Smartphone → Laptop (Message Push)
        S->>C: Send chat message (recipient: laptop-user@...)
        C->>F: POST /fcm/send with recipient topic & payload
        F-->>S: Message queued (ack receipt sent via push)
        F->>L: Push notification delivered
        L->>L: Display message in app
    end

    rect rgba(255,180,140,0.2)
        Note over S,L: 4. Laptop → Smartphone (Message Push)
        L->>C: Send chat message (recipient: smartphone-id@...)
        C->>F: POST /fcm/send with recipient topic & payload
        F-->>L: Message queued
        F->>S: Push notification delivered
        S->>S: Display message in app
    end

    rect rgba(180,255,180,0.2)
        Note over L,S: 5. Real-time Sync (WebSocket / Firestore Listener)
        C->>F: Subscribe to user message collection
        S->>F: Subscribe to user message collection
        F-->>S: New messages streamed in real time
        L-->>S: Live message bubbles update
    end

    rect rgba(220,180,255,0.15)
        Note over S,L: 6. Session / Presence Events
        S->>F: Heartbeat keep-alive every ~30s
        L->>S: Push presence event: "phone online"
        L-->>S: Update contact list status indicator
    end
```

## Overview

| Phase | Description |
|-------|-------------|
| **Device Registration** | Smartphone installs the app, registers with Firebase, and stores its FCM token on the App Server. The Laptop does the same when the user logs in there. |
| **Cloud Messaging Setup** | Both devices authenticate with Firebase so they can receive push notifications. |
| **Smartphone → Laptop** | When the Smartphone sends a message, the server posts to the FCM endpoint; Firebase delivers the push notification to the Laptop. |
| **Laptop → Smartphone** | The reverse path: the Laptop's messages flow through Firebase to reach the Smartphone in real time. |
| **Real-time Sync** | A WebSocket or Firestore listener keeps both devices' message collections synchronized without polling. |
| **Session / Presence Events** | Heartbeat and presence notifications maintain an up-to-date contact list indicator. |