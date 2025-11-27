import { defineStore } from "pinia";
import { ref, computed, triggerRef } from "vue";
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

  async function fetchCurrentRound(roomId) {
    try {
      const response = await api.get(`/game/rooms/${roomId}/current-round`);
      currentRound.value = response.data;
      console.log("fetchCurrentRound: 獲取到當前回合", currentRound.value);
      return response.data;
    } catch (error) {
      if (error.response?.status === 404) {
        // 沒有當前回合，這是正常的
        currentRound.value = null;
        console.log("fetchCurrentRound: 沒有當前回合（404），這是正常的");
        return null;
      }
      // 其他錯誤不拋出，只記錄
      console.warn(
        "fetchCurrentRound: 獲取當前回合失敗",
        error.response?.status,
        error.message
      );
      currentRound.value = null;
      return null;
    }
  }

  async function fetchRoom(roomId) {
    const response = await api.get(`/rooms/${roomId}`);
    // 創建新的對象引用以確保響應式更新
    room.value = {
      ...response.data,
      players: response.data.players ? [...response.data.players] : [],
    };
    triggerRef(room);

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

        // 更新房間資訊（使用新的對象引用以確保響應式更新，包括 players 數組）
        room.value = {
          ...updatedRoom,
          players: updatedRoom.players ? [...updatedRoom.players] : [],
        };
        triggerRef(room);

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
            // 只更新資訊，不改變身份（創建新對象以確保響應式更新）
            currentPlayer.value = { ...updated };
            triggerRef(currentPlayer);
            console.log(
              "onRoomUpdate: 更新 currentPlayer 資訊:",
              updated.nickname,
              updated.isHost ? "(房主)" : "",
              "分數:",
              updated.score
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
        console.log("=== onRoundStart: 收到回合更新 ===", {
          roundId: round?.id,
          roundNumber: round?.roundNumber,
          phase: round?.phase,
          speakerId: round?.speakerId,
          speakerNickname: round?.speakerNickname,
          isFinished: round?.isFinished,
          round: round,
        });

        // 如果 round 沒有 speakerNickname，嘗試從房間玩家列表中獲取
        if (!round.speakerNickname && round.speakerId && room.value?.players) {
          // 嘗試精確匹配
          let speaker = room.value.players.find(
            (p) => p.id === round.speakerId
          );

          // 如果精確匹配失敗，嘗試字符串匹配
          if (!speaker) {
            speaker = room.value.players.find(
              (p) => String(p.id) === String(round.speakerId)
            );
          }

          if (speaker) {
            round.speakerNickname = speaker.nickname;
            console.log(
              "onRoundStart: 從房間玩家列表獲取主講者暱稱:",
              speaker.nickname
            );
          } else {
            console.warn("onRoundStart: 無法在房間玩家列表中找到主講者", {
              speakerId: round.speakerId,
              speakerIdType: typeof round.speakerId,
              playersIds: room.value.players.map((p) => ({
                id: p.id,
                idType: typeof p.id,
                nickname: p.nickname,
              })),
            });
          }
        }

        // 記錄舊階段以便比較
        const oldPhase = currentRound.value?.phase;
        const oldRoundId = currentRound.value?.id;

        // 強制創建新對象以確保 Vue 響應式系統檢測到變化
        // 每次更新都創建全新的對象引用，確保響應式系統檢測到變化
        const newRound = {
          id: round.id,
          roomId: round.roomId,
          roundNumber: round.roundNumber,
          speakerId: round.speakerId,
          speakerNickname: round.speakerNickname || null,
          imageUrls: round.imageUrls ? [...round.imageUrls] : [],
          fakeImageUrl: round.fakeImageUrl || null,
          speakerFakeImageUrl: round.speakerFakeImageUrl || null,
          phase: round.phase || null,
          votes: round.votes ? { ...round.votes } : {},
          isFinished: round.isFinished || false,
          votingTimeLeft: round.votingTimeLeft || null,
          voteResults: round.voteResults ? { ...round.voteResults } : null,
        };

        // 無論是否為同一回合，都直接替換整個對象以確保響應式更新
        // 這樣可以確保 Vue 檢測到所有屬性的變化，包括 phase
        currentRound.value = newRound;

        // 強制觸發響應式更新（確保所有依賴 currentRound 的 computed 和 watch 都能觸發）
        triggerRef(currentRound);

        console.log("onRoundStart: 已更新 currentRound", {
          oldRoundId,
          newRoundId: round.id,
          oldPhase: oldPhase,
          newPhase: round.phase,
          phase: currentRound.value?.phase,
          isFinished: currentRound.value?.isFinished,
          currentRoundRef: currentRound.value,
        });

        // 當新回合開始時，重新獲取房間信息以確保回合數和玩家資訊是最新的
        if (room.value && room.value.id) {
          fetchRoom(room.value.id).catch((error) => {
            console.error("onRoundStart: 重新獲取房間信息失敗:", error);
          });
        }
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
        console.log("onRoundResult: 收到回合結果", {
          roundId: round?.id,
          isFinished: round?.isFinished,
          phase: round?.phase,
          voteResults: round?.voteResults,
        });

        // 強制創建新對象以確保 Vue 響應式系統檢測到變化
        const newRound = {
          id: round.id,
          roomId: round.roomId,
          roundNumber: round.roundNumber,
          speakerId: round.speakerId,
          speakerNickname: round.speakerNickname || null,
          imageUrls: round.imageUrls ? [...round.imageUrls] : [],
          fakeImageUrl: round.fakeImageUrl || null,
          speakerFakeImageUrl: round.speakerFakeImageUrl || null,
          phase: round.phase || null,
          votes: round.votes ? { ...round.votes } : {},
          isFinished: round.isFinished || false,
          votingTimeLeft: round.votingTimeLeft || null,
          voteResults: round.voteResults ? { ...round.voteResults } : null,
        };

        currentRound.value = newRound;
        triggerRef(currentRound);

        // 重新獲取房間資訊以更新玩家分數（最後一輪時尤其重要）
        if (round.roomId) {
          fetchRoom(round.roomId)
            .then(() => {
              console.log("onRoundResult: 房間資訊已更新，玩家分數已同步");
            })
            .catch((error) => {
              console.error("onRoundResult: 重新獲取房間信息失敗:", error);
            });
        }
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
    console.log("startRound: 準備開始回合", {
      hasConnection: !!connection.value,
      connectionConnected: connection.value?.connected,
      roomId: room.value?.id,
    });

    if (connection.value && connection.value.connected) {
      websocket.sendStartRound(connection.value, {
        roomId: room.value.id,
      });
      console.log("startRound: WebSocket 消息已發送");
    } else {
      console.error("startRound: WebSocket 未連接", {
        hasConnection: !!connection.value,
        connectionConnected: connection.value?.connected,
      });
    }
  }

  function nextPhase() {
    console.log("nextPhase: 準備進入下一階段", {
      hasConnection: !!connection.value,
      connectionConnected: connection.value?.connected,
      currentRound: currentRound.value?.id,
      currentPhase: currentRound.value?.phase,
    });

    if (!connection.value || !connection.value.connected) {
      console.error("nextPhase: WebSocket 未連接", {
        hasConnection: !!connection.value,
        connectionConnected: connection.value?.connected,
      });
      return;
    }

    if (!currentRound.value || !currentRound.value.id) {
      console.error("nextPhase: 沒有當前回合", {
        hasCurrentRound: !!currentRound.value,
        roundId: currentRound.value?.id,
      });
      return;
    }

    try {
      websocket.sendNextPhase(connection.value, {
        roundId: currentRound.value.id,
      });
      console.log("nextPhase: WebSocket 消息已發送", {
        roundId: currentRound.value.id,
      });
    } catch (error) {
      console.error("nextPhase: 發送消息失敗", error);
    }
  }

  function startVoting() {
    console.log("startVoting: 準備開始投票", {
      hasConnection: !!connection.value,
      connectionConnected: connection.value?.connected,
      currentRound: currentRound.value?.id,
      currentPhase: currentRound.value?.phase,
    });

    if (!connection.value || !connection.value.connected) {
      console.error("startVoting: WebSocket 未連接", {
        hasConnection: !!connection.value,
        connectionConnected: connection.value?.connected,
      });
      return;
    }

    if (!currentRound.value || !currentRound.value.id) {
      console.error("startVoting: 沒有當前回合", {
        hasCurrentRound: !!currentRound.value,
        roundId: currentRound.value?.id,
      });
      return;
    }

    try {
      websocket.sendStartVoting(connection.value, {
        roundId: currentRound.value.id,
      });
      console.log("startVoting: WebSocket 消息已發送", {
        roundId: currentRound.value.id,
      });
    } catch (error) {
      console.error("startVoting: 發送消息失敗", error);
    }
  }

  function revealResult() {
    if (connection.value && currentRound.value) {
      websocket.sendRevealResult(connection.value, {
        roundId: currentRound.value.id,
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
    nextPhase,
    startVoting,
    revealResult,
    finishRound,
    fetchCurrentRound,
    reset,
    connection,
  };
});
