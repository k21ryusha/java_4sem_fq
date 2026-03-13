package game;

import components.Car;
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
    private final Random random = new Random();
    private final TeamManager player = new TeamManager("Player Racing", 900_000, 10);
    private final List<Track> tracks = new ArrayList<>();
    private final List<RaceResult> raceHistory = new ArrayList<>();

    private final PlayerController playerController;
    private final BotController botController;
    private final IncidentService incidentService;
    private final RaceSimulator raceSimulator;

    public GameSession(Scanner scanner) {
        MarketService marketService = new MarketService(random);
        this.playerController = new PlayerController(scanner, player, marketService);
        this.botController = new BotController(random);
        this.incidentService = new IncidentService(random);
        this.raceSimulator = new RaceSimulator(random);
        initTracks();
    }

    public void run() {
        System.out.println("=== Симулятор гоночной команды ===");
        boolean running = true;
        while (running) {
            printMenu();
            int choice = playerController.readInt("Выберите пункт: ");
            switch (choice) {
                case 1 :
                    startRace();
                case 2 :
                    playerController.buyComponents();
                    break;
                case 3 :
                    playerController.assembleCar();
                    break;
                case 4 :
                    playerController.hireEngineer();
                case 5 :
                    playerController.hirePilot();
                case 6 :
                    playerController.showCars();
                case 7 :
                    playerController.showPilots();
                case 8 :
                    playerController.showRaceStats(raceHistory);
                case 9 :
                    botController.showOtherTeams();
                case 10 :
                    playerController.showOtherResults(raceHistory);
                case 11 :
                    running = false;
                default :
                    System.out.println("Нет такого пункта.");
            }
        }
        System.out.println("Выход из игры. Спасибо за игру!");
    }

    private void printMenu() {
        System.out.println("\nБюджет: " + player.getBudget() + " | Репутация: " + player.getReputation());
        System.out.println("1) Начать гонку");
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

    private void startRace() {
        System.out.println("\n--- Подготовка к гонке ---");
        if (player.getCars().isEmpty() || player.getDrivers().isEmpty() || player.getEngineers().isEmpty()) {
            System.out.println("Минимальные требования не выполнены: нужен болид, пилот и минимум 1 инженер.");
            return;
        }
        Car car = playerController.choose("болид", player.getCars());
        if (car == null || !car.operational()) {
            System.out.println("Болид недоступен.");
            return;
        }
        MainDriver driver = playerController.choose("пилота", player.getDrivers());
        if (driver == null) return;
        Track track = playerController.choose("трассу", tracks);
        if (track == null) return;

        Weather weather = Weather.values()[random.nextInt(Weather.values().length)];
        System.out.println("Погода на трассе: " + weather.getTitle());

        incidentService.preRaceMaintenance(player, car, playerController);
        double playerTime = raceSimulator.simulateRaceTime(player, car, driver, track, weather);
        boolean incident = incidentService.checkIncident(car);
        if (incident) playerTime = Double.MAX_VALUE;

        Map<String, Double> timing = new HashMap<>();
        timing.put(player.getTeamName() + (incident ? " (инцидент)" : ""), playerTime);
        for (TeamManager bot : botController.getBotTeams()) {
            timing.put(bot.getTeamName(), raceSimulator.generateBotRaceTime(bot, track, weather));
        }

        List<Map.Entry<String, Double>> standings = timing.entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue)).toList();
        List<String> table = new ArrayList<>();
        int place = 1;
        int playerPlace = -1;
        for (Map.Entry<String, Double> row : standings) {
            String value = row.getValue() == Double.MAX_VALUE ? "DNF" : String.format("%.2f c", row.getValue());
            String line = place + ". " + row.getKey() + " - " + value;
            table.add(line);
            if (row.getKey().startsWith(player.getTeamName())) playerPlace = place;
            place++;
        }

        System.out.println("\n--- Итоги гонки ---");
        table.forEach(System.out::println);
        applyPrize(playerPlace);
        incidentService.applyWear(car, track.getLaps());
        raceHistory.add(new RaceResult(track.getName(), weather, table));
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

    private void initTracks() {
        tracks.add(new Track("Silver Hills", 5.4, 20, 12, 6, 40));
        tracks.add(new Track("Urban Ring", 4.1, 26, 18, 4, 10));
        tracks.add(new Track("Desert Speed", 6.2, 18, 9, 8, 25));
        tracks.add(new Track("Mountain Crown", 5.8, 19, 14, 5, 80));
    }
}
