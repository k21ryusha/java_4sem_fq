package test;

import game.GameSession;
import org.junit.Test;
import race_weekend.Track;
import race_weekend.TrackCatalogManager;
import save.SaveManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrackEditorAndCalendarTest {
    @Test
    public void preSeasonSetupAllowsCreatingTrackAndCustomCalendar() throws Exception {
        String input = String.join("\n",
                "2",
                "My Test Track",
                "6.5",
                "45",
                "12",
                "4",
                "15",
                "4",
                "25"
        ) + "\n";

        GameSession session = new GameSession(
                new Scanner(input),
                "Tester",
                new SaveManager(Files.createTempDirectory("track-editor"))
        );

        Method ensurePreSeasonSetup = GameSession.class.getDeclaredMethod("ensurePreSeasonSetup");
        ensurePreSeasonSetup.setAccessible(true);
        ensurePreSeasonSetup.invoke(session);

        Field tracksField = GameSession.class.getDeclaredField("tracks");
        tracksField.setAccessible(true);
        List<?> seasonCalendar = (List<?>) tracksField.get(session);

        Field seasonSetupCompletedField = GameSession.class.getDeclaredField("seasonSetupCompleted");
        seasonSetupCompletedField.setAccessible(true);

        assertEquals(1, seasonCalendar.size());
        assertEquals("My Test Track", ((Track) seasonCalendar.get(0)).getName());
        assertTrue((Boolean) seasonSetupCompletedField.get(session));
    }

    @Test
    public void createdTrackBecomesAvailableForAnotherPlayer() throws Exception {
        Path tempCatalog = Files.createTempDirectory("shared-tracks").resolve("custom_tracks.xml");
        TrackCatalogManager catalogManager = new TrackCatalogManager(tempCatalog);

        GameSession firstSession = new GameSession(
                new Scanner("Shared Track\n5.4\n50\n14\n4\n12\n"),
                "PlayerOne",
                new SaveManager(Files.createTempDirectory("save-one")),
                catalogManager
        );

        Method createTrack = GameSession.class.getDeclaredMethod("createTrack");
        createTrack.setAccessible(true);
        createTrack.invoke(firstSession);

        GameSession secondSession = new GameSession(
                new Scanner(""),
                "PlayerTwo",
                new SaveManager(Files.createTempDirectory("save-two")),
                catalogManager
        );

        Field trackLibraryField = GameSession.class.getDeclaredField("trackLibrary");
        trackLibraryField.setAccessible(true);
        List<?> trackLibrary = (List<?>) trackLibraryField.get(secondSession);

        boolean found = trackLibrary.stream()
                .map(Track.class::cast)
                .anyMatch(track -> track.getName().equals("Shared Track"));

        assertTrue(found);
    }
}
