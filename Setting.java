package dev.agra.gui.core;

import java.util.Locale;

public abstract class Setting {
    public final String name, description;

    protected Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public abstract String serialize();
    public abstract void deserialize(String s);

    // ---------------------------------------------------------------
    public static class Bool extends Setting {
        public boolean value;
        public Bool(String n, String d, boolean v) { super(n, d); value = v; }
        public boolean get() { return value; }
        @Override public String serialize() { return Boolean.toString(value); }
        @Override public void deserialize(String s) { value = Boolean.parseBoolean(s); }
    }

    public static class Num extends Setting {
        public double value;
        public final double min, max, step;
        public Num(String n, String d, double v, double min, double max, double step) {
            super(n, d); this.value = v; this.min = min; this.max = max; this.step = step;
        }
        public double get() { return value; }
        public int i() { return (int) Math.round(value); }
        public float f() { return (float) value; }
        public void set(double v) {
            v = Math.round(v / step) * step;
            v = Math.round(v * 10000.0) / 10000.0;
            value = Math.max(min, Math.min(max, v));
        }
        public String display() {
            return step >= 1 ? Integer.toString(i()) : String.format(Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", value);
        }
        @Override public String serialize() { return Double.toString(value); }
        @Override public void deserialize(String s) { try { set(Double.parseDouble(s)); } catch (NumberFormatException ignored) {} }
    }

    public static class Mode extends Setting {
        public final String[] options;
        public int index;
        public Mode(String n, String d, int idx, String... options) { super(n, d); this.options = options; this.index = idx; }
        public String get() { return options[index]; }
        public boolean is(String s) { return options[index].equals(s); }
        public void next(int dir) { index = (index + dir + options.length) % options.length; }
        @Override public String serialize() { return options[index]; }
        @Override public void deserialize(String s) {
            for (int i = 0; i < options.length; i++) if (options[i].equals(s)) index = i;
        }
    }
}
