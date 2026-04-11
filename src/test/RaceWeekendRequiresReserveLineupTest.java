package test;

import game.GameSession;
import org.junit.Test;
import staff.Engineer;
import staff.MainDriver;
import staff.TeamManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class RaceWeekendRequiresReserveLineupTest {
    @Test
    public void testRaceWeekendDoesNotStartWithoutThirdDriverAndCar() throws Exception {
        Scanner scanner = new Scanner("1\n1\n1\n1\n");
        GameSession session = new GameSession(scanner);

        Field playerField = GameSession.class.getDeclaredField("player");
        playerField.setAccessible(true);
        TeamManager player = (TeamManager) playerField.get(session);

        player.getEngineers().add(new Engineer("Eng-1", 120_000, 95));
        player.getEngineers().add(new Engineer("Eng-2", 130_000, 93));
        player.getDrivers().add(new MainDriver("Driver-1", 150_000, 94, 92, 95, 90,0));
        player.getDrivers().add(new MainDriver("Driver-2", 140_000, 90, 94, 93, 92,0));

        player.getCars().add(TestSupport.createCar("A", components.EngineType.ATMOSPHERIC));
        player.getCars().add(TestSupport.createCar("B", components.EngineType.TURBO));

        Method startRaceWeekend = GameSession.class.getDeclaredMethod("startRaceWeekend");
        startRaceWeekend.setAccessible(true);
        startRaceWeekend.invoke(session);

        Field raceHistoryField = GameSession.class.getDeclaredField("raceHistory");
        raceHistoryField.setAccessible(true);
        List<?> raceHistory = (List<?>) raceHistoryField.get(session);

        Field championshipRoundField = GameSession.class.getDeclaredField("championshipRound");
        championshipRoundField.setAccessible(true);
        int championshipRound = (int) championshipRoundField.get(session);

        assertEquals(0, raceHistory.size());
        assertEquals(0, championshipRound);
    }
}
