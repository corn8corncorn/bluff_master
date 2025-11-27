<template>
  <div class="min-h-screen p-4">
    <div class="max-w-6xl mx-auto">
      <!-- 遊戲資訊 -->
      <div class="bg-white rounded-2xl shadow-xl p-6 mb-6">
        <div class="flex justify-between items-center">
          <div>
            <h2 class="text-2xl font-bold text-gray-800">
              第 {{ currentRound?.roundNumber || room?.currentRound || 1 }} 輪
            </h2>
            <p class="text-gray-600 mt-1">主講者：{{ speakerNickname }}</p>
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
      <div
        v-if="currentRound && !currentRound.isFinished && votingTimeLeft > 0"
        class="bg-yellow-100 border border-yellow-400 rounded-lg p-4 mb-6 text-center"
      >
        <div class="text-lg font-semibold text-yellow-800">
          投票剩餘時間：{{ votingTimeLeft }} 秒
        </div>
      </div>

      <!-- 主講者圖片選擇界面 -->
      <div
        v-if="currentRound && isSpeaker && !currentRound.isFinished"
        class="bg-white rounded-2xl shadow-xl p-6 mb-6"
      >
        <h3 class="text-xl font-bold text-gray-800 mb-4">選擇您的圖片</h3>
        <p class="text-gray-600 mb-4">請從您的圖片中選擇一張進行唬爛</p>

        <!-- 調試信息（可選） -->
        <div
          v-if="shuffledSpeakerImages.length === 0"
          class="mb-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg"
        >
          <p class="text-yellow-800 text-sm">
            圖片載入中... 或您還沒有上傳圖片
            <br />
            <span class="text-xs"
              >當前玩家圖片數量:
              {{ gameStore.currentPlayer?.imageUrls?.length || 0 }}</span
            >
          </p>
        </div>

        <div
          v-if="shuffledSpeakerImages.length > 0"
          class="grid grid-cols-2 md:grid-cols-4 gap-4"
        >
          <div
            v-for="(imageUrl, index) in shuffledSpeakerImages"
            :key="index"
            @click="handleSpeakerImageClick(imageUrl)"
            class="relative aspect-square rounded-lg overflow-hidden cursor-pointer transform transition-all hover:scale-105"
            :class="{
              'ring-4 ring-purple-500': selectedSpeakerImage === imageUrl,
            }"
          >
            <img
              :src="imageUrl"
              alt="主講者圖片"
              class="w-full h-full object-cover"
            />
            <div
              v-if="selectedSpeakerImage === imageUrl"
              class="absolute inset-0 bg-purple-500 bg-opacity-30 flex items-center justify-center"
            >
              <div class="bg-white rounded-full p-2">
                <svg
                  class="w-8 h-8 text-purple-500"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M5 13l4 4L19 7"
                  />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 其他玩家投票界面 -->
      <div
        v-if="currentRound && !isSpeaker && !currentRound.isFinished"
        class="bg-white rounded-2xl shadow-xl p-6 mb-6"
      >
        <h3 class="text-xl font-bold text-gray-800 mb-4">選擇唬爛圖片</h3>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div
            v-for="(imageUrl, index) in currentRound.imageUrls"
            :key="index"
            @click="handleVote(imageUrl)"
            class="relative aspect-square rounded-lg overflow-hidden cursor-pointer transform transition-all hover:scale-105"
            :class="{
              'ring-4 ring-purple-500': selectedImage === imageUrl,
              'opacity-50': hasVoted && selectedImage !== imageUrl,
            }"
          >
            <img
              :src="imageUrl"
              alt="遊戲圖片"
              class="w-full h-full object-cover"
            />
            <div
              v-if="selectedImage === imageUrl"
              class="absolute inset-0 bg-purple-500 bg-opacity-30 flex items-center justify-center"
            >
              <div class="bg-white rounded-full p-2">
                <svg
                  class="w-8 h-8 text-purple-500"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M5 13l4 4L19 7"
                  />
                </svg>
              </div>
            </div>
          </div>
        </div>

        <div
          v-if="hasVoted"
          class="mt-4 text-center text-green-600 font-semibold"
        >
          ✓ 已投票
        </div>
      </div>

      <!-- 投票結果 -->
      <div
        v-if="currentRound?.isFinished"
        class="bg-white rounded-2xl shadow-xl p-6 mb-6"
      >
        <h3 class="text-xl font-bold text-gray-800 mb-4">投票結果</h3>
        <div class="mb-4 p-4 bg-purple-50 rounded-lg">
          <div class="font-semibold text-purple-800">假圖是：</div>
          <img
            :src="currentRound.fakeImageUrl"
            alt="假圖"
            class="mt-2 w-32 h-32 object-cover rounded-lg"
          />
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
          <div
            v-for="(imageUrl, index) in currentRound.imageUrls"
            :key="index"
            class="relative aspect-square rounded-lg overflow-hidden"
            :class="{
              'ring-4 ring-red-500': imageUrl === currentRound.fakeImageUrl,
              'ring-4 ring-green-500':
                imageUrl !== currentRound.fakeImageUrl &&
                currentRound.votes &&
                Object.values(currentRound.votes).includes(imageUrl),
            }"
          >
            <img
              :src="imageUrl"
              alt="遊戲圖片"
              class="w-full h-full object-cover"
            />
            <div
              v-if="imageUrl === currentRound.fakeImageUrl"
              class="absolute top-2 left-2 bg-red-500 text-white px-2 py-1 rounded text-sm font-semibold"
            >
              假圖
            </div>
          </div>
        </div>

        <div v-if="isHost" class="text-center">
          <button
            @click="handleNextRound"
            class="px-6 py-3 bg-purple-500 text-white rounded-lg font-semibold hover:bg-purple-600"
          >
            {{ isGameFinished ? "查看結算" : "下一輪" }}
          </button>
        </div>
      </div>

      <!-- 主講者圖片展示（所有玩家都能看到） -->
      <div
        v-if="
          currentRound &&
          !currentRound.isFinished &&
          displayedSpeakerImages.length > 0
        "
        class="bg-white rounded-2xl shadow-xl p-6 mb-6"
      >
        <h3 class="text-xl font-bold text-gray-800 mb-4">主講者圖片</h3>
        <p class="text-gray-600 mb-4">主講者：{{ speakerNickname }}</p>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div
            v-for="(imageUrl, index) in displayedSpeakerImages"
            :key="index"
            class="relative aspect-square rounded-lg overflow-hidden"
          >
            <img
              :src="imageUrl"
              alt="主講者圖片"
              class="w-full h-full object-cover"
            />
          </div>
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
              'bg-gray-50': index > 0,
            }"
          >
            <div class="flex items-center">
              <div
                class="w-8 h-8 rounded-full bg-purple-500 text-white flex items-center justify-center font-bold mr-3"
              >
                {{ index + 1 }}
              </div>
              <div>
                <div class="font-semibold">{{ player.nickname }}</div>
                <div class="text-sm text-gray-500">
                  {{ player.isOnline ? "在線" : "離線" }}
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

    <!-- 圖片放大彈窗 -->
    <div
      v-if="enlargedImage"
      @click="closeEnlargedImage"
      class="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50 cursor-pointer"
    >
      <div class="max-w-4xl max-h-[90vh] p-4">
        <img
          :src="enlargedImage"
          alt="放大圖片"
          class="max-w-full max-h-full object-contain rounded-lg cursor-pointer"
          @click="closeEnlargedImage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useGameStore } from "../stores/game";

const route = useRoute();
const router = useRouter();
const gameStore = useGameStore();

const selectedImage = ref(null);
const selectedSpeakerImage = ref(null);
const hasVoted = ref(false);
const votingTimeLeft = ref(10);
const enlargedImage = ref(null);
const shuffledSpeakerImages = ref([]);
const displayedSpeakerImages = ref([]);
let votingTimer = null;

const room = computed(() => gameStore.room);
const currentRound = computed(() => gameStore.currentRound);
const isHost = computed(() => gameStore.isHost);

// 洗牌函數
function shuffleArray(array) {
  const shuffled = [...array];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
}

// 當主講者或圖片變化時，重新洗牌
function updateShuffledSpeakerImages() {
  if (
    isSpeaker.value &&
    gameStore.currentPlayer?.imageUrls &&
    gameStore.currentPlayer.imageUrls.length > 0
  ) {
    shuffledSpeakerImages.value = shuffleArray(
      gameStore.currentPlayer.imageUrls
    );
  } else {
    shuffledSpeakerImages.value = [];
  }
}

const sortedPlayers = computed(() => {
  if (!room.value?.players) return [];
  return [...room.value.players].sort((a, b) => {
    // 先按分數排序（降序）
    const scoreDiff = (b.score || 0) - (a.score || 0);
    if (scoreDiff !== 0) {
      return scoreDiff;
    }
    // 如果分數相同，房主優先
    if (a.isHost && !b.isHost) {
      return -1;
    }
    if (!a.isHost && b.isHost) {
      return 1;
    }
    // 如果都是房主或都不是房主，保持原順序
    return 0;
  });
});

const isGameFinished = computed(() => {
  return (
    room.value?.status === "FINISHED" ||
    (room.value?.currentRound || 0) >= (room.value?.totalRounds || 0)
  );
});

const isSpeaker = computed(() => {
  return currentRound.value?.speakerId === gameStore.currentPlayer?.id;
});

// 獲取主講者暱稱
const speakerNickname = computed(() => {
  // 優先使用 currentRound 中的 speakerNickname
  if (currentRound.value?.speakerNickname) {
    return currentRound.value.speakerNickname;
  }
  // 如果沒有speakerNickname，從房間玩家列表中查找
  if (currentRound.value?.speakerId && room.value?.players) {
    console.log("speakerNickname: 嘗試從房間玩家列表查找", {
      speakerId: currentRound.value.speakerId,
      speakerIdType: typeof currentRound.value.speakerId,
      players: room.value.players.map((p) => ({
        id: p.id,
        idType: typeof p.id,
        nickname: p.nickname,
        match:
          p.id === currentRound.value.speakerId ||
          String(p.id) === String(currentRound.value.speakerId),
      })),
    });

    // 嘗試精確匹配
    let speaker = room.value.players.find(
      (p) => p.id === currentRound.value.speakerId
    );

    // 如果精確匹配失敗，嘗試字符串匹配
    if (!speaker) {
      speaker = room.value.players.find(
        (p) => String(p.id) === String(currentRound.value.speakerId)
      );
    }

    if (speaker?.nickname) {
      // 同時更新 currentRound 的 speakerNickname（如果可能）
      if (currentRound.value && !currentRound.value.speakerNickname) {
        currentRound.value.speakerNickname = speaker.nickname;
      }
      console.log("speakerNickname: 找到主講者暱稱:", speaker.nickname);
      return speaker.nickname;
    } else {
      console.warn("speakerNickname: 在玩家列表中找不到對應的主講者", {
        speakerId: currentRound.value.speakerId,
        playersIds: room.value.players.map((p) => p.id),
      });
    }
  }
  // 如果還是找不到，返回空字符串或加載中
  console.warn("speakerNickname: 無法找到主講者暱稱", {
    hasCurrentRound: !!currentRound.value,
    speakerId: currentRound.value?.speakerId,
    hasPlayers: !!room.value?.players,
    playersCount: room.value?.players?.length,
    players: room.value?.players?.map((p) => ({
      id: p.id,
      nickname: p.nickname,
    })),
  });
  return "載入中...";
});

// 更新主講者展示圖片
function updateDisplayedSpeakerImages() {
  if (!currentRound.value?.speakerId || !room.value?.players) {
    displayedSpeakerImages.value = [];
    return;
  }

  const speaker = room.value.players.find(
    (p) => p.id === currentRound.value.speakerId
  );
  if (!speaker || !speaker.imageUrls || speaker.imageUrls.length === 0) {
    displayedSpeakerImages.value = [];
    return;
  }

  // 隨機排序並只取前4張（四宮格）
  const images = shuffleArray([...speaker.imageUrls]);
  displayedSpeakerImages.value = images.slice(0, 4);
}

onMounted(async () => {
  const roomId = route.params.roomId;
  await gameStore.fetchRoom(roomId);

  // 嘗試獲取當前的回合（如果存在）
  await gameStore.fetchCurrentRound(roomId);

  // 連接 WebSocket（現在會等待連接成功）
  await gameStore.connectWebSocket(roomId, gameStore.currentPlayer?.id);

  console.log("onMounted: WebSocket 連接狀態", {
    hasConnection: !!gameStore.connection,
    connected: gameStore.connection?.connected,
  });

  // 如果是房主且沒有當前回合，開始第一輪
  console.log("onMounted: 檢查是否需要開始回合", {
    isHost: isHost.value,
    hasCurrentRound: !!currentRound.value,
    roomStatus: room.value?.status,
    currentPlayer: gameStore.currentPlayer?.nickname,
  });

  if (isHost.value && !currentRound.value && room.value?.status === "PLAYING") {
    console.log("onMounted: 房主開始第一輪");
    try {
      gameStore.startRound();
      console.log("onMounted: startRound 已調用");
    } catch (error) {
      console.error("onMounted: 開始回合失敗", error);
    }
  } else {
    console.log("onMounted: 不需要開始回合", {
      reason: !isHost.value
        ? "不是房主"
        : currentRound.value
        ? "已有回合"
        : room.value?.status !== "PLAYING"
        ? "房間狀態不是PLAYING"
        : "未知原因",
    });
  }

  // 開始投票倒數
  if (currentRound.value && !currentRound.value.isFinished) {
    startVotingTimer();
  }

  // 初始化主講者圖片洗牌
  updateShuffledSpeakerImages();
  updateDisplayedSpeakerImages();
});

// 監聽回合變化，當新回合開始時重新洗牌主講者圖片並重置狀態
watch(
  [
    () => currentRound.value?.speakerId,
    () => gameStore.currentPlayer?.id,
    () => gameStore.currentPlayer?.imageUrls,
  ],
  () => {
    updateShuffledSpeakerImages();
    updateDisplayedSpeakerImages();
    // 重置主講者相關狀態
    selectedSpeakerImage.value = null;
    enlargedImage.value = null;
  },
  { deep: true }
);

// 監聽房間玩家變化，更新主講者展示圖片和暱稱
watch(
  [() => room.value?.players, () => currentRound.value?.speakerId],
  () => {
    updateDisplayedSpeakerImages();
    // 如果 currentRound 沒有 speakerNickname，嘗試從房間玩家列表中獲取
    if (
      currentRound.value &&
      !currentRound.value.speakerNickname &&
      currentRound.value.speakerId &&
      room.value?.players
    ) {
      const speaker = room.value.players.find(
        (p) => p.id === currentRound.value.speakerId
      );
      if (speaker?.nickname) {
        currentRound.value.speakerNickname = speaker.nickname;
        console.log("watch room.players: 更新主講者暱稱:", speaker.nickname);
      }
    }
  },
  { deep: true }
);

// 監聽回合 ID 變化，當新回合開始時重置投票狀態
watch(
  () => currentRound.value?.id,
  async (newRoundId, oldRoundId) => {
    if (newRoundId && newRoundId !== oldRoundId) {
      // 新回合開始，重置投票狀態
      selectedImage.value = null;
      hasVoted.value = false;
      selectedSpeakerImage.value = null;
      enlargedImage.value = null;

      // 如果當前玩家是主講者，重新獲取房間信息以確保圖片數據是最新的
      if (isSpeaker.value) {
        try {
          await gameStore.fetchRoom(room.value.id);
        } catch (error) {
          console.error("重新獲取房間信息失敗:", error);
        }
      }

      // 重新洗牌主講者圖片
      updateShuffledSpeakerImages();
      updateDisplayedSpeakerImages();
      // 重新開始投票倒數
      if (currentRound.value && !currentRound.value.isFinished) {
        startVotingTimer();
      }
    }
  }
);

onUnmounted(() => {
  if (votingTimer) {
    clearInterval(votingTimer);
  }
  gameStore.disconnectWebSocket();
});

function startVotingTimer() {
  votingTimeLeft.value = 10;
  votingTimer = setInterval(() => {
    votingTimeLeft.value--;
    if (votingTimeLeft.value <= 0) {
      clearInterval(votingTimer);
      // 自動結束投票
      if (
        isHost.value &&
        currentRound.value &&
        !currentRound.value.isFinished
      ) {
        gameStore.finishRound();
      }
    }
  }, 1000);
}

function handleVote(imageUrl) {
  if (hasVoted.value || isSpeaker.value || currentRound.value?.isFinished)
    return;

  selectedImage.value = imageUrl;
  hasVoted.value = true;
  gameStore.vote(imageUrl);
}

function handleSpeakerImageClick(imageUrl) {
  // 點擊主講者圖片時放大顯示
  if (enlargedImage.value === imageUrl) {
    // 如果已經放大，點擊關閉
    closeEnlargedImage();
  } else {
    // 否則放大顯示
    enlargedImage.value = imageUrl;
    selectedSpeakerImage.value = imageUrl;
  }
}

function closeEnlargedImage() {
  enlargedImage.value = null;
}

function handleNextRound() {
  if (isGameFinished.value) {
    // 顯示結算頁面
    alert("遊戲結束！");
    router.push("/");
  } else {
    gameStore.startRound();
    selectedImage.value = null;
    selectedSpeakerImage.value = null;
    enlargedImage.value = null;
    hasVoted.value = false;
    startVotingTimer();
  }
}
</script>
