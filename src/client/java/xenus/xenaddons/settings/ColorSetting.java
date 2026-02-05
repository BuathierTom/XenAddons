package xenus.xenaddons.settings;

public class ColorSetting extends Setting<Integer> {
    public float[] hsv = new float[3];
    public int alpha = 255;
    public boolean expanded = false;

    public ColorSetting(String name, int defaultColor) {
        super(name, defaultColor != 0 ? defaultColor : 0xFFA020F0);
        java.awt.Color c = new java.awt.Color(this.value, true);
        java.awt.Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsv);
        this.alpha = c.getAlpha();
    }

    public void setHSV(float h, float s, float v) {
        this.hsv[0] = h;
        this.hsv[1] = s;
        this.hsv[2] = v;
        updateValue();
    }

    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
        updateValue();
    }

    private void updateValue() {
        int rgb = java.awt.Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 0x00FFFFFF;
        this.setValue((alpha << 24) | rgb);
    }

    @Override
    public void setValue(Integer value) {
        if (value == null) return;
        super.setValue(value);
        java.awt.Color c = new java.awt.Color(value, true);
        java.awt.Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsv);
        this.alpha = c.getAlpha();
    }
}
