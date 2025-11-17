<template>
  <div class="min-h-screen p-4">
    <div class="max-w-6xl mx-auto">
      <!-- 遊戲資訊 -->
      <div class="bg-white rounded-2xl shadow-xl p-6 mb-6">
        <div class="flex justify-between items-center">
          <div>
            <h2 class="text-2xl font-bold text-gray-800">第 {{ currentRound?.roundNumber || 0 }} 輪</h2>
            <p class="text-gray-600 mt-1">
              主講者：{{ currentRound?.speakerNickname }}
            </p>
          </div>
          <div class="text-right">
            <div class="text-sm text-gray-500">回合</div>
            <div class="text-2xl font-bold text-gray-800">
              {{ room?.currentRound || 0 }} / {{ room?.totalRounds || 0 }}
            </div>
          </div>
        </div>
      </div>

      <!-- 投票倒數 -->
      <div v-if="currentRound && !currentRound.isFinished && votingTimeLeft > 0" class="bg-yellow-100 border border-yellow-400 rounded-lg p-4 mb-6 text-center">
        <div class="text-lg font-semibold text-yellow-800">
          投票剩餘時間：{{ votingTimeLeft }} 秒
        </div>
      </div>

      <!-- 圖片展示 -->
      <div v-if="currentRound" class="bg-white rounded-2xl shadow-xl p-6 mb-6">
        <h3 class="text-xl font-bold text-gray-800 mb-4">選擇唬爛圖片</h3>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div
            v-for="(imageUrl, index) in currentRound.imageUrls"
            :key="index"
            @click="handleVote(imageUrl)"
            class="relative aspect-square rounded-lg overflow-hidden cursor-pointer transform transition-all hover:scale-105"
            :class="{
              'ring-4 ring-purple-500': selectedImage === imageUrl,
              'opacity-50': hasVoted && selectedImage !== imageUrl
            }"
          >
            <img :src="imageUrl" alt="遊戲圖片" class="w-full h-full object-cover" />
            <div v-if="selectedImage === imageUrl" class="absolute inset-0 bg-purple-500 bg-opacity-30 flex items-center justify-center">
              <div class="bg-white rounded-full p-2">
                <svg class="w-8 h-8 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </div>
            </div>
          </div>
        </div>

        <div v-if="hasVoted" class="mt-4 text-center text-green-600 font-semibold">
          ✓ 已投票
        </div>
      </div>

      <!-- 投票結果 -->
      <div v-if="currentRound?.isFinished" class="bg-white rounded-2xl shadow-xl p-6 mb-6">
        <h3 class="text-xl font-bold text-gray-800 mb-4">投票結果</h3>
        <div class="mb-4 p-4 bg-purple-50 rounded-lg">
          <div class="font-semibold text-purple-800">假圖是：</div>
          <img :src="currentRound.fakeImageUrl" alt="假圖" class="mt-2 w-32 h-32 object-cover rounded-lg" />
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
          <div
            v-for="(imageUrl, index) in currentRound.imageUrls"
            :key="index"
            class="relative aspect-square rounded-lg overflow-hidden"
            :class="{
              'ring-4 ring-red-500': imageUrl === currentRound.fakeImageUrl,
              'ring-4 ring-green-500': imageUrl !== currentRound.fakeImageUrl && currentRound.votes && Object.values(currentRound.votes).includes(imageUrl)
            }"
          >
            <img :src="imageUrl" alt="遊戲圖片" class="w-full h-full object-cover" />
            <div v-if="imageUrl === currentRound.fakeImageUrl" class="absolute top-2 left-2 bg-red-500 text-white px-2 py-1 rounded text-sm font-semibold">
              假圖
            </div>
          </div>
        </div>

        <div v-if="isHost" class="text-center">
          <button
            @click="handleNextRound"
            class="px-6 py-3 bg-purple-500 text-white rounded-lg font-semibold hover:bg-purple-600"
          >
            {{ isGameFinished ? '查看結算' : '下一輪' }}
          </button>
        </div>
      </div>

      <!-- 玩家分數 -->
      <div class="bg-white rounded-2xl shadow-xl p-6">
        <h3 class="text-xl font-bold text-gray-800 mb-4">分數排行</h3>
        <div class="space-y-2">
          <div
            v-for="(player, index) in sortedPlayers"
            :key="player.id"
            class="flex items-center justify-between p-3 rounded-lg"
            :class="{
              'bg-yellow-100': index === 0,
              'bg-gray-50': index > 0
            }"
          >
            <div class="flex items-center">
              <div class="w-8 h-8 rounded-full bg-purple-500 text-white flex items-center justify-center font-bold mr-3">
                {{ index + 1 }}
              </div>
              <div>
                <div class="font-semibold">{{ player.nickname }}</div>
                <div class="text-sm text-gray-500">
                  {{ player.isOnline ? '在線' : '離線' }}
                </div>
              </div>
            </div>
            <div class="text-2xl font-bold text-purple-600">
              {{ player.score }}
            </div>
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

const selectedImage = ref(null)
const hasVoted = ref(false)
const votingTimeLeft = ref(10)
let votingTimer = null

const room = computed(() => gameStore.room)
const currentRound = computed(() => gameStore.currentRound)
const isHost = computed(() => gameStore.isHost)

const sortedPlayers = computed(() => {
  if (!room.value?.players) return []
  return [...room.value.players].sort((a, b) => (b.score || 0) - (a.score || 0))
})

const isGameFinished = computed(() => {
  return room.value?.status === 'FINISHED' || 
         (room.value?.currentRound || 0) >= (room.value?.totalRounds || 0)
})

const isSpeaker = computed(() => {
  return currentRound.value?.speakerId === gameStore.currentPlayer?.id
})

onMounted(async () => {
  const roomId = route.params.roomId
  await gameStore.fetchRoom(roomId)
  gameStore.connectWebSocket(roomId, gameStore.currentPlayer?.id)

  // 如果是房主且沒有當前回合，開始第一輪
  if (isHost.value && !currentRound.value && room.value?.status === 'PLAYING') {
    gameStore.startRound()
  }

  // 開始投票倒數
  if (currentRound.value && !currentRound.value.isFinished) {
    startVotingTimer()
  }
})

onUnmounted(() => {
  if (votingTimer) {
    clearInterval(votingTimer)
  }
  gameStore.disconnectWebSocket()
})

function startVotingTimer() {
  votingTimeLeft.value = 10
  votingTimer = setInterval(() => {
    votingTimeLeft.value--
    if (votingTimeLeft.value <= 0) {
      clearInterval(votingTimer)
      // 自動結束投票
      if (isHost.value && currentRound.value && !currentRound.value.isFinished) {
        gameStore.finishRound()
      }
    }
  }, 1000)
}

function handleVote(imageUrl) {
  if (hasVoted.value || isSpeaker.value || currentRound.value?.isFinished) return

  selectedImage.value = imageUrl
  hasVoted.value = true
  gameStore.vote(imageUrl)
}

function handleNextRound() {
  if (isGameFinished.value) {
    // 顯示結算頁面
    alert('遊戲結束！')
    router.push('/')
  } else {
    gameStore.startRound()
    selectedImage.value = null
    hasVoted.value = false
    startVotingTimer()
  }
}
</script>

