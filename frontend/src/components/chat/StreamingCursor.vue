<template>
  <span class="streaming-cursor"></span>
</template>

<style scoped>
.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--color-text-primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
