import SockJS from "sockjs-client";

let stompClient = null;
let Stomp = null;

// 載入 STOMP（使用動態導入處理 CommonJS 模組）
async function loadStomp() {
  if (!Stomp) {
    try {
      const stompModule = await import("stompjs");
      Stomp = stompModule.default || stompModule.Stomp || stompModule;
      if (!Stomp || !Stomp.over) {
        throw new Error("STOMP 未正確導出");
      }
    } catch (error) {
      console.error("載入 STOMP 失敗:", error);
      throw error;
    }
  }
  return Stomp;
}

async function connect(roomId, playerId, callbacks) {
  try {
    const StompLib = await loadStomp();
    const socket = new SockJS("/api/ws");
    stompClient = StompLib.over(socket);

    return new Promise((resolve, reject) => {
      stompClient.connect(
        {},
        () => {
          console.log("WebSocket 連接成功");

          // 訂閱房間更新
          stompClient.subscribe(`/topic/room/${roomId}`, (message) => {
            const room = JSON.parse(message.body);
            callbacks.onRoomUpdate?.(room);
          });

          // 訂閱回合開始
          stompClient.subscribe(`/topic/room/${roomId}/round`, (message) => {
            console.log(">>> RECEIVED /topic/room/" + roomId + "/round", {
              body: message.body,
              headers: message.headers,
            });
            try {
              const round = JSON.parse(message.body);
              console.log(">>> 解析後的回合數據:", round);
              callbacks.onRoundStart?.(round);
            } catch (error) {
              console.error(">>> 解析回合數據失敗:", error, message.body);
            }
          });

          // 訂閱投票更新
          stompClient.subscribe(`/topic/room/${roomId}/vote`, (message) => {
            const voteStatus = JSON.parse(message.body);
            callbacks.onVoteUpdate?.(voteStatus);
          });

          // 訂閱回合結果
          stompClient.subscribe(
            `/topic/room/${roomId}/round-result`,
            (message) => {
              const round = JSON.parse(message.body);
              callbacks.onRoundResult?.(round);
            }
          );

          // 訂閱玩家斷線
          stompClient.subscribe(
            `/topic/room/${roomId}/player-disconnect`,
            (message) => {
              const playerId = message.body;
              callbacks.onPlayerDisconnect?.(playerId);
            }
          );

          // 連接成功後 resolve
          resolve(stompClient);
        },
        (error) => {
          console.error("WebSocket 連接失敗:", error);
          reject(error);
        }
      );
    });
  } catch (error) {
    console.error("WebSocket 初始化失敗:", error);
    return null;
  }
}

function disconnect(stompClient) {
  if (stompClient && stompClient.connected) {
    stompClient.disconnect();
    console.log("WebSocket 已斷開");
  }
}

function sendStartRound(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send("/app/game/start-round", {}, JSON.stringify(payload));
  }
}

function sendVote(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send("/app/game/vote", {}, JSON.stringify(payload));
  }
}

function sendNextPhase(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send("/app/game/next-phase", {}, JSON.stringify(payload));
  }
}

function sendStartVoting(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send("/app/game/start-voting", {}, JSON.stringify(payload));
  }
}

function sendRevealResult(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send("/app/game/reveal", {}, JSON.stringify(payload));
  }
}

function sendFinishRound(stompClient, payload) {
  if (stompClient && stompClient.connected) {
    stompClient.send("/app/game/finish-round", {}, JSON.stringify(payload));
  }
}

export default {
  connect,
  disconnect,
  sendStartRound,
  sendVote,
  sendNextPhase,
  sendStartVoting,
  sendRevealResult,
  sendFinishRound,
};
