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
        double raceLap = calculatePlayerLapTime(team, car, driver, track, weather);
        double pitStopSeconds = RaceSimulatorConstants.DEFAULT_PIT_STOP_SECONDS;
        if (car.getTyres().getDurability() < RaceSimulatorConstants.TYRE_DURABILITY_PIT_THRESHOLD) {
            pitStopSeconds = RaceSimulatorConstants.WORN_TYRE_PIT_STOP_SECONDS;
        }
        return raceLap * track.getLaps() + pitStopSeconds;
    }

    public double simulatePracticeLap(TeamManager team, Car car, MainDriver driver, Track track, Weather weather) {
        return calculatePlayerLapTime(team, car, driver, track, weather);
    }

    public double generateBotRaceTime(TeamManager bot, Track track, Weather weather) {
        double raceLap = calculateBotLapTime(bot, track, weather);
        double pitStopSeconds = RaceSimulatorConstants.DEFAULT_PIT_STOP_SECONDS
                + random.nextDouble() * RaceSimulatorConstants.BOT_PIT_STOP_RANDOM_RANGE;
        return raceLap * track.getLaps() + pitStopSeconds;
    }

    public double generateBotPracticeLap(TeamManager bot, Track track, Weather weather) {
        return calculateBotLapTime(bot, track, weather);
    }

    private double calculatePlayerLapTime(TeamManager team, Car car, MainDriver driver, Track track, Weather weather) {
        double rainPerformanceWeight = RaceSimulatorConstants.WET_RAIN_PERFORMANCE_WEIGHT;
        if (weather == Weather.DRY) {
            rainPerformanceWeight = RaceSimulatorConstants.DRY_RAIN_PERFORMANCE_WEIGHT;
        }
        double carIndex = car.getEngine().getPower() * RaceSimulatorConstants.ENGINE_POWER_WEIGHT
                + car.getTransmission().getEfficiency() * RaceSimulatorConstants.TRANSMISSION_EFFICIENCY_WEIGHT
                + car.getChassis().getHandling() * RaceSimulatorConstants.CHASSIS_HANDLING_WEIGHT
                + car.getSuspension().getCornerBonus() * RaceSimulatorConstants.SUSPENSION_CORNER_WEIGHT
                + car.getAerodynamics().getDownforce() * RaceSimulatorConstants.AERODYNAMICS_DOWNFORCE_WEIGHT
                + car.getTyres().getGrip() * RaceSimulatorConstants.TYRE_GRIP_WEIGHT
                + car.getTyres().getRainPerformance() * rainPerformanceWeight
                + car.getEngine().effectiveQuality() * RaceSimulatorConstants.ENGINE_QUALITY_WEIGHT
                + car.getChassis().effectiveQuality() * RaceSimulatorConstants.CHASSIS_QUALITY_WEIGHT;

        double engineersSkill = team.getEngineers().stream()
                .mapToInt(Staff::getSkill)
                .average()
                .orElse(RaceSimulatorConstants.DEFAULT_PLAYER_ENGINEERS_SKILL);
        double engineersAction = RaceSimulatorConstants.ENGINEERS_ACTION_BASE
                + Math.min(RaceSimulatorConstants.WEATHER_PENALTY_BASE,
                team.getEngineers().size() / RaceSimulatorConstants.ENGINEERS_ACTION_MAX_TEAM_SIZE)
                * RaceSimulatorConstants.ENGINEERS_ACTION_BONUS;
        double principalImpact = RaceSimulatorConstants.DEFAULT_PLAYER_PRINCIPAL_IMPACT;
        if (team.getPrincipal() != null) {
            principalImpact = team.getPrincipal().getSkill() / RaceSimulatorConstants.STAFF_SKILL_NORMALIZER;
        }
        double technicalDirectorImpact = RaceSimulatorConstants.DEFAULT_PLAYER_TECHNICAL_DIRECTOR_IMPACT;
        if (team.getTechnicalDirector() != null) {
            technicalDirectorImpact = team.getTechnicalDirector().getSkill() / RaceSimulatorConstants.STAFF_SKILL_NORMALIZER;
        }

        double trackDriverSkill = (driver.getCornerSkill() * track.getCorners() + driver.getStraightSkill() * track.getStraights())
                / (double) Math.max(RaceSimulatorConstants.MIN_TRACK_SECTIONS, track.getCorners() + track.getStraights());
        double weatherDriverSkill = driver.getWetSkill();
        if (weather == Weather.DRY) {
            weatherDriverSkill = driver.getConsistency();
        }

        double cornerBias = track.getCorners()
                / (double) Math.max(RaceSimulatorConstants.MIN_TRACK_SECTIONS, track.getCorners() + track.getStraights());
        double setupMatch = (car.getSuspension().getCornerBonus() * cornerBias
                + car.getEngine().getPower() / RaceSimulatorConstants.ENGINE_POWER_SETUP_DIVISOR
                * (RaceSimulatorConstants.WEATHER_PENALTY_BASE - cornerBias))
                / RaceSimulatorConstants.SETUP_MATCH_NORMALIZER;
        double elevationPenalty = RaceSimulatorConstants.WEATHER_PENALTY_BASE
                + track.getElevation() / RaceSimulatorConstants.PLAYER_ELEVATION_DIVISOR;

        double normalizedCar = carIndex / RaceSimulatorConstants.PLAYER_CAR_NORMALIZER;
        double normalizedDriver = (trackDriverSkill * RaceSimulatorConstants.DRY_WEATHER_DRIVER_WEIGHT
                + weatherDriverSkill * RaceSimulatorConstants.DRY_WEATHER_SECONDARY_DRIVER_WEIGHT)
                / RaceSimulatorConstants.STAFF_SKILL_NORMALIZER;
        double normalizedEngineers = engineersSkill / RaceSimulatorConstants.STAFF_SKILL_NORMALIZER;
        double normalizedStaff = (principalImpact * RaceSimulatorConstants.STAFF_PRINCIPAL_WEIGHT
                + technicalDirectorImpact * RaceSimulatorConstants.STAFF_TECHNICAL_DIRECTOR_WEIGHT) * engineersAction;

        double totalPerformance = normalizedCar * RaceSimulatorConstants.TOTAL_PERFORMANCE_CAR_WEIGHT
                + normalizedDriver * RaceSimulatorConstants.TOTAL_PERFORMANCE_DRIVER_WEIGHT
                + normalizedEngineers * RaceSimulatorConstants.TOTAL_PERFORMANCE_ENGINEERS_WEIGHT
                + normalizedStaff * RaceSimulatorConstants.TOTAL_PERFORMANCE_STAFF_WEIGHT
                + setupMatch * RaceSimulatorConstants.TOTAL_PERFORMANCE_SETUP_WEIGHT;

        double baseLapSeconds = RaceSimulatorConstants.PLAYER_BASE_LAP_SECONDS
                + track.getLapKm() * RaceSimulatorConstants.PLAYER_LAP_KM_FACTOR;
        double performanceWindow = Math.max(RaceSimulatorConstants.PLAYER_PERFORMANCE_WINDOW_MIN,
                RaceSimulatorConstants.WEATHER_PENALTY_BASE
                        - (totalPerformance - RaceSimulatorConstants.PLAYER_PERFORMANCE_BASELINE)
                        * RaceSimulatorConstants.PLAYER_PERFORMANCE_WINDOW_FACTOR);
        double weatherPenalty = RaceSimulatorConstants.WEATHER_PENALTY_BASE
                + (RaceSimulatorConstants.WEATHER_PENALTY_BASE - weather.getMultiplier())
                * RaceSimulatorConstants.WEATHER_PENALTY_FACTOR;
        double degradationPenalty = car.averageWear() * RaceSimulatorConstants.DEGRADATION_PENALTY_FACTOR;
        double consistencyNoise = (random.nextDouble() - RaceSimulatorConstants.RANDOM_CENTER)
                * (RaceSimulatorConstants.CONSISTENCY_NOISE_BASE
                - driver.getConsistency() * RaceSimulatorConstants.CONSISTENCY_NOISE_SKILL_FACTOR);

        double raceLap = baseLapSeconds * performanceWindow * weatherPenalty * elevationPenalty
                + degradationPenalty + consistencyNoise;
        return Math.max(RaceSimulatorConstants.MIN_PLAYER_LAP_TIME,
                Math.min(RaceSimulatorConstants.MAX_PLAYER_LAP_TIME, raceLap));
    }

    private double calculateBotLapTime(TeamManager bot, Track track, Weather weather) {
        double virtualCar = RaceSimulatorConstants.BOT_VIRTUAL_CAR_BASE
                + bot.getReputation() / RaceSimulatorConstants.BOT_VIRTUAL_CAR_REPUTATION_DIVISOR
                + random.nextDouble() * RaceSimulatorConstants.BOT_VIRTUAL_CAR_RANDOM_RANGE;

        double engineersSkill = bot.getEngineers().stream()
                .mapToInt(Staff::getSkill)
                .average()
                .orElse(RaceSimulatorConstants.DEFAULT_BOT_ENGINEERS_SKILL);
        double engineersAction = RaceSimulatorConstants.BOT_ENGINEERS_ACTION_BASE
                + Math.min(RaceSimulatorConstants.WEATHER_PENALTY_BASE,
                bot.getEngineers().size() / RaceSimulatorConstants.ENGINEERS_ACTION_MAX_TEAM_SIZE)
                * RaceSimulatorConstants.ENGINEERS_ACTION_BONUS;
        double principalImpact = RaceSimulatorConstants.DEFAULT_BOT_PRINCIPAL_IMPACT;
        if (bot.getPrincipal() != null) {
            principalImpact = bot.getPrincipal().getSkill() / RaceSimulatorConstants.STAFF_SKILL_NORMALIZER;
        }
        double technicalDirectorImpact = RaceSimulatorConstants.DEFAULT_BOT_TECHNICAL_DIRECTOR_IMPACT;
        if (bot.getTechnicalDirector() != null) {
            technicalDirectorImpact = bot.getTechnicalDirector().getSkill() / RaceSimulatorConstants.STAFF_SKILL_NORMALIZER;
        }

        double virtualDriver = RaceSimulatorConstants.BOT_VIRTUAL_DRIVER_BASE
                + bot.getReputation() / RaceSimulatorConstants.BOT_VIRTUAL_DRIVER_REPUTATION_DIVISOR
                + random.nextDouble() * RaceSimulatorConstants.BOT_VIRTUAL_DRIVER_RANDOM_RANGE;

        double trackBias = RaceSimulatorConstants.BOT_TRACK_BIAS_BASE
                + track.getStraights()
                / (double) Math.max(RaceSimulatorConstants.MIN_TRACK_SECTIONS, track.getCorners() + track.getStraights())
                * RaceSimulatorConstants.BOT_TRACK_BIAS_FACTOR;
        double elevationPenalty = RaceSimulatorConstants.WEATHER_PENALTY_BASE
                + track.getElevation() / RaceSimulatorConstants.BOT_ELEVATION_DIVISOR;

        double totalPerformance = virtualCar * RaceSimulatorConstants.TOTAL_PERFORMANCE_CAR_WEIGHT
                + (engineersSkill / RaceSimulatorConstants.STAFF_SKILL_NORMALIZER)
                * RaceSimulatorConstants.TOTAL_PERFORMANCE_ENGINEERS_WEIGHT
                + (principalImpact * RaceSimulatorConstants.STAFF_PRINCIPAL_WEIGHT
                + technicalDirectorImpact * RaceSimulatorConstants.STAFF_TECHNICAL_DIRECTOR_WEIGHT)
                * engineersAction * RaceSimulatorConstants.TOTAL_PERFORMANCE_STAFF_WEIGHT
                + virtualDriver * RaceSimulatorConstants.TOTAL_PERFORMANCE_DRIVER_WEIGHT
                + trackBias * RaceSimulatorConstants.TOTAL_PERFORMANCE_SETUP_WEIGHT;

        double baseLapSeconds = RaceSimulatorConstants.BOT_BASE_LAP_SECONDS
                + track.getLapKm() * RaceSimulatorConstants.BOT_LAP_KM_FACTOR;
        double performanceWindow = Math.max(RaceSimulatorConstants.BOT_PERFORMANCE_WINDOW_MIN,
                RaceSimulatorConstants.WEATHER_PENALTY_BASE
                        - (totalPerformance - RaceSimulatorConstants.BOT_PERFORMANCE_BASELINE)
                        * RaceSimulatorConstants.BOT_PERFORMANCE_WINDOW_FACTOR);
        double weatherPenalty = RaceSimulatorConstants.WEATHER_PENALTY_BASE
                + (RaceSimulatorConstants.WEATHER_PENALTY_BASE - weather.getMultiplier())
                * RaceSimulatorConstants.WEATHER_PENALTY_FACTOR;
        double lapNoise = (random.nextDouble() - RaceSimulatorConstants.RANDOM_CENTER)
                * RaceSimulatorConstants.BOT_LAP_NOISE_RANGE;

        double raceLap = baseLapSeconds * performanceWindow * weatherPenalty * elevationPenalty + lapNoise;
        return Math.max(RaceSimulatorConstants.MIN_PLAYER_LAP_TIME,
                Math.min(RaceSimulatorConstants.MAX_BOT_LAP_TIME, raceLap));
    }
}
