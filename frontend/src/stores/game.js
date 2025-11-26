import { defineStore } from "pinia";
import { ref, computed } from "vue";
import api from "../services/api";
import websocket from "../services/websocket";

export const useGameStore = defineStore("game", () => {
  const room = ref(null);
  const currentPlayer = ref(null);
  const currentRound = ref(null);
  const connection = ref(null);
  const isRestoringPlayer = ref(false); // 標記是否正在從 session 恢復玩家身份

  const isHost = computed(() => {
    return currentPlayer.value?.isHost || false;
  });

  const allPlayersReady = computed(() => {
    if (!room.value?.players) return false;
    return room.value.players.every((p) => p.isReady);
  });

  const canStartGame = computed(() => {
    if (!room.value) return false;
    return (
      isHost.value &&
      room.value.players.length >= 2 &&
      allPlayersReady.value &&
      room.value.status === "WAITING"
    );
  });

  async function createRoom(nickname, gameMode, maxPlayers) {
    const response = await api.post("/rooms", {
      nickname,
      gameMode,
      maxPlayers,
    });
    room.value = response.data;
    currentPlayer.value = response.data.players.find((p) => p.isHost);
    // Session 會自動由後端保存，不需要前端處理
    return response.data;
  }

  async function joinRoom(roomCode, nickname) {
    const response = await api.post("/rooms/join", {
      roomCode,
      nickname,
    });
    room.value = response.data;
    currentPlayer.value = response.data.players.find(
      (p) => p.nickname === nickname
    );
    // Session 會自動由後端保存，不需要前端處理
    return response.data;
  }

  async function fetchRoom(roomId) {
    const response = await api.get(`/rooms/${roomId}`);
    room.value = response.data;

    // 設置標記，表示正在從 session 恢復玩家身份
    isRestoringPlayer.value = true;

    // 總是從 session 獲取玩家 ID 來驗證身份（這是唯一可信的來源）
    try {
      const playerIdResponse = await api.get("/rooms/session/player-id");
      const savedPlayerId = playerIdResponse.data;

      console.log("fetchRoom: 從 session 獲取的玩家 ID:", savedPlayerId);
      console.log(
        "fetchRoom: 房間中的玩家:",
        room.value?.players?.map((p) => ({
          id: p.id,
          nickname: p.nickname,
          isHost: p.isHost,
        }))
      );

      if (savedPlayerId && room.value?.players) {
        const foundPlayer = room.value.players.find(
          (p) => p.id === savedPlayerId
        );
        if (foundPlayer) {
          // 使用 session 中的玩家 ID 來設置 currentPlayer
          console.log(
            "fetchRoom: 設置 currentPlayer 為:",
            foundPlayer.nickname,
            foundPlayer.isHost ? "(房主)" : ""
          );
          currentPlayer.value = foundPlayer;
        } else {
          // Session 中的玩家 ID 在房間中找不到，清除 currentPlayer
          console.warn(
            "fetchRoom: Session 中的玩家 ID 在房間中找不到:",
            savedPlayerId
          );
          currentPlayer.value = null;
        }
      } else if (!savedPlayerId) {
        // Session 中沒有玩家 ID，清除 currentPlayer
        console.warn("fetchRoom: Session 中沒有玩家 ID");
        currentPlayer.value = null;
      }
    } catch (error) {
      // Session 中沒有玩家 ID 或請求失敗，清除 currentPlayer
      console.error("fetchRoom: 無法從 session 獲取玩家 ID:", error);
      currentPlayer.value = null;
    } finally {
      // 恢復完成，清除標記
      isRestoringPlayer.value = false;
    }

    return response.data;
  }

  async function leaveRoom(roomId) {
    try {
      // 斷開 WebSocket 連接
      if (connection.value) {
        websocket.disconnect(connection.value);
        connection.value = null;
      }

      // 調用後端 API 退出房間
      await api.post(`/rooms/${roomId}/leave`);

      // 清除本地狀態
      room.value = null;
      currentPlayer.value = null;
    } catch (error) {
      console.error("離開房間失敗:", error);
      // 即使失敗也清除本地狀態和連接
      if (connection.value) {
        websocket.disconnect(connection.value);
        connection.value = null;
      }
      room.value = null;
      currentPlayer.value = null;
      throw error;
    }
  }

  async function setReady() {
    await api.post(
      `/rooms/${room.value.id}/players/${currentPlayer.value.id}/ready`
    );
    currentPlayer.value.isReady = true;
  }

  async function cancelReady() {
    await api.post(
      `/rooms/${room.value.id}/players/${currentPlayer.value.id}/cancel-ready`
    );
    currentPlayer.value.isReady = false;
  }

  async function startGame() {
    await api.post(
      `/rooms/${room.value.id}/host/${currentPlayer.value.id}/start`
    );
    room.value.status = "PLAYING";
  }

  async function uploadImages(files) {
    if (!currentPlayer.value || !currentPlayer.value.id) {
      throw new Error("玩家資訊未載入，請重新整理頁面");
    }

    const formData = new FormData();
    files.forEach((file) => {
      formData.append("files", file);
    });
    const response = await api.post(
      `/images/players/${currentPlayer.value.id}/upload`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    );
    if (!currentPlayer.value.imageUrls) {
      currentPlayer.value.imageUrls = [];
    }
    currentPlayer.value.imageUrls.push(...response.data);
    return response.data;
  }

  async function deleteImage(imageUrl) {
    if (!currentPlayer.value || !currentPlayer.value.id) {
      throw new Error("玩家資訊未載入，請重新整理頁面");
    }

    await api.delete(`/images/players/${currentPlayer.value.id}/images`, {
      params: {
        imageUrl: imageUrl,
      },
    });

    // 從列表中移除
    if (currentPlayer.value.imageUrls) {
      const index = currentPlayer.value.imageUrls.indexOf(imageUrl);
      if (index > -1) {
        currentPlayer.value.imageUrls.splice(index, 1);
      }
    }

    // 重新獲取房間資訊以同步狀態
    if (room.value) {
      await fetchRoom(room.value.id);
    }
  }

  async function connectWebSocket(roomId, playerId) {
    connection.value = await websocket.connect(roomId, playerId, {
      onRoomUpdate: (updatedRoom) => {
        // 保存之前的玩家列表（用於檢測新玩家）
        const previousPlayerIds = room.value?.players?.map((p) => p.id) || [];
        const currentPlayerIds = updatedRoom.players?.map((p) => p.id) || [];

        // 找出新加入的玩家
        const newPlayerIds = currentPlayerIds.filter(
          (id) => !previousPlayerIds.includes(id)
        );
        const newPlayers =
          updatedRoom.players?.filter((p) => newPlayerIds.includes(p.id)) || [];

        // 更新房間資訊
        room.value = updatedRoom;

        // 更新當前玩家資訊
        // 如果正在從 session 恢復玩家身份（fetchRoom 中），跳過此更新，避免覆蓋
        if (isRestoringPlayer.value) {
          console.log("onRoomUpdate: 正在恢復玩家身份，跳過更新");
          return;
        }

        if (currentPlayer.value && currentPlayer.value.id) {
          // 如果已有 currentPlayer，只更新其資訊，不改變身份
          const updated = updatedRoom.players.find(
            (p) => p.id === currentPlayer.value.id
          );
          if (updated) {
            // 只更新資訊，不改變身份
            currentPlayer.value = updated;
            console.log(
              "onRoomUpdate: 更新 currentPlayer 資訊:",
              updated.nickname,
              updated.isHost ? "(房主)" : ""
            );
          } else {
            // 如果 currentPlayer 不在房間中，才嘗試從 session 恢復
            // 這通常發生在玩家被踢出或離開房間時
            console.warn(
              "onRoomUpdate: currentPlayer 不在房間中，嘗試從 session 恢復"
            );
            api
              .get("/rooms/session/player-id")
              .then((playerIdResponse) => {
                const savedPlayerId = playerIdResponse.data;
                console.log(
                  "onRoomUpdate: 從 session 獲取的玩家 ID:",
                  savedPlayerId
                );
                if (savedPlayerId && updatedRoom.players) {
                  const foundPlayer = updatedRoom.players.find(
                    (p) => p.id === savedPlayerId
                  );
                  if (foundPlayer) {
                    console.log(
                      "onRoomUpdate: 從 session 恢復 currentPlayer:",
                      foundPlayer.nickname,
                      foundPlayer.isHost ? "(房主)" : ""
                    );
                    currentPlayer.value = foundPlayer;
                  } else {
                    console.warn(
                      "onRoomUpdate: Session 中的玩家 ID 在房間中找不到"
                    );
                    currentPlayer.value = null;
                  }
                } else {
                  currentPlayer.value = null;
                }
              })
              .catch((error) => {
                console.error(
                  "onRoomUpdate: 無法從 session 獲取玩家 ID:",
                  error
                );
                currentPlayer.value = null;
              });
          }
        } else {
          // 如果 currentPlayer 為 null，才從 session 恢復
          console.log("onRoomUpdate: currentPlayer 為 null，從 session 恢復");
          api
            .get("/rooms/session/player-id")
            .then((playerIdResponse) => {
              const savedPlayerId = playerIdResponse.data;
              console.log(
                "onRoomUpdate: 從 session 獲取的玩家 ID:",
                savedPlayerId
              );
              if (savedPlayerId && updatedRoom.players) {
                const foundPlayer = updatedRoom.players.find(
                  (p) => p.id === savedPlayerId
                );
                if (foundPlayer) {
                  console.log(
                    "onRoomUpdate: 從 session 恢復 currentPlayer:",
                    foundPlayer.nickname,
                    foundPlayer.isHost ? "(房主)" : ""
                  );
                  currentPlayer.value = foundPlayer;
                } else {
                  console.warn(
                    "onRoomUpdate: Session 中的玩家 ID 在房間中找不到"
                  );
                }
              }
            })
            .catch((error) => {
              console.error("onRoomUpdate: 無法從 session 獲取玩家 ID:", error);
            });
        }

        // 如果有新玩家加入，觸發事件（供組件監聽）
        if (newPlayers.length > 0) {
          // 可以通過事件或回調通知組件
          // 這裡我們在組件層處理
        }
      },
      onRoundStart: (round) => {
        currentRound.value = round;
      },
      onVoteUpdate: (voteStatus) => {
        if (currentRound.value) {
          currentRound.value.votes = {
            ...currentRound.value.votes,
            [voteStatus.playerId]: voteStatus.imageUrl,
          };
        }
      },
      onRoundResult: (round) => {
        currentRound.value = round;
      },
      onPlayerDisconnect: (playerId) => {
        const player = room.value.players.find((p) => p.id === playerId);
        if (player) {
          player.isOnline = false;
        }
      },
    });
  }

  function disconnectWebSocket() {
    if (connection.value) {
      websocket.disconnect(connection.value);
      connection.value = null;
    }
  }

  function vote(imageUrl) {
    if (connection.value && currentRound.value) {
      websocket.sendVote(connection.value, {
        roundId: currentRound.value.id,
        playerId: currentPlayer.value.id,
        imageUrl,
        roomId: room.value.id,
      });
    }
  }

  function startRound() {
    if (connection.value) {
      websocket.sendStartRound(connection.value, {
        roomId: room.value.id,
      });
    }
  }

  function finishRound() {
    if (connection.value && currentRound.value) {
      websocket.sendFinishRound(connection.value, {
        roundId: currentRound.value.id,
      });
    }
  }

  function reset() {
    room.value = null;
    currentPlayer.value = null;
    currentRound.value = null;
    disconnectWebSocket();
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
    leaveRoom,
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
    reset,
  };
});
