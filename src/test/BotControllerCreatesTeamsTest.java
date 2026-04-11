package test;

import game.BotController;
import org.junit.Test;
import staff.TeamManager;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BotControllerCreatesTeamsTest {
    @Test
    public void botControllerCreatesExpectedTeams() {
        BotController botController = new BotController(new Random(7));

        List<TeamManager> bots = botController.getBotTeams();
        assertEquals(10, bots.size());
        assertTrue(bots.stream().allMatch(team -> team.getReputation() >= 12 && team.getReputation() <= 30));
    }
}
