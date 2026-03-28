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
        offers.add(new Engine("Vortex Atmospheric", 150_000, rand(65, 90), EngineType.ATMOSPHERIC, rand(700, 820), rand(140, 180)));
        offers.add(new Engine("Falcon Turbo", 210_000, rand(70, 94), EngineType.TURBO, rand(760, 910), rand(150, 190)));

        offers.add(new Transmission("TR-Atmosphere", 90_000, rand(60, 88), EngineType.ATMOSPHERIC, rand(72, 90)));
        offers.add(new Transmission("TR-Turbo", 110_000, rand(65, 92), EngineType.TURBO, rand(74, 93)));

        offers.add(new Chassis("C-Light", 130_000, rand(62, 88), 175, "LIGHT", rand(66, 87)));
        offers.add(new Chassis("C-Heavy", 150_000, rand(65, 91), 210, "HEAVY", rand(64, 85)));

        offers.add(new Suspension("S-Light Pro", 80_000, rand(60, 86), "LIGHT", rand(70, 92)));
        offers.add(new Suspension("S-Heavy Pro", 82_000, rand(60, 86), "HEAVY", rand(68, 90)));

        offers.add(new Aerodynamics("AeroWing", 95_000, rand(64, 90), rand(70, 95)));
        offers.add(new Aerodynamics("AeroMax", 120_000, rand(70, 94), rand(78, 98)));

        offers.add(new Tyres("Soft", 55_000, rand(66, 92), "SOFT", rand(82, 96), rand(45, 65), rand(60, 76)));
        offers.add(new Tyres("Medium", 60_000, rand(68, 92), "MEDIUM", rand(74, 88), rand(62, 80), rand(65, 80)));
        offers.add(new Tyres("Hard", 58_000, rand(65, 90), "HARD", rand(68, 84), rand(75, 92), rand(62, 78)));
        offers.add(new Tyres("Intermediate", 62_000, rand(67, 91), "INTERMEDIATE", rand(70, 84), rand(60, 78), rand(78, 92)));
        offers.add(new Tyres("Wet", 64_000, rand(68, 92), "WET", rand(66, 80), rand(58, 76), rand(88, 98)));
        return offers;
    }

    public List<TechnicalDirector> generateTechnicalDirectorCandidates() {
        List<TechnicalDirector> candidates = new ArrayList<>();
        candidates.add(new TechnicalDirector("James Allison", 140_000, rand(90, 98)));
        candidates.add(new TechnicalDirector("Pierre Wache", 138_000, rand(89, 97)));
        candidates.add(new TechnicalDirector("Enrico Cardile", 136_000, rand(88, 96)));
        candidates.add(new TechnicalDirector("Pat Fry", 132_000, rand(86, 95)));
        candidates.add(new TechnicalDirector("Adrian Newey", 150_000, rand(93, 99)));
        return candidates;
    }

    public List<Engineer> generateEngineerCandidates() {
        List<Engineer> candidates = new ArrayList<>();
        candidates.add(new Engineer("Peter Bonnington", 112_000, rand(84, 94)));
        candidates.add(new Engineer("Riccardo Adami", 109_000, rand(82, 93)));
        candidates.add(new Engineer("Will Joseph", 106_000, rand(81, 92)));
        candidates.add(new Engineer("Bryan Bozzi", 104_000, rand(80, 91)));
        candidates.add(new Engineer("Hugh Bird", 108_000, rand(82, 92)));
        candidates.add(new Engineer("Xavi Marcos", 105_000, rand(80, 90)));
        return candidates;
    }

    public List<Mechanic> generateMechanicCandidates() {
        List<Mechanic> candidates = new ArrayList<>();
        candidates.add(new Mechanic("Calum Nicholas", 96_000, rand(78, 90)));
        candidates.add(new Mechanic("Lee Stevenson", 94_000, rand(76, 89)));
        candidates.add(new Mechanic("Ole Schack", 95_000, rand(77, 89)));
        candidates.add(new Mechanic("Michael Manning", 92_000, rand(75, 88)));
        candidates.add(new Mechanic("Steve Matchett", 98_000, rand(79, 91)));
        return candidates;
    }

    public List<ElectronicsEngineer> generateElectronicsEngineerCandidates() {
        List<ElectronicsEngineer> candidates = new ArrayList<>();
        candidates.add(new ElectronicsEngineer("Michael Italiano", 98_000, rand(79, 91)));
        candidates.add(new ElectronicsEngineer("Richard Wood", 96_000, rand(77, 90)));
        candidates.add(new ElectronicsEngineer("Luca Baldisserri", 101_000, rand(81, 92)));
        candidates.add(new ElectronicsEngineer("Ron Meadows", 102_000, rand(82, 93)));
        candidates.add(new ElectronicsEngineer("Andrea Sella", 95_000, rand(76, 89)));
        return candidates;
    }

    public List<Principal> generatePrincipalCandidates() {
        List<Principal> candidates = new ArrayList<>();
        candidates.add(new Principal("Toto Wolff", 165_000, rand(93, 99)));
        candidates.add(new Principal("Fred Vasseur", 155_000, rand(90, 98)));
        candidates.add(new Principal("Zak Brown", 150_000, rand(88, 97)));
        candidates.add(new Principal("Christian Horner", 160_000, rand(92, 99)));
        candidates.add(new Principal("James Vowles", 148_000, rand(87, 96)));
        candidates.add(new Principal("Ayao Komatsu", 142_000, rand(85, 95)));
        return candidates;
    }
    public List<MainDriver> generateDriverCandidates() {
        List<MainDriver> candidates = new ArrayList<>();
        candidates.add(new MainDriver("Max Verstappen", 190_000, 98, 98, 95, 94,0));
        candidates.add(new MainDriver("Sergio Perez", 150_000, 90, 91, 88, 86,0));
        candidates.add(new MainDriver("Charles Leclerc", 185_000, 96, 96, 92, 89,0));
        candidates.add(new MainDriver("Lewis Hamilton", 185_000, 95, 95, 96, 94,0));
        candidates.add(new MainDriver("Lando Norris", 180_000, 95, 94, 91, 88,0));
        candidates.add(new MainDriver("Oscar Piastri", 176_000, 93, 93, 90, 87,0));
        candidates.add(new MainDriver("George Russell", 177_000, 94, 94, 92, 89,0));
        candidates.add(new MainDriver("Andrea Kimi Antonelli", 150_000, 88, 89, 84, 83,0));
        candidates.add(new MainDriver("Fernando Alonso", 176_000, 94, 92, 95, 93,0));
        candidates.add(new MainDriver("Lance Stroll", 140_000, 84, 85, 82, 80,0));
        candidates.add(new MainDriver("Pierre Gasly", 156_000, 90, 90, 88, 85,0));
        candidates.add(new MainDriver("Franco Colapinto", 132_000, 82, 83, 79, 78,0));
        candidates.add(new MainDriver("Yuki Tsunoda", 152_000, 89, 89, 85, 84,0));
        candidates.add(new MainDriver("Isack Hadjar", 130_000, 81, 82, 78, 77,0));
        candidates.add(new MainDriver("Esteban Ocon", 154_000, 89, 90, 87, 84,0));
        candidates.add(new MainDriver("Oliver Bearman", 134_000, 83, 84, 80, 79,0));
        candidates.add(new MainDriver("Nico Hulkenberg", 152_000, 88, 89, 90, 83,0));
        candidates.add(new MainDriver("Gabriel Bortoleto", 131_000, 82, 83, 79, 78,0));
        candidates.add(new MainDriver("Alexander Albon", 160_000, 91, 90, 87, 85,0));
        candidates.add(new MainDriver("Carlos Sainz", 174_000, 93, 93, 91, 88,0));
        candidates.add(new MainDriver("Valtteri Bottas", 155_000, 89, 90, 89, 84,0));
        candidates.add(new MainDriver("Liam Lawson", 142_000, 85, 86, 81, 80,0));
        return candidates;
    }

    private int rand(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}
