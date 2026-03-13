package components;

public abstract class Component {
    protected final String name;
    protected final int price;
    protected final int quality;
    protected double wear;
    protected boolean destroyed;

    public Component(String name, int price, int quality) {
        this.name = name;
        this.price = price;
        this.quality = quality;
        this.wear = 0;
        this.destroyed = false;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getQuality() {
        return quality;
    }

    public double getWear() {
        return wear;
    }

    public void setWear(double wear) {
        this.wear = wear;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public double effectiveQuality() {
        return destroyed ? 0 : quality * (1 - wear / 100.0);
    }

    public String condition() {
        if (destroyed) {
            return "РАЗРУШЕН";
        }
        return String.format("износ %.1f%%", wear);
    }

    @Override
    public String toString() {
        return name + " [качество=" + quality + ", " + condition() + "]";
    }
}
