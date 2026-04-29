# 🤖 AI Chatbot Web Platform

一個可嵌入網站的 AI 客服聊天機器人系統，支援多輪對話、角色切換（System Prompt）、Quick Reply 選項，以及完整對話紀錄，並透過 Docker Compose 一鍵啟動。

---

## 🎯 專案定位

本專案目標為打造一個：

✔ 可實際部署  
✔ 可嵌入任意網站（Floating Widget）  
✔ 可用於面試 Demo  
✔ 可展示 Backend / DevOps 能力  

的 AI Chatbot 系統。

---

## 🧩 系統特色

- 💬 AI 對話（整合 OpenAI API）
- 🧠 System Prompt（aiRole）角色切換
- 🔁 多輪對話（Conversation History）
- ⚡ Quick Reply / Inline Quick Reply
- 🗂 對話紀錄（MySQL chat_logs）
- 🧱 Vue Floating Chat Widget（可拖曳 / 收合）
- 🐳 Docker Compose 一鍵啟動（完整本地環境）

---

## 🏗 技術架構

| 層級       | 技術                      |
| -------- | ----------------------- |
| Frontend | Vue 3                   |
| Backend  | Spring Boot             |
| Database | MySQL                   |
| AI       | OpenAI API              |
| DevOps   | Docker / Docker Compose |

---

## 🖥 系統畫面（Demo）

👉 Floating Chat Widget（右下角 AI 客服）

![](C:\Users\arkly365\AppData\Roaming\msbmarkdown\images\2026-04-29-22-33-09-image.png)

👉 Quick Reply 按鈕操作

![](C:\Users\arkly365\AppData\Roaming\msbmarkdown\images\2026-04-29-22-34-35-image.png)

👉 多輪對話 + AI 回應

![](C:\Users\arkly365\AppData\Roaming\msbmarkdown\images\2026-04-29-22-35-16-image.png)

![](C:\Users\arkly365\AppData\Roaming\msbmarkdown\images\2026-04-29-22-36-06-image.png)



![](C:\Users\arkly365\AppData\Roaming\msbmarkdown\images\2026-04-29-22-36-46-image.png)



![](C:\Users\arkly365\AppData\Roaming\msbmarkdown\images\2026-04-29-22-37-19-image.png)



---

## ⚡ 快速啟動

# 設定 API Key（PowerShell）

$env:OPENAI_API_KEY="your_api_key"

# 啟動

docker compose down
docker compose up -d --build

---

## 🔗 API 測試

$body = @{  
  message = "你好"  
  messages = @(  
    @{  
      role = "user"  
      content = "你好"  
    }  
  )  
  aiRole = "default"  
} | ConvertTo-Json -Depth 5  

Invoke-RestMethod `  
  -Uri "http://127.0.0.1:8080/api/chat" `  
  -Method Post `  
  -ContentType "application/json; charset=utf-8" `  
  -Body $body

---

## 🧪 資料庫查看

docker exec -it chatbot-mysql mysql --default-character-set=utf8mb4 -uappuser -papppass chatbot  

SELECT * FROM chat_logs ORDER BY id DESC LIMIT 3\G

---

## 📌 專案目標

本專案用於展示以下能力：

- Backend API 設計（Spring Boot）
- AI 系統整合（Prompt + Chat API）
- 前後端整合（Vue + REST API）
- 狀態管理（Conversation History）
- 資料持久化（MySQL）
- 容器化部署（Docker Compose）
- 系統設計（可擴充 Chatbot 架構）
