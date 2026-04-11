package test;

import game.PlayerController;
import org.junit.Test;
import staff.TeamManager;

import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class PurchaseAndHiringTest {
    @Test
    public void purchaseAndHiringAreAppliedCorrectly() {
        TeamManager manager = new TeamManager("Test", 500_000, 10);
        TestSupport.FixedMarketService market = new TestSupport.FixedMarketService();

        PlayerController buyController = new PlayerController(new Scanner("1\n0\n"), manager, market);
        buyController.buyComponents();

        assertEquals(1, manager.getInventory().size());
        assertEquals(400_000, manager.getBudget());

        PlayerController hireController = new PlayerController(new Scanner("1\n"), manager, market);
        hireController.hireEngineer();

        assertEquals(1, manager.getEngineers().size());
        assertEquals(320_000, manager.getBudget());
    }

    @Test
    public void sameEngineerAndDriverCannotBeBoughtTwice() {
        TeamManager manager = new TeamManager("Test", 500_000, 10);
        TestSupport.FixedMarketService market = new TestSupport.FixedMarketService();

        PlayerController hireEngineerController = new PlayerController(new Scanner("1\n"), manager, market);
        hireEngineerController.hireEngineer();
        PlayerController duplicateEngineerController = new PlayerController(new Scanner(""), manager, market);
        duplicateEngineerController.hireEngineer();

        PlayerController hireDriverController = new PlayerController(new Scanner("1\n"), manager, market);
        hireDriverController.hirePilot();
        PlayerController duplicateDriverController = new PlayerController(new Scanner(""), manager, market);
        duplicateDriverController.hirePilot();

        assertEquals(1, manager.getEngineers().size());
        assertEquals(1, manager.getDrivers().size());
        assertEquals(330_000, manager.getBudget());
    }
}
