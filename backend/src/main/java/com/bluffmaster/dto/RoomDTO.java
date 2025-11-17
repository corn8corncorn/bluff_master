package com.bluffmaster.dto;

import com.bluffmaster.model.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private String id;
    private String roomCode;
    private Integer maxPlayers;
    private Room.GameMode gameMode;
    private Room.RoomStatus status;
    private List<PlayerDTO> players;
    private Integer currentRound;
    private Integer totalRounds;
    private String hostId;
}

