package race_weekend;

import components.Car;
import components.Component;
import staff.MainDriver;
import staff.TeamManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ParallelRaceEngine {
    private static final int[] F1_POINTS = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};

    private final Random random;

    public ParallelRaceEngine(Random random) {
        this.random = random;
    }

    public RaceSimulationResult simulateRace(Track track,
                                             Map<String, Double> quali,
                                             Map<MainDriver, Car> weekendLineup,
                                             TeamManager player,
                                             RaceStrategyPlan playerStrategy) {
        List<Map.Entry<String, Double>> grid = quali.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();
        if (grid.isEmpty()) {
            return new RaceSimulationResult(List.of(), Weather.DRY, new RaceStatistics());
        }

        RaceEventLogger logger = new RaceEventLogger();
        RaceCommentator commentator = new RaceCommentator(logger);
        AtomicBoolean raceActive = new AtomicBoolean(true);
        RaceClock clock = new RaceClock(10.0, 1L);
        Weather[] raceWeather = raceWeatherValues();
        AtomicReference<Weather> weatherRef = new AtomicReference<>(raceWeather[random.nextInt(raceWeather.length)]);
        AtomicInteger leaderLap = new AtomicInteger(0);
        RaceStatistics stats = new RaceStatistics();
        PitBoxManager pitBoxManager = new PitBoxManager(Math.max(1, track.getCorners() / 6), logger);
        List<RacerState> racers = buildGrid(grid, track, weekendLineup, player, playerStrategy, weatherRef.get());

        logger.log(0.0, "СИГНАЛ СТАРТА! " + racers.size() + " болидов уходят в первый поворот.");
        logger.log(0.0, "Выбранная стратегия игрока: " + playerStrategy.getTitle()
                + " (" + playerStrategy.getDescription() + ")");

        Thread clockThread = new Thread(() -> clock.run(raceActive), "race-clock");
        Thread weatherThread = new Thread(() -> runWeather(track, racers, weatherRef, leaderLap, raceActive, commentator, stats, clock),
                "weather-thread");
        Thread incidentsThread = new Thread(() -> runIncidents(racers, weatherRef, raceActive, commentator, stats, clock),
                "incident-thread");

        List<Thread> racerThreads = new ArrayList<>();
        for (RacerState racer : racers) {
            Thread thread = new Thread(() -> runRacer(track, racer, weatherRef, leaderLap, pitBoxManager, raceActive, commentator, stats, clock),
                    "racer-" + racer.entryName.replace(' ', '_'));
            racerThreads.add(thread);
        }

        clockThread.start();
        weatherThread.start();
        incidentsThread.start();
        racerThreads.forEach(Thread::start);

        Map<String, Integer> previousPositions = buildPositionMap(racers);
        int observedTick = clock.getCurrentTick();
        while (racers.stream().anyMatch(racer -> !racer.isTerminal())) {
            observedTick = clock.awaitNextTick(observedTick, raceActive);
            int currentLeaderLap = racers.stream()
                    .mapToInt(RacerState::getCompletedLaps)
                    .max()
                    .orElse(0);
            leaderLap.accumulateAndGet(currentLeaderLap, Math::max);
            previousPositions = logOvertakes(racers, previousPositions, commentator, clock);
        }

        raceActive.set(false);
        joinAll(racerThreads);
        joinAll(List.of(weatherThread, incidentsThread, clockThread));

        List<RaceClassificationEntry> classification = buildClassification(racers);
        if (!classification.isEmpty()) {
            RaceClassificationEntry winner = classification.get(0);
            if (winner.isFinished()) {
                logger.log(winner.getTotalTime(),
                        "ФИНИШ! " + winner.getDriverTeam() + " пересекает линию первым.");
            }
        }

        return new RaceSimulationResult(classification, weatherRef.get(), stats);
    }

    private List<RacerState> buildGrid(List<Map.Entry<String, Double>> grid,
                                       Track track,
                                       Map<MainDriver, Car> weekendLineup,
                                       TeamManager player,
                                       RaceStrategyPlan playerStrategy,
                                       Weather initialWeather) {
        List<RacerState> racers = new ArrayList<>();
        int botStrategyOffset = 0;
        for (int i = 0; i < grid.size(); i++) {
            String entryName = grid.get(i).getKey();
            Car playerCar = findPlayerCar(entryName, weekendLineup, player);
            MainDriver playerDriver = findPlayerDriver(entryName, weekendLineup, player);
            boolean playerControlled = playerCar != null && playerDriver != null;
            TeamManager team;
            if (playerControlled) {
                team = player;
            } else {
                team = new TeamManager(extractTeam(entryName), 0, 55 + random.nextInt(30));
            }
            RaceStrategyPlan strategyPlan;
            if (playerControlled) {
                strategyPlan = playerStrategy;
            } else {
                strategyPlan = RaceStrategyPlan.values()[(botStrategyOffset++ + random.nextInt(RaceStrategyPlan.values().length))
                        % RaceStrategyPlan.values().length];
            }
            Random racerRandom = new Random(random.nextLong());
            RaceSimulator simulator = new RaceSimulator(racerRandom);
            double baseLap;
            if (playerControlled) {
                baseLap = simulator.simulatePracticeLap(team, playerCar, playerDriver, track, initialWeather);
            } else {
                baseLap = simulator.generateBotPracticeLap(team, track, initialWeather);
            }
            racers.add(new RacerState(
                    entryName,
                    extractDriver(entryName),
                    extractTeam(entryName),
                    team,
                    playerCar,
                    playerDriver,
                    playerControlled,
                    i,
                    baseLap,
                    strategyPlan,
                    simulator,
                    racerRandom
            ));
        }
        return racers;
    }

    private void runRacer(Track track,
                          RacerState racer,
                          AtomicReference<Weather> weatherRef,
                          AtomicInteger leaderLap,
                          PitBoxManager pitBoxManager,
                          AtomicBoolean raceActive,
                          RaceCommentator commentator,
                          RaceStatistics stats,
                          RaceClock clock) {
        racer.beginLap(computeLapTime(track, racer, weatherRef.get()));
        int observedTick = clock.getCurrentTick();
        while (raceActive.get() && !racer.isTerminal() && racer.getCompletedLaps() < track.getLaps()) {
            observedTick = clock.awaitNextTick(observedTick, raceActive);
            if (!raceActive.get() || racer.isTerminal()) {
                break;
            }

            racer.advanceBy(clock.getTickSeconds());
            leaderLap.accumulateAndGet(racer.getCompletedLaps(), Math::max);

            if (racer.getCompletedLaps() >= track.getLaps()) {
                racer.finishRace();
                break;
            }

            if (!racer.isLapInProgress()) {
                Weather weather = weatherRef.get();
                if (racer.shouldAttemptPit(weather, track)
                        && pitBoxManager.tryAcquireBox(racer, clock.getCurrentTimeSeconds())) {
                    stats.incrementPitStops();
                    double pitLoss = 17.0 + racer.getRandom().nextDouble() * 6.0;
                    racer.applyPitStop(pitLoss);
                    if (racer.isPlayerControlled()) {
                        commentator.info(clock.getCurrentTimeSeconds(),
                                "ПИТ-СТОП: " + racer.getDriverName() + " меняет шины и возвращается в гонку.");
                    }
                    pitBoxManager.releaseBox(racer, clock.getCurrentTimeSeconds());
                }
                racer.beginLap(computeLapTime(track, racer, weather));
            }
        }
    }

    private double computeLapTime(Track track, RacerState racer, Weather weather) {
        double baseLap;
        if (racer.isPlayerControlled()) {
            baseLap = racer.getSimulator().simulatePracticeLap(racer.getTeam(), racer.getCar(), racer.getDriver(), track, weather);
        } else {
            baseLap = racer.getSimulator().generateBotPracticeLap(racer.getTeam(), track, weather);
        }

        double strategyModifier = racer.getStrategyPlan().getTactic().modifierFor(weather);
        double tyrePenalty = 1.0 + racer.getTyreWear() / 220.0;
        if (racer.hasTyreMismatch()) {
            tyrePenalty += 0.10;
        }
        double damagePenalty = 1.0 + racer.getDamagePenalty();
        double gridPenalty = 1.0;
        if (racer.getCompletedLaps() == 0) {
            gridPenalty = 1.0 + racer.getGridPosition() * 0.002;
        }
        double freshTyreBonus = racer.consumeFreshTyreBonus();
        double consistencyNoise = 0.992 + racer.getRandom().nextDouble() * 0.018;
        return baseLap * strategyModifier * tyrePenalty * damagePenalty * gridPenalty * freshTyreBonus * consistencyNoise;
    }

    private void runWeather(Track track,
                            List<RacerState> racers,
                            AtomicReference<Weather> weatherRef,
                            AtomicInteger leaderLap,
                            AtomicBoolean raceActive,
                            RaceCommentator commentator,
                            RaceStatistics stats,
                            RaceClock clock) {
        int lapInterval = Math.max(3, track.getLaps() / 4);
        int nextChange = lapInterval;
        int observedTick = clock.getCurrentTick();
        while (raceActive.get()) {
            observedTick = clock.awaitNextTick(observedTick, raceActive);
            if (leaderLap.get() >= nextChange) {
                Weather current = weatherRef.get();
                Weather next = rollNextWeather(current);
                weatherRef.set(next);
                stats.incrementWeatherChanges();
                if (requiresTyreWindowChange(current, next)) {
                    for (RacerState racer : racers) {
                        racer.requireWeatherTyreChange(next);
                    }
                }
                commentator.weatherUpdate(clock.getCurrentTimeSeconds(), next);
                nextChange += lapInterval;
            }
        }
    }

    private void runIncidents(List<RacerState> racers,
                              AtomicReference<Weather> weatherRef,
                              AtomicBoolean raceActive,
                              RaceCommentator commentator,
                              RaceStatistics stats,
                              RaceClock clock) {
        int incidentsLeft = Math.max(2, racers.size() / 8);
        int observedTick = clock.getCurrentTick();
        int nextIncidentTick = observedTick + 6;
        while (raceActive.get() && incidentsLeft > 0) {
            observedTick = clock.awaitNextTick(observedTick, raceActive);
            if (observedTick < nextIncidentTick) {
                continue;
            }
            nextIncidentTick = observedTick + 6;
            List<RacerState> active = racers.stream()
                    .filter(RacerState::isRunning)
                    .toList();
            if (active.isEmpty() || random.nextDouble() > 0.18) {
                continue;
            }
            RacerState target = active.get(random.nextInt(active.size()));
            Weather weather = weatherRef.get();
            double currentTime = clock.getCurrentTimeSeconds();
            if (random.nextDouble() < 0.22) {
                target.retire("сход (авария)");
                commentator.incident(currentTime,
                        "Серьезный инцидент у " + target.getEntryName() + ". Болид больше не может продолжать гонку.");
            } else {
                double penalty = 4.0 + random.nextDouble() * 9.0;
                double perLapSlowdown = 0.008 + random.nextDouble() * 0.014;
                target.applyIncidentDamage(penalty, perLapSlowdown);
                commentator.incident(currentTime,
                        target.getEntryName() + " получает повреждения и теряет темп.");
            }
            if (weather == Weather.RAIN && random.nextDouble() < 0.25) {
                commentator.comment(currentTime,
                        "Дождь делает трассу особенно коварной, механики готовят экстренные решения.");
            }
            stats.incrementIncidents();
            incidentsLeft--;
        }
    }

    private List<RaceClassificationEntry> buildClassification(List<RacerState> racers) {
        return racers.stream()
                .sorted((left, right) -> {
                    if (left.isFinished() != right.isFinished()) {
                        return Boolean.compare(right.isFinished(), left.isFinished());
                    }
                    if (!left.isFinished()) {
                        int byLaps = Integer.compare(right.getCompletedLaps(), left.getCompletedLaps());
                        if (byLaps != 0) {
                            return byLaps;
                        }
                    }
                    return Double.compare(left.getElapsedSeconds(), right.getElapsedSeconds());
                })
                .map(racer -> {
                    String status = racer.getStatus();
                    if (racer.isFinished()) {
                        status = "";
                    }
                    return new RaceClassificationEntry(
                            racer.getEntryName(),
                            racer.getElapsedSeconds(),
                            racer.getBestLap(),
                            racer.getGridPosition(),
                            racer.isFinished(),
                            status,
                            racer.getCompletedLaps()
                    );
                })
                .toList();
    }

    private int compareRacersLive(RacerState left, RacerState right) {
        if (left.isFinished() != right.isFinished()) {
            return Boolean.compare(right.isFinished(), left.isFinished());
        }
        int byLaps = Integer.compare(right.getCompletedLaps(), left.getCompletedLaps());
        if (byLaps != 0) {
            return byLaps;
        }
        int byProgress = Double.compare(right.getLapProgress(), left.getLapProgress());
        if (byProgress != 0) {
            return byProgress;
        }
        return Double.compare(left.getElapsedSeconds(), right.getElapsedSeconds());
    }

    private Map<String, Integer> buildPositionMap(List<RacerState> racers) {
        List<RacerState> ordered = racers.stream()
                .sorted(this::compareRacersLive)
                .toList();
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            positions.put(ordered.get(i).getEntryName(), i + 1);
        }
        return positions;
    }

    private Map<String, Integer> logOvertakes(List<RacerState> racers,
                                              Map<String, Integer> previousPositions,
                                              RaceCommentator commentator,
                                              RaceClock clock) {
        List<RacerState> ordered = racers.stream()
                .sorted(this::compareRacersLive)
                .toList();
        Map<String, Integer> currentPositions = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            RacerState racer = ordered.get(i);
            int currentPosition = i + 1;
            currentPositions.put(racer.getEntryName(), currentPosition);
            Integer previousPosition = previousPositions.get(racer.getEntryName());
            if (previousPosition != null
                    && racer.getCompletedLaps() > 0
                    && racer.isRunning()
                    && racer.isPlayerControlled()
                    && racer.shouldReportPositionChange(currentPosition)) {
                if (currentPosition < previousPosition) {
                    commentator.info(clock.getCurrentTimeSeconds(),
                            "ОБГОН: " + racer.getDriverName() + " поднимается с "
                                    + previousPosition + "-й на " + currentPosition + "-ю позицию.");
                } else if (currentPosition > previousPosition) {
                    commentator.info(clock.getCurrentTimeSeconds(),
                            "ПОТЕРЯ ПОЗИЦИИ: " + racer.getDriverName() + " откатывается с "
                                    + previousPosition + "-й на " + currentPosition + "-ю позицию.");
                }
            }
            racer.markPositionSnapshotProcessed(currentPosition);
        }
        return currentPositions;
    }

    private Weather rollNextWeather(Weather current) {
        Weather next = current;
        Weather[] raceWeather = raceWeatherValues();
        while (next == current) {
            next = raceWeather[random.nextInt(raceWeather.length)];
        }
        return next;
    }

    private Weather[] raceWeatherValues() {
        return new Weather[]{Weather.DRY, Weather.WET, Weather.RAIN};
    }

    private boolean requiresTyreWindowChange(Weather current, Weather next) {
        return isDryWeather(current) != isDryWeather(next);
    }

    private boolean isDryWeather(Weather weather) {
        return weather == Weather.DRY;
    }

    private Car findPlayerCar(String entryName, Map<MainDriver, Car> weekendLineup, TeamManager player) {
        for (Map.Entry<MainDriver, Car> entry : weekendLineup.entrySet()) {
            if ((entry.getKey().getName() + " (" + player.getTeamName() + ")").equals(entryName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private MainDriver findPlayerDriver(String entryName, Map<MainDriver, Car> weekendLineup, TeamManager player) {
        for (Map.Entry<MainDriver, Car> entry : weekendLineup.entrySet()) {
            if ((entry.getKey().getName() + " (" + player.getTeamName() + ")").equals(entryName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String extractDriver(String entryName) {
        int separator = entryName.indexOf(" (");
        if (separator > 0) {
            return entryName.substring(0, separator);
        }
        return entryName;
    }

    private String extractTeam(String entryName) {
        int start = entryName.indexOf('(');
        int end = entryName.lastIndexOf(')');
        if (start >= 0 && end > start) {
            return entryName.substring(start + 1, end);
        }
        return "Unknown Team";
    }

    private static void joinAll(List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class RaceClock {
        private final double tickSeconds;
        private final long tickMillis;
        private int currentTick;

        private RaceClock(double tickSeconds, long tickMillis) {
            this.tickSeconds = tickSeconds;
            this.tickMillis = tickMillis;
        }

        public void run(AtomicBoolean raceActive) {
            while (raceActive.get()) {
                sleepSilently(tickMillis);
                synchronized (this) {
                    currentTick++;
                    notifyAll();
                }
            }
            synchronized (this) {
                notifyAll();
            }
        }

        public synchronized int getCurrentTick() {
            return currentTick;
        }

        public synchronized int awaitNextTick(int observedTick, AtomicBoolean raceActive) {
            while (raceActive.get() && currentTick <= observedTick) {
                try {
                    wait(tickMillis * 2L + 1L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return currentTick;
                }
            }
            return currentTick;
        }

        public double getTickSeconds() {
            return tickSeconds;
        }

        public synchronized double getCurrentTimeSeconds() {
            return currentTick * tickSeconds;
        }
    }

    private static String formatLap(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0) {
            return "--:--.---";
        }
        int totalMillis = (int) Math.round(seconds * 1000);
        int mm = totalMillis / 60_000;
        int ss = (totalMillis % 60_000) / 1000;
        int ms = totalMillis % 1000;
        return String.format(Locale.ROOT, "%02d:%02d.%03d", mm, ss, ms);
    }

    public static final class RaceSimulationResult {
        private final List<RaceClassificationEntry> classification;
        private final Weather finalWeather;
        private final RaceStatistics statistics;

        public RaceSimulationResult(List<RaceClassificationEntry> classification, Weather finalWeather, RaceStatistics statistics) {
            this.classification = classification;
            this.finalWeather = finalWeather;
            this.statistics = statistics;
        }

        public List<RaceClassificationEntry> getClassification() {
            return classification;
        }

        public Weather getFinalWeather() {
            return finalWeather;
        }

        public RaceStatistics getStatistics() {
            return statistics;
        }
    }

    public static final class RaceStatistics {
        private final AtomicInteger pitStops = new AtomicInteger();
        private final AtomicInteger incidents = new AtomicInteger();
        private final AtomicInteger weatherChanges = new AtomicInteger();

        public void incrementPitStops() {
            pitStops.incrementAndGet();
        }

        public void incrementIncidents() {
            incidents.incrementAndGet();
        }

        public void incrementWeatherChanges() {
            weatherChanges.incrementAndGet();
        }

        public int getPitStops() {
            return pitStops.get();
        }

        public int getIncidents() {
            return incidents.get();
        }

        public int getWeatherChanges() {
            return weatherChanges.get();
        }
    }

    public static final class RaceClassificationEntry {
        private final String driverTeam;
        private final double totalTime;
        private final double fastestLap;
        private final int gridPosition;
        private final boolean finished;
        private final String status;
        private final int completedLaps;

        public RaceClassificationEntry(String driverTeam,
                                       double totalTime,
                                       double fastestLap,
                                       int gridPosition,
                                       boolean finished,
                                       String status,
                                       int completedLaps) {
            this.driverTeam = driverTeam;
            this.totalTime = totalTime;
            this.fastestLap = fastestLap;
            this.gridPosition = gridPosition;
            this.finished = finished;
            this.status = status;
            this.completedLaps = completedLaps;
        }

        public String getDriverTeam() {
            return driverTeam;
        }

        public double getTotalTime() {
            return totalTime;
        }

        public double getFastestLap() {
            return fastestLap;
        }

        public int getGridPosition() {
            return gridPosition;
        }

        public boolean isFinished() {
            return finished;
        }

        public String getStatus() {
            return status;
        }

        public int getCompletedLaps() {
            return completedLaps;
        }

        public int getPoints(String fastestLapOwner, int position) {
            int points = 0;
            if (finished && position < F1_POINTS.length) {
                points = F1_POINTS[position];
            }
            if (finished && position < 10 && driverTeam.equals(fastestLapOwner)) {
                points += 1;
            }
            return points;
        }
    }

    private static final class RaceEventLogger {
        public synchronized void log(double seconds, String message) {
            System.out.printf("[%s] %s%n", formatClock(seconds), message);
        }

        private String formatClock(double seconds) {
            int totalSeconds = Math.max(0, (int) Math.round(seconds));
            int mm = totalSeconds / 60;
            int ss = totalSeconds % 60;
            return String.format(Locale.ROOT, "%02d:%02d", mm, ss);
        }
    }

    private static final class RaceCommentator {
        private final RaceEventLogger logger;

        private RaceCommentator(RaceEventLogger logger) {
            this.logger = logger;
        }

        public void comment(double seconds, String message) {
            logger.log(seconds, "КОММЕНТАТОР: " + message);
        }

        public void incident(double seconds, String message) {
            logger.log(seconds, message);
        }

        public void weatherUpdate(double seconds, Weather weather) {
            logger.log(seconds, "ПОГОДА: " + weather.getRaceMessage());
        }

        public void info(double seconds, String message) {
            logger.log(seconds, message);
        }
    }

    private static final class PitBoxManager {
        private final Semaphore semaphore;
        private final RaceEventLogger logger;

        private PitBoxManager(int capacity, RaceEventLogger logger) {
            this.semaphore = new Semaphore(capacity, true);
            this.logger = logger;
        }

        public boolean tryAcquireBox(RacerState racer, double seconds) {
            boolean acquired = semaphore.tryAcquire();
            if (acquired && racer.isPlayerControlled()) {
                logger.log(seconds, "ПИТ-СТОП: " + racer.getEntryName() + " заезжает в бокс.");
            }
            return acquired;
        }

        public void releaseBox(RacerState racer, double seconds) {
            semaphore.release();
            if (racer.isPlayerControlled()) {
                logger.log(seconds, "ПИТ-СТОП: " + racer.getEntryName() + " возвращается на трассу.");
            }
        }
    }

    private static final class RacerState {
        private final String entryName;
        private final String driverName;
        private final String teamName;
        private final TeamManager team;
        private final Car car;
        private final MainDriver driver;
        private final boolean playerControlled;
        private final int gridPosition;
        private final double seedLap;
        private final RaceStrategyPlan strategyPlan;
        private final RaceSimulator simulator;
        private final Random random;

        private volatile int completedLaps;
        private volatile double elapsedSeconds;
        private volatile double bestLap = Double.MAX_VALUE;
        private volatile boolean finished;
        private volatile boolean retired;
        private volatile String status = "";
        private volatile double tyreWear;
        private volatile double damagePenalty;
        private volatile int pitStops;
        private volatile int freshTyreLaps;
        private volatile boolean wetTyres;
        private volatile boolean expectedWetTyres;
        private volatile boolean mandatoryTyreChange;
        private volatile int lastProcessedPositionLap = -1;
        private volatile int lastReportedPosition = -1;
        private volatile double currentLapTime;
        private volatile double remainingLapSeconds;

        private RacerState(String entryName,
                           String driverName,
                           String teamName,
                           TeamManager team,
                           Car car,
                           MainDriver driver,
                           boolean playerControlled,
                           int gridPosition,
                           double seedLap,
                           RaceStrategyPlan strategyPlan,
                           RaceSimulator simulator,
                           Random random) {
            this.entryName = entryName;
            this.driverName = driverName;
            this.teamName = teamName;
            this.team = team;
            this.car = car;
            this.driver = driver;
            this.playerControlled = playerControlled;
            this.gridPosition = gridPosition;
            this.seedLap = seedLap;
            this.strategyPlan = strategyPlan;
            this.simulator = simulator;
            this.random = random;
            this.wetTyres = !isDryWeatherStatic(Weather.DRY);
            this.expectedWetTyres = this.wetTyres;
        }

        public synchronized void beginLap(double lapTime) {
            if (retired || finished) {
                return;
            }
            currentLapTime = Math.max(58.0, lapTime);
            remainingLapSeconds = currentLapTime;
        }

        public synchronized boolean isLapInProgress() {
            return remainingLapSeconds > 0.0;
        }

        public synchronized void advanceBy(double tickSeconds) {
            if (retired || finished || remainingLapSeconds <= 0.0) {
                return;
            }
            double timeSlice = Math.min(tickSeconds, remainingLapSeconds);
            elapsedSeconds += timeSlice;
            remainingLapSeconds -= timeSlice;
            if (remainingLapSeconds <= 0.0) {
                completedLaps++;
                bestLap = Math.min(bestLap, currentLapTime);
                tyreWear += 6.0 + random.nextDouble() * 7.0;
                if (freshTyreLaps > 0) {
                    freshTyreLaps--;
                }
                currentLapTime = 0.0;
                remainingLapSeconds = 0.0;
            }
        }

        public synchronized boolean shouldAttemptPit(Weather weather, Track track) {
            if (retired || finished) {
                return false;
            }
            if (pitStops >= 2) {
                return mandatoryTyreChange;
            }
            boolean weatherRisk = weather == Weather.RAIN || weather == Weather.WET;
            boolean finalLap = completedLaps >= track.getLaps() - 2;
            return !finalLap && (mandatoryTyreChange
                    || tyreWear >= strategyPlan.getTactic().getPitThreshold()
                    || (weatherRisk && tyreWear > 35.0));
        }

        public synchronized void applyPitStop(double pitLossSeconds) {
            elapsedSeconds += pitLossSeconds;
            tyreWear = Math.max(8.0, tyreWear * 0.22);
            pitStops++;
            freshTyreLaps = 4;
            wetTyres = expectedWetTyres;
            mandatoryTyreChange = false;
        }

        public synchronized double consumeFreshTyreBonus() {
            if (freshTyreLaps > 0) {
                return 0.972;
            }
            return 1.0;
        }

        public synchronized void applyIncidentDamage(double timeLoss, double perLapSlowdown) {
            if (retired || finished) {
                return;
            }
            elapsedSeconds += timeLoss;
            damagePenalty += perLapSlowdown;
            if (car != null) {
                List<Component> components = car.components().stream()
                        .filter(component -> !component.isDestroyed())
                        .toList();
                if (!components.isEmpty()) {
                    Component component = components.get(random.nextInt(components.size()));
                    component.setWear(Math.min(100, component.getWear() + 12 + random.nextDouble() * 12));
                }
            }
            status = "повреждения";
        }

        public synchronized void retire(String retireStatus) {
            if (retired || finished) {
                return;
            }
            retired = true;
            status = retireStatus;
            if (car != null) {
                List<Component> components = car.components().stream()
                        .filter(component -> !component.isDestroyed())
                        .toList();
                if (!components.isEmpty()) {
                    components.get(random.nextInt(components.size())).setDestroyed(true);
                }
            }
        }

        public synchronized void finishRace() {
            if (!retired) {
                finished = true;
                status = "";
            }
        }

        public synchronized void requireWeatherTyreChange(Weather weather) {
            mandatoryTyreChange = true;
            expectedWetTyres = !isDryWeatherStatic(weather);
        }

        public boolean isRunning() {
            return !retired && !finished;
        }

        public boolean isTerminal() {
            return retired || finished;
        }

        public String getEntryName() {
            return entryName;
        }

        public String getDriverName() {
            return driverName;
        }

        public TeamManager getTeam() {
            return team;
        }

        public Car getCar() {
            return car;
        }

        public MainDriver getDriver() {
            return driver;
        }

        public boolean isPlayerControlled() {
            return playerControlled;
        }

        public int getGridPosition() {
            return gridPosition;
        }

        public RaceStrategyPlan getStrategyPlan() {
            return strategyPlan;
        }

        public RaceSimulator getSimulator() {
            return simulator;
        }

        public Random getRandom() {
            return random;
        }

        public int getCompletedLaps() {
            return completedLaps;
        }

        public double getElapsedSeconds() {
            return elapsedSeconds;
        }

        public synchronized double getLapProgress() {
            if (finished) {
                return 1.0;
            }
            if (currentLapTime <= 0.0) {
                return 0.0;
            }
            double progress = (currentLapTime - remainingLapSeconds) / currentLapTime;
            return Math.max(0.0, Math.min(1.0, progress));
        }

        public double getBestLap() {
            if (bestLap == Double.MAX_VALUE) {
                return seedLap;
            }
            return bestLap;
        }

        public boolean isFinished() {
            return finished;
        }

        public String getStatus() {
            return status;
        }

        public double getTyreWear() {
            return tyreWear;
        }

        public double getDamagePenalty() {
            return damagePenalty;
        }

        public boolean hasTyreMismatch() {
            return mandatoryTyreChange || wetTyres != expectedWetTyres;
        }

        public boolean shouldReportPositionChange(int currentPosition) {
            return currentPosition != lastReportedPosition
                    && (completedLaps > lastProcessedPositionLap || finished || retired);
        }

        public void markPositionSnapshotProcessed(int currentPosition) {
            lastProcessedPositionLap = completedLaps;
            lastReportedPosition = currentPosition;
        }

        private static boolean isDryWeatherStatic(Weather weather) {
            return weather == Weather.DRY;
        }
    }
}
