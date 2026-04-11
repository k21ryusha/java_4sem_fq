package test;

import components.*;
import economic.MarketService;
import staff.Engineer;
import staff.MainDriver;

import java.util.List;
import java.util.Random;

final class TestSupport {
    private TestSupport() {
    }

    static Car createCar() {
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

    static Car createCar(String suffix, EngineType type) {
        return new Car(
                "Car-" + suffix,
                new Engine("E-" + suffix, 100, 90, type, 850, 150),
                new Transmission("T-" + suffix, 100, 90, type, 85),
                new Chassis("C-" + suffix, 100, 90, 180, "LIGHT", 80),
                new Suspension("S-" + suffix, 100, 90, "LIGHT", 82),
                new Aerodynamics("A-" + suffix, 100, 90, 84),
                new Tyres("Ty-" + suffix, 100, 90, "SOFT", 88, 75, 65)
        );
    }

    static List<Tyres> createWeekendTyreSets(String suffix) {
        return List.of(
                new Tyres("Soft-" + suffix, 100, 90, "SOFT", 88, 55, 65),
                new Tyres("Medium-" + suffix, 100, 90, "MEDIUM", 82, 72, 70),
                new Tyres("Hard-" + suffix, 100, 90, "HARD", 76, 85, 68),
                new Tyres("Intermediate-" + suffix, 100, 90, "INTERMEDIATE", 78, 70, 86),
                new Tyres("Wet-" + suffix, 100, 90, "WET", 72, 68, 94)
        );
    }

    static final class FixedMarketService extends MarketService {
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

        @Override
        public List<MainDriver> generateDriverCandidates() {
            return List.of(new MainDriver("Fixed Driver", 90_000, 88, 88, 88, 88,0));
        }
    }

    static class PredictableRandom extends Random {
        private final double[] doubles;
        private final int[] ints;
        private int doubleIndex;
        private int intIndex;

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

    static final class ConstantRandom extends Random {
        private final double constant;

        ConstantRandom(double constant) {
            this.constant = constant;
        }

        @Override
        public double nextDouble() {
            return constant;
        }
    }
}
