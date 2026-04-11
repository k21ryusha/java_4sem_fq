package game;

import components.Car;
import staff.MainDriver;
import staff.TeamManager;
import weapons.MeleeWeapon;
import weapons.RangedWeapon;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SurvivalParticipant implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String entryName;
    private final String teamName;
    private final boolean playerControlled;
    private final TeamManager team;
    private final MainDriver driver;
    private final Car car;
    private final List<MeleeWeapon> meleeWeapons = new ArrayList<>();
    private RangedWeapon rangedWeapon;
    private int progress;
    private boolean eliminated;
    private boolean finished;

    public SurvivalParticipant(String entryName, String teamName, boolean playerControlled,
                               TeamManager team, MainDriver driver, Car car, int progress) {
        this.entryName = entryName;
        this.teamName = teamName;
        this.playerControlled = playerControlled;
        this.team = team;
        this.driver = driver;
        this.car = car;
        this.progress = progress;
    }

    public String getEntryName() {
        return entryName;
    }

    public boolean isPlayerControlled() {
        return playerControlled;
    }

    public TeamManager getTeam() {
        return team;
    }

    public MainDriver getDriver() {
        return driver;
    }

    public Car getCar() {
        return car;
    }

    public List<MeleeWeapon> getMeleeWeapons() {
        return meleeWeapons;
    }

    public RangedWeapon getRangedWeapon() {
        return rangedWeapon;
    }

    public void setRangedWeapon(RangedWeapon rangedWeapon) {
        this.rangedWeapon = rangedWeapon;
    }

    public int getProgress() {
        return progress;
    }

    public void addProgress(int value) {
        progress += value;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isActive() {
        return !eliminated && !finished;
    }

    public int totalWeapons() {
        int rangedWeaponsCount = 0;
        if (rangedWeapon != null) {
            rangedWeaponsCount = 1;
        }
        return meleeWeapons.size() + rangedWeaponsCount;
    }
}
