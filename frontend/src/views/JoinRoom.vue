<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full">
      <h2 class="text-2xl font-bold text-gray-800 mb-6 text-center">加入房間</h2>

      <form @submit.prevent="handleJoin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">房間代碼</label>
          <input
            v-model="roomCode"
            type="text"
            required
            maxlength="6"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-center text-2xl font-mono tracking-widest uppercase"
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

        <div class="flex space-x-4 pt-4">
          <button
            type="button"
            @click="$router.back()"
            class="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
          >
            返回
          </button>
          <button
            type="submit"
            :disabled="loading"
            class="flex-1 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50"
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

const router = useRouter()
const gameStore = useGameStore()

const roomCode = ref('')
const nickname = ref('')
const loading = ref(false)

async function handleJoin() {
  if (!roomCode.value.trim() || !nickname.value.trim()) return

  loading.value = true
  try {
    const room = await gameStore.joinRoom(roomCode.value.toUpperCase(), nickname.value)
    router.push(`/room/${room.id}`)
  } catch (error) {
    alert(error.response?.data?.message || '加入房間失敗')
  } finally {
    loading.value = false
  }
}
</script>

