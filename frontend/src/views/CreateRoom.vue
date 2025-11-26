<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full">
      <h2 class="text-2xl font-bold text-gray-800 mb-6 text-center">創建房間</h2>

      <form @submit.prevent="handleCreate" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">暱稱</label>
          <input
            v-model="nickname"
            type="text"
            required
            maxlength="20"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            placeholder="輸入你的暱稱"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">遊戲模式</label>
          <div class="space-y-2">
            <label class="flex items-center p-3 border rounded-lg cursor-pointer hover:bg-gray-50">
              <input
                v-model="gameMode"
                type="radio"
                value="NORMAL"
                class="mr-3"
              />
              <div>
                <div class="font-medium">一般模式</div>
                <div class="text-sm text-gray-500">完整輪次，完整積分</div>
              </div>
            </label>
            <label class="flex items-center p-3 border rounded-lg cursor-pointer hover:bg-gray-50">
              <input
                v-model="gameMode"
                type="radio"
                value="QUICK"
                class="mr-3"
              />
              <div>
                <div class="font-medium">快速模式</div>
                <div class="text-sm text-gray-500">3-5 輪，快速結束</div>
              </div>
            </label>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">人數上限</label>
          <select
            v-model.number="maxPlayers"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
          >
            <option v-for="n in 9" :key="n + 1" :value="n + 1">{{ n + 1 }} 人</option>
          </select>
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
            class="flex-1 px-4 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 disabled:opacity-50"
          >
            {{ loading ? '創建中...' : '創建' }}
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

const nickname = ref('')
const gameMode = ref('NORMAL')
const maxPlayers = ref(2) // 默認 2 人，方便測試
const loading = ref(false)

async function handleCreate() {
  if (!nickname.value.trim()) return

  loading.value = true
  try {
    const room = await gameStore.createRoom(nickname.value, gameMode.value, maxPlayers.value)
    router.push(`/room/${room.id}`)
  } catch (error) {
    alert(error.response?.data?.message || '創建房間失敗')
  } finally {
    loading.value = false
  }
}
</script>

