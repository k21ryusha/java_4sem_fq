package test;

import game.GameSession;
import org.junit.Test;
import save.SaveManager;
import staff.Engineer;
import staff.MainDriver;
import staff.TeamManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RaceWeekendRunTest {
    @Test
    public void testRaceWeekendRun() throws Exception {
        Scanner scanner = new Scanner("1\n1\n1\n1\n");
        SaveManager saveManager = new SaveManager(Files.createTempDirectory("race-weekend-run"));
        GameSession session = new GameSession(scanner, "Player", saveManager);

        Field playerField = GameSession.class.getDeclaredField("player");
        playerField.setAccessible(true);
        TeamManager player = (TeamManager) playerField.get(session);

        player.getEngineers().add(new Engineer("Eng-1", 120_000, 95));
        player.getEngineers().add(new Engineer("Eng-2", 130_000, 93));
        player.getDrivers().add(new MainDriver("Driver-1", 150_000, 94, 92, 95, 90,0));
        player.getDrivers().add(new MainDriver("Driver-2", 140_000, 90, 94, 93, 92,0));
        player.getDrivers().add(new MainDriver("Driver-3", 135_000, 89, 89, 91, 88,0));

        player.getCars().add(TestSupport.createCar("A", components.EngineType.ATMOSPHERIC));
        player.getCars().add(TestSupport.createCar("B", components.EngineType.TURBO));
        player.getCars().add(TestSupport.createCar("C", components.EngineType.TURBO));

        Method startRaceWeekend = GameSession.class.getDeclaredMethod("startRaceWeekend");
        startRaceWeekend.setAccessible(true);
        startRaceWeekend.invoke(session);

        Field raceHistoryField = GameSession.class.getDeclaredField("raceHistory");
        raceHistoryField.setAccessible(true);
        List<?> raceHistory = (List<?>) raceHistoryField.get(session);

        Field championshipRoundField = GameSession.class.getDeclaredField("championshipRound");
        championshipRoundField.setAccessible(true);
        int championshipRound = (int) championshipRoundField.get(session);

        Field parcFermeLockedField = GameSession.class.getDeclaredField("parcFermeLocked");
        parcFermeLockedField.setAccessible(true);
        boolean parcFermeLocked = (boolean) parcFermeLockedField.get(session);

        assertEquals(1, raceHistory.size());
        assertEquals(1, championshipRound);
        assertTrue(parcFermeLocked);
    }
}
