package test;

import components.*;
import economic.MarketService;
import game.PlayerController;
import org.junit.Test;
import staff.TeamManager;

import java.util.Random;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;

public class CannotAssembleCarWhenComponentsMissingTest {
    @Test
    public void cannotAssembleCarWhenSomeComponentsMissing() {
        TeamManager manager = new TeamManager("Test", 1_000_000, 10);
        MarketService market = new MarketService(new Random(5));

        manager.getInventory().add(new Engine("E", 100, 90, EngineType.TURBO, 850, 150));
        manager.getInventory().add(new Transmission("T", 100, 90, EngineType.TURBO, 85));
        manager.getInventory().add(new Chassis("C", 100, 90, 180, "LIGHT", 80));
        manager.getInventory().add(new Suspension("S", 100, 90, "LIGHT", 82));
        manager.getInventory().add(new Aerodynamics("A", 100, 90, 84));

        PlayerController controller = new PlayerController(new Scanner(""), manager, market);
        controller.assembleCar();

        assertTrue(manager.getCars().isEmpty());
    }
}
