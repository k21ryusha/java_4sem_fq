package game;

import components.Car;
import race_weekend.RaceSimulator;
import race_weekend.Track;
import race_weekend.Weather;
import save.GameState;
import save.SaveManager;
import save.SaveSlot;
import staff.MainDriver;
import staff.TeamManager;
import weapons.MeleeWeapon;
import weapons.RangedWeapon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SurvivalModeSession {
    private static final int FINISH_DISTANCE = 1000;

    private final Scanner scanner;
    private final SaveManager saveManager;
    private final Random random;
    private final RaceSimulator raceSimulator;
    private final PlayerController playerController;
    private final List<Track> trackLibrary;
    private final GameState currentMainState;

    public SurvivalModeSession(Scanner scanner,
                               SaveManager saveManager,
                               Random random,
                               RaceSimulator raceSimulator,
                               PlayerController playerController,
                               List<Track> trackLibrary,
                               GameState currentMainState) {
        this.scanner = scanner;
        this.saveManager = saveManager;
        this.random = random;
        this.raceSimulator = raceSimulator;
        this.playerController = playerController;
        this.trackLibrary = trackLibrary;
        this.currentMainState = currentMainState;
    }

    public void run(SurvivalProgress existingProgress, BotController botController) {
        SurvivalProgress progress = existingProgress;
        if (progress == null) {
            progress = createNewProgress(botController);
            if (progress == null) {
                return;
            }
        } else {
            System.out.println("\nВозобновлен режим выживания: " + progress.getTrackName());
        }

        play(progress);
    }

    private SurvivalProgress createNewProgress(BotController botController) {
        GameState baseline = currentMainState.deepCopy();
        TeamManager survivalPlayer = baseline.deepCopy().getPlayer();

        List<SurvivalParticipant> participants = new ArrayList<>();
        List<Car> activeCars = survivalPlayer.getCars().stream().filter(Car::operational).toList();
        List<MainDriver> activeDrivers = survivalPlayer.getDrivers();
        if (activeCars.isEmpty() || activeDrivers.isEmpty()) {
            System.out.println("Для режима выживания нужен хотя бы один исправный болид и один пилот.");
            return null;
        }

        int playerEntrants = Math.min(activeCars.size(), activeDrivers.size());
        if (playerEntrants == 0) {
            System.out.println("Не удалось сформировать состав игрока для режима выживания.");
            return null;
        }

        System.out.println("\n=== Режим выживания ===");
        System.out.println("Сейчас вы настроите вооружение для своих болидов. Это вооружение существует только внутри развлекательного режима.");

        for (int i = 0; i < playerEntrants; i++) {
            MainDriver driver = activeDrivers.get(i);
            Car car = activeCars.get(i);
            SurvivalParticipant participant = new SurvivalParticipant(
                    driver.getName() + " (" + survivalPlayer.getTeamName() + ")",
                    survivalPlayer.getTeamName(),
                    true,
                    survivalPlayer,
                    driver,
                    car,
                    0
            );
            configurePlayerWeapons(participant);
            participants.add(participant);
        }

        int botIndex = 0;
        for (TeamManager bot : botController.getBotTeams()) {
            MainDriver botDriver = new MainDriver("BOT-" + (++botIndex), 0,
                    72 + random.nextInt(18), 72 + random.nextInt(18),
                    70 + random.nextInt(20), 68 + random.nextInt(20), 0);
            Car botCar = createBotCar(botIndex);
            SurvivalParticipant participant = new SurvivalParticipant(
                    botDriver.getName() + " (" + bot.getTeamName() + ")",
                    bot.getTeamName(),
                    false,
                    bot,
                    botDriver,
                    botCar,
                    40 + random.nextInt(120)
            );
            equipBotWeapons(participant);
            participants.add(participant);
        }

        String trackName;
        if (trackLibrary.isEmpty()) {
            trackName = "Survival Arena";
        } else {
            trackName = trackLibrary.get(random.nextInt(trackLibrary.size())).getName();
        }
        return new SurvivalProgress(baseline, participants, trackName, FINISH_DISTANCE, 1, 0);
    }

    private void play(SurvivalProgress progress) {
        System.out.println("Трасса режима выживания: " + progress.getTrackName());
        while (true) {
            List<SurvivalParticipant> turnOrder = buildTurnOrder(progress.getParticipants());
            if (turnOrder.isEmpty()) {
                System.out.println("Все участники выбыли. Режим завершен.");
                return;
            }
            if (progress.getTurnIndex() == 0) {
                System.out.println("\n--- Раунд " + progress.getRoundNumber() + " ---");
                printOrder(turnOrder, progress.getFinishDistance());
            }
            if (progress.getTurnIndex() >= turnOrder.size()) {
                progress.setTurnIndex(0);
                progress.incrementRoundNumber();
                continue;
            }

            SurvivalParticipant actor = turnOrder.get(progress.getTurnIndex());
            if (!actor.isActive()) {
                progress.setTurnIndex(progress.getTurnIndex() + 1);
                continue;
            }

            if (actor.isPlayerControlled()) {
                if (!handlePlayerTurn(progress, actor)) {
                    return;
                }
            } else {
                handleBotTurn(progress, actor);
            }

            String endMessage = evaluateEndState(progress);
            if (endMessage != null) {
                System.out.println(endMessage);
                return;
            }
            progress.setTurnIndex(progress.getTurnIndex() + 1);
        }
    }

    private boolean handlePlayerTurn(SurvivalProgress progress, SurvivalParticipant actor) {
        System.out.println("\nХод игрока: " + actor.getEntryName() + " | дистанция=" + actor.getProgress()
                + "/" + progress.getFinishDistance() + " | оружие=" + actor.totalWeapons());
        while (true) {
            System.out.println("1) Попытка обгона");
            System.out.println("2) Атака");
            System.out.println("3) Ручное сохранение режима");
            System.out.println("4) Выйти из режима выживания");
            int choice = playerController.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> {
                    attemptOvertake(actor, progress);
                    return true;
                }
                case 2 -> {
                    if (actor.totalWeapons() == 0) {
                        System.out.println("На этом болиду нет оружия.");
                        continue;
                    }
                    performPlayerAttack(actor, progress);
                    return true;
                }
                case 3 -> saveSurvivalProgress(progress);
                case 4 -> {
                    System.out.println("Вы вышли из режима выживания. Основная карьера не изменена.");
                    return false;
                }
                default -> System.out.println("Нет такого пункта.");
            }
        }
    }

    private void handleBotTurn(SurvivalProgress progress, SurvivalParticipant actor) {
        List<SurvivalParticipant> aliveOpponents = progress.getParticipants().stream()
                .filter(candidate -> candidate != actor && candidate.isActive())
                .toList();
        if (aliveOpponents.isEmpty()) {
            return;
        }

        boolean attacked = false;
        if (actor.totalWeapons() > 0 && random.nextDouble() < 0.55) {
            attacked = performBotAttack(actor, aliveOpponents, progress.getParticipants());
        }
        if (!attacked) {
            advanceParticipant(actor, calculateAdvanceDistance(actor, progress.getTrackName()));
            System.out.println(actor.getEntryName() + " пытается прорваться вперед.");
        }
    }

    private void performPlayerAttack(SurvivalParticipant actor, SurvivalProgress progress) {
        if (!actor.getMeleeWeapons().isEmpty()) {
            for (MeleeWeapon weapon : actor.getMeleeWeapons()) {
                List<SurvivalParticipant> targets = findAdjacentTargets(actor, progress.getParticipants());
                if (targets.isEmpty()) {
                    System.out.println("Для ближней атаки рядом нет целей.");
                    continue;
                }
                SurvivalParticipant target = chooseTarget("Выберите цель для " + weapon.getName(), targets);
                if (target != null) {
                    resolveAttack(actor, target, weapon.getAccuracy(), weapon.getName());
                }
            }
        }
        if (actor.getRangedWeapon() != null) {
            List<SurvivalParticipant> targets = progress.getParticipants().stream()
                    .filter(candidate -> candidate != actor && candidate.isActive())
                    .toList();
            if (targets.isEmpty()) {
                return;
            }
            SurvivalParticipant target = chooseTarget("Выберите цель для " + actor.getRangedWeapon().getName(), targets);
            if (target != null) {
                resolveAttack(actor, target, actor.getRangedWeapon().getAccuracy(), actor.getRangedWeapon().getName());
            }
        }
    }

    private boolean performBotAttack(SurvivalParticipant actor,
                                     List<SurvivalParticipant> aliveOpponents,
                                     List<SurvivalParticipant> allParticipants) {
        if (!actor.getMeleeWeapons().isEmpty()) {
            List<SurvivalParticipant> adjacentTargets = findAdjacentTargets(actor, allParticipants);
            if (!adjacentTargets.isEmpty()) {
                resolveAttack(actor, adjacentTargets.get(random.nextInt(adjacentTargets.size())),
                        actor.getMeleeWeapons().get(0).getAccuracy(),
                        actor.getMeleeWeapons().get(0).getName());
                return true;
            }
        }
        if (actor.getRangedWeapon() != null) {
            List<SurvivalParticipant> playerTargets = aliveOpponents.stream()
                    .filter(SurvivalParticipant::isPlayerControlled)
                    .toList();
            List<SurvivalParticipant> availableTargets;
            if (playerTargets.isEmpty()) {
                availableTargets = aliveOpponents;
            } else {
                availableTargets = playerTargets;
            }
            resolveAttack(actor,
                    availableTargets.get(random.nextInt(availableTargets.size())),
                    actor.getRangedWeapon().getAccuracy(),
                    actor.getRangedWeapon().getName());
            return true;
        }
        return false;
    }

    private void resolveAttack(SurvivalParticipant attacker, SurvivalParticipant target, double accuracy, String weaponName) {
        if (random.nextDouble() <= accuracy) {
            target.setEliminated(true);
            System.out.println(attacker.getEntryName() + " атакует " + weaponName + " и выбивает " + target.getEntryName() + " из гонки.");
        } else {
            System.out.println(attacker.getEntryName() + " промахивается из " + weaponName + " по " + target.getEntryName() + ".");
        }
    }

    private void attemptOvertake(SurvivalParticipant actor, SurvivalProgress progress) {
        int move = calculateAdvanceDistance(actor, progress.getTrackName());
        if (random.nextDouble() <= calculateOvertakeChance(actor, progress.getTrackName())) {
            advanceParticipant(actor, move);
            System.out.println(actor.getEntryName() + " успешно продвигается на " + move + " единиц дистанции.");
        } else {
            advanceParticipant(actor, Math.max(20, move / 3));
            System.out.println(actor.getEntryName() + " не смог обогнать, но сохраняет темп.");
        }
    }

    private void saveSurvivalProgress(SurvivalProgress progress) {
        GameState state = new GameState(
                currentMainState.getPlayerName(),
                currentMainState.getPlayer(),
                currentMainState.getRaceHistory(),
                currentMainState.getDriverPoints(),
                currentMainState.getTeamPoints(),
                currentMainState.getTrackLibrary(),
                currentMainState.getSeasonCalendar(),
                currentMainState.getChampionshipRound(),
                currentMainState.isParcFermeLocked(),
                currentMainState.isSeasonSetupCompleted(),
                progress
        );
        SaveSlot slot = saveManager.saveManual(state);
        System.out.println("Ручное сохранение режима создано: " + slot.getDisplayName());
    }

    private String evaluateEndState(SurvivalProgress progress) {
        List<SurvivalParticipant> aliveParticipants = progress.getParticipants().stream()
                .filter(participant -> !participant.isEliminated())
                .collect(Collectors.toList());
        List<SurvivalParticipant> alivePlayerParticipants = aliveParticipants.stream()
                .filter(SurvivalParticipant::isPlayerControlled)
                .toList();
        if (alivePlayerParticipants.isEmpty()) {
            return "Все ваши болиды уничтожены. Режим выживания проигран.";
        }
        if (aliveParticipants.size() == alivePlayerParticipants.size()) {
            return "Все боты уничтожены. Вы победили в режиме выживания.";
        }

        SurvivalParticipant leader = aliveParticipants.stream()
                .max(Comparator.comparingInt(SurvivalParticipant::getProgress))
                .orElse(null);
        if (leader != null && leader.getProgress() >= progress.getFinishDistance()) {
            leader.setFinished(true);
            if (leader.isPlayerControlled()) {
                return "Ваш болид первым достиг финиша. Победа в режиме выживания.";
            }
            return leader.getEntryName() + " первым достиг финиша. Режим выживания проигран.";
        }
        return null;
    }

    private List<SurvivalParticipant> buildTurnOrder(List<SurvivalParticipant> participants) {
        return participants.stream()
                .filter(participant -> !participant.isEliminated())
                .sorted(Comparator.comparingInt(SurvivalParticipant::getProgress).reversed()
                        .thenComparing(participant -> participant.getEntryName().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private void printOrder(List<SurvivalParticipant> participants, int finishDistance) {
        System.out.println("Порядок на трассе:");
        for (int i = 0; i < participants.size(); i++) {
            SurvivalParticipant participant = participants.get(i);
            String type;
            if (participant.isPlayerControlled()) {
                type = "игрок";
            } else {
                type = "бот";
            }
            System.out.printf("%d) %s | %s | %d/%d%n", i + 1, participant.getEntryName(), type,
                    participant.getProgress(), finishDistance);
        }
    }

    private SurvivalParticipant chooseTarget(String prompt, List<SurvivalParticipant> targets) {
        System.out.println(prompt + ":");
        for (int i = 0; i < targets.size(); i++) {
            System.out.println((i + 1) + ") " + targets.get(i).getEntryName());
        }
        System.out.println("0) Отмена");
        int choice = playerController.readInt("Цель: ");
        if (choice <= 0 || choice > targets.size()) {
            return null;
        }
        return targets.get(choice - 1);
    }

    private List<SurvivalParticipant> findAdjacentTargets(SurvivalParticipant actor, List<SurvivalParticipant> participants) {
        List<SurvivalParticipant> order = buildTurnOrder(participants);
        int index = order.indexOf(actor);
        List<SurvivalParticipant> targets = new ArrayList<>();
        if (index > 0) {
            targets.add(order.get(index - 1));
        }
        if (index >= 0 && index < order.size() - 1) {
            targets.add(order.get(index + 1));
        }
        return targets.stream().filter(candidate -> candidate != actor && candidate.isActive()).toList();
    }

    private void configurePlayerWeapons(SurvivalParticipant participant) {
        int carMass = participant.getCar().estimatedCombatMass();
        List<MeleeWeapon> meleeOptions = compatibleMeleeWeapons(carMass);
        List<RangedWeapon> rangedOptions = compatibleRangedWeapons(carMass);

        System.out.println("\nНастройка вооружения для " + participant.getEntryName()
                + " | масса болида=" + carMass);
        for (int slot = 0; slot < 2; slot++) {
            MeleeWeapon selected = chooseOptionalWeapon("Оружие ближнего боя #" + (slot + 1), meleeOptions);
            if (selected != null) {
                participant.getMeleeWeapons().add(selected);
            }
        }
        participant.setRangedWeapon(chooseOptionalWeapon("Оружие дальнего боя", rangedOptions));
    }

    private void equipBotWeapons(SurvivalParticipant participant) {
        int carMass = participant.getCar().estimatedCombatMass();
        List<MeleeWeapon> meleeOptions = compatibleMeleeWeapons(carMass);
        List<RangedWeapon> rangedOptions = compatibleRangedWeapons(carMass);
        participant.getMeleeWeapons().add(meleeOptions.get(random.nextInt(meleeOptions.size())));
        if (random.nextDouble() < 0.65) {
            participant.getMeleeWeapons().add(meleeOptions.get(random.nextInt(meleeOptions.size())));
        }
        if (random.nextDouble() < 0.85) {
            participant.setRangedWeapon(rangedOptions.get(random.nextInt(rangedOptions.size())));
        }
    }

    private <T> T chooseOptionalWeapon(String title, List<T> weapons) {
        System.out.println(title + ":");
        for (int i = 0; i < weapons.size(); i++) {
            System.out.println((i + 1) + ") " + weapons.get(i));
        }
        System.out.println("0) Не устанавливать");
        int choice = playerController.readInt("Ваш выбор: ");
        if (choice <= 0 || choice > weapons.size()) {
            return null;
        }
        return weapons.get(choice - 1);
    }

    private List<MeleeWeapon> compatibleMeleeWeapons(int carMass) {
        return List.of(
                new MeleeWeapon("Таранный модуль", carMass + 40, 0.62),
                new MeleeWeapon("Роторные клинки", carMass + 15, 0.54),
                new MeleeWeapon("Электрошоковый бампер", carMass + 60, 0.57)
        ).stream().filter(weapon -> weapon.supportsMass(carMass)).toList();
    }

    private List<RangedWeapon> compatibleRangedWeapons(int carMass) {
        return List.of(
                new RangedWeapon("Импульсная пушка", carMass + 50, 0.44),
                new RangedWeapon("Ракетный блок", carMass + 25, 0.47),
                new RangedWeapon("ЭМИ-излучатель", carMass + 70, 0.41)
        ).stream().filter(weapon -> weapon.supportsMass(carMass)).toList();
    }

    private Car createBotCar(int index) {
        String suffix = "BOT-" + index;
        return new Car(
                "BotCar-" + suffix,
                new components.Engine("Engine-" + suffix, 0, 82 + random.nextInt(12), components.EngineType.TURBO, 820 + random.nextInt(80), 140 + random.nextInt(16)),
                new components.Transmission("Transmission-" + suffix, 0, 80 + random.nextInt(12), components.EngineType.TURBO, 78 + random.nextInt(12)),
                new components.Chassis("Chassis-" + suffix, 0, 80 + random.nextInt(12), 185, "SURVIVAL", 76 + random.nextInt(14)),
                new components.Suspension("Suspension-" + suffix, 0, 80 + random.nextInt(12), "SURVIVAL", 76 + random.nextInt(14)),
                new components.Aerodynamics("Aero-" + suffix, 0, 80 + random.nextInt(12), 76 + random.nextInt(14)),
                new components.Tyres("Tyres-" + suffix, 0, 80 + random.nextInt(12), "SURVIVAL", 76 + random.nextInt(14), 80, 75)
        );
    }

    private int calculateAdvanceDistance(SurvivalParticipant participant, String trackName) {
        Track track = new Track(trackName, 5.0, 50, 14, 4, 20);
        double lapTime = raceSimulator.simulatePracticeLap(
                participant.getTeam(),
                participant.getCar(),
                participant.getDriver(),
                track,
                Weather.DRY
        );
        int pace = (int) Math.round(155 - lapTime + random.nextDouble() * 28);
        return Math.max(25, pace);
    }

    private double calculateOvertakeChance(SurvivalParticipant participant, String trackName) {
        Track track = new Track(trackName, 5.0, 50, 14, 4, 20);
        double lapTime = raceSimulator.simulatePracticeLap(
                participant.getTeam(),
                participant.getCar(),
                participant.getDriver(),
                track,
                Weather.DRY
        );
        double normalized = Math.max(0.18, Math.min(0.82, (125 - lapTime) / 70.0));
        return normalized;
    }

    private void advanceParticipant(SurvivalParticipant participant, int move) {
        participant.addProgress(move);
    }
}
