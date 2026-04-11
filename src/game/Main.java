package game;

import save.GameState;
import save.SaveManager;
import save.SaveSlot;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SaveManager saveManager = new SaveManager();
        System.out.println("=== Симулятор гоночной команды ===");

        String playerName = readPlayerName(scanner);
        List<SaveSlot> saves = saveManager.listSaves(playerName);

        GameSession session;
        if (saves.isEmpty()) {
            System.out.println("Сохранений для игрока " + playerName + " пока нет. Запускается новая игра.");
            session = new GameSession(scanner, playerName, saveManager);
        } else {
            session = chooseSession(scanner, saveManager, playerName, saves);
        }
        session.run();
    }

    private static String readPlayerName(Scanner scanner) {
        while (true) {
            System.out.print("Введите имя игрока: ");
            String playerName = scanner.nextLine().trim();
            if (!playerName.isEmpty()) {
                return playerName;
            }
            System.out.println("Имя игрока не должно быть пустым.");
        }
    }

    private static GameSession chooseSession(Scanner scanner,
                                             SaveManager saveManager,
                                             String playerName,
                                             List<SaveSlot> saves) {
        while (true) {
            System.out.println("\nНайдены сохранения для игрока " + playerName + ".");
            System.out.println("1) Новая игра");
            System.out.println("2) Загрузить сохранение");
            System.out.print("Выберите пункт: ");

            String line = scanner.nextLine().trim();
            if ("1".equals(line)) {
                return new GameSession(scanner, playerName, saveManager);
            }
            if ("2".equals(line)) {
                SaveSlot slot = chooseSaveSlot(scanner, saves);
                GameState state = saveManager.load(slot);
                return new GameSession(scanner, state, saveManager);
            }
            System.out.println("Нет такого пункта.");
        }
    }

    private static SaveSlot chooseSaveSlot(Scanner scanner, List<SaveSlot> saves) {
        while (true) {
            System.out.println("\nДоступные сохранения:");
            for (int i = 0; i < saves.size(); i++) {
                System.out.printf("%d) %s%n", i + 1, saves.get(i).getDisplayName());
            }
            System.out.print("Какое сохранение загрузить: ");

            String line = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(line);
                if (choice >= 1 && choice <= saves.size()) {
                    return saves.get(choice - 1);
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Введите номер сохранения из списка.");
        }
    }
}
