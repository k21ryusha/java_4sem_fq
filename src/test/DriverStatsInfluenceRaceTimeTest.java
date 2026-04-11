package test;

import components.Car;
import org.junit.Test;
import race_weekend.RaceSimulator;
import race_weekend.Track;
import race_weekend.Weather;
import staff.MainDriver;
import staff.TeamManager;

import static org.junit.Assert.assertTrue;

public class DriverStatsInfluenceRaceTimeTest {
    @Test
    public void driverStatsInfluenceRaceTime() {
        TeamManager team = new TeamManager("Team", 1_000_000, 20);
        Car car = TestSupport.createCar();
        Track track = new Track("Track", 5.0, 50, 15, 3, 20);

        MainDriver weak = new MainDriver("Weak", 0, 70, 70, 70, 70,0);
        MainDriver strong = new MainDriver("Strong", 0, 98, 98, 98, 98,0);

        double weakTime = new RaceSimulator(new TestSupport.ConstantRandom(0.5)).simulateRaceTime(team, car, weak, track, Weather.DRY);
        double strongTime = new RaceSimulator(new TestSupport.ConstantRandom(0.5)).simulateRaceTime(team, car, strong, track, Weather.DRY);

        assertTrue(strongTime < weakTime);
    }
}
