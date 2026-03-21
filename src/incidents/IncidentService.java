package incidents;

import components.Car;
import components.Component;
import game.PlayerController;
import staff.TeamManager;

import java.util.List;
import java.util.Random;

public class IncidentService {
    private final Random random;

    public IncidentService(Random random) {
        this.random = random;
    }

    public void preRaceMaintenance(TeamManager player, Car car, PlayerController input) {
        for (Component c : car.components()) {
            if (c.getWear() > 50 && !c.isDestroyed()) {
                System.out.printf("Компонент %s имеет высокий износ %.1f%%. Починить за %d? (1-да, 0-нет)%n", c.getName(), c.getWear(), 30_000);
                int choice = input.readInt("Ваш выбор: ");
                if (choice == 1 && player.spend(30_000) && !player.getEngineers().isEmpty()) {
                    c.setWear(Math.max(0, c.getWear() - 35));
                    System.out.println("Компонент частично отремонтирован.");
                }
            }
        }
    }

    public boolean checkIncident(Car car) {
        double highWearCount = car.components().stream().filter(c -> c.getWear() > 50).count();
        double chance = 0.05 + highWearCount * 0.12;
        return random.nextDouble() < chance;
    }

    public Component checkMechanicalFailure(Car car) {
        List<Component> candidates = car.components().stream().filter(c -> c.getWear() > 40 && !c.isDestroyed()).toList();
        if (candidates.isEmpty()) {
            return null;
        }

        double highWearCount = candidates.stream().filter(c -> c.getWear() > 55).count();
        double severeWearCount = candidates.stream().filter(c -> c.getWear() > 75).count();
        double chance = 0.02 + highWearCount * 0.05 + severeWearCount * 0.04;
        if (random.nextDouble() >= chance) {
            return null;
        }

        Component broken = candidates.get(random.nextInt(candidates.size()));
        broken.setDestroyed(true);
        return broken;
    }

    public void applyWear(Car car, int laps) {
        for (Component c : car.components()) {
            if (!c.isDestroyed()) {
                c.setWear(Math.min(100, c.getWear() + 6 + laps * 0.4 + random.nextDouble() * 6));
            }
        }
    }
}
