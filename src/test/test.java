package test;

import components.*;
import economic.MarketService;
import game.BotController;
import game.PlayerController;
import incidents.IncidentService;
import org.junit.jupiter.api.Test;
import race_weekend.RaceSimulator;
import race_weekend.Track;
import race_weekend.Weather;
import staff.*;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class GameLogicTest {

    @Test
    void purchaseAndHiringAreAppliedCorrectly() {
        TeamManager manager = new TeamManager("Test", 500_000, 10);
        FixedMarketService market = new FixedMarketService();

        PlayerController buyController = new PlayerController(new Scanner("1\n0\n"), manager, market);
        buyController.buyComponents();

        assertEquals(1, manager.getInventory().size());
        assertEquals(400_000, manager.getBudget());

        PlayerController hireController = new PlayerController(new Scanner("1\n"), manager, market);
        hireController.hireEngineer();

        assertEquals(1, manager.getEngineers().size());
        assertEquals(320_000, manager.getBudget());
    }

    @Test
    void incidentMechanismDestroysComponentWhenChanceTriggers() {
        Car car = createCar();
        car.getEngine().setWear(70);
        car.getTransmission().setWear(30);

        IncidentService service = new IncidentService(new PredictableRandom(new double[]{0.0}, new int[]{0}));

        boolean happened = service.checkIncident(car);

        assertTrue(happened);
        assertTrue(car.getEngine().isDestroyed());
    }

    @Test
    void componentCompatibilityIsValidatedDuringAssembly() {
        TeamManager manager = new TeamManager("Test", 1_000_000, 10);
        MarketService market = new MarketService(new Random(1));

        manager.getInventory().add(new Engine("E", 100, 90, EngineType.TURBO, 850, 150));
        manager.getInventory().add(new Transmission("T", 100, 90, EngineType.ATMOSPHERIC, 85));
        manager.getInventory().add(new Chassis("C", 100, 90, 180, "LIGHT", 80));
        manager.getInventory().add(new Suspension("S", 100, 90, "LIGHT", 82));
        manager.getInventory().add(new Aerodynamics("A", 100, 90, 84));
        manager.getInventory().add(new Tyres("Ty", 100, 90, "SOFT", 88, 75, 65));

        PlayerController controller = new PlayerController(new Scanner("1\n1\n1\n1\n1\n1\n"), manager, market);
        controller.assembleCar();

        assertTrue(manager.getCars().isEmpty());
    }

    @Test
    void raceSimulatorProducesPositiveRaceTime() {
        RaceSimulator simulator = new RaceSimulator(new Random(2));
        TeamManager team = new TeamManager("Team", 1_000_000, 20);
        Car car = createCar();
        MainDriver driver = new MainDriver("Driver", 0, 85, 85, 85, 85);
        Track track = new Track("Track", 5.0, 50, 15, 3, 20);

        double time = simulator.simulateRaceTime(team, car, driver, track, Weather.DRY);

        assertTrue(time > 0);
    }

    @Test
    void weatherAffectsRaceTime() {
        RaceSimulator simulator = new RaceSimulator(new Random(3));
        TeamManager team = new TeamManager("Team", 1_000_000, 20);
        Car car = createCar();
        MainDriver driver = new MainDriver("Driver", 0, 85, 85, 85, 85);
        Track track = new Track("Track", 5.0, 50, 15, 3, 20);

        double dry = simulator.simulateRaceTime(team, car, driver, track, Weather.DRY);
        double wet = simulator.simulateRaceTime(team, car, driver, track, Weather.WET);

        assertNotEquals(dry, wet);
    }

    @Test
    void staffBonusImprovesResult() {
        RaceSimulator simulator = new RaceSimulator(new Random(4));
        Car car = createCar();
        MainDriver driver = new MainDriver("Driver", 0, 85, 85, 85, 85);
        Track track = new Track("Track", 5.0, 50, 15, 3, 20);

        TeamManager noStaff = new TeamManager("NoStaff", 1_000_000, 20);
        TeamManager withStaff = new TeamManager("WithStaff", 1_000_000, 20);
        withStaff.getEngineers().add(new Engineer("Strong Engineer", 0, 100));

        double timeWithout = simulator.simulateRaceTime(noStaff, car, driver, track, Weather.DRY);
        double timeWith = simulator.simulateRaceTime(withStaff, car, driver, track, Weather.DRY);

        assertTrue(timeWith < timeWithout);
    }

    @Test
    void cannotAssembleCarWhenSomeComponentsMissing() {
        TeamManager manager = new TeamManager("Test", 1_000_000, 10);
        MarketService market = new MarketService(new Random(5));

        manager.getInventory().add(new Engine("E", 100, 90, EngineType.TURBO, 850, 150));
        manager.getInventory().add(new Transmission("T", 100, 90, EngineType.TURBO, 85));
        manager.getInventory().add(new Chassis("C", 100, 90, 180, "LIGHT", 80));
        manager.getInventory().add(new Suspension("S", 100, 90, "LIGHT", 82));
        manager.getInventory().add(new Aerodynamics("A", 100, 90, 84));

        PlayerController controller = new PlayerController(new Scanner(""), manager, market);
        controller.assembleCar();

        assertTrue(manager.getCars().isEmpty());
    }

    @Test
    void driverStatsInfluenceRaceTime() {
        RaceSimulator simulator = new RaceSimulator(new Random(6));
        TeamManager team = new TeamManager("Team", 1_000_000, 20);
        Car car = createCar();
        Track track = new Track("Track", 5.0, 50, 15, 3, 20);

        MainDriver weak = new MainDriver("Weak", 0, 70, 70, 70, 70);
        MainDriver strong = new MainDriver("Strong", 0, 98, 98, 98, 98);

        double weakTime = simulator.simulateRaceTime(team, car, weak, track, Weather.DRY);
        double strongTime = simulator.simulateRaceTime(team, car, strong, track, Weather.DRY);



        assertTrue(strongTime < weakTime);
    }

    @Test
    void botControllerCreatesExpectedTeams() {
        BotController botController = new BotController(new Random(7));

        List<TeamManager> bots = botController.getBotTeams();
        assertEquals(10, bots.size());
        assertTrue(bots.stream().allMatch(team -> team.getReputation() >= 12 && team.getReputation() <= 30));
    }

    private Car createCar() {
        return new Car(
                "TestCar",
                new Engine("E", 100, 90, EngineType.TURBO, 850, 150),
                new Transmission("T", 100, 90, EngineType.TURBO, 85),
                new Chassis("C", 100, 90, 180, "LIGHT", 80),
                new Suspension("S", 100, 90, "LIGHT", 82),
                new Aerodynamics("A", 100, 90, 84),
                new Tyres("Ty", 100, 90, "SOFT", 88, 75, 65)
        );
    }

    private static class FixedMarketService extends MarketService {
        FixedMarketService() {
            super(new Random(0));
        }

        @Override
        public List<Component> generateComponentOffers() {
            return List.of(new Engine("Fixed", 100_000, 90, EngineType.TURBO, 850, 150));
        }

        @Override
        public List<Engineer> generateEngineerCandidates() {
            return List.of(new Engineer("Fixed Engineer", 80_000, 90));
        }
    }

    private static class PredictableRandom extends Random {
        private final double[] doubles;
        private final int[] ints;
        private int doubleIndex = 0;
        private int intIndex = 0;

        PredictableRandom(double[] doubles, int[] ints) {
            this.doubles = doubles;
            this.ints = ints;
        }

        @Override
        public double nextDouble() {
            if (doubleIndex >= doubles.length) {
                return doubles[doubles.length - 1];
            }
            return doubles[doubleIndex++];
        }

        @Override
        public int nextInt(int bound) {
            if (intIndex >= ints.length) {
                return 0;
            }
            return Math.floorMod(ints[intIndex++], bound);
        }
    }
}