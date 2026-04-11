package save;

import com.google.gson.Gson;
import game.SurvivalProgress;
import race_weekend.RaceResult;
import race_weekend.Track;
import staff.TeamManager;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState implements Serializable {
    private static final Gson GSON = GsonFactory.create();

    @Serial
    private static final long serialVersionUID = 1L;

    private final String playerName;
    private final TeamManager player;
    private final List<RaceResult> raceHistory;
    private final Map<String, Integer> driverPoints;
    private final Map<String, Integer> teamPoints;
    private final List<Track> trackLibrary;
    private final List<Track> seasonCalendar;
    private final int championshipRound;
    private final boolean parcFermeLocked;
    private final boolean seasonSetupCompleted;
    private final SurvivalProgress survivalProgress;

    public GameState(String playerName,
                     TeamManager player,
                     List<RaceResult> raceHistory,
                     Map<String, Integer> driverPoints,
                     Map<String, Integer> teamPoints,
                     List<Track> trackLibrary,
                     List<Track> seasonCalendar,
                     int championshipRound,
                     boolean parcFermeLocked,
                     boolean seasonSetupCompleted,
                     SurvivalProgress survivalProgress) {
        this.playerName = playerName;
        this.player = player;
        this.raceHistory = new ArrayList<>(raceHistory);
        this.driverPoints = new HashMap<>(driverPoints);
        this.teamPoints = new HashMap<>(teamPoints);
        this.trackLibrary = new ArrayList<>(trackLibrary);
        this.seasonCalendar = new ArrayList<>(seasonCalendar);
        this.championshipRound = championshipRound;
        this.parcFermeLocked = parcFermeLocked;
        this.seasonSetupCompleted = seasonSetupCompleted;
        this.survivalProgress = survivalProgress;
    }

    public GameState(String playerName,
                     TeamManager player,
                     List<RaceResult> raceHistory,
                     Map<String, Integer> driverPoints,
                     Map<String, Integer> teamPoints,
                     List<Track> trackLibrary,
                     List<Track> seasonCalendar,
                     int championshipRound,
                     boolean parcFermeLocked,
                     boolean seasonSetupCompleted) {
        this(playerName, player, raceHistory, driverPoints, teamPoints, trackLibrary, seasonCalendar,
                championshipRound, parcFermeLocked, seasonSetupCompleted, null);
    }

    public String getPlayerName() {
        return playerName;
    }

    public TeamManager getPlayer() {
        return player;
    }

    public List<RaceResult> getRaceHistory() {
        if (raceHistory == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(raceHistory);
    }

    public Map<String, Integer> getDriverPoints() {
        if (driverPoints == null) {
            return new HashMap<>();
        }
        return new HashMap<>(driverPoints);
    }

    public Map<String, Integer> getTeamPoints() {
        if (teamPoints == null) {
            return new HashMap<>();
        }
        return new HashMap<>(teamPoints);
    }

    public int getChampionshipRound() {
        return championshipRound;
    }

    public boolean isParcFermeLocked() {
        return parcFermeLocked;
    }

    public List<Track> getTrackLibrary() {
        if (trackLibrary == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(trackLibrary);
    }

    public List<Track> getSeasonCalendar() {
        if (seasonCalendar == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(seasonCalendar);
    }

    public boolean isSeasonSetupCompleted() {
        return seasonSetupCompleted;
    }

    public SurvivalProgress getSurvivalProgress() {
        return survivalProgress;
    }

    public GameState deepCopy() {
        return GSON.fromJson(GSON.toJson(this), GameState.class);
    }
}
