<template>
  <div class="min-h-screen p-4">
    <div class="max-w-4xl mx-auto">
      <!-- 房間資訊 -->
      <div class="bg-white rounded-2xl shadow-xl p-6 mb-6">
        <div class="flex justify-between items-center mb-4">
          <div>
            <h2 class="text-2xl font-bold text-gray-800">房間代碼</h2>
            <p class="text-3xl font-mono tracking-widest text-purple-600 mt-2">
              {{ room?.roomCode }}
            </p>
          </div>
          <div class="text-right">
            <div class="text-sm text-gray-500">人數</div>
            <div class="text-2xl font-bold text-gray-800">
              {{ room?.players?.length || 0 }} / {{ room?.maxPlayers }}
            </div>
          </div>
        </div>

        <div class="text-sm text-gray-600">
          模式：{{ room?.gameMode === 'NORMAL' ? '一般模式' : '快速模式' }}
        </div>
      </div>

      <!-- 玩家列表 -->
      <div class="bg-white rounded-2xl shadow-xl p-6 mb-6">
        <h3 class="text-xl font-bold text-gray-800 mb-4">玩家列表</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div
            v-for="player in room?.players"
            :key="player.id"
            class="p-4 border rounded-lg"
            :class="{
              'border-purple-500 bg-purple-50': player.isHost,
              'border-gray-200': !player.isHost,
              'opacity-50': !player.isOnline
            }"
          >
            <div class="flex items-center justify-between">
              <div>
                <div class="font-semibold">
                  {{ player.nickname }}
                  <span v-if="player.isHost" class="text-xs bg-purple-500 text-white px-2 py-1 rounded ml-2">
                    房主
                  </span>
                  <span v-if="!player.isOnline" class="text-xs bg-gray-400 text-white px-2 py-1 rounded ml-2">
                    離線
                  </span>
                </div>
                <div class="text-sm text-gray-500 mt-1">
                  圖片：{{ player.imageUrls?.length || 0 }} 張
                </div>
              </div>
              <div class="text-right">
                <div v-if="player.isReady" class="text-green-500 font-semibold">✓ 已準備</div>
                <div v-else class="text-gray-400">未準備</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 圖片上傳 -->
      <div class="bg-white rounded-2xl shadow-xl p-6 mb-6">
        <h3 class="text-xl font-bold text-gray-800 mb-4">上傳圖片</h3>
        <div class="mb-4">
          <div class="text-sm text-gray-600 mb-2">
            需要上傳：{{ requiredImages }} 張（單張 < 5MB）
          </div>
          <div class="text-sm text-gray-600">
            已上傳：{{ currentPlayer?.imageUrls?.length || 0 }} 張
          </div>
        </div>

        <input
          ref="fileInput"
          type="file"
          multiple
          accept="image/*"
          @change="handleFileSelect"
          class="hidden"
        />

        <button
          @click="$refs.fileInput.click()"
          :disabled="uploading"
          class="w-full px-4 py-3 border-2 border-dashed border-gray-300 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors disabled:opacity-50"
        >
          {{ uploading ? '上傳中...' : '選擇圖片' }}
        </button>

        <div v-if="currentPlayer?.imageUrls?.length > 0" class="mt-4 grid grid-cols-4 gap-2">
          <div
            v-for="(url, index) in currentPlayer.imageUrls"
            :key="index"
            class="relative aspect-square rounded-lg overflow-hidden"
          >
            <img :src="url" alt="上傳的圖片" class="w-full h-full object-cover" />
          </div>
        </div>
      </div>

      <!-- 操作按鈕 -->
      <div class="bg-white rounded-2xl shadow-xl p-6">
        <div class="space-y-4">
          <button
            v-if="!currentPlayer?.isReady"
            @click="handleReady"
            :disabled="!canReady"
            class="w-full px-6 py-3 bg-green-500 text-white rounded-lg font-semibold hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            準備
          </button>

          <button
            v-if="isHost && canStartGame"
            @click="handleStartGame"
            class="w-full px-6 py-3 bg-purple-500 text-white rounded-lg font-semibold text-lg hover:bg-purple-600"
          >
            開始遊戲
          </button>

          <div v-if="isHost && !canStartGame && allPlayersReady" class="text-center text-sm text-gray-500">
            等待所有玩家準備完成
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../stores/game'

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()

const fileInput = ref(null)
const uploading = ref(false)

const room = computed(() => gameStore.room)
const currentPlayer = computed(() => gameStore.currentPlayer)
const isHost = computed(() => gameStore.isHost)
const allPlayersReady = computed(() => gameStore.allPlayersReady)
const canStartGame = computed(() => gameStore.canStartGame)

const requiredImages = computed(() => {
  if (!room.value) return 0
  return room.value.gameMode === 'NORMAL'
    ? room.value.maxPlayers + 2
    : 5
})

const canReady = computed(() => {
  return currentPlayer.value?.imageUrls?.length >= requiredImages.value
})

onMounted(async () => {
  const roomId = route.params.roomId
  await gameStore.fetchRoom(roomId)
  gameStore.connectWebSocket(roomId, currentPlayer.value?.id)
})

onUnmounted(() => {
  gameStore.disconnectWebSocket()
})

async function handleFileSelect(event) {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  uploading.value = true
  try {
    await gameStore.uploadImages(files)
    // 重新獲取房間資訊
    await gameStore.fetchRoom(room.value.id)
  } catch (error) {
    alert(error.response?.data?.message || '上傳失敗')
  } finally {
    uploading.value = false
    event.target.value = '' // 重置 input
  }
}

async function handleReady() {
  try {
    await gameStore.setReady()
  } catch (error) {
    alert(error.response?.data?.message || '操作失敗')
  }
}

async function handleStartGame() {
  try {
    await gameStore.startGame()
    router.push(`/game/${room.value.id}`)
  } catch (error) {
    alert(error.response?.data?.message || '開始遊戲失敗')
  }
}
</script>

