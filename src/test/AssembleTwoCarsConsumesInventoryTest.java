package test;

import components.*;
import economic.MarketService;
import game.PlayerController;
import org.junit.Test;
import staff.Engineer;
import staff.MainDriver;
import staff.TeamManager;

import java.util.Random;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class AssembleTwoCarsConsumesInventoryTest {
    @Test
    public void assembleTwoCarsConsumesInventory() {
        TeamManager manager = new TeamManager("Test", 1_000_000, 10);
        MarketService market = new MarketService(new Random(1));

        manager.getInventory().add(new Engine("E", 100, 90, EngineType.ATMOSPHERIC, 850, 150));
        manager.getInventory().add(new Transmission("T", 100, 90, EngineType.ATMOSPHERIC, 85));
        manager.getInventory().add(new Chassis("C", 100, 90, 180, "LIGHT", 80));
        manager.getInventory().add(new Suspension("S", 100, 90, "LIGHT", 82));
        manager.getInventory().add(new Aerodynamics("A", 100, 90, 84));
        manager.getInventory().addAll(TestSupport.createWeekendTyreSets("1"));
        manager.getInventory().add(new Engine("E", 100, 90, EngineType.TURBO, 850, 150));
        manager.getInventory().add(new Transmission("T", 100, 90, EngineType.TURBO, 85));
        manager.getInventory().add(new Chassis("C", 100, 90, 180, "LIGHT", 80));
        manager.getInventory().add(new Suspension("S", 100, 90, "LIGHT", 82));
        manager.getInventory().add(new Aerodynamics("A", 100, 90, 84));
        manager.getInventory().addAll(TestSupport.createWeekendTyreSets("2"));
        manager.getEngineers().add(new Engineer("Pop", 100, 10));
        manager.getEngineers().add(new Engineer("Pip", 150, 9));
        manager.getDrivers().add(new MainDriver("POOp", 100, 100, 100, 100, 100,0));
        manager.getDrivers().add(new MainDriver("Pip", 100, 100, 100, 100, 100,0));

        PlayerController controller = new PlayerController(new Scanner("1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n"), manager, market);

        controller.assembleCar();
        controller.assembleCar();

        assertEquals(2, manager.getCars().size());
        assertEquals(0, manager.getInventory().size());
    }
}
