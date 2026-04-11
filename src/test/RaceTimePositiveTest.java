package test;

import components.Car;
import org.junit.Test;
import race_weekend.RaceSimulator;
import race_weekend.Track;
import race_weekend.Weather;
import staff.MainDriver;
import staff.TeamManager;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class RaceTimePositiveTest {
    @Test
    public void raceSimulatorProducesPositiveRaceTime() {
        RaceSimulator simulator = new RaceSimulator(new Random(2));
        TeamManager team = new TeamManager("Team", 1_000_000, 20);
        Car car = TestSupport.createCar();
        MainDriver driver = new MainDriver("Driver", 0, 85, 85, 85, 85,0);
        Track track = new Track("Track", 5.0, 50, 15, 3, 20);

        double time = simulator.simulateRaceTime(team, car, driver, track, Weather.DRY);

        assertTrue(time > 0);
    }
}
