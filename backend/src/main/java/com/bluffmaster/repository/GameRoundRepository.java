package com.bluffmaster.repository;

import com.bluffmaster.model.GameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRoundRepository extends JpaRepository<GameRound, String> {
    List<GameRound> findByRoomId(String roomId);
    Optional<GameRound> findByRoomIdAndIsFinishedFalse(String roomId);
}

