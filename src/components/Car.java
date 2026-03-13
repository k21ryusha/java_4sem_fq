package components;

import java.util.ArrayList;
import java.util.List;

public class Car {
    private final String name;
    private final Engine engine;
    private final Transmission transmission;
    private final Chassis chassis;
    private final Suspension suspension;
    private final Aerodynamics aerodynamics;
    private final Tyres tyres;

    public Car(String name, Engine engine, Transmission transmission, Chassis chassis, Suspension suspension, Aerodynamics aerodynamics, Tyres tyres) {
        this.name = name;
        this.engine = engine;
        this.transmission = transmission;
        this.chassis = chassis;
        this.suspension = suspension;
        this.aerodynamics = aerodynamics;
        this.tyres = tyres;
    }

    public String getName() { return name; }
    public Engine getEngine() { return engine; }
    public Transmission getTransmission() { return transmission; }
    public Chassis getChassis() { return chassis; }
    public Suspension getSuspension() { return suspension; }
    public Aerodynamics getAerodynamics() { return aerodynamics; }
    public Tyres getTyres() { return tyres; }

    public boolean operational() {
        return components().stream().noneMatch(Component::isDestroyed);
    }

    public List<Component> components() {
        List<Component> list = new ArrayList<>();
        list.add(engine);
        list.add(transmission);
        list.add(chassis);
        list.add(suspension);
        list.add(aerodynamics);
        list.add(tyres);
        return list;
    }

    public double averageWear() {
        return components().stream().mapToDouble(Component::getWear).average().orElse(0);
    }

    @Override
    public String toString() {
        return name + " (износ " + String.format("%.1f", averageWear()) + "%)";
    }
}
