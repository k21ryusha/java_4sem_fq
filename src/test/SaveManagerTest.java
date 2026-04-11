package test;

import game.GameSession;
import org.junit.Test;
import race_weekend.RaceResult;
import race_weekend.Track;
import race_weekend.Weather;
import save.GameState;
import save.SaveManager;
import save.SaveSlot;
import staff.Engineer;
import staff.MainDriver;
import staff.TeamManager;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SaveManagerTest {
    @Test
    public void saveManagerStoresAndLoadsPlayerState() throws Exception {
        Path tempDir = Files.createTempDirectory("f1-saves");
        SaveManager saveManager = new SaveManager(tempDir);

        TeamManager player = new TeamManager("Kirill Racing", 123_456, 17);
        player.getEngineers().add(new Engineer("Eng-1", 80_000, 90));
        player.getDrivers().add(new MainDriver("Pilot-1", 90_000, 88, 87, 86, 85, 12));
        player.getCars().add(TestSupport.createCar());

        GameState original = new GameState(
                "Kirill",
                player,
                List.of(new RaceResult("Monza", Weather.DRY, List.of("1. Pilot-1"))),
                Map.of("Pilot-1", 25),
                Map.of("Kirill Racing", 25),
                List.of(new Track("Custom", 4.2, 55, 12, 3, 20)),
                List.of(new Track("Custom", 4.2, 55, 12, 3, 20)),
                4,
                true,
                true
        );

        SaveSlot created = saveManager.saveManual(original);
        List<SaveSlot> saves = saveManager.listSaves("Kirill");
        GameState restored = saveManager.load(created);

        assertEquals(1, saves.size());
        assertTrue(created.getPath().getFileName().toString().endsWith(".json"));
        assertEquals("Kirill", restored.getPlayerName());
        assertEquals(123_456, restored.getPlayer().getBudget());
        assertEquals(1, restored.getPlayer().getCars().size());
        assertEquals(1, restored.getRaceHistory().size());
        assertEquals(1, restored.getTrackLibrary().size());
        assertEquals("Custom", restored.getSeasonCalendar().get(0).getName());
        assertEquals(4, restored.getChampionshipRound());
        assertEquals(25, (int) restored.getDriverPoints().get("Pilot-1"));
        assertFalse(saves.get(0).isAutoSave());
    }

    @Test
    public void gameSessionRestoresLoadedProgress() throws Exception {
        Path tempDir = Files.createTempDirectory("f1-session-saves");
        SaveManager saveManager = new SaveManager(tempDir);

        TeamManager player = new TeamManager("Loaded Racing", 777_000, 21);
        player.getDrivers().add(new MainDriver("Loaded Driver", 100_000, 90, 89, 88, 87, 3));
        GameState state = new GameState(
                "Loaded",
                player,
                List.of(new RaceResult("Spa", Weather.RAIN, List.of("1. Loaded Driver"))),
                Map.of("Loaded Driver", 18),
                Map.of("Loaded Racing", 18),
                List.of(new Track("Loaded Track", 5.0, 60, 15, 4, 30)),
                List.of(new Track("Loaded Track", 5.0, 60, 15, 4, 30)),
                7,
                true,
                true
        );

        GameSession session = new GameSession(new Scanner(""), state, saveManager);

        Field playerField = GameSession.class.getDeclaredField("player");
        playerField.setAccessible(true);
        TeamManager restoredPlayer = (TeamManager) playerField.get(session);

        Field championshipRoundField = GameSession.class.getDeclaredField("championshipRound");
        championshipRoundField.setAccessible(true);

        Field parcFermeLockedField = GameSession.class.getDeclaredField("parcFermeLocked");
        parcFermeLockedField.setAccessible(true);

        Field raceHistoryField = GameSession.class.getDeclaredField("raceHistory");
        raceHistoryField.setAccessible(true);
        List<?> restoredHistory = (List<?>) raceHistoryField.get(session);

        assertEquals("Loaded Racing", restoredPlayer.getTeamName());
        assertEquals(777_000, restoredPlayer.getBudget());
        assertEquals(7, championshipRoundField.get(session));
        assertEquals(true, parcFermeLockedField.get(session));
        assertEquals(1, restoredHistory.size());
    }
}
