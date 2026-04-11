package test;

import components.*;
import economic.MarketService;
import game.PlayerController;
import org.junit.Test;
import staff.TeamManager;

import java.util.Random;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;

public class ComponentCompatibilityTest {
    @Test
    public void componentCompatibilityIsValidatedDuringAssembly() {
        TeamManager manager = new TeamManager("Test", 1_000_000, 10);
        MarketService market = new MarketService(new Random(1));

        manager.getInventory().add(new Engine("E", 100, 90, EngineType.TURBO, 850, 150));
        manager.getInventory().add(new Transmission("T", 100, 90, EngineType.ATMOSPHERIC, 85));
        manager.getInventory().add(new Chassis("C", 100, 90, 180, "LIGHT", 80));
        manager.getInventory().add(new Suspension("S", 100, 90, "LIGHT", 82));
        manager.getInventory().add(new Aerodynamics("A", 100, 90, 84));
        manager.getInventory().add(new Tyres("Ty", 100, 90, "SOFT", 88, 75, 65));

        PlayerController controller = new PlayerController(new Scanner("1\n1\n1\n1\n1\n1\n"), manager, market);
        controller.assembleCar();

        assertTrue(manager.getCars().isEmpty());
    }
}
