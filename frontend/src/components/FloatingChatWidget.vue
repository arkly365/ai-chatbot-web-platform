<script setup>
import { ref, nextTick } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const isOpen = ref(false)

const inputRef = ref(null)
const chatBoxRef = ref(null)

const inputMessage = ref('')
const loading = ref(false)
const selectedRole = ref('default')

const quickReplyOptions = [
  '醫院',
  '圖書館',
  '加油站',
  '百貨公司'
]

const messages = ref([
  {
    role: 'assistant',
    text: '你好，我是 AI Chatbot。你可以輸入「我想查附近資訊」，我會提供快速選項。',
    quickReplies: []
  }
])

async function openChat() {
  isOpen.value = true
  await nextTick()
  inputRef.value?.focus()
  scrollToBottom()
}

async function scrollToBottom() {
  await nextTick()
  if (chatBoxRef.value) {
    chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
  }
}

function closeChat() {
  isOpen.value = false
}

function shouldShowQuickReplies(text) {
  return text.includes('附近') || text.includes('查詢') || text.includes('推薦')
}

async function sendMessage(customText) {
  const text = customText || inputMessage.value.trim()

  if (!text || loading.value) {
    return
  }

  messages.value.push({
    role: 'user',
    text,
    quickReplies: []
  })

  await scrollToBottom()

  inputMessage.value = ''

  if (shouldShowQuickReplies(text)) {
    messages.value.push({
      role: 'assistant',
      text: '請選擇你想查詢的類型：',
      quickReplies: quickReplyOptions
    })
    await scrollToBottom()
    return
  }

  await callChatApi(text)
}

async function callChatApi(text) {
  loading.value = true

  const assistantIndex = messages.value.length

  messages.value.push({
    role: 'assistant',
    text: '',
    quickReplies: []
  })

  await scrollToBottom()

  try {
    const response = await fetch(`${API_BASE_URL}/api/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        message: text,
        messages: buildConversationHistory(),
        aiRole: selectedRole.value
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP Error: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')

    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()

      if (done) {
        break
      }

      const chunk = decoder.decode(value, { stream: true })

      console.log('stream chunk:', chunk)

      buffer += chunk

      const events = buffer.split(/\r?\n\r?\n/)
      buffer = events.pop() || ''

      for (const eventText of events) {
        const lines = eventText.split(/\r?\n/)

        let eventName = ''
        let eventData = ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.replace('event:', '').trim()
          }

          if (line.startsWith('data:')) {
            eventData += line.replace('data:', '')
          }
        }

        if (eventName === 'message') {
          messages.value[assistantIndex].text += eventData
          await nextTick()
          await scrollToBottom()
        }

        if (eventName === 'done') {
          loading.value = false
          await scrollToBottom()
          return
        }

        if (eventName === 'error') {
          messages.value[assistantIndex].text += '\n[Streaming 發生錯誤]'
          loading.value = false
          await scrollToBottom()
          return
        }
      }
    }
  } catch (error) {
    messages.value[assistantIndex].text = '呼叫後端失敗，請確認 Spring Boot 是否已啟動。'
    console.error(error)
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function buildConversationHistory() {
  return messages.value
    .filter(msg => msg.role === 'user' || msg.role === 'assistant')
    .filter(msg => msg.text)
    .slice(-8)
    .map(msg => ({
      role: msg.role,
      content: msg.text
    }))
}

async function clickQuickReply(reply) {
  const text = `請介紹附近的${reply}資訊`

  messages.value.push({
    role: 'user',
    text,
    quickReplies: []
  })

  await scrollToBottom()
  await callChatApi(text)
}

const widgetPosition = ref({
  right: 24,
  bottom: 24
})

const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

function startDrag(event) {
  isDragging.value = true

  dragOffset.value = {
    x: event.clientX,
    y: event.clientY
  }

  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopDrag)
}

function onDrag(event) {
  if (!isDragging.value) return

  const deltaX = event.clientX - dragOffset.value.x
  const deltaY = event.clientY - dragOffset.value.y

  widgetPosition.value.right -= deltaX
  widgetPosition.value.bottom -= deltaY

  dragOffset.value = {
    x: event.clientX,
    y: event.clientY
  }
}

function stopDrag() {
  isDragging.value = false

  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
}
</script>

<template>
  <div
    class="chat-widget"
    :style="{
      right: widgetPosition.right + 'px',
      bottom: widgetPosition.bottom + 'px'
    }"
  >
    <button
      v-if="!isOpen"
      class="chat-toggle-button"
      @click="openChat"
      @mousedown="startDrag"
    >
      💬
    </button>

    <div v-if="isOpen" class="chat-window">
      <div class="chat-header">
        <div>
          <div class="chat-title">AI Chatbot Web Lab</div>
          <div class="chat-subtitle">智慧客服助手</div>
        </div>

        <button class="chat-close-button" @click="closeChat">×</button>
      </div>

      <div class="role-select">
        <label>角色：</label>
        <select v-model="selectedRole">
          <option value="default">一般助理</option>
          <option value="travel">旅遊導覽</option>
          <option value="customer_service">客服人員</option>
          <option value="medical">醫療助理</option>
        </select>
      </div>

      <div class="chat-box" ref="chatBoxRef">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message', msg.role]"
        >
          <div class="message-content">
            <div class="bubble">
              {{ msg.text }}
            </div>

            <div
              v-if="msg.quickReplies && msg.quickReplies.length > 0"
              class="inline-quick-replies"
            >
              <button
                v-for="reply in msg.quickReplies"
                :key="reply"
                class="quick-button"
                :disabled="loading"
                @click="clickQuickReply(reply)"
              >
                {{ reply }}
              </button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="message assistant">
          <div class="message-content">
            <div class="bubble cursor">▋</div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <input
          ref="inputRef"
          v-model="inputMessage"
          placeholder="請輸入訊息..."
          @keyup.enter="sendMessage()"
        />

        <button class="send-button" @click="sendMessage()" :disabled="loading">
          送出
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-widget {
  position: fixed;
  z-index: 9999;
  font-family: Arial, sans-serif;
}

.chat-toggle-button {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: none;
  background: #2563eb;
  color: white;
  font-size: 28px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
  user-select: none;
}

.chat-window {
  width: 420px;
  height: 620px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  background: #2563eb;
  color: white;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-title {
  font-size: 16px;
  font-weight: bold;
}

.chat-subtitle {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 2px;
}

.chat-close-button {
  border: none;
  background: transparent;
  color: white;
  font-size: 28px;
  cursor: pointer;
}

.role-select {
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #e5e7eb;
}

select {
  padding: 6px;
  border-radius: 6px;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
}

.message {
  display: flex;
  margin-bottom: 10px;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-content {
  max-width: 80%;
}

.bubble {
  padding: 10px 12px;
  border-radius: 12px;
  line-height: 1.5;
  word-break: break-word;
}

.user .bubble {
  background: #2563eb;
  color: white;
}

.assistant .bubble {
  background: #e5e7eb;
  color: #111827;
}

.cursor {
  width: fit-content;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0.2;
  }
}

.inline-quick-replies {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.quick-button {
  padding: 7px 12px;
  border: 1px solid #2563eb;
  background: white;
  color: #2563eb;
  border-radius: 999px;
  cursor: pointer;
}

.quick-button:hover {
  background: #eff6ff;
}

.quick-button:disabled {
  color: #9ca3af;
  border-color: #9ca3af;
  cursor: not-allowed;
}

.input-area {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #e5e7eb;
}

input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 6px;
}

.send-button {
  padding: 10px 16px;
  border: none;
  background: #2563eb;
  color: white;
  border-radius: 6px;
  cursor: pointer;
}

.send-button:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}
</style>