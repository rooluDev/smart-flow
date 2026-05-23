<template>
  <div v-if="visible" class="mcp-error-banner">
    <span class="banner-icon">⚠</span>
    <span class="banner-text">
      Google 서비스 연결이 필요합니다.
      <a class="relogin-link" @click="relogin">Google 계정 재연결</a>
    </span>
    <button class="close-btn" @click="visible = false">✕</button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useAuth } from '@/composables/useAuth'

const messageStore = useMessageStore()
const { initiateGoogleLogin } = useAuth()
const visible = ref(false)

const MCP_ERROR_CODES = ['MCP_001', 'MCP_002', 'MCP_003', 'MCP_004', 'MCP_005']

watch(() => messageStore.messages, (msgs) => {
  if (msgs.length === 0) return
  const last = msgs[msgs.length - 1]
  if (last?.isError) {
    const isMcpError = MCP_ERROR_CODES.some(code =>
      last.content?.includes(code) ||
      last.content?.includes('Google') ||
      last.content?.includes('Gmail') ||
      last.content?.includes('Calendar') ||
      last.content?.includes('Drive')
    )
    if (isMcpError) visible.value = true
  }
}, { deep: true })

function relogin() {
  initiateGoogleLogin()
}
</script>

<style scoped>
.mcp-error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #fff8e1;
  border-bottom: 1px solid #f6cc00;
  font-size: 13px;
  color: #7d4e00;
}

.banner-icon {
  font-size: 16px;
}

.banner-text {
  flex: 1;
}

.relogin-link {
  color: var(--color-accent);
  cursor: pointer;
  font-weight: 500;
  text-decoration: underline;
  margin-left: 6px;
}

.close-btn {
  background: transparent;
  border: none;
  color: #7d4e00;
  cursor: pointer;
  font-size: 14px;
  padding: 2px 6px;
}
</style>
