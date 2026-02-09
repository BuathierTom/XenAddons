package xenus.xenaddons.features.impl.dungeons.secretroutes.multistorage;

public class Tuple<Y, Z> {
    private Y one;
    private Z two;

    public Tuple(Y one, Z two) {
        this.one = one;
        this.two = two;
    }

    public Y getOne() {
        return one;
    }

    public void setOne(Y one) {
        this.one = one;
    }

    public Z getTwo() {
        return two;
    }

    public void setTwo(Z two) {
        this.two = two;
    }
}
