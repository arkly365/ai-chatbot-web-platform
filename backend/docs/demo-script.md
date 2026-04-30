# 🎬 AI Chatbot Web Platform - Demo Script

---

## 🧭 1. 說明

這是一個我自己設計並實作的 AI Chatbot Web Platform，  
可以嵌入在任何網站中作為客服聊天機器人。

前端使用 Vue 3 建立 Floating Chat Widget，  
後端使用 Spring Boot 串接 OpenAI API，  
並透過 MySQL 保存完整對話紀錄，  
最後使用 Docker Compose 完成一鍵啟動。

---

## 🖥 2. 系統 Demo

### Step 1：啟動系統

```bash
docker compose up -d --build
```

### Step 2：開啟前端

http://127.0.0.1:5173

---

### Step 3：展示功能

我這邊展示幾個重點功能：

1️⃣ Floating Chat Widget  
→ 右下角可開啟 / 收合 / 拖曳

2️⃣ Quick Reply  
→ 使用者可以快速點選問題

3️⃣ Inline Quick Reply  
→ AI 回應中帶有建議選項

4️⃣ 多輪對話  
→ 系統會保留 conversation history

5️⃣ aiRole（System Prompt）  
→ 可以切換不同 AI 行為

---

### Step 4：API 展示

POST /api/chat

這個 API 會：

- 接收 message + messages（history）
- 根據 aiRole 注入 system prompt
- 呼叫 OpenAI API
- 回傳 AI 回應

---

### Step 5：資料庫驗證

docker exec -it chatbot-mysql mysql --default-character-set=utf8mb4 -uappuser -papppass chatbot  

SELECT * FROM chat_logs ORDER BY id DESC LIMIT 3\G

可以看到每一次對話都有被保存。

## 🧠 3. 設計重點

這個專案我特別關注三個設計：

### ① Prompt 抽象化（aiRole）

我沒有把 prompt 寫死在程式裡，  
而是設計成 aiRole，讓系統可以切換不同角色。

---

### ② Conversation History

前端會把歷史對話一起送給後端，  
讓 AI 可以理解上下文，達成多輪對話。

---

### ③ 可嵌入式 Widget

我將 UI 設計成 Floating Widget，  
未來可以嵌入任何網站（類似客服系統）。

---

## 🚀 4. DevOps / 部署設計

- 使用 Docker Compose 整合三個服務
- frontend / backend / mysql
- 所有 port 綁定 127.0.0.1（避免暴露）
- 使用獨立 network（chatbot-net）

---

## 🎯 5. 結尾

這個專案主要是用來展示：

- AI 系統整合能力
- 前後端整合能力
- 可部署的系統設計能力

未來也可以擴充：

- RAG FAQ 知識庫
- Streaming 回應
- Prometheus 監控
