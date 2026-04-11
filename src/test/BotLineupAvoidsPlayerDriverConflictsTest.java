package test;

import game.GameSession;
import org.junit.Test;
import save.GameState;
import save.SaveManager;
import staff.MainDriver;
import staff.TeamManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BotLineupAvoidsPlayerDriverConflictsTest {
    @Test
    public void botTeamsReplaceDriversAlreadyHiredByPlayer() throws Exception {
        TeamManager player = new TeamManager("Player Racing", 9_000_000, 10);
        player.getDrivers().add(new MainDriver("Charles Leclerc", 185_000, 96, 96, 92, 89, 0));
        player.getDrivers().add(new MainDriver("Lando Norris", 180_000, 95, 94, 91, 88, 0));

        GameState state = new GameState(
                "Player",
                player,
                List.of(),
                new HashMap<>(),
                new HashMap<>(),
                List.of(),
                List.of(),
                0,
                false,
                false
        );

        GameSession session = new GameSession(new Scanner(System.in), state, new SaveManager());

        Field teamDriversField = GameSession.class.getDeclaredField("teamDrivers");
        teamDriversField.setAccessible(true);
        Map<String, List<String>> teamDrivers = (Map<String, List<String>>) teamDriversField.get(session);

        assertEquals(2, teamDrivers.get("Ferrari").size());
        assertEquals(2, teamDrivers.get("McLaren").size());
        assertFalse(teamDrivers.get("Ferrari").contains("Charles Leclerc"));
        assertFalse(teamDrivers.get("McLaren").contains("Lando Norris"));

        Map<String, Integer> assignedCounts = new HashMap<>();
        for (List<String> lineup : teamDrivers.values()) {
            for (String driverName : lineup) {
                assignedCounts.merge(driverName, 1, Integer::sum);
            }
        }

        assertTrue(assignedCounts.values().stream().allMatch(count -> count == 1));
        assertFalse(assignedCounts.containsKey("Charles Leclerc"));
        assertFalse(assignedCounts.containsKey("Lando Norris"));
    }
}
