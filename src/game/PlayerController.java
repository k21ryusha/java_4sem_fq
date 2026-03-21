package game;

import components.*;
import economic.MarketService;
import race_weekend.RaceResult;
import staff.MainDriver;
import staff.*;
import staff.TeamManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class PlayerController {
    private final Scanner scanner;
    private final TeamManager player;
    private final MarketService marketService;

    public PlayerController(Scanner scanner, TeamManager player, MarketService marketService) {
        this.scanner = scanner;
        this.player = player;
        this.marketService = marketService;
    }

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try { return Integer.parseInt(line);} catch (NumberFormatException e) { System.out.println("Введите число."); }
        }
    }

    public void buyComponents() {
            while (true) {
                List<Component> offers = marketService.generateComponentOffers();
                System.out.println("\n--- Рынок компонентов ---");
                for (int i = 0; i < offers.size(); i++) {
                    Component c = offers.get(i);
                    System.out.printf("%d) %s | цена: %d | качество: %d%n", i + 1, describeComponent(c), c.getPrice(), c.getQuality());
                }
                System.out.println("0) Выход из магазина");
                int choice = readInt("Что купить: ");
                if (choice == 0) {
                    System.out.println("Вы вышли из магазина комплектующих.");
                    return;
                }
                if (choice < 0 || choice > offers.size()) {
                    System.out.println("Нет такого пункта.");
                    continue;
                }

                Component selected = offers.get(choice - 1);
                if (!player.spend(selected.getPrice())) {
                    System.out.println("Недостаточно бюджета.");
                    continue;
                }
                player.getInventory().add(selected);
                System.out.println("Куплен компонент: " + describeComponent(selected));
        }
    }

    public void assembleCar() {
        System.out.println("\n--- Сборка болида ---");
        List<Engine> engines = filter(Engine.class);
        List<Transmission> transmissions = filter(Transmission.class);
        List<Chassis> chassisList = filter(Chassis.class);
        List<Suspension> suspensions = filter(Suspension.class);
        List<Aerodynamics> aerodynamicsList = filter(Aerodynamics.class);
        List<Tyres> tyreInventory = filter(Tyres.class);
        Map<String, Tyres> tyrePackage = collectTyrePackage(tyreInventory);

        if (engines.isEmpty() || transmissions.isEmpty() || chassisList.isEmpty() || suspensions.isEmpty() || aerodynamicsList.isEmpty()) {
            System.out.println("Не хватает комплектующих для сборки полного болида.");
            return;
        }
        if (tyrePackage.size() < 5) {
            System.out.println("Для сборки болида нужен полный комплект шин: SOFT, MEDIUM, HARD, INTERMEDIATE и WET.");
            return;
        }

        Engine engine = choose("двигатель", engines); if (engine == null) return;
        Transmission tr = choose("трансмиссию", transmissions); if (tr == null) return;
        Chassis ch = choose("шасси", chassisList); if (ch == null) return;
        Suspension sus = choose("подвеску", suspensions); if (sus == null) return;
        Aerodynamics aero = choose("аэродинамику", aerodynamicsList); if (aero == null) return;

        if (tr.getCompatibleType() != engine.getType()) { System.out.println("Несовместимость: трансмиссия не подходит к типу двигателя."); return; }
        if (engine.getMass() > ch.getMaxEngineMass()) { System.out.println("Несовместимость: двигатель слишком тяжелый для шасси."); return; }
        if (!sus.getCompatibleClass().equals(ch.getChassisClass())) { System.out.println("Несовместимость: подвеска не подходит к классу шасси."); return; }

        Tyres tyres = buildWeekendTyrePackage(tyrePackage);
        Car car = new Car("Car-" + (player.getCars().size() + 1), engine, tr, ch, sus, aero, tyres);
        player.getCars().add(car);
        player.getInventory().remove(engine);player.getInventory().remove(tr);player.getInventory().remove(ch);
        player.getInventory().remove(sus);player.getInventory().remove(aero);
        tyrePackage.values().forEach(player.getInventory()::remove);
        System.out.println("Болид собран: " + car.getName());
    }
    public void hireStaffMenu() {
        while (true) {
            System.out.println("\n--- Покупка персонала ---");
            System.out.println("1) Технический директор");
            System.out.println("2) Инженер");
            System.out.println("3) Механик");
            System.out.println("4) Электронщик");
            System.out.println("5) Принципал");
            System.out.println("0) Назад");

            int choice = readInt("Выберите категорию: ");
            switch (choice) {
                case 1 -> hireTechnicalDirector();
                case 2 -> hireEngineer();
                case 3 -> hireMechanic();
                case 4 -> hireElectronicsEngineer();
                case 5 -> hirePrincipal();
                case 0 -> { return; }
                default -> System.out.println("Нет такого пункта.");
            }
        }
    }
    public void hireTechnicalDirector() {
        List<TechnicalDirector> candidates = marketService.generateTechnicalDirectorCandidates().stream()
                .filter(candidate -> !isStaffAlreadyHired(candidate.getName()))
                .toList();
        if (candidates.isEmpty()) { System.out.println("Все доступные технические директора уже наняты."); return; }
        TechnicalDirector selected = chooseStaff("технического директора", candidates);
        if (selected == null) return;
        if (!player.spend(selected.getSalary())) { System.out.println("Недостаточно бюджета."); return; }
        player.setTechnicalDirector(selected);
        player.getEngineers().add(selected);
        System.out.println("Нанят технический директор: " + selected.getName());
    }

    public void hireEngineer() {
        List<Engineer> candidates = marketService.generateEngineerCandidates().stream()
                .filter(candidate -> !isStaffAlreadyHired(candidate.getName()))
                .toList();
        if (candidates.isEmpty()) { System.out.println("Все доступные инженеры уже наняты."); return; }
        Engineer selected = chooseStaff("инженера", candidates);
        if (selected == null) return;
        if (!player.spend(selected.getSalary())) { System.out.println("Недостаточно бюджета."); return; }
        player.getEngineers().add(selected);
        System.out.println("Нанят инженер: " + selected.getName());
    }

    public void hireMechanic() {
        List<Mechanic> candidates = marketService.generateMechanicCandidates().stream()
                .filter(candidate -> !isStaffAlreadyHired(candidate.getName()))
                .toList();
        if (candidates.isEmpty()) { System.out.println("Все доступные механики уже наняты."); return; }
        Mechanic selected = chooseStaff("механика", candidates);
        if (selected == null) return;
        if (!player.spend(selected.getSalary())) { System.out.println("Недостаточно бюджета."); return; }
        player.getEngineers().add(selected);
        System.out.println("Нанят механик: " + selected.getName());
    }

    public void hireElectronicsEngineer() {
        List<ElectronicsEngineer> candidates = marketService.generateElectronicsEngineerCandidates().stream()
                .filter(candidate -> !isStaffAlreadyHired(candidate.getName()))
                .toList();
        if (candidates.isEmpty()) { System.out.println("Все доступные электронщики уже наняты."); return; }
        ElectronicsEngineer selected = chooseStaff("электронщика", candidates);
        if (selected == null) return;
        if (!player.spend(selected.getSalary())) { System.out.println("Недостаточно бюджета."); return; }
        player.getEngineers().add(selected);
        System.out.println("Нанят электронщик: " + selected.getName());
    }

    public void hirePrincipal() {
        List<Principal> candidates = marketService.generatePrincipalCandidates().stream()
                .filter(candidate -> !isStaffAlreadyHired(candidate.getName()))
                .toList();
        if (candidates.isEmpty()) { System.out.println("Все доступные принципалы уже наняты."); return; }
        Principal selected = chooseStaff("принципала", candidates);
        if (selected == null) return;
        if (!player.spend(selected.getSalary())) { System.out.println("Недостаточно бюджета."); return; }
        player.setPrincipal(selected);
        System.out.println("Нанят принципал: " + selected.getName());
    }

    private <T extends Staff> T chooseStaff(String role, List<T> candidates) {
        System.out.println("\n--- Школа персонала: " + role + " ---");
        for (int i = 0; i < candidates.size(); i++) {
            Staff e = candidates.get(i);
            System.out.printf("%d) %s | skill=%d | контракт=%d%n", i + 1, e.getName(), e.getSkill(), e.getSalary());
        }
        System.out.println("0) Назад");
        int choice = readInt("Кого нанять: ");
        if (choice <= 0 || choice > candidates.size()) return null;
        return candidates.get(choice - 1);
    }
    public void hirePilot() {
        List<MainDriver> candidates = marketService.generateDriverCandidates().stream()
                .filter(candidate -> !isDriverAlreadyHired(candidate.getName()))
                .toList();
        if (candidates.isEmpty()) { System.out.println("Все доступные пилоты уже наняты."); return; }
        System.out.println("\n--- Школа пилотов ---");
        for (int i = 0; i < candidates.size(); i++) {
            MainDriver d = candidates.get(i);
            System.out.printf("%d) %s | corner=%d straight=%d consistency=%d wet=%d | контракт=%d%n", i + 1,
                    d.getName(), d.getCornerSkill(), d.getStraightSkill(), d.getConsistency(), d.getWetSkill(), d.getContractCost());
        }
        System.out.println("0) Назад");
        int choice = readInt("Кого нанять: ");
        if (choice <= 0 || choice > candidates.size()) return;
        MainDriver selected = candidates.get(choice - 1);
        if (!player.spend(selected.getContractCost())) { System.out.println("Недостаточно бюджета."); return; }
        player.getDrivers().add(selected);
        System.out.println("Нанят пилот: " + selected.getName());
    }

    public void showCars() {
        System.out.println("\n--- Ваши болиды ---");
        if (player.getCars().isEmpty()) { System.out.println("Нет собранных болидов."); return; }
        for (Car car : player.getCars()) {
            String condition;
            if (car.operational()) {
                condition = "исправен";
            } else {
                condition = "неисправен";
            }
            System.out.printf("%s | состояние=%s | средний износ=%.1f%%%n", car.getName(), condition, car.averageWear());
        }
    }

    public void showPilots() {
        System.out.println("\n--- Ваши пилоты ---");
        if (player.getDrivers().isEmpty()) { System.out.println("Пилотов пока нет."); return; }
        for (MainDriver p : player.getDrivers()) {
            System.out.printf("%s | corner=%d straight=%d consistency=%d wet=%d%n", p.getName(), p.getCornerSkill(), p.getStraightSkill(), p.getConsistency(), p.getWetSkill());
        }
    }

    public void showRaceStats(List<RaceResult> history) {
        System.out.println("\n--- Статистика гонок ---");
        if (history.isEmpty()) { System.out.println("Гонок еще не было."); return; }
        int i = 1; for (RaceResult r : history) {
            System.out.printf("Гонка %d: %s | Погода: %s%n", i++, r.getTrackName(), r.getWeather().getTitle());
        }
    }

    public void showOtherResults(List<RaceResult> history) {
        System.out.println("\n--- Подробные результаты ---");
        if (history.isEmpty()) { System.out.println("Нет данных."); return; }
        for (String row : history.get(history.size() - 1).getTable()) System.out.println(row);
    }

    private String describeComponent(Component c) {
        if (c instanceof Engine e) return "Двигатель " + e.getName() + " (" + e.getType() + ", power=" + e.getPower() +", engine_weight =" + e.getMass() + ")";
        if (c instanceof Transmission t) return "Трансмиссия " + t.getName() + " (для " + t.getCompatibleType() + ")";
        if (c instanceof Chassis ch) return "Шасси " + ch.getName() + " (class=" + ch.getChassisClass() + ", max_2engine_weight =" + ch.getMaxEngineMass()+")";
        if (c instanceof Suspension s) return "Подвеска " + s.getName() + " (для " + s.getCompatibleClass() + ")";
        if (c instanceof Aerodynamics a) return "Аэродинамика " + a.getName() + " (downforce=" + a.getDownforce() + ")";
        if (c instanceof Tyres t) return "Шины " + t.getName() + " (" + t.getCompound() + ")";
        return c.getName();
    }

    private boolean isDriverAlreadyHired(String name) {
        return player.getDrivers().stream().anyMatch(driver -> driver.getName().equalsIgnoreCase(name));
    }

    private boolean isStaffAlreadyHired(String name) {
        if (player.getPrincipal() != null && player.getPrincipal().getName().equalsIgnoreCase(name)) {
            return true;
        }
        if (player.getTechnicalDirector() != null && player.getTechnicalDirector().getName().equalsIgnoreCase(name)) {
            return true;
        }
        return player.getEngineers().stream().anyMatch(staff -> staff.getName().equalsIgnoreCase(name));
    }

    private Map<String, Tyres> collectTyrePackage(List<Tyres> tyresList) {
        Map<String, Tyres> packageByCompound = new LinkedHashMap<>();
        for (Tyres tyres : tyresList) {
            packageByCompound.putIfAbsent(tyres.getCompound(), tyres);
        }
        return packageByCompound;
    }

    private Tyres buildWeekendTyrePackage(Map<String, Tyres> tyrePackage) {
        List<Tyres> selectedSets = new ArrayList<>(tyrePackage.values());
        int averagePrice = (int) Math.round(selectedSets.stream().mapToInt(Tyres::getPrice).average().orElse(0));
        int averageQuality = (int) Math.round(selectedSets.stream().mapToInt(Tyres::getQuality).average().orElse(0));
        int averageGrip = (int) Math.round(selectedSets.stream().mapToInt(Tyres::getGrip).average().orElse(0));
        int averageDurability = (int) Math.round(selectedSets.stream().mapToInt(Tyres::getDurability).average().orElse(0));
        int averageRainPerformance = (int) Math.round(selectedSets.stream().mapToInt(Tyres::getRainPerformance).average().orElse(0));
        return new Tyres("Weekend Package", averagePrice, averageQuality, "FULL_SET", averageGrip, averageDurability, averageRainPerformance);
    }

    public <T extends Component> List<T> filter(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component c : player.getInventory()) if (type.isInstance(c) && !c.isDestroyed()) result.add(type.cast(c));
        return result;
    }

    public <T> T choose(String what, List<T> list) {
        System.out.println("Выберите " + what + ":");
        for (int i = 0; i < list.size(); i++) System.out.println((i + 1) + ") " + list.get(i));
        System.out.println("0) Отмена");
        int idx = readInt("Ваш выбор: ");
        if (idx <= 0 || idx > list.size()) return null;
        return list.get(idx - 1);
    }
}
