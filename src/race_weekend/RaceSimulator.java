package race_weekend;

import components.Car;
import staff.MainDriver;
import staff.Staff;
import staff.TeamManager;

import java.util.Random;

public class RaceSimulator {
    private final Random random;

    public RaceSimulator(Random random) {
        this.random = random;
    }

    public double simulateRaceTime(TeamManager team, Car car, MainDriver driver, Track track, Weather weather) {
        double carIndex = car.getEngine().getPower() * 0.08
                + car.getTransmission().getEfficiency() * 0.35
                + car.getChassis().getHandling() * 0.25
                + car.getSuspension().getCornerBonus() * 0.20
                + car.getAerodynamics().getDownforce() * 0.18
                + car.getTyres().getGrip() * 0.22
                + car.getTyres().getRainPerformance() * (weather == Weather.DRY ? 0.04 : 0.14)
                + car.getEngine().effectiveQuality() * 0.25
                + car.getChassis().effectiveQuality() * 0.25;

        double engineersSkill = team.getEngineers().stream().mapToInt(Staff::getSkill).average().orElse(56);
        double engineersAction = 0.96 + Math.min(1.0, team.getEngineers().size() / 4.0) * 0.06;
        double principalImpact = team.getPrincipal() != null ? team.getPrincipal().getSkill() / 100.0 : 0.55;
        double technicalDirectorImpact = team.getTechnicalDirector() != null ? team.getTechnicalDirector().getSkill() / 100.0 : 0.55;

        double trackDriverSkill = (driver.getCornerSkill() * track.getCorners() + driver.getStraightSkill() * track.getStraights())
                / (double) Math.max(1, track.getCorners() + track.getStraights());
        double weatherDriverSkill = weather == Weather.DRY ? driver.getConsistency() : driver.getWetSkill();

        double cornerBias = track.getCorners() / (double) Math.max(1, track.getCorners() + track.getStraights());
        double setupMatch = (car.getSuspension().getCornerBonus() * cornerBias
                + car.getEngine().getPower() / 12.0 * (1 - cornerBias)) / 100.0;
        double elevationPenalty = 1.0 + track.getElevation() / 1200.0;

        double normalizedCar = carIndex / 220.0;
        double normalizedDriver = (trackDriverSkill * 0.75 + weatherDriverSkill * 0.25) / 100.0;
        double normalizedEngineers = engineersSkill / 100.0;
        double normalizedStaff = (principalImpact * 0.45 + technicalDirectorImpact * 0.55) * engineersAction;

        double totalPerformance = normalizedCar * 0.45
                + normalizedDriver * 0.25
                + normalizedEngineers * 0.15
                + normalizedStaff * 0.10
                + setupMatch * 0.05;

        double baseLapSeconds = 57.0 + track.getLapKm() * 10;
        double weatherPenalty = 1.0 + (1.0 / weather.getMultiplier() - 1.0) * 0.55;
        double degradationPenalty = car.averageWear() * 0.025;
        double consistencyNoise = (random.nextDouble() - 0.5) * (2.8 - driver.getConsistency() * 0.02);

        double raceLap = baseLapSeconds / Math.max(0.78, totalPerformance) * weatherPenalty * elevationPenalty
                + degradationPenalty + consistencyNoise;
        raceLap = Math.max(62.0, Math.min(118.5, raceLap));

        double pitStopSeconds = car.getTyres().getDurability() < 70 ? 22.0 : 16.0;
        return raceLap * track.getLaps() + pitStopSeconds;
    }

    public double generateBotRaceTime(TeamManager bot, Track track, Weather weather) {
        double virtualCar = 0.80 + bot.getReputation() / 220.0 + random.nextDouble() * 0.08;

        double engineersSkill = bot.getEngineers().stream().mapToInt(Staff::getSkill).average().orElse(58);
        double engineersAction = 0.95 + Math.min(1.0, bot.getEngineers().size() / 4.0) * 0.06;
        double principalImpact = bot.getPrincipal() != null ? bot.getPrincipal().getSkill() / 100.0 : 0.58;
        double technicalDirectorImpact = bot.getTechnicalDirector() != null ? bot.getTechnicalDirector().getSkill() / 100.0 : 0.58;

        double virtualDriver = 0.82 + bot.getReputation() / 250.0 + random.nextDouble() * 0.06;

        double trackBias = 0.96 + track.getStraights() / (double) Math.max(1, track.getCorners() + track.getStraights()) * 0.05;
        double elevationPenalty = 1.0 + track.getElevation() / 1250.0;

        double totalPerformance = virtualCar * 0.45
                + (engineersSkill / 100.0) * 0.15
                + (principalImpact * 0.45 + technicalDirectorImpact * 0.55) * engineersAction * 0.10
                + virtualDriver * 0.25
                + trackBias * 0.05;

        double baseLapSeconds = 57.0 + track.getLapKm() * 8.5;
        double weatherPenalty = 1.0 + (1.0 / weather.getMultiplier() - 1.0) * 0.55;
        double lapNoise = (random.nextDouble() - 0.5) * 1.6;

        double raceLap = baseLapSeconds / Math.max(0.80, totalPerformance) * weatherPenalty * elevationPenalty + lapNoise;
        raceLap = Math.max(62.0, Math.min(118.5, raceLap));

        double pitStopSeconds = 16.0 + random.nextDouble() * 6.0;
        return raceLap * track.getLaps() + pitStopSeconds;
    }
}
