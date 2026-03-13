package staff;

public abstract class FreeAgent {
    protected final String name;
    protected final int contractCost;

    protected FreeAgent(String name, int contractCost) {
        this.name = name;
        this.contractCost = contractCost;
    }

    public String getName() { return name; }
    public int getContractCost() { return contractCost; }
}
