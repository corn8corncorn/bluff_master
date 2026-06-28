import { defineStore } from "pinia";
import { ref, computed, triggerRef } from "vue";
import api, { uploadApi } from "../services/api";
import websocket from "../services/websocket";
import {
  FAKE_IMAGE_REGIONS,
  FAKE_IMAGE_CATEGORIES,
} from "../constants/fakeImageThemes";

export const useGameStore = defineStore("game", () => {
  const room = ref(null);
  const currentPlayer = ref(null);
  const currentRound = ref(null);
  const connection = ref(null);
  const isRestoringPlayer = ref(false); // 標記是否正在從 session 恢復玩家身份
  const fakeThemeRegions = ref([...FAKE_IMAGE_REGIONS]);
  const fakeThemeCategories = ref([...FAKE_IMAGE_CATEGORIES]);

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

  async function playAgain() {
    const response = await api.post(
      `/rooms/${room.value.id}/host/${currentPlayer.value.id}/play-again`
    );
    room.value = response.data;
    currentRound.value = null;
    if (currentPlayer.value) {
      const updated = response.data.players?.find(
        (p) => p.id === currentPlayer.value.id
      );
      if (updated) {
        currentPlayer.value = updated;
      }
    }
    return response.data;
  }

  async function uploadImages(files, onProgress) {
    if (!currentPlayer.value || !currentPlayer.value.id) {
      throw new Error("玩家資訊未載入，請重新整理頁面");
    }

    const uploadedUrls = []
    const total = files.length

    for (let i = 0; i < files.length; i++) {
      onProgress?.({ phase: 'upload', current: i + 1, total })

      const formData = new FormData()
      formData.append('files', files[i])

      const response = await uploadApi.post(
        `/images/players/${currentPlayer.value.id}/upload`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      )

      const urls = response.data || []
      uploadedUrls.push(...urls)

      if (!currentPlayer.value.imageUrls) {
        currentPlayer.value.imageUrls = []
      }
      currentPlayer.value.imageUrls = [...currentPlayer.value.imageUrls, ...urls]
      triggerRef(currentPlayer)

      // 同步房間玩家列表中的圖片數量（讓自己與其他人即時看到）
      if (room.value?.players) {
        const playerInRoom = room.value.players.find(
          (p) => p.id === currentPlayer.value.id
        )
        if (playerInRoom) {
          playerInRoom.imageUrls = [...(playerInRoom.imageUrls || []), ...urls]
          room.value = {
            ...room.value,
            players: room.value.players.map((p) =>
              p.id === currentPlayer.value.id
                ? { ...p, imageUrls: [...(p.imageUrls || []), ...urls] }
                : p
            ),
          }
          triggerRef(room)
        }
      }
    }

    return uploadedUrls
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
    if (connection.value?.connected) {
      await markPlayerOnline(playerId);
      await fetchRoom(roomId);
      return connection.value;
    }

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
        applyRoundUpdate(round);
      },
      onVoteUpdate: (voteStatus) => {
        if (currentRound.value && voteStatus.playerId !== currentRound.value.speakerId) {
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
          fakeImageRegion: round.fakeImageRegion || null,
          fakeImageCategory: round.fakeImageCategory || null,
          fakeThemeSelected: round.fakeThemeSelected || false,
          fakeThemeLabel: round.fakeThemeLabel || null,
          speakerFakeImageUrl: round.speakerFakeImageUrl || null,
          phase: round.phase || null,
          votes: round.votes ? { ...round.votes } : {},
          isFinished: round.isFinished || false,
          votingTimeLeft: round.votingTimeLeft ?? null,
          votingEndsAt: round.votingEndsAt || null,
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
        if (!room.value?.players) return;
        room.value = {
          ...room.value,
          players: room.value.players.map((p) =>
            p.id === playerId ? { ...p, isOnline: false } : p
          ),
        };
        triggerRef(room);
      },
    });

    await markPlayerOnline(playerId);
    await fetchRoom(roomId);
    return connection.value;
  }

  async function markPlayerOnline(playerId) {
    if (!playerId) return;
    try {
      await api.post(`/game/players/${playerId}/reconnect`);
    } catch (error) {
      console.warn("markPlayerOnline: 失敗", error);
    }
  }

  function applyRoundUpdate(round) {
    if (!round) return;

    if (!round.speakerNickname && round.speakerId && room.value?.players) {
      let speaker = room.value.players.find((p) => p.id === round.speakerId);
      if (!speaker) {
        speaker = room.value.players.find(
          (p) => String(p.id) === String(round.speakerId)
        );
      }
      if (speaker) {
        round.speakerNickname = speaker.nickname;
      }
    }

    currentRound.value = {
      id: round.id,
      roomId: round.roomId,
      roundNumber: round.roundNumber,
      speakerId: round.speakerId,
      speakerNickname: round.speakerNickname || null,
      imageUrls: round.imageUrls ? [...round.imageUrls] : [],
      fakeImageUrl: round.fakeImageUrl || null,
      fakeImageRegion: round.fakeImageRegion || null,
      fakeImageCategory: round.fakeImageCategory || null,
      fakeThemeSelected: round.fakeThemeSelected || false,
      fakeThemeLabel: round.fakeThemeLabel || null,
      speakerFakeImageUrl: round.speakerFakeImageUrl || null,
      phase: round.phase || null,
      votes: round.votes ? { ...round.votes } : {},
      isFinished: round.isFinished || false,
      votingTimeLeft: round.votingTimeLeft ?? null,
      votingEndsAt: round.votingEndsAt || null,
      voteResults: round.voteResults ? { ...round.voteResults } : null,
    };
    triggerRef(currentRound);

    if (room.value?.id) {
      fetchRoom(room.value.id).catch((error) => {
        console.error("applyRoundUpdate: 重新獲取房間信息失敗:", error);
      });
    }
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

  async function startRound() {
    if (!room.value?.id) {
      console.error("startRound: 沒有房間 ID");
      return null;
    }

    try {
      const response = await api.post(
        `/game/rooms/${room.value.id}/rounds/start`
      );
      applyRoundUpdate(response.data);
      return response.data;
    } catch (error) {
      console.error("startRound: REST 啟動失敗，嘗試 WebSocket", error);
      if (connection.value?.connected) {
        websocket.sendStartRound(connection.value, {
          roomId: room.value.id,
          playerId: currentPlayer.value?.id,
        });
      }
      throw error;
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
        playerId: currentPlayer.value?.id,
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
        playerId: currentPlayer.value?.id,
      });
      console.log("startVoting: WebSocket 消息已發送", {
        roundId: currentRound.value.id,
      });
    } catch (error) {
      console.error("startVoting: 發送消息失敗", error);
    }
  }

  async function selectFakeTheme(region, category) {
    if (!currentRound.value?.id) {
      throw new Error("沒有當前回合");
    }
    if (!currentPlayer.value?.id) {
      throw new Error("玩家資訊未載入");
    }

    try {
      const response = await api.post(
        `/game/rounds/${currentRound.value.id}/select-fake-theme`,
        null,
        {
          params: {
            region,
            category,
            playerId: currentPlayer.value.id,
          },
          timeout: 90000,
        }
      );
      applyRoundUpdate(response.data);
      return response.data;
    } catch (error) {
      console.error("selectFakeTheme: REST 失敗，嘗試 WebSocket", error);
      if (connection.value?.connected) {
        websocket.sendSelectFakeTheme(connection.value, {
          roundId: currentRound.value.id,
          playerId: currentPlayer.value.id,
          region,
          category,
        });
      }
      throw error;
    }
  }

  function revealResult() {
    if (connection.value && currentRound.value) {
      websocket.sendRevealResult(connection.value, {
        roundId: currentRound.value.id,
        playerId: currentPlayer.value?.id,
      });
    }
  }

  function finishRound() {
    if (connection.value && currentRound.value) {
      websocket.sendFinishRound(connection.value, {
        roundId: currentRound.value.id,
        playerId: currentPlayer.value?.id,
      });
    }
  }

  async function fetchFakeThemeOptions() {
    try {
      const response = await api.get("/game/fake-theme-options");
      if (response.data?.regions?.length) {
        fakeThemeRegions.value = response.data.regions;
      }
      if (response.data?.categories?.length) {
        fakeThemeCategories.value = response.data.categories;
      }
    } catch (error) {
      console.warn("載入假圖主題選項失敗，使用預設列表", error);
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
    fakeThemeRegions,
    fakeThemeCategories,
    createRoom,
    joinRoom,
    fetchRoom,
    leaveRoom,
    setReady,
    cancelReady,
    startGame,
    playAgain,
    uploadImages,
    deleteImage,
    connectWebSocket,
    disconnectWebSocket,
    vote,
    startRound,
    nextPhase,
    startVoting,
    selectFakeTheme,
    revealResult,
    finishRound,
    fetchCurrentRound,
    fetchFakeThemeOptions,
    reset,
    connection,
  };
});
