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
    return response.data
  }

  async function joinRoom(roomCode, nickname) {
    const response = await api.post('/rooms/join', {
      roomCode,
      nickname
    })
    room.value = response.data
    currentPlayer.value = response.data.players.find(p => p.nickname === nickname)
    return response.data
  }

  async function fetchRoom(roomId) {
    const response = await api.get(`/rooms/${roomId}`)
    room.value = response.data
    return response.data
  }

  async function setReady() {
    await api.post(`/rooms/${room.value.id}/players/${currentPlayer.value.id}/ready`)
    currentPlayer.value.isReady = true
  }

  async function startGame() {
    await api.post(`/rooms/${room.value.id}/host/${currentPlayer.value.id}/start`)
    room.value.status = 'PLAYING'
  }

  async function uploadImages(files) {
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

  function connectWebSocket(roomId, playerId) {
    connection.value = websocket.connect(roomId, playerId, {
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
    startGame,
    uploadImages,
    connectWebSocket,
    disconnectWebSocket,
    vote,
    startRound,
    finishRound,
    reset
  }
})

