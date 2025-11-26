import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../services/api'
import websocket from '../services/websocket'

export const useGameStore = defineStore('game', () => {
  const room = ref(null)
  const currentPlayer = ref(null)
  const currentRound = ref(null)
  const connection = ref(null)

  const isHost = computed(() => {
    return currentPlayer.value?.isHost || false
  })

  const allPlayersReady = computed(() => {
    if (!room.value?.players) return false
    return room.value.players.every(p => p.isReady)
  })

  const canStartGame = computed(() => {
    if (!room.value) return false
    return isHost.value && 
           room.value.players.length >= 4 && 
           allPlayersReady.value &&
           room.value.status === 'WAITING'
  })

  async function createRoom(nickname, gameMode, maxPlayers) {
    const response = await api.post('/rooms', {
      nickname,
      gameMode,
      maxPlayers
    })
    room.value = response.data
    currentPlayer.value = response.data.players.find(p => p.isHost)
    // 保存玩家 ID 到 localStorage，以便刷新後恢復
    if (currentPlayer.value) {
      localStorage.setItem('currentPlayerId', currentPlayer.value.id)
    }
    return response.data
  }

  async function joinRoom(roomCode, nickname) {
    const response = await api.post('/rooms/join', {
      roomCode,
      nickname
    })
    room.value = response.data
    currentPlayer.value = response.data.players.find(p => p.nickname === nickname)
    // 保存玩家 ID 到 localStorage，以便刷新後恢復
    if (currentPlayer.value) {
      localStorage.setItem('currentPlayerId', currentPlayer.value.id)
    }
    return response.data
  }

  async function fetchRoom(roomId) {
    const response = await api.get(`/rooms/${roomId}`)
    room.value = response.data
    
    // 如果 currentPlayer 還沒有設置，嘗試從房間中找到當前玩家
    // 這通常發生在刷新頁面時
    if (!currentPlayer.value && room.value?.players) {
      // 可以通過 localStorage 或其他方式保存玩家 ID
      const savedPlayerId = localStorage.getItem('currentPlayerId')
      if (savedPlayerId) {
        currentPlayer.value = room.value.players.find(p => p.id === savedPlayerId)
      }
      // 如果還是找不到，使用第一個玩家（臨時方案）
      // 實際應該通過 session 或其他方式識別
      if (!currentPlayer.value && room.value.players.length > 0) {
        currentPlayer.value = room.value.players[0]
      }
    } else if (currentPlayer.value && room.value?.players) {
      // 更新 currentPlayer 的資訊
      const updated = room.value.players.find(p => p.id === currentPlayer.value.id)
      if (updated) {
        currentPlayer.value = updated
      }
    }
    
    return response.data
  }

  async function setReady() {
    await api.post(`/rooms/${room.value.id}/players/${currentPlayer.value.id}/ready`)
    currentPlayer.value.isReady = true
  }

  async function cancelReady() {
    await api.post(`/rooms/${room.value.id}/players/${currentPlayer.value.id}/cancel-ready`)
    currentPlayer.value.isReady = false
  }

  async function startGame() {
    await api.post(`/rooms/${room.value.id}/host/${currentPlayer.value.id}/start`)
    room.value.status = 'PLAYING'
  }

  async function uploadImages(files) {
    if (!currentPlayer.value || !currentPlayer.value.id) {
      throw new Error('玩家資訊未載入，請重新整理頁面')
    }
    
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    const response = await api.post(
      `/images/players/${currentPlayer.value.id}/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    )
    if (!currentPlayer.value.imageUrls) {
      currentPlayer.value.imageUrls = []
    }
    currentPlayer.value.imageUrls.push(...response.data)
    return response.data
  }

  async function deleteImage(imageUrl) {
    if (!currentPlayer.value || !currentPlayer.value.id) {
      throw new Error('玩家資訊未載入，請重新整理頁面')
    }
    
    await api.delete(
      `/images/players/${currentPlayer.value.id}/images`,
      {
        params: {
          imageUrl: imageUrl
        }
      }
    )
    
    // 從列表中移除
    if (currentPlayer.value.imageUrls) {
      const index = currentPlayer.value.imageUrls.indexOf(imageUrl)
      if (index > -1) {
        currentPlayer.value.imageUrls.splice(index, 1)
      }
    }
    
    // 重新獲取房間資訊以同步狀態
    if (room.value) {
      await fetchRoom(room.value.id)
    }
  }

  async function connectWebSocket(roomId, playerId) {
    connection.value = await websocket.connect(roomId, playerId, {
      onRoomUpdate: (updatedRoom) => {
        room.value = updatedRoom
        if (currentPlayer.value) {
          const updated = updatedRoom.players.find(p => p.id === currentPlayer.value.id)
          if (updated) {
            currentPlayer.value = updated
          }
        }
      },
      onRoundStart: (round) => {
        currentRound.value = round
      },
      onVoteUpdate: (voteStatus) => {
        if (currentRound.value) {
          currentRound.value.votes = {
            ...currentRound.value.votes,
            [voteStatus.playerId]: voteStatus.imageUrl
          }
        }
      },
      onRoundResult: (round) => {
        currentRound.value = round
      },
      onPlayerDisconnect: (playerId) => {
        const player = room.value.players.find(p => p.id === playerId)
        if (player) {
          player.isOnline = false
        }
      }
    })
  }

  function disconnectWebSocket() {
    if (connection.value) {
      websocket.disconnect(connection.value)
      connection.value = null
    }
  }

  function vote(imageUrl) {
    if (connection.value && currentRound.value) {
      websocket.sendVote(connection.value, {
        roundId: currentRound.value.id,
        playerId: currentPlayer.value.id,
        imageUrl,
        roomId: room.value.id
      })
    }
  }

  function startRound() {
    if (connection.value) {
      websocket.sendStartRound(connection.value, {
        roomId: room.value.id
      })
    }
  }

  function finishRound() {
    if (connection.value && currentRound.value) {
      websocket.sendFinishRound(connection.value, {
        roundId: currentRound.value.id
      })
    }
  }

  function reset() {
    room.value = null
    currentPlayer.value = null
    currentRound.value = null
    disconnectWebSocket()
  }

  return {
    room,
    currentPlayer,
    currentRound,
    isHost,
    allPlayersReady,
    canStartGame,
    createRoom,
    joinRoom,
    fetchRoom,
    setReady,
    cancelReady,
    startGame,
    uploadImages,
    deleteImage,
    connectWebSocket,
    disconnectWebSocket,
    vote,
    startRound,
    finishRound,
    reset
  }
})

