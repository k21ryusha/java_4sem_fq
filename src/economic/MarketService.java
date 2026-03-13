package economic;

import components.*;
import staff.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MarketService {
    private final Random random;

    public MarketService(Random random) {
        this.random = random;
    }

    public List<Component> generateComponentOffers() {
        List<Component> offers = new ArrayList<>();
        offers.add(new Engine("Vortex A1", 150_000, rand(65, 90), EngineType.ATMOSPHERIC, rand(700, 820), rand(140, 180)));
        offers.add(new Engine("Falcon Turbo", 210_000, rand(70, 94), EngineType.TURBO, rand(760, 910), rand(150, 190)));

        offers.add(new Transmission("TR-AT", 90_000, rand(60, 88), EngineType.ATMOSPHERIC, rand(72, 90)));
        offers.add(new Transmission("TR-TB", 110_000, rand(65, 92), EngineType.TURBO, rand(74, 93)));

        offers.add(new Chassis("C-Light", 130_000, rand(62, 88), 175, "LIGHT", rand(66, 87)));
        offers.add(new Chassis("C-Heavy", 150_000, rand(65, 91), 210, "HEAVY", rand(64, 85)));

        offers.add(new Suspension("S-Light Pro", 80_000, rand(60, 86), "LIGHT", rand(70, 92)));
        offers.add(new Suspension("S-Heavy Pro", 82_000, rand(60, 86), "HEAVY", rand(68, 90)));

        offers.add(new Aerodynamics("AeroWing", 95_000, rand(64, 90), rand(70, 95)));
        offers.add(new Aerodynamics("AeroMax", 120_000, rand(70, 94), rand(78, 98)));

        offers.add(new Tyres("Soft", 55_000, rand(66, 92), "SOFT", rand(82, 96), rand(45, 65), rand(60, 76)));
        offers.add(new Tyres("Medium", 60_000, rand(68, 92), "MEDIUM", rand(74, 88), rand(62, 80), rand(65, 80)));
        offers.add(new Tyres("Hard", 58_000, rand(65, 90), "HARD", rand(68, 84), rand(75, 92), rand(62, 78)));
        return offers;
    }

    public List<Staff> generateEngineerCandidates() {
        List<Staff> candidates = new ArrayList<>();
        candidates.add(new Mechanic("Alex Mechanic", 70_000, rand(55, 85)));
        candidates.add(new ElectronicsEngineer("Maks Electronics", 65_000, rand(50, 88)));
        candidates.add(new TechnicalDirector("Ira Technical", 90_000, rand(60, 92)));
        return candidates;
    }

    public List<MainDriver> generateDriverCandidates() {
        List<MainDriver> candidates = new ArrayList<>();
        candidates.add(new MainDriver("Veteran Ivan", 130_000, 80, 78, 90, 70));
        candidates.add(new SecondDriver("Rookie Dan", 90_000, 74, 82, 63, 65));
        candidates.add(new MainDriver("Rain Master Mia", 120_000, 77, 75, 82, 93));
        return candidates;
    }

    private int rand(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}
