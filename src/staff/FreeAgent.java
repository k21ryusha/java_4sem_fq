package staff;

import java.io.Serial;
import java.io.Serializable;

public abstract class FreeAgent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    protected final String name;
    protected final int contractCost;

    protected FreeAgent(String name, int contractCost) {
        this.name = name;
        this.contractCost = contractCost;
    }

    public String getName() { return name; }
    public int getContractCost() { return contractCost; }
}
