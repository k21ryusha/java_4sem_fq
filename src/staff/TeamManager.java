package staff;

import components.Car;
import components.Component;

import java.util.ArrayList;
import java.util.List;

public class TeamManager {
    private final String teamName;
    private int budget;
    private int reputation;

    private final List<Component> inventory = new ArrayList<>();
    private final List<Car> cars = new ArrayList<>();
    private final List<MainDriver> drivers = new ArrayList<>();
    private final List<Staff> engineers = new ArrayList<>();

    private Principal principal;
    private TechnicalDirector technicalDirector;

    public TeamManager(String teamName, int budget, int reputation) {
        this.teamName = teamName;
        this.budget = budget;
        this.reputation = reputation;
    }

    public String getTeamName() { return teamName; }
    public int getBudget() { return budget; }
    public int getReputation() { return reputation; }

    public void addBudget(int value) { budget += value; }
    public void addReputation(int value) { reputation += value; }

    public boolean spend(int value) {
        if (budget < value) return false;
        budget -= value;
        return true;
    }

    public List<Component> getInventory() { return inventory; }
    public List<Car> getCars() { return cars; }
    public List<MainDriver> getDrivers() { return drivers; }
    public List<Staff> getEngineers() { return engineers; }

    public Principal getPrincipal() { return principal; }
    public void setPrincipal(Principal principal) { this.principal = principal; }

    public TechnicalDirector getTechnicalDirector() { return technicalDirector; }
    public void setTechnicalDirector(TechnicalDirector technicalDirector) { this.technicalDirector = technicalDirector; }
}
