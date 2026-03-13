package staff;

public abstract class Staff {
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
