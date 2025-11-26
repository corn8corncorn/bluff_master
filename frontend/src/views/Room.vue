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
          :disabled="uploading || currentPlayer?.isReady"
          class="w-full px-4 py-3 border-2 border-dashed border-gray-300 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ uploading ? '上傳中...' : currentPlayer?.isReady ? '已準備，無法上傳' : '選擇圖片' }}
        </button>

        <div v-if="currentPlayer?.imageUrls?.length > 0" class="mt-4 grid grid-cols-4 gap-2">
          <div
            v-for="(url, index) in currentPlayer.imageUrls"
            :key="index"
            class="relative aspect-square rounded-lg overflow-hidden group"
          >
            <img :src="url" alt="上傳的圖片" class="w-full h-full object-cover" />
            <button
              v-if="!currentPlayer?.isReady"
              @click="handleDeleteImage(url)"
              class="absolute top-1 right-1 w-6 h-6 bg-red-500 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-600"
              title="刪除圖片"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
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
            v-if="currentPlayer?.isReady"
            @click="handleCancelReady"
            class="w-full px-6 py-3 bg-gray-500 text-white rounded-lg font-semibold hover:bg-gray-600"
          >
            取消準備
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
import { useNotification } from '../composables/useNotification'

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()
const { error: showError, success: showSuccess } = useNotification()

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
  await gameStore.connectWebSocket(roomId, currentPlayer.value?.id)
})

onUnmounted(() => {
  gameStore.disconnectWebSocket()
})

async function handleFileSelect(event) {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  // 檢查當前玩家是否存在
  if (!currentPlayer.value || !currentPlayer.value.id) {
    showError('玩家資訊未載入，請重新整理頁面')
    event.target.value = ''
    return
  }

  // 檢查玩家是否已準備
  if (currentPlayer.value.isReady) {
    showError('已準備狀態下無法上傳圖片，請先取消準備')
    event.target.value = ''
    return
  }

  uploading.value = true
  try {
    await gameStore.uploadImages(files)
    // 重新獲取房間資訊
    await gameStore.fetchRoom(room.value.id)
    showSuccess('圖片上傳成功')
  } catch (error) {
    console.error('上傳圖片錯誤:', error)
    console.error('錯誤詳情:', {
      message: error.message,
      response: error.response,
      data: error.response?.data,
      status: error.response?.status
    })
    // 從後端返回的錯誤訊息中提取詳細信息
    let errorMessage = '上傳失敗，請檢查圖片格式和大小'
    if (error.response?.data) {
      if (typeof error.response.data === 'string') {
        errorMessage = error.response.data
      } else if (error.response.data.message) {
        errorMessage = error.response.data.message
      } else if (error.response.data.error) {
        errorMessage = error.response.data.error
      }
    } else if (error.message) {
      errorMessage = error.message
    }
    showError(errorMessage)
  } finally {
    uploading.value = false
    event.target.value = '' // 重置 input
  }
}

async function handleReady() {
  try {
    await gameStore.setReady()
    // 重新獲取房間資訊以同步狀態
    await gameStore.fetchRoom(room.value.id)
    showSuccess('已準備')
  } catch (error) {
    console.error('準備錯誤:', error)
    const errorMessage = error.response?.data?.message || error.message || '操作失敗'
    showError(errorMessage)
  }
}

async function handleCancelReady() {
  try {
    await gameStore.cancelReady()
    // 重新獲取房間資訊以同步狀態
    await gameStore.fetchRoom(room.value.id)
    showSuccess('已取消準備')
  } catch (error) {
    console.error('取消準備錯誤:', error)
    const errorMessage = error.response?.data?.message || error.message || '操作失敗'
    showError(errorMessage)
  }
}

async function handleStartGame() {
  try {
    await gameStore.startGame()
    router.push(`/game/${room.value.id}`)
  } catch (error) {
    console.error('開始遊戲錯誤:', error)
    const errorMessage = error.response?.data?.message || error.message || '開始遊戲失敗'
    showError(errorMessage)
  }
}

async function handleDeleteImage(imageUrl) {
  if (!currentPlayer.value || !currentPlayer.value.id) {
    showError('玩家資訊未載入，請重新整理頁面')
    return
  }

  // 檢查玩家是否已準備
  if (currentPlayer.value.isReady) {
    showError('已準備狀態下無法刪除圖片，請先取消準備')
    return
  }

  try {
    await gameStore.deleteImage(imageUrl)
    // 重新獲取房間資訊
    await gameStore.fetchRoom(room.value.id)
    showSuccess('圖片已刪除')
  } catch (error) {
    console.error('刪除圖片錯誤:', error)
    const errorMessage = error.response?.data?.message || error.message || '刪除圖片失敗'
    showError(errorMessage)
  }
}
</script>

