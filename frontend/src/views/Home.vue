<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full">
      <div class="text-center mb-8">
        <h1 class="text-4xl font-bold text-gray-800 mb-2">唬爛王</h1>
        <p class="text-gray-600">Bluff Master</p>
      </div>

      <div class="space-y-4">
        <button
          @click="$router.push('/create')"
          class="w-full bg-gradient-to-r from-purple-500 to-pink-500 text-white py-4 rounded-lg font-semibold text-lg hover:from-purple-600 hover:to-pink-600 transition-all transform hover:scale-105"
        >
          創建房間
        </button>

        <button
          @click="$router.push('/join')"
          class="w-full bg-gradient-to-r from-blue-500 to-cyan-500 text-white py-4 rounded-lg font-semibold text-lg hover:from-blue-600 hover:to-cyan-600 transition-all transform hover:scale-105"
        >
          加入房間
        </button>
      </div>

      <div class="mt-8 text-center text-sm text-gray-500">
        <p>適合 4-10 人的派對遊戲</p>
        <p class="mt-2">露營、民宿、聚會的最佳選擇</p>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

