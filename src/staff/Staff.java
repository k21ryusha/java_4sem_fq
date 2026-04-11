package staff;

import java.io.Serial;
import java.io.Serializable;

public abstract class Staff implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    protected final String name;
    protected final int salary;
    protected final int skill;

    protected Staff(String name, int salary, int skill) {
        this.name = name;
        this.salary = salary;
        this.skill = skill;
    }

    public String getName() { return name; }
    public int getSalary() { return salary; }
    public int getSkill() { return skill; }
}
