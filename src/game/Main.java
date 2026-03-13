package game;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GameSession session = new GameSession(scanner);
        session.run();
    }
}