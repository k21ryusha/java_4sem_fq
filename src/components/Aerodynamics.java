package components;

public class Aerodynamics extends Component {
    private final int downforce;

    public Aerodynamics(String name, int price, int quality, int downforce) {
        super(name, price, quality);
        this.downforce = downforce;
    }

    public int getDownforce() {
        return downforce;
    }
}
