<template>
  <div class="min-h-screen p-2 sm:p-4">
    <div class="max-w-6xl mx-auto">
      <!-- 遊戲資訊 -->
      <div
        class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6 mb-3 sm:mb-6"
      >
        <div class="flex flex-row justify-between items-center gap-3 sm:gap-4">
          <div class="flex-1">
            <h2 class="text-lg sm:text-2xl font-bold text-gray-800">
              第 {{ currentRound?.roundNumber || room?.currentRound || 1 }} 輪
            </h2>
            <p class="text-sm sm:text-base text-gray-600 mt-1">
              主講者：{{ speakerNickname }}
            </p>
            <p class="text-xs sm:text-sm text-gray-500 mt-1">
              階段：{{ getPhaseName(currentRound?.phase) }}
            </p>
          </div>
          <div class="text-right flex-shrink-0">
            <div class="text-xs sm:text-sm text-gray-500">回合</div>
            <div class="text-lg sm:text-2xl font-bold text-gray-800">
              {{ currentRound?.roundNumber || room?.currentRound || 0 }} /
              {{ room?.totalRounds || 0 }}
            </div>
          </div>
        </div>
      </div>

      <!-- 投票倒數 -->
      <div
        v-if="currentRound?.phase === 'VOTING' && votingTimeLeft > 0"
        class="bg-yellow-100 border border-yellow-400 rounded-lg p-3 sm:p-4 mb-3 sm:mb-6 text-center"
      >
        <div class="text-base sm:text-lg font-semibold text-yellow-800">
          投票剩餘時間：{{ votingTimeLeft }} 秒
        </div>
      </div>

      <!-- 主講者界面 -->
      <div
        v-if="currentRound && isSpeaker && !currentRound.isFinished"
        class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6 mb-3 sm:mb-6"
      >
        <!-- 講故事階段 -->
        <div v-if="currentRound.phase === 'STORY_TELLING'">
          <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
            講故事階段
          </h3>
          <p class="text-sm sm:text-base text-gray-600 mb-3 sm:mb-4">
            請為您的四張圖片講述故事
          </p>
          <button
            @click="handleNextPhase"
            class="w-full sm:w-auto px-4 sm:px-6 py-2.5 sm:py-3 bg-purple-500 text-white rounded-lg text-sm sm:text-base font-semibold hover:bg-purple-600 active:bg-purple-700 transition-colors"
          >
            完成講故事，進入發問階段
          </button>
        </div>

        <!-- 發問階段 -->
        <div v-if="currentRound.phase === 'QUESTIONING'">
          <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
            發問階段
          </h3>
          <p class="text-sm sm:text-base text-gray-600 mb-3 sm:mb-4">
            等待其他玩家發問完畢
          </p>
          <button
            @click="handleStartVoting"
            class="w-full sm:w-auto px-4 sm:px-6 py-2.5 sm:py-3 bg-green-500 text-white rounded-lg text-sm sm:text-base font-semibold hover:bg-green-600 active:bg-green-700 transition-colors"
          >
            開始投票
          </button>
        </div>

        <!-- 投票階段 - 主講者查看圖片（不可投票） -->
        <div v-if="currentRound.phase === 'VOTING'">
          <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
            投票階段
          </h3>
          <p class="text-xs sm:text-sm text-gray-600 mb-3 sm:mb-4">
            其他玩家正在投票中，您可以查看圖片但無法投票
          </p>
          <div
            v-if="votingTimeLeft <= 0"
            class="mb-4 p-3 bg-red-100 border border-red-400 rounded-lg text-center"
          >
            <p class="text-red-800 font-semibold">
              投票時間已結束，等待公布結果...
            </p>
          </div>

          <!-- 圖片網格（所有圖片同時渲染，但用遮罩覆蓋直到全部加載完成） -->
          <div
            v-if="currentRound.imageUrls && currentRound.imageUrls.length > 0"
            class="grid grid-cols-2 gap-2 sm:gap-4 mb-3 sm:mb-4"
          >
            <div
              v-for="(imageUrl, index) in currentRound.imageUrls"
              :key="`speaker-img-${imageUrl}-${index}`"
              class="relative aspect-square rounded-lg overflow-hidden"
            >
              <!-- 在DOM中渲染圖片以觸發瀏覽器加載，但在未加載完成前完全隱藏 -->
              <img
                :src="imageUrl"
                alt="投票圖片"
                class="w-full h-full object-cover"
                @load="(event) => handleImageLoad(event, imageUrl, index)"
                @error="(event) => handleImageError(event, imageUrl, index)"
                :style="{
                  opacity: votingImagesLoaded ? 1 : 0,
                  transition: 'opacity 0.3s',
                }"
              />
              <!-- 加載中的遮罩層（始終顯示在圖片上方，直到所有圖片加載完成） -->
              <div
                v-if="!votingImagesLoaded"
                class="absolute inset-0 bg-gray-100 flex flex-col items-center justify-center z-10 rounded-lg"
              >
                <svg
                  class="w-12 h-12 text-gray-400 animate-spin mb-2"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                  />
                </svg>
                <p class="text-xs sm:text-sm text-gray-500 font-medium">
                  圖片加載中...
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 其他玩家界面 -->
      <div
        v-if="currentRound && !isSpeaker && !currentRound.isFinished"
        class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6 mb-3 sm:mb-6"
      >
        <!-- 調試信息 -->
        <div
          v-if="!currentRound.phase"
          class="mb-4 p-2 bg-yellow-50 text-yellow-800 text-sm"
        >
          警告：回合階段未定義 (phase: {{ currentRound.phase }})
        </div>

        <!-- 講故事階段 -->
        <div v-if="currentRound.phase === 'STORY_TELLING'">
          <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
            講故事階段
          </h3>
          <p class="text-sm sm:text-base text-gray-600 mb-3 sm:mb-4">
            請聆聽主講者講述故事
          </p>
        </div>

        <!-- 發問階段 -->
        <div v-if="currentRound.phase === 'QUESTIONING'">
          <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
            發問階段
          </h3>
          <p class="text-sm sm:text-base text-gray-600 mb-3 sm:mb-4">
            您可以向主講者發問
          </p>
        </div>

        <!-- 投票階段 -->
        <div v-if="currentRound.phase === 'VOTING'">
          <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
            選擇唬爛圖片
          </h3>
          <div
            v-if="votingTimeLeft <= 0"
            class="mb-4 p-3 bg-red-100 border border-red-400 rounded-lg text-center"
          >
            <p class="text-red-800 font-semibold">
              投票時間已結束，等待公布結果...
            </p>
          </div>
          <!-- 圖片網格（所有圖片同時渲染，但用遮罩覆蓋直到全部加載完成） -->
          <div
            v-if="currentRound.imageUrls && currentRound.imageUrls.length > 0"
            class="grid grid-cols-2 gap-2 sm:gap-4"
            :class="{
              'opacity-50 pointer-events-none': votingTimeLeft <= 0,
            }"
          >
            <div
              v-for="(imageUrl, index) in currentRound.imageUrls"
              :key="`img-${imageUrl}-${index}`"
              @click="handleVote(imageUrl)"
              class="relative aspect-square rounded-lg overflow-hidden cursor-pointer transform transition-all hover:scale-105"
              :class="{
                'ring-4 ring-purple-500':
                  selectedImage === imageUrl && votingImagesLoaded,
              }"
            >
              <!-- 在DOM中渲染圖片以觸發瀏覽器加載，但在未加載完成前完全隱藏 -->
              <img
                :src="imageUrl"
                alt="遊戲圖片"
                class="w-full h-full object-cover"
                @load="(event) => handleImageLoad(event, imageUrl, index)"
                @error="(event) => handleImageError(event, imageUrl, index)"
                :style="{
                  opacity: votingImagesLoaded ? 1 : 0,
                  transition: 'opacity 0.3s',
                }"
              />
              <!-- 加載中的遮罩層（始終顯示在圖片上方，直到所有圖片加載完成） -->
              <div
                v-if="!votingImagesLoaded"
                class="absolute inset-0 bg-gray-100 flex flex-col items-center justify-center z-10 rounded-lg"
              >
                <svg
                  class="w-12 h-12 text-gray-400 animate-spin mb-2"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                  />
                </svg>
                <p class="text-xs sm:text-sm text-gray-500 font-medium">
                  圖片加載中...
                </p>
              </div>
              <!-- 選中標記（只有所有圖片加載完成後才顯示） -->
              <div
                v-if="selectedImage === imageUrl && votingImagesLoaded"
                class="absolute inset-0 bg-purple-500 bg-opacity-30 flex items-center justify-center z-20"
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
            v-if="selectedImage && votingImagesLoaded"
            class="mt-4 text-center text-green-600 font-semibold"
          >
            ✓ 當前選擇：已選中一張圖片（可隨時更改）
          </div>
        </div>
      </div>

      <!-- 投票結果 -->
      <div
        v-if="currentRound?.phase === 'REVEALING' || currentRound?.isFinished"
        class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6 mb-3 sm:mb-6"
      >
        <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
          投票結果
        </h3>
        <div class="mb-4 p-4 bg-purple-50 rounded-lg">
          <div class="font-semibold text-purple-800">主講者說謊的圖片是：</div>
          <img
            :src="currentRound.speakerFakeImageUrl || currentRound.fakeImageUrl"
            alt="說謊圖片"
            class="mt-2 w-32 h-32 object-cover rounded-lg"
          />
        </div>

        <div class="grid grid-cols-2 gap-2 sm:gap-4 mb-4 sm:mb-6">
          <div
            v-for="(imageUrl, index) in currentRound.imageUrls"
            :key="index"
            class="relative aspect-square rounded-lg overflow-hidden"
            :class="{
              'ring-4 ring-red-500':
                imageUrl ===
                (currentRound.speakerFakeImageUrl || currentRound.fakeImageUrl),
              'ring-4 ring-green-500':
                imageUrl !==
                  (currentRound.speakerFakeImageUrl ||
                    currentRound.fakeImageUrl) &&
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
              v-if="
                imageUrl ===
                (currentRound.speakerFakeImageUrl || currentRound.fakeImageUrl)
              "
              class="absolute top-2 left-2 bg-red-500 text-white px-2 py-1 rounded text-sm font-semibold"
            >
              說謊圖片
            </div>
          </div>
        </div>

        <!-- 玩家投票結果 -->
        <div
          v-if="
            currentRound?.voteResults ||
            (currentRound?.votes && currentRound?.phase === 'REVEALING')
          "
          class="mb-6"
        >
          <h4
            class="text-base sm:text-lg font-semibold text-gray-800 mb-2 sm:mb-3"
          >
            玩家投票結果
          </h4>
          <div class="space-y-2 sm:space-y-3">
            <!-- 如果有完整的投票結果（revealResult 或 finishRound 後） -->
            <template v-if="currentRound?.voteResults">
              <div
                v-for="(result, playerId) in currentRound.voteResults"
                :key="playerId"
                class="p-2 sm:p-4 rounded-lg border-2"
                :class="{
                  'bg-green-50 border-green-300':
                    result.scoreChange > 0 && !result.isSpeaker,
                  'bg-red-50 border-red-300': result.scoreChange < 0,
                  'bg-yellow-50 border-yellow-300':
                    result.scoreChange === 0 &&
                    !result.isSpeaker &&
                    result.votedImageUrl,
                  'bg-purple-50 border-purple-300': result.isSpeaker,
                  'ring-4 ring-blue-300':
                    playerId === gameStore.currentPlayer?.id,
                }"
              >
                <div
                  class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 sm:gap-0"
                >
                  <div
                    class="flex flex-col sm:flex-row items-start sm:items-center gap-2 sm:gap-3 w-full sm:w-auto"
                  >
                    <div
                      class="font-semibold text-sm sm:text-base text-gray-800"
                    >
                      {{ result.playerNickname }}
                      <span
                        v-if="playerId === gameStore.currentPlayer?.id"
                        class="ml-1 sm:ml-2 text-xs bg-blue-500 text-white px-1.5 sm:px-2 py-0.5 sm:py-1 rounded"
                      >
                        我
                      </span>
                      <span
                        v-if="result.isSpeaker"
                        class="ml-1 sm:ml-2 text-xs bg-purple-500 text-white px-1.5 sm:px-2 py-0.5 sm:py-1 rounded"
                      >
                        主講者
                      </span>
                    </div>
                    <div
                      v-if="result.votedImageUrl"
                      class="flex items-center gap-1.5 sm:gap-2"
                    >
                      <span class="text-xs sm:text-sm text-gray-600"
                        >投票：</span
                      >
                      <img
                        :src="result.votedImageUrl"
                        alt="投票圖片"
                        class="w-8 h-8 sm:w-12 sm:h-12 object-cover rounded border-2"
                        :class="{
                          'border-green-500':
                            result.scoreChange > 0 && !result.isSpeaker,
                          'border-red-500':
                            result.scoreChange === 0 &&
                            !result.isSpeaker &&
                            result.votedImageUrl,
                          'border-purple-500': result.isSpeaker,
                          'border-blue-500':
                            playerId === gameStore.currentPlayer?.id,
                        }"
                      />
                      <span
                        v-if="result.scoreChange > 0 && !result.isSpeaker"
                        class="text-xs sm:text-sm font-semibold text-green-600"
                      >
                        ✓ 答對
                      </span>
                      <span
                        v-else-if="
                          result.scoreChange === 0 &&
                          !result.isSpeaker &&
                          result.votedImageUrl
                        "
                        class="text-xs sm:text-sm font-semibold text-red-600"
                      >
                        ✗ 答錯
                      </span>
                    </div>
                    <div
                      v-else-if="!result.isSpeaker"
                      class="text-xs sm:text-sm text-gray-500"
                    >
                      未投票
                    </div>
                  </div>
                  <div class="flex items-center gap-1.5 sm:gap-2">
                    <span class="text-xs sm:text-sm text-gray-600"
                      >得分變化：</span
                    >
                    <span
                      class="text-base sm:text-lg font-bold"
                      :class="{
                        'text-green-600': result.scoreChange > 0,
                        'text-red-600': result.scoreChange < 0,
                        'text-gray-600': result.scoreChange === 0,
                      }"
                    >
                      {{ result.scoreChange > 0 ? "+" : ""
                      }}{{ result.scoreChange }}
                    </span>
                  </div>
                </div>
              </div>
            </template>
            <!-- 如果只有投票信息但沒有 voteResults（不應該發生，但作為後備） -->
            <template v-else-if="currentRound?.votes && room?.players">
              <div
                v-for="player in room.players"
                :key="player.id"
                class="p-4 rounded-lg border-2 bg-gray-50 border-gray-300"
              >
                <div class="flex items-center justify-between">
                  <div class="flex items-center space-x-3">
                    <div class="font-semibold text-gray-800">
                      {{ player.nickname }}
                      <span
                        v-if="player.id === currentRound.speakerId"
                        class="ml-2 text-xs bg-purple-500 text-white px-2 py-1 rounded"
                      >
                        主講者
                      </span>
                    </div>
                    <div
                      v-if="currentRound.votes[player.id]"
                      class="flex items-center space-x-2"
                    >
                      <span class="text-sm text-gray-600">投票：</span>
                      <img
                        :src="currentRound.votes[player.id]"
                        alt="投票圖片"
                        class="w-12 h-12 object-cover rounded"
                      />
                    </div>
                    <div
                      v-else-if="player.id !== currentRound.speakerId"
                      class="text-sm text-gray-500"
                    >
                      未投票
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 進行下一輪遊戲按鈕 -->
        <div
          v-if="
            (currentRound?.isFinished || currentRound?.phase === 'REVEALING') &&
            !isLastRound
          "
          class="text-center mt-4 sm:mt-6"
        >
          <button
            v-if="isSpeaker"
            @click="handleNextRound"
            :disabled="!currentRound?.voteResults"
            class="w-full sm:w-auto px-4 sm:px-6 py-2.5 sm:py-3 bg-purple-500 text-white rounded-lg text-sm sm:text-base font-semibold hover:bg-purple-600 active:bg-purple-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            進行下一輪遊戲
          </button>
          <p v-else class="text-xs sm:text-sm text-gray-500 mt-2">
            等待主講者開始下一輪
          </p>
        </div>

        <!-- 最後一回合結算按鈕 -->
        <div
          v-if="
            (currentRound?.isFinished || currentRound?.phase === 'REVEALING') &&
            isLastRound
          "
          class="text-center mt-4 sm:mt-6"
        >
          <button
            @click="handleNextRound"
            :disabled="!currentRound?.voteResults"
            class="w-full sm:w-auto px-4 sm:px-6 py-2.5 sm:py-3 bg-purple-500 text-white rounded-lg text-sm sm:text-base font-semibold hover:bg-purple-600 active:bg-purple-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            查看結算
          </button>
          <p
            v-if="!currentRound?.voteResults"
            class="text-sm text-gray-500 mt-2"
          >
            計算中...
          </p>
        </div>
      </div>

      <!-- 主講者圖片展示（所有玩家都能看到） -->
      <div
        v-if="
          currentRound &&
          !currentRound.isFinished &&
          currentRound.imageUrls &&
          currentRound.imageUrls.length > 0 &&
          (currentRound.phase === 'STORY_TELLING' ||
            currentRound.phase === 'QUESTIONING')
        "
        class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6 mb-3 sm:mb-6"
      >
        <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
          主講者圖片
        </h3>
        <p class="text-sm sm:text-base text-gray-600 mb-3 sm:mb-4">
          主講者：{{ speakerNickname }}
        </p>
        <p class="text-xs sm:text-sm text-gray-500 mb-3 sm:mb-4">
          請為這4張圖片講述故事（包含1張假圖）
        </p>
        <div class="grid grid-cols-2 gap-2 sm:gap-4">
          <div
            v-for="(imageUrl, index) in currentRound.imageUrls"
            :key="index"
            class="relative aspect-square rounded-lg overflow-hidden cursor-pointer"
            @click="enlargedImage = imageUrl"
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
      <div class="bg-white rounded-xl sm:rounded-2xl shadow-xl p-3 sm:p-6">
        <h3 class="text-lg sm:text-xl font-bold text-gray-800 mb-3 sm:mb-4">
          分數排行
        </h3>
        <div class="space-y-2">
          <div
            v-for="(player, index) in sortedPlayers"
            :key="player.id"
            class="flex items-center justify-between p-2 sm:p-3 rounded-lg"
            :class="{
              'bg-yellow-100 border-2 border-yellow-400': isBluffMaster(player),
              'bg-gray-50': !isBluffMaster(player),
            }"
          >
            <div class="flex items-center">
              <div
                class="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center font-bold mr-2 sm:mr-3 text-xs sm:text-sm"
                :class="{
                  'bg-yellow-500 text-white': isBluffMaster(player),
                  'bg-purple-500 text-white': !isBluffMaster(player),
                }"
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

    <!-- 結算 Modal -->
    <div
      v-if="showFinalResultModal"
      class="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50"
    >
      <div
        class="bg-white rounded-xl sm:rounded-2xl shadow-2xl p-4 sm:p-8 max-w-2xl w-full mx-2 sm:mx-4"
        @click.stop
      >
        <div class="text-center">
          <h2
            class="text-2xl sm:text-4xl font-bold text-purple-600 mb-4 sm:mb-6"
          >
            遊戲結束！
          </h2>
          <div
            v-if="bluffMasters.length > 0"
            class="mb-6 sm:mb-8 p-4 sm:p-6 bg-gradient-to-r from-yellow-400 to-orange-500 rounded-lg"
          >
            <div class="text-4xl sm:text-6xl mb-3 sm:mb-4">🏆</div>
            <div class="text-xl sm:text-3xl font-bold text-white mb-2">
              {{ bluffMasters.length > 1 ? "唬爛王們" : "唬爛王" }}
            </div>
            <div class="space-y-2">
              <div
                v-for="master in bluffMasters"
                :key="master.id"
                class="text-lg sm:text-2xl font-semibold text-white"
              >
                {{ master.nickname }}
              </div>
            </div>
            <div class="text-base sm:text-xl text-white mt-3">
              最終分數：{{ bluffMasters[0].score }} 分
            </div>
          </div>

          <div class="mb-4 sm:mb-6">
            <h3
              class="text-lg sm:text-xl font-semibold text-gray-800 mb-3 sm:mb-4"
            >
              最終排行
            </h3>
            <div class="space-y-2">
              <div
                v-for="(player, index) in sortedPlayers"
                :key="player.id"
                class="flex items-center justify-between p-2 sm:p-3 rounded-lg"
                :class="{
                  'bg-yellow-100 border-2 border-yellow-400':
                    isBluffMaster(player),
                  'bg-gray-50': !isBluffMaster(player),
                }"
              >
                <div class="flex items-center">
                  <div
                    class="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center font-bold mr-2 sm:mr-3 text-xs sm:text-sm"
                    :class="{
                      'bg-yellow-500 text-white': isBluffMaster(player),
                      'bg-gray-400 text-white': !isBluffMaster(player),
                    }"
                  >
                    {{ index + 1 }}
                  </div>
                  <div>
                    <div class="font-semibold text-sm sm:text-base">
                      {{ player.nickname }}
                    </div>
                    <div class="text-xs sm:text-sm text-gray-500">
                      {{ player.isOnline ? "在線" : "離線" }}
                    </div>
                  </div>
                </div>
                <div class="text-lg sm:text-2xl font-bold text-purple-600">
                  {{ player.score }}
                </div>
              </div>
            </div>
          </div>

          <button
            @click="closeFinalResultModal"
            class="w-full sm:w-auto px-6 sm:px-8 py-2.5 sm:py-3 bg-purple-500 text-white rounded-lg font-semibold text-sm sm:text-lg hover:bg-purple-600 active:bg-purple-700 transition-colors"
          >
            返回首頁
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useGameStore } from "../stores/game";

const route = useRoute();
const router = useRouter();
const gameStore = useGameStore();

const selectedImage = ref(null);
const selectedSpeakerImage = ref(null);
const selectedSpeakerFakeImage = ref(null);
const votingTimeLeft = ref(10);
const enlargedImage = ref(null);
const shuffledSpeakerImages = ref([]);
const displayedSpeakerImages = ref([]);
const showFinalResultModal = ref(false);
const votingImagesLoaded = ref(false);
const imageLoadStatus = ref(new Map()); // 追蹤每張圖片的加載狀態
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

// 獲取唬爛王（所有分數最高且相同的玩家）
const bluffMasters = computed(() => {
  if (!sortedPlayers.value || sortedPlayers.value.length === 0) return [];
  const highestScore = sortedPlayers.value[0]?.score || 0;
  // 返回所有分數等於最高分的玩家
  return sortedPlayers.value.filter(
    (player) => (player.score || 0) === highestScore
  );
});

const isGameFinished = computed(() => {
  return (
    room.value?.status === "FINISHED" ||
    (room.value?.currentRound || 0) >= (room.value?.totalRounds || 0)
  );
});

// 是否為最後一回合
const isLastRound = computed(() => {
  if (!room.value || !currentRound.value) return false;
  return (currentRound.value.roundNumber || 0) >= (room.value.totalRounds || 0);
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

  // 顯示前4張圖片（四宮格）
  displayedSpeakerImages.value = speaker.imageUrls.slice(0, 4);
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

  // 如果進入投票階段，開始倒數並預載圖片
  if (currentRound.value?.phase === "VOTING") {
    votingImagesLoaded.value = false;
    preloadVotingImages();
    startVotingTimer();
  }
  // 如果進入公布結果階段，直接結束回合（不需要倒數）
  if (
    currentRound.value?.phase === "REVEALING" &&
    isHost.value &&
    !currentRound.value.isFinished
  ) {
    setTimeout(() => {
      gameStore.finishRound();
    }, 2000);
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
      selectedSpeakerImage.value = null;
      selectedSpeakerFakeImage.value = null;
      enlargedImage.value = null;
      votingTimeLeft.value = 10;

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
    }
  }
);

// 監聽房間狀態變化，當遊戲結束時確保分數已更新
watch(
  () => room.value?.status,
  async (newStatus, oldStatus) => {
    if (newStatus === "FINISHED" && oldStatus !== "FINISHED") {
      console.log("房間狀態變為 FINISHED，確保分數已更新");
      // 重新獲取房間資訊以確保包含最後一輪的分數
      if (room.value?.id) {
        try {
          await gameStore.fetchRoom(room.value.id);
          console.log("房間狀態 FINISHED: 已更新分數");
        } catch (error) {
          console.error("更新房間資訊失敗:", error);
        }
      }
    }
  }
);

// 監聽階段變化
watch(
  () => currentRound.value?.phase,
  async (newPhase, oldPhase) => {
    console.log("階段變化監聽觸發:", {
      oldPhase,
      newPhase,
      currentRoundId: currentRound.value?.id,
      currentRoundPhase: currentRound.value?.phase,
    });

    // 如果階段沒有實際變化，不執行後續邏輯
    if (newPhase === oldPhase) {
      return;
    }

    // 使用 nextTick 確保 DOM 更新完成
    await nextTick();

    if (newPhase === "REVEALING") {
      // 公布結果階段不需要倒數，直接結束回合
      console.log("進入公布結果階段，直接結束回合");
      // 清除投票倒數
      if (votingTimer) {
        clearInterval(votingTimer);
        votingTimer = null;
      }
      // 自動結束回合並計算分數（由房主觸發）
      if (
        isHost.value &&
        currentRound.value &&
        !currentRound.value.isFinished
      ) {
        // 延遲一點時間讓用戶看到結果
        setTimeout(() => {
          gameStore.finishRound();
        }, 2000);
      }
    }

    // 階段變化時重置投票狀態
    if (newPhase === "VOTING") {
      console.log("進入投票階段，重置投票狀態並開始倒數");
      selectedImage.value = null;
      selectedSpeakerFakeImage.value = null;
      // 重置圖片加載狀態
      votingImagesLoaded.value = false;
      imageLoadStatus.value.clear();
      // 等待nextTick確保DOM更新後再開始預載
      nextTick(() => {
        preloadVotingImages();
      });
      // 開始投票倒數
      startVotingTimer();
    } else if (votingTimer && newPhase !== "VOTING") {
      clearInterval(votingTimer);
      votingTimer = null;
      // 離開投票階段時重置加載狀態
      votingImagesLoaded.value = false;
      imageLoadStatus.value.clear();
    }

    // 如果是發問階段，清除投票相關狀態
    if (newPhase === "QUESTIONING") {
      console.log("進入發問階段");
      selectedImage.value = null;
      selectedSpeakerFakeImage.value = null;
      if (votingTimer) {
        clearInterval(votingTimer);
        votingTimer = null;
      }
    }
  },
  { immediate: true }
);

// 監聽投票圖片 URLs 變化，確保所有4張圖片都準備好後再預載
watch(
  () => currentRound.value?.imageUrls,
  async (newImageUrls, oldImageUrls) => {
    // 只有在投票階段且圖片URLs確實變化時才處理
    if (
      currentRound.value?.phase === "VOTING" &&
      newImageUrls &&
      newImageUrls.length > 0
    ) {
      // 確保有4張圖片URL（3張真圖 + 1張假圖）
      if (newImageUrls.length === 4) {
        // 檢查URLs是否真的變化（避免重複加載）
        if (JSON.stringify(newImageUrls) !== JSON.stringify(oldImageUrls)) {
          console.log("投票圖片URLs變化，開始預載所有4張圖片");
          votingImagesLoaded.value = false;
          await preloadVotingImages();
        } else if (!votingImagesLoaded.value) {
          // 如果URLs沒變化但還沒加載，也開始加載
          console.log("投票圖片URLs未變化，但尚未加載，開始預載");
          await preloadVotingImages();
        }
      } else {
        // 如果圖片數量不對，等待
        console.log(
          `投票圖片URLs數量不正確 (${newImageUrls.length}/4)，等待...`
        );
        votingImagesLoaded.value = false;
      }
    }
  },
  { deep: true, immediate: false }
);

onUnmounted(() => {
  if (votingTimer) {
    clearInterval(votingTimer);
  }
  gameStore.disconnectWebSocket();
});

function startVotingTimer() {
  votingTimeLeft.value = 10;
  if (votingTimer) {
    clearInterval(votingTimer);
  }
  votingTimer = setInterval(() => {
    votingTimeLeft.value--;
    if (votingTimeLeft.value <= 0) {
      clearInterval(votingTimer);
      // 投票時間結束，鎖定投票並自動進入公布結果階段
      // 由主講者或房主觸發（主講者優先）
      if (
        currentRound.value &&
        currentRound.value.phase === "VOTING" &&
        !currentRound.value.isFinished
      ) {
        if (isSpeaker.value || isHost.value) {
          console.log("投票時間結束，自動進入公布結果階段");
          gameStore.revealResult();
        }
      }
    }
  }, 1000);
}

function handleVote(imageUrl) {
  if (isSpeaker.value || currentRound.value?.isFinished) return;

  if (currentRound.value?.phase !== "VOTING") {
    return;
  }

  // 如果投票倒數已結束，不能再投票
  if (votingTimeLeft.value <= 0) {
    return;
  }

  // 允許多次更改投票，直到倒數結束
  selectedImage.value = imageUrl;
  gameStore.vote(imageUrl);
}

function isSpeakerImage(imageUrl) {
  // 檢查圖片是否屬於主講者（不在 currentRound.imageUrls 中的假圖）
  // 如果圖片在主講者的 imageUrls 中，則返回 true
  if (!gameStore.currentPlayer?.imageUrls || !imageUrl) {
    return false;
  }
  return gameStore.currentPlayer.imageUrls.includes(imageUrl);
}

function handleSpeakerFakeImageSelect(imageUrl) {
  if (currentRound.value?.phase !== "VOTING" || !isSpeaker.value) {
    return;
  }

  // 如果投票倒數已結束，不能再選擇
  if (votingTimeLeft.value <= 0) {
    return;
  }

  // 只能選擇主講者自己的圖片，不能選擇假圖
  if (!isSpeakerImage(imageUrl)) {
    return;
  }

  selectedSpeakerFakeImage.value = imageUrl;
  gameStore.vote(imageUrl);
}

function handleNextPhase() {
  if (isSpeaker.value && currentRound.value?.phase === "STORY_TELLING") {
    gameStore.nextPhase();
  }
}

function handleStartVoting() {
  if (isSpeaker.value && currentRound.value?.phase === "QUESTIONING") {
    gameStore.startVoting();
  }
}

// 預載所有投票圖片，重置加載狀態（圖片會在DOM中自動加載）
function preloadVotingImages() {
  // 確保有圖片URLs且數量正確（應該是4張）
  if (
    !currentRound.value?.imageUrls ||
    currentRound.value.imageUrls.length === 0
  ) {
    console.log("預載圖片: 圖片URLs不存在或為空，等待...");
    votingImagesLoaded.value = false;
    // 如果圖片URLs還沒準備好，等待一段時間後重試
    setTimeout(() => {
      if (
        currentRound.value?.imageUrls &&
        currentRound.value.imageUrls.length > 0
      ) {
        preloadVotingImages();
      }
    }, 500);
    return;
  }

  // 確保有4張圖片
  if (currentRound.value.imageUrls.length !== 4) {
    console.log(
      `預載圖片: 圖片數量不正確 (${currentRound.value.imageUrls.length}/4)，等待...`
    );
    votingImagesLoaded.value = false;
    setTimeout(() => {
      if (
        currentRound.value?.imageUrls &&
        currentRound.value.imageUrls.length === 4
      ) {
        preloadVotingImages();
      }
    }, 500);
    return;
  }

  console.log(
    "預載圖片: 重置加載狀態，等待DOM中的圖片加載",
    currentRound.value.imageUrls
  );
  // 重置加載狀態，圖片會在DOM中自動加載，@load事件會觸發handleImageLoad
  votingImagesLoaded.value = false;
  imageLoadStatus.value.clear();

  // 等待DOM更新後，檢查圖片是否已經在緩存中（如果是刷新頁面，圖片可能已加載）
  nextTick(() => {
    setTimeout(() => {
      checkCachedImages();
    }, 100);
  });

  // 設置超時，如果10秒內還沒加載完成，也顯示（避免無限等待）
  setTimeout(() => {
    if (!votingImagesLoaded.value) {
      console.warn("⚠️ 圖片加載超時，強制顯示已加載的圖片");
      votingImagesLoaded.value = true;
    }
  }, 10000);
}

// 檢查已緩存的圖片（如果圖片已經在瀏覽器緩存中，load事件可能不會觸發）
function checkCachedImages() {
  const imageUrls = currentRound.value?.imageUrls;
  if (!imageUrls || imageUrls.length === 0 || imageUrls.length !== 4) {
    return;
  }

  imageUrls.forEach((imageUrl, index) => {
    if (!imageLoadStatus.value.has(imageUrl)) {
      // 查找DOM中的img元素
      const imgElements = document.querySelectorAll(`img[src="${imageUrl}"]`);
      imgElements.forEach((img) => {
        if (img.complete && img.naturalWidth > 0 && img.naturalHeight > 0) {
          // 圖片已在緩存中並已加載
          imageLoadStatus.value.set(imageUrl, { loaded: true, index });
          console.log(
            `✅ 圖片 ${index + 1}/4 已在緩存中加載完成: ${imageUrl.substring(
              0,
              50
            )}...`
          );
          checkAllImagesLoaded();
        }
      });
    }
  });
}

// 處理圖片加載完成事件（從DOM中的img標籤觸發）
function handleImageLoad(event, imageUrl, index) {
  const imgElement = event.target;
  // 驗證圖片確實加載完成
  if (
    imgElement &&
    imgElement.complete &&
    imgElement.naturalWidth > 0 &&
    imgElement.naturalHeight > 0
  ) {
    if (!imageLoadStatus.value.has(imageUrl)) {
      imageLoadStatus.value.set(imageUrl, { loaded: true, index });
      const total = currentRound.value?.imageUrls?.length || 4;
      console.log(
        `✅ 圖片 ${index + 1}/${total} 加載完成: ${imageUrl.substring(
          0,
          50
        )}...`
      );
      checkAllImagesLoaded();
    }
  } else {
    // 如果圖片還沒完全加載，等待一下再檢查
    setTimeout(() => {
      if (
        imgElement &&
        imgElement.complete &&
        imgElement.naturalWidth > 0 &&
        imgElement.naturalHeight > 0
      ) {
        if (!imageLoadStatus.value.has(imageUrl)) {
          imageLoadStatus.value.set(imageUrl, { loaded: true, index });
          const total = currentRound.value?.imageUrls?.length || 4;
          console.log(
            `✅ 圖片 ${
              index + 1
            }/${total} 加載完成（延遲驗證）: ${imageUrl.substring(0, 50)}...`
          );
          checkAllImagesLoaded();
        }
      }
    }, 100);
  }
}

// 處理圖片加載錯誤事件
function handleImageError(event, imageUrl, index) {
  const total = currentRound.value?.imageUrls?.length || 4;
  console.error(
    `❌ 圖片 ${index + 1}/${total} 加載失敗: ${imageUrl.substring(0, 50)}...`
  );
  // 即使加載失敗也標記為已處理，避免無限等待
  if (!imageLoadStatus.value.has(imageUrl)) {
    imageLoadStatus.value.set(imageUrl, { loaded: false, index, error: true });
    // 加載失敗的圖片不計入成功加載，但仍需等待所有圖片都處理完成
    checkAllImagesLoaded();
  }
}

// 檢查所有圖片是否都已加載完成
function checkAllImagesLoaded() {
  const imageUrls = currentRound.value?.imageUrls;
  if (!imageUrls || imageUrls.length === 0) {
    return;
  }

  // 確保有4張圖片
  if (imageUrls.length !== 4) {
    console.log(`等待圖片URLs準備完成 (${imageUrls.length}/4)...`);
    return;
  }

  // 檢查所有4張圖片是否都已處理（加載成功或失敗）
  const allProcessed = imageUrls.every((url) => imageLoadStatus.value.has(url));

  if (allProcessed && imageUrls.length === imageLoadStatus.value.size) {
    // 檢查所有圖片是否都成功加載
    const loadedImages = imageUrls.filter((url) => {
      const status = imageLoadStatus.value.get(url);
      return status && status.loaded;
    });

    console.log(
      `圖片加載狀態: ${loadedImages.length}/${imageUrls.length} 成功`
    );

    // 只有當所有4張圖片都成功加載時，才顯示
    if (loadedImages.length === imageUrls.length) {
      console.log("✅ 所有圖片都已成功加載，準備顯示");
      // 額外等待一點時間確保所有圖片都完全渲染到瀏覽器
      setTimeout(() => {
        votingImagesLoaded.value = true;
        console.log("✅ 所有圖片現在已顯示");
      }, 300);
    } else {
      // 如果部分圖片加載失敗，等待更長時間後再顯示（至少顯示已加載的）
      console.warn(
        `⚠️ 部分圖片加載失敗 (${loadedImages.length}/${imageUrls.length})，等待後顯示`
      );
      setTimeout(() => {
        votingImagesLoaded.value = true;
      }, 2000);
    }
  } else {
    // 還有圖片未處理，繼續等待
    const processedCount = imageLoadStatus.value.size;
    console.log(`等待更多圖片加載... (${processedCount}/${imageUrls.length})`);
  }
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

function getPhaseName(phase) {
  const phaseMap = {
    STORY_TELLING: "講故事階段",
    QUESTIONING: "發問階段",
    VOTING: "投票階段",
    REVEALING: "公布結果階段",
    FINISHED: "已完成",
  };
  return phaseMap[phase] || "未知階段";
}

async function handleNextRound() {
  if (isGameFinished.value || isLastRound.value) {
    // 先獲取最新的房間資訊以確保包含最後一輪的分數
    console.log("handleNextRound: 獲取最新房間資訊以顯示結算");
    try {
      await gameStore.fetchRoom(room.value.id);
      // 等待一點時間確保分數已更新
      await new Promise((resolve) => setTimeout(resolve, 500));
      // 顯示結算 modal
      showFinalResultModal.value = true;
    } catch (error) {
      console.error("handleNextRound: 獲取房間資訊失敗", error);
      // 即使失敗也顯示 modal（使用當前已知的分數）
      showFinalResultModal.value = true;
    }
  } else {
    // 直接開始下一輪遊戲（使用玩家已上傳的圖片）
    if (isSpeaker.value) {
      console.log("handleNextRound: 開始下一輪遊戲");
      gameStore.startRound();
    } else {
      console.log("handleNextRound: 只有當前主講者可以開始下一輪");
    }
  }
}

function closeFinalResultModal() {
  showFinalResultModal.value = false;
  router.push("/");
}

// 判斷玩家是否為唬爛王（分數最高）
function isBluffMaster(player) {
  if (!bluffMasters.value || bluffMasters.value.length === 0) return false;
  return bluffMasters.value.some((master) => master.id === player.id);
}
</script>
