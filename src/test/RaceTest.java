package test;

import components.*;
import economic.MarketService;
import game.GameSession;
import game.PlayerController;
import org.junit.Test;
import staff.Engineer;
import staff.MainDriver;
import staff.TeamManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RaceTest {
    @Test
    public void raceSimulatorRaceTime() {
        TeamManager manager = new TeamManager("Test", 1_000_000, 10);
        MarketService market = new MarketService(new Random(1));

        manager.getInventory().add(new Engine("E", 100, 90, EngineType.ATMOSPHERIC, 850, 150));
        manager.getInventory().add(new Transmission("T", 100, 90, EngineType.ATMOSPHERIC, 85));
        manager.getInventory().add(new Chassis("C", 100, 90, 180, "LIGHT", 80));
        manager.getInventory().add(new Suspension("S", 100, 90, "LIGHT", 82));
        manager.getInventory().add(new Aerodynamics("A", 100, 90, 84));
        manager.getInventory().add(new Tyres("Ty", 100, 90, "SOFT", 88, 75, 65));
        manager.getInventory().add(new Engine("E", 100, 90, EngineType.TURBO, 850, 150));
        manager.getInventory().add(new Transmission("T", 100, 90, EngineType.TURBO, 85));
        manager.getInventory().add(new Chassis("C", 100, 90, 180, "LIGHT", 80));
        manager.getInventory().add(new Suspension("S", 100, 90, "LIGHT", 82));
        manager.getInventory().add(new Aerodynamics("A", 100, 90, 84));
        manager.getInventory().add(new Tyres("Ty", 100, 90, "SOFT", 88, 75, 65));
        manager.getEngineers().add(new Engineer("Pop",100,10));
        manager.getEngineers().add(new Engineer("Pip",150,9));
        manager.getDrivers().add(new MainDriver("POOp", 100, 100, 100,100,100));
        manager.getDrivers().add(new MainDriver("Pip", 100, 100, 100,100,100));

        PlayerController controller = new PlayerController(new Scanner("1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n"), manager, market);

        controller.assembleCar();
        controller.assembleCar();

        assertEquals(2, manager.getCars().size());
        assertEquals(0, manager.getInventory().size());
    }

    @Test
    public void testRaceWeekendRun() throws Exception {
        Scanner scanner = new Scanner("1\n1\n1\n1\n");
        GameSession session = new GameSession(scanner);

        Field playerField = GameSession.class.getDeclaredField("player");
        playerField.setAccessible(true);
        TeamManager player = (TeamManager) playerField.get(session);

        player.getEngineers().add(new Engineer("Eng-1", 120_000, 95));
        player.getEngineers().add(new Engineer("Eng-2", 130_000, 93));
        MainDriver d1 = new MainDriver("Driver-1", 150_000, 94, 92, 95, 90);
        MainDriver d2 = new MainDriver("Driver-2", 140_000, 90, 94, 93, 92);
        player.getDrivers().add(d1);
        player.getDrivers().add(d2);

        player.getCars().add(buildCar("A", EngineType.ATMOSPHERIC));
        player.getCars().add(buildCar("B", EngineType.TURBO));

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

    private Car buildCar(String suffix, EngineType type) {
        return new Car(
                "Car-" + suffix,
                new Engine("E-" + suffix, 100, 90, type, 850, 150),
                new Transmission("T-" + suffix, 100, 90, type, 85),
                new Chassis("C-" + suffix, 100, 90, 180, "LIGHT", 80),
                new Suspension("S-" + suffix, 100, 90, "LIGHT", 82),
                new Aerodynamics("A-" + suffix, 100, 90, 84),
                new Tyres("Ty-" + suffix, 100, 90, "SOFT", 88, 75, 65)
        );
    }
}