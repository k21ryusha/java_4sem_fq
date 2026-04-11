package game;

import save.GameState;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SurvivalProgress implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final GameState baselineState;
    private final List<SurvivalParticipant> participants;
    private final String trackName;
    private final int finishDistance;
    private int roundNumber;
    private int turnIndex;

    public SurvivalProgress(GameState baselineState,
                            List<SurvivalParticipant> participants,
                            String trackName,
                            int finishDistance,
                            int roundNumber,
                            int turnIndex) {
        this.baselineState = baselineState;
        this.participants = new ArrayList<>(participants);
        this.trackName = trackName;
        this.finishDistance = finishDistance;
        this.roundNumber = roundNumber;
        this.turnIndex = turnIndex;
    }

    public List<SurvivalParticipant> getParticipants() {
        return participants;
    }

    public String getTrackName() {
        return trackName;
    }

    public int getFinishDistance() {
        return finishDistance;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void incrementRoundNumber() {
        roundNumber++;
    }

    public int getTurnIndex() {
        return turnIndex;
    }

    public void setTurnIndex(int turnIndex) {
        this.turnIndex = turnIndex;
    }
}
