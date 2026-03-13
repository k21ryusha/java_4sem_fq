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
        botTeams.add(new TeamManager("Ferrari", 980_000, rand(18, 30)));
        botTeams.add(new TeamManager("Mercedes", 970_000, rand(18, 30)));
        botTeams.add(new TeamManager("McLaren", 960_000, rand(17, 29)));
        botTeams.add(new TeamManager("Aston Martin", 920_000, rand(15, 27)));
        botTeams.add(new TeamManager("Alpine", 900_000, rand(14, 26)));
        botTeams.add(new TeamManager("Williams", 890_000, rand(13, 25)));
        botTeams.add(new TeamManager("Racing Bulls", 880_000, rand(13, 24)));
        botTeams.add(new TeamManager("Haas", 870_000, rand(12, 23)));
        botTeams.add(new TeamManager("Audi", 910_000, rand(15, 27)));
        botTeams.add(new TeamManager("Cadillac", 905_000, rand(14, 26)));
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
