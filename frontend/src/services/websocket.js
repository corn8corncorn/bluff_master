import SockJS from 'sockjs-client'
import { Stomp } from 'stompjs'

let stompClient = null

function connect(roomId, playerId, callbacks) {
  const socket = new SockJS('/ws')
  stompClient = Stomp.over(socket)
  
  stompClient.connect({}, () => {
    console.log('WebSocket 連接成功')

    // 訂閱房間更新
    stompClient.subscribe(`/topic/room/${roomId}`, (message) => {
      const room = JSON.parse(message.body)
      callbacks.onRoomUpdate?.(room)
    })

    // 訂閱回合開始
    stompClient.subscribe(`/topic/room/${roomId}/round`, (message) => {
      const round = JSON.parse(message.body)
      callbacks.onRoundStart?.(round)
    })

    // 訂閱投票更新
    stompClient.subscribe(`/topic/room/${roomId}/vote`, (message) => {
      const voteStatus = JSON.parse(message.body)
      callbacks.onVoteUpdate?.(voteStatus)
    })

    // 訂閱回合結果
    stompClient.subscribe(`/topic/room/${roomId}/round-result`, (message) => {
      const round = JSON.parse(message.body)
      callbacks.onRoundResult?.(round)
    })

    // 訂閱玩家斷線
    stompClient.subscribe(`/topic/room/${roomId}/player-disconnect`, (message) => {
      const playerId = message.body
      callbacks.onPlayerDisconnect?.(playerId)
    })
  }, (error) => {
    console.error('WebSocket 連接失敗:', error)
  })

  return stompClient
}

function disconnect(stompClient) {
  if (stompClient && stompClient.connected) {
    stompClient.disconnect()
    console.log('WebSocket 已斷開')
  }
}

function sendStartRound(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send('/app/game/start-round', {}, JSON.stringify(payload))
  }
}

function sendVote(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send('/app/game/vote', {}, JSON.stringify(payload))
  }
}

function sendFinishRound(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send('/app/game/finish-round', {}, JSON.stringify(payload))
  }
}

export default {
  connect,
  disconnect,
  sendStartRound,
  sendVote,
  sendFinishRound
}

