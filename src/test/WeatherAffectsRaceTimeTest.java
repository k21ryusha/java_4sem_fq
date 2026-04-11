package test;

import components.Car;
import org.junit.Test;
import race_weekend.RaceSimulator;
import race_weekend.Track;
import race_weekend.Weather;
import staff.MainDriver;
import staff.TeamManager;

import static org.junit.Assert.assertNotEquals;

public class WeatherAffectsRaceTimeTest {
    @Test
    public void weatherAffectsRaceTime() {
        TeamManager team = new TeamManager("Team", 1_000_000, 20);
        Car car = TestSupport.createCar();
        MainDriver driver = new MainDriver("Driver", 0, 85, 85, 85, 85,0);
        Track track = new Track("Track", 5.0, 50, 15, 3, 20);

        double dry = new RaceSimulator(new TestSupport.ConstantRandom(0.5)).simulateRaceTime(team, car, driver, track, Weather.DRY);
        double wet = new RaceSimulator(new TestSupport.ConstantRandom(0.5)).simulateRaceTime(team, car, driver, track, Weather.WET);

        assertNotEquals(dry, wet, 0.0001);
    }
}
