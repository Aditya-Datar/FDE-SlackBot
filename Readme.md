# **Nixo FDE Mission Control: Intelligent Slackbot Dashboard**

A minimal end to end system that listens to customer conversations in Slack, classifies relevant messages, groups related issues, and displays them in a realtime dashboard for a Forward-Deployed Engineer (FDE).

This project demonstrates the complete realtime data flow from Slack into the backend, through the database, and into a live updating frontend UI. It includes intelligent relevance filtering and grouping.

---

# **1. Features**

### ⚡ Low-Latency Ingestion (<500ms)
* **Async Event Processing:** Decouples Slack webhooks from heavy AI processing to ensure the bot never times out.
* **Smart Caching:** Uses Levenshtein Distance (>90% similarity) and Spring `@Cacheable` to serve repeated queries instantly without hitting AI APIs.

### 🧠 Intelligent Classification
* **Context Inheritance:** Zero-shot optimization that checks thread parentage first; if a message is a reply to a known ticket, it skips the AI call entirely.
* **Relevance Filtering:** Distinguishes between **Bug Reports**, **Feature Requests**, and **Support** vs. noise (e.g., "Lunch time?", "Thanks!").

### 🔗 Hybrid Grouping Strategy
* **Fast Path:** Instant grouping via Slack `thread_ts` (Thread ID).
* **Slow Path:** Vector Embedding + Cosine Similarity search to group semantically similar issues across different channels or timeframes.

### 🛡️ Deduplication & Concurrency
* **Connection Pool Protection:** Long-running AI tasks run outside of transactional boundaries.
* **Visual State:** Dynamic "NEW" vs "UPDATED" badges with distinct timestamps.

### Intelligent classification

Detects messages that are:

* Bug Reports
* Feature Request
* Support Questions
* Product Related Questions

Ignores casual messages such as:

* Thank you!
* Greeting!
* Planning or Social Messages

### Automatic grouping

Related messages are combined into a single ticket using:

* Old Slack Thread Context
* Semantic Similarity
* Channel Information
* Time-Based Grouping

### Duplicate avoidance

Tickets are uniquely tracked using message ID, thread ID, and a similarity fingerprint to avoid duplication in Database.

### Local friendly

Runs fully on localhost with ngrok acting as the public callback URL for Slack events.

---

# **2. Tech Stack**

### Backend
* **Core:** Java 17/21, Spring Boot 3.
* **Architecture:** Strategy Pattern (Swappable `AIServiceInterface` for OpenAI/Gemini).
* **Data:** Spring Data JPA with **Eager Fetching** (optimized `JOIN FETCH` queries).
* **Search:** In-Memory Vector Search (Cosine Similarity) & Levenshtein Distance.

### Frontend
* **Framework:** React (Vite) + Tailwind CSS (Mission Control Dark Mode).
* **Realtime:** WebSockets (STOMP over SockJS).
* **Animation:** Framer Motion for live list reordering.

---

# **3. Prerequisites**

| Requirement     | Version                            |
| --------------- | ---------------------------------- |
| Java            | 17 or 21                           |
| Node.js         | 18 or higher                       |
| PostgreSQL      | Any version                        |
| Ngrok           | Latest version                     |
| Slack Workspace | One FDE user and one customer user |
| Slack App       | With correct scopes                |

---

# **4. Environment Setup**

## Backend Configuration

Create or edit `Backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<your-db-url>
    username: <your-db-username>
    password: <your-db-password>
  jpa:
    hibernate:
      ddl-auto: update

slack:
  bot:
    token: xoxb-your-bot-token
    signing-secret: your-signing-secret

ai:
  provider: openai # Options: 'openai' or 'gemini' (Swappable via Strategy Pattern)
```

### Running the Backend

Ensure that all dependencies are installed through Maven.
Start the Spring Boot application from your IDE or run it through the standard Maven command in the project root.
Confirm that the server starts on port `8080` and connects to your PostgreSQL instance.

---

## Frontend Configuration

The frontend is configured to proxy requests to `http://localhost:8080` by default.
No additional environment variables are required.

### Running the Frontend

Navigate to the frontend directory and install the required packages.
Start the development server using the usual npm commands.
The application should run locally on the default React port and automatically route API calls to the backend.

---

# **5. Slack App Setup Guide**

Follow these steps carefully.

## Step 1: Create the App

Visit [https://api.slack.com/apps](https://api.slack.com/apps) and create a new app from scratch.

## Step 2: Add OAuth Permissions

Under Bot Token Scopes, add:

```
channels:history
groups:history
im:history
mpim:history
chat:write
channels:read
groups:read
metadata.message:read
```

Save the changes.

## Step 3: Configure Event Subscriptions

1. Start ngrok:

```
ngrok http 8080
```

2. Copy the generated HTTPS domain.
3. Enable Event Subscriptions.
4. Set the Request URL to:

```
https://<ngrok-id>.ngrok-free.app/slack/events
```

5. Subscribe to the following events:

```
message.channels
message.groups
message.im
message.mpim
```

Save the configuration.

## Step 4: Install the App

Install the app into your Slack workspace.

## Step 5: Invite the Bot to a Channel

In your Slack channel, run:

```
/invite @your-bot-name
```

---

# **6. Running the Project**

## Start the Backend

```bash
cd Backend
mvn spring-boot:run
```

Wait for "Started Application" in the logs.

## Start the Frontend

```bash
cd Frontend
npm install
npm run dev
```

Open the dashboard at:
[http://localhost:3000](http://localhost:3000)

---

# **7. Running the Demo**

### Test 1: Relevant Issue Detection

Send in Slack:
**The export to CSV button is throwing a 500 error.**

Expected result:
A new ticket appears under Bugs within a few seconds.

---

### Test 2: Grouping

Reply to the previous message in a thread:
**It only happens on Safari.**

Expected result:
The existing ticket updates.
No new ticket is created.

---

### Test 3: Noise Filtering

Send:
**Thanks for the help**
or
**Lunch time**

Expected result:
No ticket is created.
Dashboard remains unchanged.

---

### Test 4: Cross Channel Grouping

Send in another channel:
**This CSV issue is still happening today.**

Expected result:
Same ticket gets updated using semantic grouping.

---

# **8. Project Structure**

```
Backend/
  src/
    main/
      java/
      resources/
Frontend/
  src/
README.md
WRITEUP.md
```

---

# **9. Architecture & Optimizations**

* **Vector Deserialization Cache:** To speed up the "Slow Path" search, embeddings are cached in memory (ConcurrentHashMap) to avoid parsing JSON from the DB on every request.
* **Thread Context Inheritance:** The system checks if a message is a reply in a known thread before asking the AI. This reduces API latency from ~800ms to **0ms** for ongoing conversations.
* **Levenshtein Fuzzy Matching:** Local string distance algorithms catch typos and near-exact matches locally, saving API costs.
* **Realtime Flow:** Slack Webhook → Ngrok → Spring Boot Event Listener (`@Async`) → DB → WebSocket Push → React Client.

---

# **10. Troubleshooting**

### Slack cannot validate the Request URL

* Ensure backend is running.
* Ensure ngrok is running.
* Ensure the endpoint `/slack/events` exists.

### Bot does not receive messages

* Confirm the bot has been invited to the channel.
* Confirm message events are enabled.

### Dashboard does not update

* Check WebSocket logs in browser console.
* Check backend logs for event ingestion.