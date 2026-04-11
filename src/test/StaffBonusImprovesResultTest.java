package test;

import components.Car;
import org.junit.Test;
import race_weekend.RaceSimulator;
import race_weekend.Track;
import race_weekend.Weather;
import staff.Engineer;
import staff.MainDriver;
import staff.TeamManager;

import static org.junit.Assert.assertTrue;

public class StaffBonusImprovesResultTest {
    @Test
    public void staffBonusImprovesResult() {
        Car car = TestSupport.createCar();
        MainDriver driver = new MainDriver("Driver", 0, 85, 85, 85, 85,0);
        Track track = new Track("Track", 5.0, 50, 15, 3, 20);

        TeamManager noStaff = new TeamManager("NoStaff", 1_000_000, 20);
        TeamManager withStaff = new TeamManager("WithStaff", 1_000_000, 20);
        withStaff.getEngineers().add(new Engineer("Strong Engineer", 0, 100));

        double timeWithout = new RaceSimulator(new TestSupport.ConstantRandom(0.5)).simulateRaceTime(noStaff, car, driver, track, Weather.DRY);
        double timeWith = new RaceSimulator(new TestSupport.ConstantRandom(0.5)).simulateRaceTime(withStaff, car, driver, track, Weather.DRY);

        assertTrue(timeWith < timeWithout);
    }
}
