package game;

import components.Car;
import economic.MarketService;
import incidents.IncidentService;
import race_weekend.ParallelRaceEngine;
import race_weekend.RaceResult;
import race_weekend.RaceSimulator;
import race_weekend.RaceStrategyPlan;
import race_weekend.Track;
import race_weekend.TrackCatalogManager;
import race_weekend.Weather;
import save.GameState;
import save.SaveManager;
import save.SaveSlot;
import staff.MainDriver;
import staff.TeamManager;

import java.util.*;
import java.util.stream.Collectors;

public class GameSession {
    private static final int DISCONTENT_STANDINGS_POSITION_THRESHOLD = 10;
    private static final int DISCONTENT_GAIN_FOR_LOW_STANDINGS = 5;
    private static final int INITIAL_PLAYER_BUDGET = 9000_000;
    private static final int INITIAL_PLAYER_REPUTATION = 10;

    private final Random random = new Random();
    private final Scanner scanner;
    private final String playerName;
    private final TeamManager player;
    private final List<Track> trackLibrary = new ArrayList<>();
    private final List<Track> tracks = new ArrayList<>();
    private final Set<String> sharedCustomTrackNames = new HashSet<>();
    private final List<RaceResult> raceHistory = new ArrayList<>();

    private final Map<String, Integer> driverPoints = new HashMap<>();
    private final Map<String, Integer> teamPoints = new HashMap<>();
    private final Map<String, List<String>> teamDrivers = new HashMap<>();

    private final SaveManager saveManager;
    private final TrackCatalogManager trackCatalogManager;
    private final PlayerController playerController;
    private final BotController botController;
    private final IncidentService incidentService;
    private final RaceSimulator raceSimulator;
    private final MarketService marketService;

    private int championshipRound = 0;
    private boolean parcFermeLocked = false;
    private boolean seasonSetupCompleted = false;
    private SurvivalProgress survivalProgress;

    public GameSession(Scanner scanner) {
        this(scanner, "Player", new SaveManager(), new TrackCatalogManager());
    }

    public GameSession(Scanner scanner, String playerName, SaveManager saveManager) {
        this(scanner, playerName, saveManager, new TrackCatalogManager());
    }

    public GameSession(Scanner scanner, String playerName, SaveManager saveManager, TrackCatalogManager trackCatalogManager) {
        this.scanner = scanner;
        this.playerName = playerName;
        this.player = new TeamManager(playerName + " Racing", INITIAL_PLAYER_BUDGET, INITIAL_PLAYER_REPUTATION);
        this.saveManager = saveManager;
        this.trackCatalogManager = trackCatalogManager;
        this.marketService = new MarketService(random);
        this.playerController = new PlayerController(scanner, player, marketService);
        this.botController = new BotController(random);
        this.incidentService = new IncidentService(random);
        this.raceSimulator = new RaceSimulator(random);
        initTracks();
        loadSharedCustomTracks();
        resetSeasonCalendarToCurrentLibrary();
        initChampionshipEntries();
    }

    public GameSession(Scanner scanner, GameState state, SaveManager saveManager) {
        this(scanner, state, saveManager, new TrackCatalogManager());
    }

    public GameSession(Scanner scanner, GameState state, SaveManager saveManager, TrackCatalogManager trackCatalogManager) {
        this.scanner = scanner;
        this.playerName = state.getPlayerName();
        this.player = state.getPlayer();
        this.saveManager = saveManager;
        this.trackCatalogManager = trackCatalogManager;
        this.marketService = new MarketService(random);
        this.playerController = new PlayerController(scanner, player, marketService);
        this.botController = new BotController(random);
        this.incidentService = new IncidentService(random);
        this.raceSimulator = new RaceSimulator(random);
        initTracks();
        overlayTracks(state.getTrackLibrary(), false);
        loadSharedCustomTracks();
        tracks.addAll(state.getSeasonCalendar());
        if (tracks.isEmpty()) {
            resetSeasonCalendarToCurrentLibrary();
        }
        initChampionshipEntries();
        raceHistory.addAll(state.getRaceHistory());
        driverPoints.putAll(state.getDriverPoints());
        teamPoints.putAll(state.getTeamPoints());
        championshipRound = state.getChampionshipRound();
        parcFermeLocked = state.isParcFermeLocked();
        seasonSetupCompleted = state.isSeasonSetupCompleted();
        survivalProgress = state.getSurvivalProgress();
    }

    public void run() {
        System.out.println("\nИгрок: " + playerName + " | Команда: " + player.getTeamName());
        boolean running = true;
        while (running) {
            ensurePreSeasonSetup();
            printMenu();
            int choice = playerController.readInt("Выберите пункт: ");
            switch (choice) {
                case 1 :
                    startRaceWeekend();
                    break;
                case 2 :
                    if (parcFermeLocked) {
                        System.out.println("Парк-ферме закрыт: после квалификации нельзя покупать комплектующие до начала следующей Практики 1.");
                    } else {
                        playerController.buyComponents();
                    }
                    break;
                case 3 :
                    if (parcFermeLocked) {
                        System.out.println("Парк-ферме закрыт: после квалификации нельзя применять новые комплектующие до начала следующей Практики 1.");
                    } else {
                        playerController.assembleCar();
                    }
                    break;
                case 4 :
                    playerController.hireStaffMenu();
                    break;
                case 5 :
                    if (playerController.hirePilot() != null) {
                        initChampionshipEntries();
                    }
                    break;
                case 6 :
                    playerController.showCars();
                    break;
                case 7 :
                    playerController.showPilots();
                    break;
                case 8 :
                    playerController.showRaceStats(raceHistory);
                    break;
                case 9 :
                    botController.showOtherTeams();
                    break;
                case 10 :
                    playerController.showOtherResults(raceHistory);
                    break;
                case 11 :
                    saveCurrentProgress(false);
                    break;
                case 12 :
                    launchSurvivalMode();
                    break;
                case 13 :
                    running = false;
                    break;
                default :
                    System.out.println("Нет такого пункта.");
            }
        }
        System.out.println("Выход из игры. Спасибо за игру!");
    }

    private void printMenu() {
        System.out.println("\nИгрок: " + playerName);
        System.out.println("Бюджет: " + player.getBudget() + " | Репутация: " + player.getReputation());
        System.out.println("Раунд чемпионата: " + (championshipRound + 1) + "/" + tracks.size());
        System.out.println("Календарь сезона: " + tracks.size() + " трасс");
        System.out.println("1) Провести этап чемпионата (FP1-FP2-FP3-Quali-Race)");
        System.out.println("2) Купить комплектующие");
        System.out.println("3) Собрать болид");
        System.out.println("4) Нанять команду");
        System.out.println("5) Нанять пилота");
        System.out.println("6) Просмотреть болиды");
        System.out.println("7) Просмотреть пилотов");
        System.out.println("8) Просмотреть статистику гонок");
        System.out.println("9) Просмотреть другие команды");
        System.out.println("10) Просмотреть другие результаты");
        System.out.println("11) Сохранить игру");
        if (survivalProgress == null) {
            System.out.println("12) Развлекательный режим: выживание");
        } else {
            System.out.println("12) Продолжить режим выживания");
        }
        System.out.println("13) Выход");
    }

    private void startRaceWeekend() {
        if (countOperationalCars() < 3 || player.getDrivers().size() < 3 || player.getEngineers().size() < 2) {
            System.out.println("Минимальные требования не выполнены: нужно 3 пилота, 3 исправных болида и минимум 2 инженера. В гонке участвуют двое, третий остается запасным.");
            return;
        }

        if (parcFermeLocked) {
            System.out.println("\nОткрыто окно изменений: начинается Практика 1 следующего этапа.");
            parcFermeLocked = false;
        }

        Track track = tracks.get(championshipRound % tracks.size());
        System.out.println("\n=== Этап чемпионата: " + track.getName() + " ===");
        runPractice("Практика 1", track);
        runPractice("Практика 2", track);
        runPractice("Практика 3", track);

        Map<MainDriver, Car> weekendLineup = chooseWeekendLineup();
        if (weekendLineup.size() < 2) {
            System.out.println("Для этапа необходимо выбрать двух пилотов и два разных исправных болида.");
            return;
        }

        for (Car car : weekendLineup.values()) {
            incidentService.preRaceMaintenance(player, car, playerController);
        }

        Weather weekendWeather = Weather.values()[random.nextInt(Weather.values().length)];
        Map<String, Double> qualiTimes = runQualifying(track, weekendLineup, weekendWeather);

        parcFermeLocked = true;
        RaceStrategyPlan strategyPlan = playerController.chooseRaceStrategy();
        runRace(track, qualiTimes, weekendLineup, weekendWeather, strategyPlan);

        championshipRound++;
        if (championshipRound % tracks.size() == 0) {
            System.out.println("\n=== Сезон завершен! Итоговые таблицы чемпионата ===");
            printDriverStandings();
            printTeamStandings();
            prepareNextSeason();
        }
        saveCurrentProgress(true);
    }

    private void runPractice(String title, Track track) {
        System.out.println("\n--- " + title + " ---");
        Weather practiceWeather = Weather.values()[random.nextInt(Weather.values().length)];
        System.out.println("Погода на трассе: " + practiceWeather.getTitle());

        double bestLap = Double.MAX_VALUE;

        for (MainDriver driver : player.getDrivers()) {
            for (Car car : player.getCars()) {
                if (!car.operational()) {
                    continue;
                }
                double lap = raceSimulator.simulatePracticeLap(player, car, driver, track, practiceWeather);
                bestLap = Math.min(bestLap, lap);
            }
        }

        for (TeamManager bot : botController.getBotTeams()) {
            List<String> botDrivers = teamDrivers.getOrDefault(bot.getTeamName(), List.of("BOT-1", "BOT-2"));
            for (int i = 0; i < botDrivers.size(); i++) {
                double lap = raceSimulator.generateBotPracticeLap(bot, track, practiceWeather);
                bestLap = Math.min(bestLap, lap);
            }
        }

        if (bestLap == Double.MAX_VALUE) {
            System.out.println("Лучшее время сессии: н/д");
            return;
        }

        System.out.println("Лучшее время сессии: " + formatLap(bestLap));
    }

    private Map<String, Double> runQualifying(Track track, Map<MainDriver, Car> weekendLineup, Weather weather) {
        System.out.println("\n--- Квалификация ---");
        System.out.println("Погода на трассе: " + weather.getTitle());
        Map<String, Double> quali = new HashMap<>();

        for (Map.Entry<MainDriver, Car> lineupEntry : weekendLineup.entrySet()) {
            MainDriver playerDriver = lineupEntry.getKey();
            Car car = lineupEntry.getValue();
            double playerRaceTime = raceSimulator.simulateRaceTime(player, car, playerDriver, track, weather);
            double playerQuali = playerRaceTime / track.getLaps() * (0.975 + random.nextDouble() * 0.01);
            quali.put(playerDriver.getName() + " (" + player.getTeamName() + ")", playerQuali);
        }

        int idx = 0;
        for (TeamManager bot : botController.getBotTeams()) {
            double botRaceTime = raceSimulator.generateBotRaceTime(bot, track, weather);
            double q = botRaceTime / track.getLaps() * (0.972 + random.nextDouble() * 0.013);
            List<String> botDrivers = teamDrivers.getOrDefault(bot.getTeamName(), List.of("BOT-" + (++idx), "BOT-" + (++idx)));
            for (String botDriver : botDrivers) {
                double driverOffset = 0.998 + random.nextDouble() * 0.01;
                quali.put(botDriver + " (" + bot.getTeamName() + ")", q * driverOffset);
            }
        }

        List<Map.Entry<String, Double>> grid = quali.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
        System.out.println("\nСтартовая решетка:");
        for (int i = 0; i < grid.size(); i++) {
            System.out.printf("%d) %s - %s%n", i + 1, grid.get(i).getKey(), formatLap(grid.get(i).getValue()));
        }
        return quali;
    }

    private void runRace(Track track,
                         Map<String, Double> quali,
                         Map<MainDriver, Car> weekendLineup,
                         Weather weather,
                         RaceStrategyPlan strategyPlan) {
        if (quali.isEmpty()) return;
        System.out.println("\n--- Гонка ---");

        ParallelRaceEngine engine = new ParallelRaceEngine(random);
        ParallelRaceEngine.RaceSimulationResult simulationResult =
                engine.simulateRace(track, quali, weekendLineup, player, strategyPlan);
        for (Car car : weekendLineup.values()) {
            incidentService.applyWear(car, track.getLaps());
        }

        List<ParallelRaceEngine.RaceClassificationEntry> finish = simulationResult.getClassification();
        double leaderTime = finish.stream().filter(ParallelRaceEngine.RaceClassificationEntry::isFinished)
                .findFirst()
                .map(ParallelRaceEngine.RaceClassificationEntry::getTotalTime)
                .orElse(0.0);
        String fastestLapOwner = finish.stream()
                .filter(ParallelRaceEngine.RaceClassificationEntry::isFinished)
                .min(Comparator.comparingDouble(ParallelRaceEngine.RaceClassificationEntry::getFastestLap))
                .map(ParallelRaceEngine.RaceClassificationEntry::getDriverTeam)
                .orElse("");

        List<String> table = new ArrayList<>();
        System.out.println("\nИтоговые результаты:");
        System.out.println("Позиция | Пилот (Команда) | БК | Время/Статус | Очки");
        System.out.println("---------------------------------------------------------------");

        for (int i = 0; i < finish.size(); i++) {
            ParallelRaceEngine.RaceClassificationEntry entry = finish.get(i);
            String key = entry.getDriverTeam();
            double total = entry.getTotalTime();
            int pts = entry.getPoints(fastestLapOwner, i);

            String timeCol;
            if (entry.isFinished()) {
                if (i == 0) {
                    timeCol = formatRaceTime(total);
                } else {
                    timeCol = "+" + formatGap(total - leaderTime);
                }
            } else {
                timeCol = entry.getStatus() + ", " + entry.getCompletedLaps() + " кр.";
            }
            String fl;
            if (entry.isFinished()) {
                fl = formatLap(entry.getFastestLap());
            } else {
                fl = "---";
            }
            System.out.printf("%7d | %s | %s | %s | %d%n", i + 1, key, fl, timeCol, pts);
            table.add(String.format("%d. %s | БК %s | %s | %d очков", i + 1, key, fl, timeCol, pts));
            if (pts > 0) {
                addPoints(key, pts);
            }
        }

        updatePlayerDriversDiscontent();

        printDriverStandings();
        printTeamStandings();

        int playerPlace = -1;
        for (int i = 0; i < finish.size(); i++) {
            if (finish.get(i).getDriverTeam().contains(player.getTeamName()) && finish.get(i).isFinished()) {
                playerPlace = i + 1;
                break;
            }
        }
        applyPrize(playerPlace);
        ParallelRaceEngine.RaceStatistics statistics = simulationResult.getStatistics();
        System.out.printf("%nСТАТИСТИКА: %d пит-стопов, %d инцидентов, %d смен погоды%n",
                statistics.getPitStops(), statistics.getIncidents(), statistics.getWeatherChanges());
        raceHistory.add(new RaceResult(track.getName(), simulationResult.getFinalWeather(), table));
    }

    private void saveCurrentProgress(boolean autoSave) {
        GameState state;
        if (autoSave) {
            state = buildCurrentState(null);
        } else {
            state = buildCurrentState(survivalProgress);
        }

        SaveSlot slot;
        if (autoSave) {
            slot = saveManager.saveAuto(state);
            System.out.println("Автосохранение создано: " + slot.getDisplayName());
        } else {
            slot = saveManager.saveManual(state);
            System.out.println("Игра сохранена: " + slot.getDisplayName());
        }
    }

    private GameState buildCurrentState(SurvivalProgress progress) {
        return new GameState(
                playerName,
                player,
                raceHistory,
                driverPoints,
                teamPoints,
                trackLibrary,
                tracks,
                championshipRound,
                parcFermeLocked,
                seasonSetupCompleted,
                progress
        );
    }

    private void launchSurvivalMode() {
        SurvivalModeSession survivalModeSession = new SurvivalModeSession(
                scanner,
                saveManager,
                random,
                raceSimulator,
                playerController,
                new ArrayList<>(trackLibrary),
                buildCurrentState(null).deepCopy()
        );
        survivalModeSession.run(survivalProgress, botController);
        survivalProgress = null;
    }

    private void addPoints(String driverTeam, int pts) {
        String team = driverTeam.substring(driverTeam.indexOf('(') + 1, driverTeam.length() - 1);
        String driver = driverTeam.substring(0, driverTeam.indexOf(" ("));
        driverPoints.put(driver, driverPoints.getOrDefault(driver, 0) + pts);
        teamPoints.put(team, teamPoints.getOrDefault(team, 0) + pts);
    }

    private void printDriverStandings() {
        System.out.println("\n--- Таблица пилотов ---");
        buildDriverStandings().stream().limit(22)
                .forEach(e -> System.out.printf("%s - %d%n", e.getKey(), e.getValue()));
    }

    private void printTeamStandings() {
        System.out.println("\n--- Таблица команд ---");
        teamPoints.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> System.out.printf("%s - %d%n", e.getKey(), e.getValue()));
    }

    private String formatLap(double seconds) {
        int totalMillis = (int) Math.round(seconds * 1000);
        int mm = totalMillis / 60_000;
        int ss = (totalMillis % 60_000) / 1000;
        int ms = totalMillis % 1000;
        return String.format("%02d:%02d.%03d", mm, ss, ms);
    }

    private Map<MainDriver, Car> chooseWeekendLineup() {
        System.out.println("\nВыбор состава на этап: 2 пилота и 2 разных болида. Третий пилот и третий болид остаются запасными.");

        MainDriver firstDriver = playerController.choose("первого пилота", player.getDrivers());
        if (firstDriver == null) return Map.of();
        Car firstCar = playerController.choose("болид для первого пилота", player.getCars());
        if (firstCar == null || !firstCar.operational()) return Map.of();

        List<MainDriver> secondDriverChoices = new ArrayList<>(player.getDrivers());
        secondDriverChoices.remove(firstDriver);
        MainDriver secondDriver = playerController.choose("второго пилота", secondDriverChoices);
        if (secondDriver == null) return Map.of();

        List<Car> secondCarChoices = new ArrayList<>(player.getCars());
        secondCarChoices.remove(firstCar);
        Car secondCar = playerController.choose("болид для второго пилота", secondCarChoices);
        if (secondCar == null || !secondCar.operational()) return Map.of();

        Map<MainDriver, Car> lineup = new LinkedHashMap<>();
        lineup.put(firstDriver, firstCar);
        lineup.put(secondDriver, secondCar);
        return lineup;
    }

    private long countOperationalCars() {
        return player.getCars().stream()
                .filter(Car::operational)
                .count();
    }

    private void updatePlayerDriversDiscontent() {
        List<Map.Entry<String, Integer>> standings = buildDriverStandings();
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < standings.size(); i++) {
            positions.put(standings.get(i).getKey(), i + 1);
        }

        for (MainDriver driver : player.getDrivers()) {
            int position = positions.getOrDefault(driver.getName(), standings.size() + 1);
            if (position >= DISCONTENT_STANDINGS_POSITION_THRESHOLD) {
                int previousDiscontent = driver.getDiscontent();
                driver.addDiscontent(DISCONTENT_GAIN_FOR_LOW_STANDINGS);
                int currentDiscontent = driver.getDiscontent();
                if (currentDiscontent == 0 && previousDiscontent > 0) {
                    System.out.printf("Недовольство пилота %s достигло максимума и сбросилось до 0.%n",
                            driver.getName());
                } else if (currentDiscontent != previousDiscontent) {
                    System.out.printf("Недовольство пилота %s увеличилось: %d -> %d.%n",
                            driver.getName(), previousDiscontent, currentDiscontent);
                }
            }
        }
    }

    private List<Map.Entry<String, Integer>> buildDriverStandings() {
        Map<String, Integer> standings = new HashMap<>();
        for (List<String> drivers : teamDrivers.values()) {
            for (String driver : drivers) {
                standings.put(driver, driverPoints.getOrDefault(driver, 0));
            }
        }
        for (MainDriver driver : player.getDrivers()) {
            standings.put(driver.getName(), driverPoints.getOrDefault(driver.getName(), 0));
        }

        return standings.entrySet().stream()
                .sorted((a, b) -> {
                    int byPoints = Integer.compare(b.getValue(), a.getValue());
                    if (byPoints != 0) {
                        return byPoints;
                    }
                    return a.getKey().compareToIgnoreCase(b.getKey());
                })
                .toList();
    }

    private String formatRaceTime(double seconds) {
        return formatLap(seconds);
    }

    private String formatGap(double seconds) {
        return String.format("%.3f", seconds);
    }

    private void applyPrize(int playerPlace) {
        if (playerPlace == 1) {
            player.addBudget(300_000); player.addReputation(4); System.out.println("Победа! Призовые: 300000");
        } else if (playerPlace == 2) {
            player.addBudget(200_000); player.addReputation(2); System.out.println("2 место! Призовые: 200000");
        } else if (playerPlace == 3) {
            player.addBudget(120_000); player.addReputation(1); System.out.println("3 место! Призовые: 120000");
        } else if (playerPlace > 0) {
            player.addBudget(40_000); System.out.println("Финиш вне подиума. Утешительные призовые: 40000");
        }
    }

    private void initChampionshipEntries() {
        teamDrivers.clear();

        Map<String, List<String>> defaultLineups = new LinkedHashMap<>();
        defaultLineups.put("Ferrari", List.of("Charles Leclerc", "Lewis Hamilton"));
        defaultLineups.put("Mercedes", List.of("George Russell", "Andrea Kimi Antonelli"));
        defaultLineups.put("McLaren", List.of("Lando Norris", "Oscar Piastri"));
        defaultLineups.put("Aston Martin", List.of("Fernando Alonso", "Lance Stroll"));
        defaultLineups.put("Alpine", List.of("Pierre Gasly", "Franco Colapinto"));
        defaultLineups.put("Williams", List.of("Alexander Albon", "Carlos Sainz"));
        defaultLineups.put("Racing Bulls", List.of("Yuki Tsunoda", "Isack Hadjar"));
        defaultLineups.put("Haas", List.of("Esteban Ocon", "Oliver Bearman"));
        defaultLineups.put("Audi", List.of("Nico Hulkenberg", "Gabriel Bortoleto"));
        defaultLineups.put("Cadillac", List.of("Valtteri Bottas", "Liam Lawson"));

        Set<String> playerDriverNames = player.getDrivers().stream()
                .map(driver -> driver.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> usedDriverNames = new LinkedHashSet<>(playerDriverNames);
        List<String> reserveDrivers = marketService.generateDriverCandidates().stream()
                .map(MainDriver::getName)
                .toList();

        for (Map.Entry<String, List<String>> entry : defaultLineups.entrySet()) {
            List<String> assignedDrivers = new ArrayList<>();
            for (String driverName : entry.getValue()) {
                assignedDrivers.add(resolveBotDriverName(driverName, reserveDrivers, usedDriverNames));
            }
            teamDrivers.put(entry.getKey(), assignedDrivers);
        }
    }

    private String resolveBotDriverName(String preferredName, List<String> reserveDrivers, Set<String> usedDriverNames) {
        String normalizedPreferredName = preferredName.toLowerCase(Locale.ROOT);
        if (!usedDriverNames.contains(normalizedPreferredName)) {
            usedDriverNames.add(normalizedPreferredName);
            return preferredName;
        }

        for (String candidateName : reserveDrivers) {
            String normalizedCandidateName = candidateName.toLowerCase(Locale.ROOT);
            if (!usedDriverNames.contains(normalizedCandidateName)) {
                usedDriverNames.add(normalizedCandidateName);
                return candidateName;
            }
        }

        throw new IllegalStateException("Недостаточно уникальных пилотов для формирования состава чемпионата.");
    }

    private void initTracks() {
        trackLibrary.add(new Track("Australian Grand Prix | Australia, Albert Park Circuit (Melbourne)", 5.278, 58, 14, 3, 40));
        trackLibrary.add(new Track("Chinese Grand Prix | China, Shanghai International Circuit (Shanghai)", 5.451, 56, 16, 3, 10));
        trackLibrary.add(new Track("Japanese Grand Prix | Japan, Suzuka Circuit (Suzuka)", 5.807, 53, 18, 3, 25));
        trackLibrary.add(new Track("Bahrain Grand Prix | Bahrain International Circuit (Sakhir)", 5.412, 57, 15, 4, 25));
        trackLibrary.add(new Track("Saudi Arabian Grand Prix | Jeddah Corniche Circuit (Jeddah)", 6.174, 50, 27, 4, 12));
        trackLibrary.add(new Track("Miami Grand Prix | USA, Miami International Autodrome (Miami Gardens)", 5.412, 57, 19, 3, 10));
        trackLibrary.add(new Track("Canadian Grand Prix | Canada, Circuit Gilles Villeneuve (Montreal)", 4.361, 70, 14, 4, 8));
        trackLibrary.add(new Track("Monaco Grand Prix | Monaco, Circuit de Monaco", 3.337, 78, 19, 1, 35));
        trackLibrary.add(new Track("Spanish Grand Prix | Spain, Circuit de Barcelona-Catalunya (Montmelo)", 4.657, 66, 14, 3, 30));
        trackLibrary.add(new Track("Austrian Grand Prix | Austria, Red Bull Ring (Spielberg)", 4.318, 71, 10, 3, 65));
        trackLibrary.add(new Track("British Grand Prix | UK, Silverstone Circuit (Silverstone)", 5.891, 52, 18, 4, 45));
        trackLibrary.add(new Track("Belgian Grand Prix | Belgium, Spa-Francorchamps (Stavelot)", 7.004, 44, 19, 5, 100));
        trackLibrary.add(new Track("Hungarian Grand Prix | Hungary, Hungaroring (Mogyorod)", 4.381, 70, 14, 2, 34));
        trackLibrary.add(new Track("Dutch Grand Prix | Netherlands, Circuit Zandvoort (Zandvoort)", 4.259, 72, 14, 2, 35));
        trackLibrary.add(new Track("Italian Grand Prix | Italy, Monza Circuit (Monza)", 5.793, 53, 11, 4, 22));
        trackLibrary.add(new Track("Spanish Grand Prix | Spain, Madring (Madrid)", 5.470, 57, 22, 4, 18));
        trackLibrary.add(new Track("Azerbaijan Grand Prix | Azerbaijan, Baku City Circuit (Baku)", 6.003, 51, 20, 3, 15));
        trackLibrary.add(new Track("Singapore Grand Prix | Singapore, Marina Bay Street Circuit", 4.940, 62, 19, 2, 10));
        trackLibrary.add(new Track("United States Grand Prix | USA, Circuit of the Americas (Austin)", 5.513, 56, 20, 4, 40));
        trackLibrary.add(new Track("Mexico City Grand Prix | Mexico, Autodromo Hermanos Rodriguez (Mexico City)", 4.304, 71, 17, 3, 12));
        trackLibrary.add(new Track("Sao Paulo Grand Prix | Brazil, Interlagos (Sao Paulo)", 4.309, 71, 15, 3, 43));
        trackLibrary.add(new Track("Las Vegas Grand Prix | USA, Las Vegas Strip Circuit", 6.201, 50, 17, 3, 8));
        trackLibrary.add(new Track("Qatar Grand Prix | Qatar, Lusail International Circuit (Lusail)", 5.419, 57, 16, 3, 12));
        trackLibrary.add(new Track("Abu Dhabi Grand Prix | UAE, Yas Marina Circuit (Abu Dhabi)", 5.281, 58, 16, 3, 5));
    }

    private void ensurePreSeasonSetup() {
        if (seasonSetupCompleted) {
            return;
        }

        System.out.println("\n=== Предсезонная настройка ===");
        boolean configuring = true;
        while (configuring) {
            System.out.println("1) Редактировать существующую трассу");
            System.out.println("2) Создать новую трассу");
            System.out.println("3) Показать библиотеку трасс");
            System.out.println("4) Сформировать календарь сезона");
            System.out.println("5) Оставить полный календарь и начать сезон");
            int choice = playerController.readInt("Выберите пункт: ");
            switch (choice) {
                case 1 -> editExistingTrack();
                case 2 -> createTrack();
                case 3 -> showTrackLibrary();
                case 4 -> configuring = !configureSeasonCalendar();
                case 5 -> {
                    resetSeasonCalendarToCurrentLibrary();
                    seasonSetupCompleted = true;
                    configuring = false;
                    System.out.println("Сезон начнется с полного календаря.");
                }
                default -> System.out.println("Нет такого пункта.");
            }
        }
    }

    private void showTrackLibrary() {
        System.out.println("\n--- Библиотека трасс ---");
        for (int i = 0; i < trackLibrary.size(); i++) {
            System.out.printf("%d) %s%n", i + 1, describeTrack(trackLibrary.get(i)));
        }
    }

    private void editExistingTrack() {
        showTrackLibrary();
        int choice = playerController.readInt("Какую трассу редактировать: ");
        if (choice < 1 || choice > trackLibrary.size()) {
            System.out.println("Нет такой трассы.");
            return;
        }

        Track current = trackLibrary.get(choice - 1);
        String originalName = current.getName();
        Track updated = new Track(
                readTrackName("Название трассы", current.getName()),
                readDouble("Длина круга в км", current.getLapKm(), 0.1),
                readInt("Количество кругов", current.getLaps(), 1),
                readInt("Количество поворотов", current.getCorners(), 1),
                readInt("Количество прямых", current.getStraights(), 1),
                readInt("Перепад высот", current.getElevation(), 0)
        );
        trackLibrary.set(choice - 1, updated);
        if (isSharedCustomTrack(originalName)) {
            trackCatalogManager.updateTrack(originalName, updated);
            sharedCustomTrackNames.remove(originalName.toLowerCase(Locale.ROOT));
            sharedCustomTrackNames.add(updated.getName().toLowerCase(Locale.ROOT));
        }
        if (!seasonSetupCompleted) {
            resetSeasonCalendarToCurrentLibrary();
        }
        System.out.println("Трасса обновлена: " + updated.getName());
    }

    private void createTrack() {
        System.out.println("\n--- Создание трассы ---");
        Track track = new Track(
                readTrackName("Название трассы", null),
                readDouble("Длина круга в км", null, 0.1),
                readInt("Количество кругов", null, 1),
                readInt("Количество поворотов", null, 1),
                readInt("Количество прямых", null, 1),
                readInt("Перепад высот", null, 0)
        );
        trackLibrary.add(track);
        trackCatalogManager.saveNewTrack(track);
        sharedCustomTrackNames.add(track.getName().toLowerCase(Locale.ROOT));
        if (!seasonSetupCompleted) {
            resetSeasonCalendarToCurrentLibrary();
        }
        System.out.println("Новая трасса добавлена: " + track.getName());
    }

    private boolean configureSeasonCalendar() {
        showTrackLibrary();
        System.out.println("Введите номера трасс через запятую в порядке календаря.");
        String line = scanner.nextLine().trim();
        if (line.isEmpty()) {
            System.out.println("Календарь не может быть пустым.");
            return false;
        }

        String[] parts = line.split(",");
        List<Track> selectedTracks = new ArrayList<>();
        Set<Integer> usedIndexes = new HashSet<>();
        for (String part : parts) {
            try {
                int index = Integer.parseInt(part.trim());
                if (index < 1 || index > trackLibrary.size()) {
                    System.out.println("В календаре есть несуществующая трасса.");
                    return false;
                }
                if (!usedIndexes.add(index)) {
                    System.out.println("Одна и та же трасса не должна повторяться в календаре.");
                    return false;
                }
                selectedTracks.add(trackLibrary.get(index - 1));
            } catch (NumberFormatException e) {
                System.out.println("Календарь должен состоять из номеров трасс.");
                return false;
            }
        }

        tracks.clear();
        tracks.addAll(selectedTracks);
        seasonSetupCompleted = true;
        System.out.println("Календарь сезона сформирован на " + tracks.size() + " этап(ов).");
        return true;
    }

    private void resetSeasonCalendarToCurrentLibrary() {
        tracks.clear();
        tracks.addAll(trackLibrary);
    }

    private void loadSharedCustomTracks() {
        overlayTracks(trackCatalogManager.loadCustomTracks(), true);
    }

    private void overlayTracks(List<Track> sourceTracks, boolean sharedCustom) {
        for (Track track : sourceTracks) {
            replaceOrAddTrack(trackLibrary, track);
            if (sharedCustom) {
                sharedCustomTrackNames.add(track.getName().toLowerCase(Locale.ROOT));
            }
        }
    }

    private void replaceOrAddTrack(List<Track> destination, Track track) {
        for (int i = 0; i < destination.size(); i++) {
            if (destination.get(i).getName().equalsIgnoreCase(track.getName())) {
                destination.set(i, track);
                return;
            }
        }
        destination.add(track);
    }

    private boolean isSharedCustomTrack(String trackName) {
        return sharedCustomTrackNames.contains(trackName.toLowerCase(Locale.ROOT));
    }

    private void prepareNextSeason() {
        player.getCars().clear();
        driverPoints.clear();
        teamPoints.clear();
        championshipRound = 0;
        parcFermeLocked = false;
        seasonSetupCompleted = false;
        resetSeasonCalendarToCurrentLibrary();
        System.out.println("\nПодготовка к новому сезону: болиды списаны, бюджет, пилоты и персонал сохранены.");
    }

    private String describeTrack(Track track) {
        return String.format("%s | %.3f км | кругов=%d | поворотов=%d | прямых=%d | перепад=%d",
                track.getName(), track.getLapKm(), track.getLaps(), track.getCorners(), track.getStraights(), track.getElevation());
    }

    private String readTrackName(String label, String currentValue) {
        while (true) {
            if (currentValue == null) {
                System.out.print(label + ": ");
            } else {
                System.out.print(label + " [" + currentValue + "]: ");
            }
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            if (currentValue != null) {
                return currentValue;
            }
            System.out.println("Название не должно быть пустым.");
        }
    }

    private int readInt(String label, Integer currentValue, int minValue) {
        while (true) {
            if (currentValue == null) {
                System.out.print(label + ": ");
            } else {
                System.out.print(label + " [" + currentValue + "]: ");
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty() && currentValue != null) {
                return currentValue;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= minValue) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Введите число не меньше " + minValue + ".");
        }
    }

    private double readDouble(String label, Double currentValue, double minValue) {
        while (true) {
            if (currentValue == null) {
                System.out.print(label + ": ");
            } else {
                System.out.print(label + " [" + currentValue + "]: ");
            }
            String line = scanner.nextLine().trim().replace(',', '.');
            if (line.isEmpty() && currentValue != null) {
                return currentValue;
            }
            try {
                double value = Double.parseDouble(line);
                if (value >= minValue) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Введите число не меньше " + minValue + ".");
        }
    }
}
