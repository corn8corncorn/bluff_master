<template>
  <div class="min-h-screen flex items-center justify-center p-3 sm:p-4">
    <div class="bg-white rounded-xl sm:rounded-2xl shadow-2xl p-6 sm:p-8 max-w-md w-full mx-2">
      <h2 class="text-xl sm:text-2xl font-bold text-gray-800 mb-4 sm:mb-6 text-center">加入房間</h2>

      <form @submit.prevent="handleJoin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">房間代碼</label>
          <input
            v-model="roomCode"
            type="text"
            required
            maxlength="6"
            class="w-full px-3 sm:px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-center text-xl sm:text-2xl font-mono tracking-widest uppercase"
            placeholder="ABCD12"
            @input="roomCode = roomCode.toUpperCase()"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">暱稱</label>
          <input
            v-model="nickname"
            type="text"
            required
            maxlength="20"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="輸入你的暱稱"
          />
        </div>

        <div class="flex space-x-3 sm:space-x-4 pt-3 sm:pt-4">
          <button
            type="button"
            @click="$router.back()"
            class="flex-1 px-3 sm:px-4 py-2 text-sm sm:text-base border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 active:bg-gray-100 transition-colors"
          >
            返回
          </button>
          <button
            type="submit"
            :disabled="loading"
            class="flex-1 px-3 sm:px-4 py-2 text-sm sm:text-base bg-blue-500 text-white rounded-lg hover:bg-blue-600 active:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {{ loading ? '加入中...' : '加入' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../stores/game'
import { useNotification } from '../composables/useNotification'

const router = useRouter()
const gameStore = useGameStore()
const { error: showError } = useNotification()

const roomCode = ref('')
const nickname = ref('')
const loading = ref(false)

async function handleJoin() {
  // Trim 暱稱和房間代碼，並更新 ref 值讓 UI 顯示 trim 後的結果
  nickname.value = nickname.value.trim()
  roomCode.value = roomCode.value.trim()
  
  if (!roomCode.value || !nickname.value) return

  loading.value = true
  try {
    const room = await gameStore.joinRoom(roomCode.value.toUpperCase(), nickname.value)
    router.push(`/room/${room.id}`)
  } catch (error) {
    showError(error.response?.data?.message || '加入房間失敗')
  } finally {
    loading.value = false
  }
}
</script>

