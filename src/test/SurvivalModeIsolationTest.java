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

public class SurvivalModeIsolationTest {
    @Test
    public void survivalModeLeavesMainCareerStateUntouchedAfterExit() throws Exception {
        Scanner scanner = new Scanner("0\n0\n0\n0\n0\n0\n0\n0\n0\n4\n");
        SaveManager saveManager = new SaveManager(Files.createTempDirectory("survival-isolation"));
        GameSession session = new GameSession(scanner, "Player", saveManager);

        Field playerField = GameSession.class.getDeclaredField("player");
        playerField.setAccessible(true);
        TeamManager player = (TeamManager) playerField.get(session);

        player.getEngineers().add(new Engineer("Eng-1", 120_000, 95));
        player.getEngineers().add(new Engineer("Eng-2", 130_000, 93));
        player.getDrivers().add(new MainDriver("Driver-1", 150_000, 94, 92, 95, 90, 0));
        player.getDrivers().add(new MainDriver("Driver-2", 140_000, 90, 94, 93, 92, 0));
        player.getDrivers().add(new MainDriver("Driver-3", 135_000, 89, 89, 91, 88, 0));
        player.getCars().add(TestSupport.createCar("A", components.EngineType.TURBO));
        player.getCars().add(TestSupport.createCar("B", components.EngineType.TURBO));
        player.getCars().add(TestSupport.createCar("C", components.EngineType.TURBO));

        int budgetBefore = player.getBudget();
        int reputationBefore = player.getReputation();
        List<Double> wearBefore = player.getCars().stream().map(car -> car.averageWear()).toList();

        Method launchSurvivalMode = GameSession.class.getDeclaredMethod("launchSurvivalMode");
        launchSurvivalMode.setAccessible(true);
        launchSurvivalMode.invoke(session);

        assertEquals(budgetBefore, player.getBudget());
        assertEquals(reputationBefore, player.getReputation());
        assertEquals(wearBefore, player.getCars().stream().map(car -> car.averageWear()).toList());
    }
}
