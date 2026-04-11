package test;

import components.Car;
import game.GameSession;
import incidents.IncidentService;
import org.junit.Test;
import staff.MainDriver;
import staff.TeamManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DriverDiscontentIncidentLogicTest {
    @Test
    public void lowStandingAddsDiscontentAndRaisesIncidentChance() throws Exception {
        GameSession session = new GameSession(new Scanner(""));

        Field playerField = GameSession.class.getDeclaredField("player");
        playerField.setAccessible(true);
        TeamManager player = (TeamManager) playerField.get(session);

        MainDriver driver = new MainDriver("Player Driver", 150_000, 90, 90, 90, 90, 15);
        player.getDrivers().add(driver);

        Field driverPointsField = GameSession.class.getDeclaredField("driverPoints");
        driverPointsField.setAccessible(true);
        Map<String, Integer> driverPoints = (Map<String, Integer>) driverPointsField.get(session);
        driverPoints.put("Lando Norris", 25);
        driverPoints.put("Oscar Piastri", 18);
        driverPoints.put("George Russell", 15);
        driverPoints.put("Charles Leclerc", 12);
        driverPoints.put("Lewis Hamilton", 10);
        driverPoints.put("Fernando Alonso", 8);
        driverPoints.put("Pierre Gasly", 6);
        driverPoints.put("Alexander Albon", 4);
        driverPoints.put("Yuki Tsunoda", 2);

        Method updatePlayerDriversDiscontent = GameSession.class.getDeclaredMethod("updatePlayerDriversDiscontent");
        updatePlayerDriversDiscontent.setAccessible(true);
        updatePlayerDriversDiscontent.invoke(session);

        assertEquals(20, driver.getDiscontent());

        Car car = TestSupport.createCar();
        IncidentService incidentService = new IncidentService(new TestSupport.ConstantRandom(0.12));

        assertFalse(incidentService.checkIncident(car, new MainDriver("Calm Driver", 100_000, 90, 90, 90, 90, 19)));
        assertTrue(incidentService.checkIncident(car, driver));
    }

    @Test
    public void printsMessageWhenPlayerDriverDiscontentChanges() throws Exception {
        GameSession session = new GameSession(new Scanner(""));

        Field playerField = GameSession.class.getDeclaredField("player");
        playerField.setAccessible(true);
        TeamManager player = (TeamManager) playerField.get(session);

        MainDriver driver = new MainDriver("Player Driver", 150_000, 90, 90, 90, 90, 15);
        player.getDrivers().add(driver);

        Field driverPointsField = GameSession.class.getDeclaredField("driverPoints");
        driverPointsField.setAccessible(true);
        Map<String, Integer> driverPoints = (Map<String, Integer>) driverPointsField.get(session);
        driverPoints.put("Lando Norris", 25);
        driverPoints.put("Oscar Piastri", 18);
        driverPoints.put("George Russell", 15);
        driverPoints.put("Charles Leclerc", 12);
        driverPoints.put("Lewis Hamilton", 10);
        driverPoints.put("Fernando Alonso", 8);
        driverPoints.put("Pierre Gasly", 6);
        driverPoints.put("Alexander Albon", 4);
        driverPoints.put("Yuki Tsunoda", 2);

        Method updatePlayerDriversDiscontent = GameSession.class.getDeclaredMethod("updatePlayerDriversDiscontent");
        updatePlayerDriversDiscontent.setAccessible(true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));
        try {
            updatePlayerDriversDiscontent.invoke(session);
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString().contains("Недовольство пилота Player Driver увеличилось: 15 -> 20."));
    }

    @Test
    public void printsResetMessageWhenPlayerDriverDiscontentHitsMaximum() throws Exception {
        GameSession session = new GameSession(new Scanner(""));

        Field playerField = GameSession.class.getDeclaredField("player");
        playerField.setAccessible(true);
        TeamManager player = (TeamManager) playerField.get(session);

        MainDriver driver = new MainDriver("Player Driver", 150_000, 90, 90, 90, 90, 95);
        player.getDrivers().add(driver);

        Field driverPointsField = GameSession.class.getDeclaredField("driverPoints");
        driverPointsField.setAccessible(true);
        Map<String, Integer> driverPoints = (Map<String, Integer>) driverPointsField.get(session);
        driverPoints.put("Lando Norris", 25);
        driverPoints.put("Oscar Piastri", 18);
        driverPoints.put("George Russell", 15);
        driverPoints.put("Charles Leclerc", 12);
        driverPoints.put("Lewis Hamilton", 10);
        driverPoints.put("Fernando Alonso", 8);
        driverPoints.put("Pierre Gasly", 6);
        driverPoints.put("Alexander Albon", 4);
        driverPoints.put("Yuki Tsunoda", 2);

        Method updatePlayerDriversDiscontent = GameSession.class.getDeclaredMethod("updatePlayerDriversDiscontent");
        updatePlayerDriversDiscontent.setAccessible(true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));
        try {
            updatePlayerDriversDiscontent.invoke(session);
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString().contains("Недовольство пилота Player Driver достигло максимума и сбросилось до 0."));
    }

    @Test
    public void discontentResetsToZeroWhenItReachesMaximum() {
        MainDriver driver = new MainDriver("Player Driver", 150_000, 90, 90, 90, 90, 95);

        driver.addDiscontent(5);

        assertEquals(0, driver.getDiscontent());
        assertFalse(new IncidentService(new TestSupport.ConstantRandom(0.12)).checkIncident(TestSupport.createCar(), driver));
    }
}
