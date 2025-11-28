<template>
  <div class="min-h-screen p-2 sm:p-4">
    <div class="max-w-4xl mx-auto">
      <!-- 房間資訊 -->
      <div class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6 mb-3 sm:mb-6">
        <div class="flex flex-row justify-between items-center mb-3 sm:mb-4 gap-3 sm:gap-4">
          <div class="flex-1">
            <h2 class="text-lg sm:text-2xl font-bold text-gray-800">房間代碼</h2>
            <p 
              @click="copyRoomCode"
              class="text-xl sm:text-3xl font-mono tracking-widest text-purple-600 mt-2 cursor-pointer hover:text-purple-700 hover:underline transition-colors select-all"
              :title="'點擊複製：' + room?.roomCode"
            >
              {{ room?.roomCode }}
            </p>
          </div>
          <div class="text-right flex-shrink-0">
            <div class="text-xs sm:text-sm text-gray-500">人數</div>
            <div class="text-lg sm:text-2xl font-bold text-gray-800">
              {{ room?.players?.length || 0 }} / {{ room?.maxPlayers }}
            </div>
          </div>
        </div>

        <div class="text-xs sm:text-sm text-gray-600">
          模式：{{ room?.gameMode === 'NORMAL' ? '一般模式' : '快速模式' }}
        </div>
      </div>

      <!-- 玩家列表 -->
      <div class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6 mb-3 sm:mb-6">
        <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">玩家列表</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div
            v-for="player in room?.players"
            :key="player.id"
            class="p-4 border rounded-lg"
            :class="{
              'border-purple-500 bg-purple-50': currentPlayer?.id === player.id,
              'border-gray-200': currentPlayer?.id !== player.id,
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
      <div v-if="currentPlayer" class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6 mb-3 sm:mb-6">
        <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">上傳圖片</h3>
        <div class="mb-3 sm:mb-4">
          <div class="text-xs sm:text-sm text-gray-600 mb-2">
            需要上傳：{{ requiredImages }} 張
          </div>
          <div class="text-xs sm:text-sm text-gray-600">
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
          :disabled="uploading || (currentPlayer?.isReady && room?.status === 'WAITING')"
          class="w-full px-3 sm:px-4 py-2 sm:py-3 text-sm sm:text-base border-2 border-dashed border-gray-300 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ uploading ? '上傳中...' : (currentPlayer?.isReady && room?.status === 'WAITING') ? '已準備，無法上傳' : '選擇圖片' }}
        </button>

        <div v-if="currentPlayer?.imageUrls?.length > 0" class="mt-3 sm:mt-4 grid grid-cols-4 gap-1.5 sm:gap-2">
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
      <div class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6">
        <div class="space-y-3 sm:space-y-4">
          <!-- 如果找不到玩家，顯示重新加入表單 -->
          <div v-if="!currentPlayer && room" class="space-y-4">
            <div class="text-center text-gray-600 mb-4">
              <p class="mb-2">無法識別您的身份，請重新加入房間</p>
              <p class="text-sm text-gray-500">房間代碼：{{ room.roomCode }}</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">輸入您的暱稱</label>
              <input
                v-model="rejoinNickname"
                type="text"
                maxlength="20"
                @keyup.enter="handleRejoin"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="輸入您的暱稱"
              />
            </div>
            <button
              @click="handleRejoin"
              :disabled="!rejoinNickname.trim() || rejoining"
              class="w-full px-6 py-3 bg-purple-500 text-white rounded-lg font-semibold hover:bg-purple-600 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ rejoining ? '加入中...' : '重新加入' }}
            </button>
          </div>

          <!-- 正常操作按鈕 -->
          <template v-else>
            <button
              v-if="!currentPlayer?.isReady"
              @click="handleReady"
              :disabled="!canReady"
              class="w-full px-4 sm:px-6 py-2.5 sm:py-3 bg-green-500 text-white rounded-lg text-sm sm:text-base font-semibold hover:bg-green-600 active:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              準備
            </button>

            <button
              v-if="currentPlayer?.isReady && room?.status === 'WAITING'"
              @click="handleCancelReady"
              class="w-full px-4 sm:px-6 py-2.5 sm:py-3 bg-gray-500 text-white rounded-lg text-sm sm:text-base font-semibold hover:bg-gray-600 active:bg-gray-700 transition-colors"
            >
              取消準備
            </button>
            
            <!-- 遊戲進行中，顯示返回遊戲按鈕 -->
            <button
              v-if="room?.status === 'PLAYING'"
              @click="router.push(`/game/${room.id}`)"
              class="w-full px-4 sm:px-6 py-2.5 sm:py-3 bg-purple-500 text-white rounded-lg text-sm sm:text-base font-semibold hover:bg-purple-600 active:bg-purple-700 transition-colors"
            >
              返回遊戲
            </button>

            <button
              v-if="isHost && canStartGame"
              @click="handleStartGame"
              class="w-full px-4 sm:px-6 py-2.5 sm:py-3 bg-purple-500 text-white rounded-lg text-base sm:text-lg font-semibold hover:bg-purple-600 active:bg-purple-700 transition-colors"
            >
              開始遊戲
            </button>

            <div v-if="isHost && !canStartGame && allPlayersReady" class="text-center text-sm text-gray-500">
              等待所有玩家準備完成
            </div>

            <!-- 退出房間按鈕 -->
            <button
              @click="handleLeave"
              class="w-full px-4 sm:px-6 py-2.5 sm:py-3 bg-red-500 text-white rounded-lg text-sm sm:text-base font-semibold hover:bg-red-600 active:bg-red-700 mt-3 sm:mt-4 transition-colors"
            >
              退出房間
            </button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../stores/game'
import { useNotification } from '../composables/useNotification'

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()
const { error: showError, success: showSuccess } = useNotification()

const fileInput = ref(null)
const uploading = ref(false)
const rejoinNickname = ref('')
const rejoining = ref(false)

const room = computed(() => gameStore.room)
const currentPlayer = computed(() => gameStore.currentPlayer)
const isHost = computed(() => gameStore.isHost)
const allPlayersReady = computed(() => gameStore.allPlayersReady)
const canStartGame = computed(() => gameStore.canStartGame)

// 監聽房間更新，檢測新玩家加入和遊戲開始
watch(() => gameStore.room, (newRoom, oldRoom) => {
  // 只在有舊房間資料時才檢測變化（避免首次載入時觸發）
  if (newRoom && oldRoom) {
    // 檢測遊戲開始：狀態從 WAITING 變為 PLAYING
    if (oldRoom.status === 'WAITING' && newRoom.status === 'PLAYING') {
      // 確保當前在 Room 頁面，然後導航到遊戲頁面
      if (route.name === 'Room' && newRoom.id) {
        router.push(`/game/${newRoom.id}`)
        return
      }
    }
    
    // 檢測新玩家加入
    if (oldRoom.players) {
      const oldPlayerIds = oldRoom.players.map(p => p.id) || []
      const newPlayerIds = newRoom.players?.map(p => p.id) || []
      
      // 找出新加入的玩家
      const newPlayerIdsList = newPlayerIds.filter(id => !oldPlayerIds.includes(id))
      if (newPlayerIdsList.length > 0 && newRoom.players) {
        const newPlayers = newRoom.players.filter(p => newPlayerIdsList.includes(p.id))
        newPlayers.forEach(player => {
          // 不顯示自己加入的通知
          if (currentPlayer.value && player.id !== currentPlayer.value.id) {
            showSuccess(`${player.nickname} 已加入房間`)
          }
        })
      }
    }
  }
}, { deep: true })

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
  
  // 如果找不到玩家，不連接 WebSocket
  if (currentPlayer.value) {
    await gameStore.connectWebSocket(roomId, currentPlayer.value.id)
  }
})

onUnmounted(() => {
  gameStore.disconnectWebSocket()
})

async function handleFileSelect(event) {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  // 檢查當前玩家是否存在
  if (!currentPlayer.value || !currentPlayer.value.id) {
    showError('玩家資訊未載入，請重新加入房間')
    event.target.value = ''
    return
  }

  // 檢查玩家是否已準備（只有在等待狀態下才限制）
  if (currentPlayer.value.isReady && room.value?.status === 'WAITING') {
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

async function handleLeave() {
  if (!room.value || !room.value.id) {
    router.push('/')
    return
  }

  try {
    await gameStore.leaveRoom(room.value.id)
    showSuccess('已退出房間')
    router.push('/')
  } catch (error) {
    console.error('退出房間錯誤:', error)
    const errorMessage = error.response?.data?.message || error.message || '退出房間失敗'
    showError(errorMessage)
    // 即使失敗也導航回首頁
    router.push('/')
  }
}

async function handleDeleteImage(imageUrl) {
  if (!currentPlayer.value || !currentPlayer.value.id) {
    showError('玩家資訊未載入，請重新加入房間')
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

async function handleRejoin() {
  if (!rejoinNickname.value.trim() || !room.value) return

  rejoining.value = true
  try {
    await gameStore.joinRoom(room.value.roomCode, rejoinNickname.value.trim())
    // 重新獲取房間資訊
    await gameStore.fetchRoom(room.value.id)
    // 連接 WebSocket
    await gameStore.connectWebSocket(room.value.id, currentPlayer.value?.id)
    showSuccess('重新加入成功')
    rejoinNickname.value = ''
  } catch (error) {
    console.error('重新加入錯誤:', error)
    const errorMessage = error.response?.data?.message || error.message || '重新加入失敗'
    showError(errorMessage)
  } finally {
    rejoining.value = false
  }
}

async function copyRoomCode() {
  if (!room.value?.roomCode) return
  
  try {
    await navigator.clipboard.writeText(room.value.roomCode)
    showSuccess('房間代碼已複製到剪貼板')
  } catch (error) {
    console.error('複製失敗:', error)
    // 降級方案：使用傳統方法
    const textArea = document.createElement('textarea')
    textArea.value = room.value.roomCode
    textArea.style.position = 'fixed'
    textArea.style.opacity = '0'
    document.body.appendChild(textArea)
    textArea.select()
    try {
      document.execCommand('copy')
      showSuccess('房間代碼已複製到剪貼板')
    } catch (err) {
      showError('複製失敗，請手動複製')
    }
    document.body.removeChild(textArea)
  }
}
</script>

