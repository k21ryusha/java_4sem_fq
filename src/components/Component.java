package components;

public abstract class Component {
    final String name;
    final int price;
    final int quality;
    double wear;
    boolean destroyed;

    Component(String name, int price, int quality) {
        this.name = name;
        this.price = price;
        this.quality = quality;
        this.wear = 0;
        this.destroyed = false;
    }

    double effectiveQuality() {
        return destroyed ? 0 : quality * (1 - wear / 100.0);
    }

    String condition() {
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
