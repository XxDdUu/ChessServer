# Frontend API Instructions - Chess Application

This document provides instructions for the frontend implementation to interact with the newly added backend APIs and WebSocket features.

## 1. REST APIs

### 1.1 Home API: User Profile & Stats
**Endpoint:** `GET /api/user/me`
* Returns the current logged-in user's profile and stats based on the JWT token.

**Endpoint:** `GET /api/user/{id}/stats`
**Response:**
```json
{
  "userId": 1,
  "username": "thanglm",
  "countryCode": "VN",
  "rating": 1200,
  "gamesPlayed": 10,
  "wins": 5,
  "losses": 3,
  "draws": 2
}
```

### 1.2 Game History
**Endpoint:** `GET /api/game/history?userId={id}&page=0&size=10`
**Response:** Array of games:
```json
[
  {
    "gameId": 12,
    "opponentId": 3,
    "opponentName": "John",
    "myColor": "WHITE",
    "result": "1-0",
    "pgn": "1. e4 e5 ...",
    "playedAt": "2026-05-05T10:00:00Z"
  }
]
```

### 1.3 Friend System
**Endpoint:** `GET /api/friends/list?userId={id}`
**Response:** List of friends and their current status.
```json
[
  {
    "userId": 2,
    "username": "alice",
    "status": "ONLINE",
    "rating": 1250
  }
]
```

**Endpoint:** `GET /api/friends/pending?userId={id}`
**Response:** List of pending friend requests received by the user.

**Endpoint:** `POST /api/friends/request?senderId={id}&receiverId={id}`
**Response:** `Friend request sent` (String)

**Endpoint:** `POST /api/friends/accept?user1={id}&user2={id}`
**Response:** `Friend request accepted` (String)

### 1.4 Admin Direct Messaging
* **Endpoint:** `GET /api/user/{userId}/messages`
  * Returns user's inbox messages sent by Admin. Requires JWT (user themselves or Admin).
* **Endpoint:** `POST /api/user/{userId}/messages`
  * Send direct message from Admin to a user. Requires Admin JWT.
  * **Request Body:**
    ```json
    {
      "title": "Cảnh báo vi phạm",
      "content": "Vui lòng tuân thủ quy tắc ứng xử",
      "type": "WARNING"
    }
    ```
  * **Response:**
    ```json
    {
      "id": 1,
      "messageId": 1,
      "senderId": 1,
      "senderUsername": "Admin",
      "recipientId": 2,
      "recipientUsername": "thanglm",
      "title": "Cảnh báo vi phạm",
      "content": "Vui lòng tuân thủ quy tắc ứng xử",
      "type": "WARNING",
      "read": false,
      "isRead": false,
      "sentAt": "2026-09-05T21:00:00Z"
    }
    ```
* **Endpoint:** `POST /api/user/{userId}/messages/{messageId}/read`
  * Mark message as read.
* **Endpoint:** `POST /api/user/{userId}/messages/read-all`
  * Mark all inbox messages as read.
* **Endpoint:** `POST /api/admin/messages/broadcast` (or `POST /api/user/messages/broadcast`)
  * Broadcast a message to **all registered users**. Requires Admin JWT.
  * **Request Body:**
    ```json
    {
      "title": "Bảo trì máy chủ",
      "content": "Máy chủ sẽ bảo trì trong 30 phút",
      "type": "ANNOUNCEMENT"
    }
    ```
  * **Response:**
    ```json
    {
      "message": "Broadcast message sent successfully to all users",
      "totalRecipients": 150,
      "onlineRecipientsNotified": 12,
      "title": "Bảo trì máy chủ",
      "type": "ANNOUNCEMENT",
      "sentAt": "2026-09-06T19:30:00Z"
    }
    ```
* **WebSocket Notification:**
  * When an admin sends a message or broadcast, online recipients automatically receive:
    ```json
    {
      "type": "ADMIN_DIRECT_MESSAGE",
      "id": 1,
      "recipientId": 2,
      "senderUsername": "Admin",
      "title": "Bảo trì máy chủ",
      "content": "...",
      "messageType": "ANNOUNCEMENT",
      "isBroadcast": true
    }
    ```

**Error Response (Global):**
```json
{
  "message": "Error description here"
}
```

## 2. Matchmaking & Game Flow

### 2.1 Matchmaking Logic
1. **Join Queue**: `POST /api/matchmaking/join?userId={id}&type={matchType}`.
2. **Find Match**: Server pairs two users.
3. **Prepare Game**: Both users receive `PREPARE_GAME` WebSocket event.
   ```json
   {
     "type": "PREPARE_GAME",
     "gameId": "uuid",
     "opponentId": 2,
     "opponentName": "Alice",
     "opponentCountry": "US",
     "opponentRating": 1300,
     "timeout": 10
   }
   ```
4. **Ready Check**: Client must send `READY` within the timeout.
   `{"type": "READY", "gameId": "uuid"}`
5. **Game Start**: Once both are ready, `GAME_START` is sent.

### 2.2 Game Logic Flow
1. **Initial State**: Users receive `GAME_START`.
   ```json
   {
     "type": "GAME_START",
     "gameId": "uuid",
     "side": "WHITE",
     "opponent": 2
   }
   ```
2. **Making a Move**:
   `{"type": "MOVE", "gameId": "uuid", "move": "E2E4"}` (Use coordinate notation)
3. **Validation**: Server validates move. If invalid, sends `ERROR`.
4. **Broadcast**: Opponent receives `OPPONENT_MOVE`.
   ```json
   {
     "type": "OPPONENT_MOVE",
     "move": "e2e4",
     "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
     "timeRemaining": 595
   }
   ```
5. **End Game**: When checkmate, draw, or resignation occurs, `GAME_OVER` is sent.

## 3. WebSocket Events

All WebSocket messages are sent and received as JSON strings over `ws://<domain>/ws?token=<JWT_TOKEN>`.

### 3.1 Presence Events
- **User Online:** `{"type": "USER_ONLINE", "userId": 2}`
- **User Offline:** `{"type": "USER_OFFLINE", "userId": 2}`
- *The frontend should use these to update the status in the friends list in real-time.*

### 3.2 Private Rooms
- **To Create a Room:**
  Send: `{"type": "CREATE_ROOM", "matchType": "rapid"}`
  Receive: `{"type": "ROOM_CREATED", "code": "A7B9"}`
- **To Join a Room:**
  Send: `{"type": "JOIN_ROOM", "code": "A7B9"}`

### 3.3 Direct Friend Invitation
- **To Invite an Online Friend:**
  Send: `{"type": "INVITE_FRIEND", "friendId": 2, "matchType": "rapid"}`
- **Receiving an Invitation:**
  The friend will receive: `{"type": "MATCH_INVITE", "hostId": 1, "hostName": "thanglm"}`
- **To Accept Invitation:**
  Send: `{"type": "ACCEPT_INVITE", "hostId": 1}`

### 3.4 Rematch
- **To Offer Rematch:**
  Send: `{"type": "REMATCH_OFFER", "gameId": "<uuid>"}`
- **Receiving Rematch Offer:**
  You will receive: `{"type": "REMATCH_OFFERED", "gameId": "<uuid>"}`
- **To Accept/Reject Rematch:**
  Send: `{"type": "REMATCH_RESPONSE", "gameId": "<uuid>", "accepted": true}`

### 3.5 In-Game Chat
- **To Send Message:**
  Send: `{"type": "CHAT_MESSAGE", "gameId": "<uuid>", "text": "Hello!"}`
- **To Receive Message:**
  Listen for: `{"type": "CHAT_MESSAGE", "senderId": 1, "senderName": "thanglm", "text": "Hello!"}`

### 3.6 Timers
- Every `OPPONENT_MOVE` event includes the opponent's `timeRemaining` (in seconds).
- The `RECONNECT_GAME` event includes `timeWhite` and `timeBlack`.
- The frontend should implement local countdown timers synchronized with these values.
