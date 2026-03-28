package game;

import components.Car;
import components.Component;
import economic.MarketService;
import incidents.IncidentService;
import race_weekend.RaceResult;
import race_weekend.RaceSimulator;
import race_weekend.Track;
import race_weekend.Weather;
import staff.MainDriver;
import staff.TeamManager;

import java.util.*;

public class GameSession {
    private static final int DISCONTENT_STANDINGS_POSITION_THRESHOLD = 10;
    private static final int DISCONTENT_GAIN_FOR_LOW_STANDINGS = 5;

    private final Random random = new Random();
    private final TeamManager player = new TeamManager("Player Racing", 9000_000, 10);
    private final List<Track> tracks = new ArrayList<>();
    private final List<RaceResult> raceHistory = new ArrayList<>();

    private final Map<String, Integer> driverPoints = new HashMap<>();
    private final Map<String, Integer> teamPoints = new HashMap<>();
    private final Map<String, List<String>> teamDrivers = new HashMap<>();

    private final PlayerController playerController;
    private final BotController botController;
    private final IncidentService incidentService;
    private final RaceSimulator raceSimulator;

    private int championshipRound = 0;
    private boolean parcFermeLocked = false;

    public GameSession(Scanner scanner) {
        MarketService marketService = new MarketService(random);
        this.playerController = new PlayerController(scanner, player, marketService);
        this.botController = new BotController(random);
        this.incidentService = new IncidentService(random);
        this.raceSimulator = new RaceSimulator(random);
        initTracks();
        initChampionshipEntries();
    }

    public void run() {
        System.out.println("=== Симулятор гоночной команды ===");
        boolean running = true;
        while (running) {
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
                    playerController.hirePilot();
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
                    running = false;
                    break;
                default :
                    System.out.println("Нет такого пункта.");
            }
        }
        System.out.println("Выход из игры. Спасибо за игру!");
    }

    private void printMenu() {
        System.out.println("\nБюджет: " + player.getBudget() + " | Репутация: " + player.getReputation());
        System.out.println("Раунд чемпионата: " + (championshipRound + 1) + "/" + tracks.size());
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
        System.out.println("11) Выход");
    }

    private void startRaceWeekend() {
        if (player.getCars().size() < 2 || player.getDrivers().size() < 2 || player.getEngineers().size() < 2) {
            System.out.println("Минимальные требования не выполнены: нужно 2 болида, 2 пилота и минимум 2 инженера.");
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
        runRace(track, qualiTimes, weekendLineup, weekendWeather);

        championshipRound++;
        if (championshipRound % tracks.size() == 0) {
            System.out.println("\n=== Сезон завершен! Итоговые таблицы чемпионата ===");
            printDriverStandings();
            printTeamStandings();
        }
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

    private void runRace(Track track, Map<String, Double> quali, Map<MainDriver, Car> weekendLineup, Weather weather) {
        if (quali.isEmpty()) return;
        System.out.println("\n--- Гонка ---");

        Map<String, Car> playerCarsByEntry = new HashMap<>();
        for (Map.Entry<MainDriver, Car> entry : weekendLineup.entrySet()) {
            String driverEntry = entry.getKey().getName() + " (" + player.getTeamName() + ")";
            playerCarsByEntry.put(driverEntry, entry.getValue());
            incidentService.preRaceMaintenance(player, entry.getValue(), playerController);
        }

        List<Map.Entry<String, Double>> grid = quali.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
        List<RaceClassificationEntry> classification = new ArrayList<>();

        for (int i = 0; i < grid.size(); i++) {
            String driverTeam = grid.get(i).getKey();
            double qLap = grid.get(i).getValue();
            double race = qLap * track.getLaps() * (1.015 + random.nextDouble() * 0.02) + i * 0.75;
            double fl = qLap * (0.985 + random.nextDouble() * 0.015);
            boolean finished = true;
            String status = "";

            Car playerCar = playerCarsByEntry.get(driverTeam);
            if (playerCar != null) {
                Component brokenComponent = incidentService.checkMechanicalFailure(playerCar);
                if (brokenComponent != null) {
                    finished = false;
                    status = "DNF";
                    System.out.println("Сход! " + driverTeam + " выбыл из гонки: отказал компонент " + brokenComponent.getName() + ".");
                } else if (incidentService.checkIncident(playerCar, findWeekendDriverByEntry(driverTeam, weekendLineup))) {
                    race += 25 + random.nextDouble() * 35;
                    fl *= 1.02;
                }
            } else if (checkVirtualMechanicalFailure()) {
                finished = false;
                status = "DNF";
                System.out.println("Сход! " + driverTeam + " выбыл из гонки: отказал компонент " + randomBotComponentName() + ".");
            } else if (checkVirtualIncident()) {
                race += 18 + random.nextDouble() * 30;
                fl *= 1.015;
            }

            classification.add(new RaceClassificationEntry(driverTeam, race, fl, i, finished, status));
        }
        for (Car car : weekendLineup.values()) {
            incidentService.applyWear(car, track.getLaps());
        }

        List<RaceClassificationEntry> finish = classification.stream()
                .sorted((a, b) -> {
                    if (a.finished != b.finished) {
                        return Boolean.compare(b.finished, a.finished);
                    }
                    if (!a.finished) {
                        return Integer.compare(a.gridPosition, b.gridPosition);
                    }
                    return Double.compare(a.totalTime, b.totalTime);
                })
                .toList();
        double leaderTime = finish.stream().filter(entry -> entry.finished).findFirst().map(entry -> entry.totalTime).orElse(0.0);
        String fastestLapOwner = finish.stream()
                .filter(entry -> entry.finished)
                .min(Comparator.comparingDouble(entry -> entry.fastestLap))
                .map(entry -> entry.driverTeam)
                .orElse("");

        int[] f1Points = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};

        List<String> table = new ArrayList<>();
        System.out.println("\nПозиция | Пилот (Команда) | БК | Время/Отставание | Очки");
        System.out.println("----------------------------------------------------------------");

        for (int i = 0; i < finish.size(); i++) {
            RaceClassificationEntry entry = finish.get(i);
            String key = entry.driverTeam;
            double total = entry.totalTime;
            int pts = 0;
            if (entry.finished && i < f1Points.length) {
                pts = f1Points[i];
            }
            if (entry.finished && i < 10 && key.equals(fastestLapOwner)) pts += 1;

            String timeCol;
            if (entry.finished) {
                if (i == 0) {
                    timeCol = formatRaceTime(total);
                } else {
                    timeCol = "+" + formatGap(total - leaderTime);
                }
            } else {
                timeCol = entry.status;
            }
            String fl;
            if (entry.finished) {
                fl = formatLap(entry.fastestLap);
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
            if (finish.get(i).driverTeam.contains(player.getTeamName()) && finish.get(i).finished) {
                playerPlace = i + 1;
                break;
            }
        }
        applyPrize(playerPlace);
        raceHistory.add(new RaceResult(track.getName(), weather, table));
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
        System.out.println("\nВыбор состава на этап: 2 пилота и 2 разных болида.");

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

    private void updatePlayerDriversDiscontent() {
        List<Map.Entry<String, Integer>> standings = buildDriverStandings();
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < standings.size(); i++) {
            positions.put(standings.get(i).getKey(), i + 1);
        }

        for (MainDriver driver : player.getDrivers()) {
            int position = positions.getOrDefault(driver.getName(), standings.size() + 1);
            if (position >= DISCONTENT_STANDINGS_POSITION_THRESHOLD) {
                driver.addDiscontent(DISCONTENT_GAIN_FOR_LOW_STANDINGS);
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

    private boolean checkVirtualIncident() {
        return random.nextDouble() < 0.09;
    }

    private MainDriver findWeekendDriverByEntry(String driverTeam, Map<MainDriver, Car> weekendLineup) {
        for (MainDriver driver : weekendLineup.keySet()) {
            String entryName = driver.getName() + " (" + player.getTeamName() + ")";
            if (entryName.equals(driverTeam)) {
                return driver;
            }
        }
        return null;
    }

    private boolean checkVirtualMechanicalFailure() {
        return random.nextDouble() < 0.035;
    }

    private String randomBotComponentName() {
        String[] components = {"двигатель", "трансмиссия", "шасси", "подвеска", "аэродинамика", "шины"};
        return components[random.nextInt(components.length)];
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

    private static final class RaceClassificationEntry {
        private final String driverTeam;
        private final double totalTime;
        private final double fastestLap;
        private final int gridPosition;
        private final boolean finished;
        private final String status;

        private RaceClassificationEntry(String driverTeam, double totalTime, double fastestLap, int gridPosition, boolean finished, String status) {
            this.driverTeam = driverTeam;
            this.totalTime = totalTime;
            this.fastestLap = fastestLap;
            this.gridPosition = gridPosition;
            this.finished = finished;
            this.status = status;
        }
    }

    private void initChampionshipEntries() {
        teamDrivers.put("Ferrari", List.of("Charles Leclerc", "Lewis Hamilton"));
        teamDrivers.put("Mercedes", List.of("George Russell", "Andrea Kimi Antonelli"));
        teamDrivers.put("McLaren", List.of("Lando Norris", "Oscar Piastri"));
        teamDrivers.put("Aston Martin", List.of("Fernando Alonso", "Lance Stroll"));
        teamDrivers.put("Alpine", List.of("Pierre Gasly", "Franco Colapinto"));
        teamDrivers.put("Williams", List.of("Alexander Albon", "Carlos Sainz"));
        teamDrivers.put("Racing Bulls", List.of("Yuki Tsunoda", "Isack Hadjar"));
        teamDrivers.put("Haas", List.of("Esteban Ocon", "Oliver Bearman"));
        teamDrivers.put("Audi", List.of("Nico Hulkenberg", "Gabriel Bortoleto"));
        teamDrivers.put("Cadillac", List.of("Valtteri Bottas", "Liam Lawson"));
    }

    private void initTracks() {
        tracks.add(new Track("Australian Grand Prix | Australia, Albert Park Circuit (Melbourne)", 5.278, 58, 14, 3, 40));
        tracks.add(new Track("Chinese Grand Prix | China, Shanghai International Circuit (Shanghai)", 5.451, 56, 16, 3, 10));
        tracks.add(new Track("Japanese Grand Prix | Japan, Suzuka Circuit (Suzuka)", 5.807, 53, 18, 3, 25));
        tracks.add(new Track("Bahrain Grand Prix | Bahrain International Circuit (Sakhir)", 5.412, 57, 15, 4, 25));
        tracks.add(new Track("Saudi Arabian Grand Prix | Jeddah Corniche Circuit (Jeddah)", 6.174, 50, 27, 4, 12));
        tracks.add(new Track("Miami Grand Prix | USA, Miami International Autodrome (Miami Gardens)", 5.412, 57, 19, 3, 10));
        tracks.add(new Track("Canadian Grand Prix | Canada, Circuit Gilles Villeneuve (Montreal)", 4.361, 70, 14, 4, 8));
        tracks.add(new Track("Monaco Grand Prix | Monaco, Circuit de Monaco", 3.337, 78, 19, 1, 35));
        tracks.add(new Track("Spanish Grand Prix | Spain, Circuit de Barcelona-Catalunya (Montmelo)", 4.657, 66, 14, 3, 30));
        tracks.add(new Track("Austrian Grand Prix | Austria, Red Bull Ring (Spielberg)", 4.318, 71, 10, 3, 65));
        tracks.add(new Track("British Grand Prix | UK, Silverstone Circuit (Silverstone)", 5.891, 52, 18, 4, 45));
        tracks.add(new Track("Belgian Grand Prix | Belgium, Spa-Francorchamps (Stavelot)", 7.004, 44, 19, 5, 100));
        tracks.add(new Track("Hungarian Grand Prix | Hungary, Hungaroring (Mogyorod)", 4.381, 70, 14, 2, 34));
        tracks.add(new Track("Dutch Grand Prix | Netherlands, Circuit Zandvoort (Zandvoort)", 4.259, 72, 14, 2, 35));
        tracks.add(new Track("Italian Grand Prix | Italy, Monza Circuit (Monza)", 5.793, 53, 11, 4, 22));
        tracks.add(new Track("Spanish Grand Prix | Spain, Madring (Madrid)", 5.470, 57, 22, 4, 18));
        tracks.add(new Track("Azerbaijan Grand Prix | Azerbaijan, Baku City Circuit (Baku)", 6.003, 51, 20, 3, 15));
        tracks.add(new Track("Singapore Grand Prix | Singapore, Marina Bay Street Circuit", 4.940, 62, 19, 2, 10));
        tracks.add(new Track("United States Grand Prix | USA, Circuit of the Americas (Austin)", 5.513, 56, 20, 4, 40));
        tracks.add(new Track("Mexico City Grand Prix | Mexico, Autodromo Hermanos Rodriguez (Mexico City)", 4.304, 71, 17, 3, 12));
        tracks.add(new Track("Sao Paulo Grand Prix | Brazil, Interlagos (Sao Paulo)", 4.309, 71, 15, 3, 43));
        tracks.add(new Track("Las Vegas Grand Prix | USA, Las Vegas Strip Circuit", 6.201, 50, 17, 3, 8));
        tracks.add(new Track("Qatar Grand Prix | Qatar, Lusail International Circuit (Lusail)", 5.419, 57, 16, 3, 12));
        tracks.add(new Track("Abu Dhabi Grand Prix | UAE, Yas Marina Circuit (Abu Dhabi)", 5.281, 58, 16, 3, 5));
    }
}
