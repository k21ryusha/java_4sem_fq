package game;

import components.*;
import economic.MarketService;
import race_weekend.RaceResult;
import staff.MainDriver;
import staff.Staff;
import staff.TeamManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PlayerController implements IntInput {
    private final Scanner scanner;
    private final TeamManager player;
    private final MarketService marketService;

    public PlayerController(Scanner scanner, TeamManager player, MarketService marketService) {
        this.scanner = scanner;
        this.player = player;
        this.marketService = marketService;
    }

    @Override
    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try { return Integer.parseInt(line);} catch (NumberFormatException e) { System.out.println("Введите число."); }
        }
    }

    public void buyComponents() {
        List<Component> offers = marketService.generateComponentOffers();
        System.out.println("\n--- Рынок компонентов ---");
        for (int i = 0; i < offers.size(); i++) {
            Component c = offers.get(i);
            System.out.printf("%d) %s | цена: %d | качество: %d%n", i + 1, describeComponent(c), c.getPrice(), c.getQuality());
        }
        System.out.println("0) Назад");
        int choice = readInt("Что купить: ");
        if (choice <= 0 || choice > offers.size()) return;
        Component selected = offers.get(choice - 1);
        if (!player.spend(selected.getPrice())) {
            System.out.println("Недостаточно бюджета.");
            return;
        }
        player.getInventory().add(selected);
        System.out.println("Куплен компонент: " + describeComponent(selected));
    }

    public void assembleCar() {
        System.out.println("\n--- Сборка болида ---");
        List<Engine> engines = filter(Engine.class);
        List<Transmission> transmissions = filter(Transmission.class);
        List<Chassis> chassisList = filter(Chassis.class);
        List<Suspension> suspensions = filter(Suspension.class);
        List<Aerodynamics> aerodynamicsList = filter(Aerodynamics.class);
        List<Tyres> tyresList = filter(Tyres.class);

        if (engines.isEmpty() || transmissions.isEmpty() || chassisList.isEmpty() || suspensions.isEmpty() || aerodynamicsList.isEmpty() || tyresList.isEmpty()) {
            System.out.println("Не хватает комплектующих для сборки полного болида.");
            return;
        }

        Engine engine = choose("двигатель", engines); if (engine == null) return;
        Transmission tr = choose("трансмиссию", transmissions); if (tr == null) return;
        Chassis ch = choose("шасси", chassisList); if (ch == null) return;
        Suspension sus = choose("подвеску", suspensions); if (sus == null) return;
        Aerodynamics aero = choose("аэродинамику", aerodynamicsList); if (aero == null) return;
        Tyres tyres = choose("шины", tyresList); if (tyres == null) return;

        if (tr.getCompatibleType() != engine.getType()) { System.out.println("Несовместимость: трансмиссия не подходит к типу двигателя."); return; }
        if (engine.getMass() > ch.getMaxEngineMass()) { System.out.println("Несовместимость: двигатель слишком тяжелый для шасси."); return; }
        if (!sus.getCompatibleClass().equals(ch.getChassisClass())) { System.out.println("Несовместимость: подвеска не подходит к классу шасси."); return; }

        Car car = new Car("Car-" + (player.getCars().size() + 1), engine, tr, ch, sus, aero, tyres);
        player.getCars().add(car);
        player.getInventory().remove(engine);player.getInventory().remove(tr);player.getInventory().remove(ch);
        player.getInventory().remove(sus);player.getInventory().remove(aero);player.getInventory().remove(tyres);
        System.out.println("Болид собран: " + car.getName());
    }

    public void hireEngineer() {
        List<Staff> candidates = marketService.generateEngineerCandidates();
        System.out.println("\n--- Школа персонала ---");
        for (int i = 0; i < candidates.size(); i++) {
            Staff e = candidates.get(i);
            System.out.printf("%d) %s | skill=%d | контракт=%d%n", i + 1, e.getName(), e.getSkill(), e.getSalary());
        }
        System.out.println("0) Назад");
        int choice = readInt("Кого нанять: ");
        if (choice <= 0 || choice > candidates.size()) return;
        Staff selected = candidates.get(choice - 1);
        if (!player.spend(selected.getSalary())) { System.out.println("Недостаточно бюджета."); return; }
        player.getEngineers().add(selected);
        System.out.println("Нанят инженер: " + selected.getName());
    }

    public void hirePilot() {
        List<MainDriver> candidates = marketService.generateDriverCandidates();
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
            System.out.printf("%s | состояние=%s | средний износ=%.1f%%%n", car.getName(), car.operational() ? "исправен" : "неисправен", car.averageWear());
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
        if (c instanceof Engine e) return "Двигатель " + e.getName() + " (" + e.getType() + ", power=" + e.getPower() + ")";
        if (c instanceof Transmission t) return "Трансмиссия " + t.getName() + " (для " + t.getCompatibleType() + ")";
        if (c instanceof Chassis ch) return "Шасси " + ch.getName() + " (class=" + ch.getChassisClass() + ")";
        if (c instanceof Suspension s) return "Подвеска " + s.getName() + " (для " + s.getCompatibleClass() + ")";
        if (c instanceof Aerodynamics a) return "Аэродинамика " + a.getName() + " (downforce=" + a.getDownforce() + ")";
        if (c instanceof Tyres t) return "Шины " + t.getName() + " (" + t.getCompound() + ")";
        return c.getName();
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
