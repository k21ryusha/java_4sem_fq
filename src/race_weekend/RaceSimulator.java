package race_weekend;

import components.Car;
import staff.MainDriver;
import staff.TeamManager;

import java.util.Random;

public class RaceSimulator {
    private final Random random;

    public RaceSimulator(Random random) {
        this.random = random;
    }

    public double simulateRaceTime(TeamManager team, Car car, MainDriver driver, Track track, Weather weather) {
        double carScore = car.getEngine().getPower() * 0.34
                + car.getTransmission().getEfficiency() * 18
                + car.getChassis().getHandling() * 1.6
                + car.getSuspension().getCornerBonus() * 1.8
                + car.getAerodynamics().getDownforce() * 1.2
                + car.getTyres().getGrip() * 2.5
                + car.getEngine().effectiveQuality() * 0.4
                + car.getChassis().effectiveQuality() * 0.4;

        double engineerBonus = team.getEngineers().stream().mapToInt(e -> e.getSkill()).average().orElse(50) / 100.0;

        double driverFactor = (driver.getCornerSkill() * track.getCorners() + driver.getStraightSkill() * track.getStraights())
                / (double) (track.getCorners() + track.getStraights());
        driverFactor = driverFactor * 0.65 + driver.getConsistency() * 0.2 + (weather == Weather.RAIN ? driver.getWetSkill() * 0.15 : 10);

        double degradationPenalty = car.averageWear() * 0.45;
        double lapBase = (track.getLapKm() * 2200) / Math.max(120, carScore * 0.7 + driverFactor * 2 + engineerBonus * 100);
        double weatherPenalty = 1 / weather.getMultiplier();
        double elevationPenalty = 1 + track.getElevation() / 300.0;
        double pitStopSeconds = car.getTyres().getDurability() < 70 ? 20 : 12;

        double total = lapBase * track.getLaps() * weatherPenalty * elevationPenalty + pitStopSeconds + degradationPenalty;
        total += random.nextDouble() * (12 - driver.getConsistency() * 0.08);
        return total;
    }

    public double generateBotRaceTime(TeamManager bot, Track track, Weather weather) {
        double strength = 150 + bot.getReputation() * 6 + random.nextDouble() * 60;
        double lap = (track.getLapKm() * 2200) / strength;
        return lap * track.getLaps() * (1 / weather.getMultiplier()) * (1 + track.getElevation() / 320.0) + random.nextDouble() * 20;
    }
}
