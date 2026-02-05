package xenus.xenaddons.settings;

public class NumberSetting extends Setting<Double> {
    private final double min, max, increment;

    public NumberSetting(String name, double defaultValue, double min, double max, double increment) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getIncrement() { return increment; }

    @Override
    public void setValue(Double value) {
        double val = Math.round(value / increment) * increment;
        this.value = Math.max(min, Math.min(max, val));
    }
}
