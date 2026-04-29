<template>
  <div class="page">
    <h1>Demo Website Page</h1>
    <p>這裡模擬一般網站頁面，右下角是 AI Chatbot Widget。</p>

    <FloatingChatWidget />
  </div>
</template>

<script setup>
import FloatingChatWidget from './components/FloatingChatWidget.vue'
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 40px;
  background: #f3f4f6;
}
</style>