package game;

import staff.TeamManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotController {
    private final Random random;
    private final List<TeamManager> botTeams = new ArrayList<>();

    public BotController(Random random) {
        this.random = random;
        initBots();
    }

    private void initBots() {
        botTeams.add(new TeamManager("Red Nebula", 800_000, rand(12, 28)));
        botTeams.add(new TeamManager("TurboFox", 760_000, rand(10, 24)));
        botTeams.add(new TeamManager("NorthWind GP", 820_000, rand(13, 29)));
        botTeams.add(new TeamManager("AeroPulse", 780_000, rand(11, 27)));
    }

    public List<TeamManager> getBotTeams() { return botTeams; }

    public void showOtherTeams() {
        System.out.println("\n--- Другие команды ---");
        for (TeamManager t : botTeams) {
            System.out.printf("%s | бюджет=%d | репутация=%d%n", t.getTeamName(), t.getBudget(), t.getReputation());
        }
    }

    private int rand(int min, int max) { return random.nextInt(max - min + 1) + min; }
}
