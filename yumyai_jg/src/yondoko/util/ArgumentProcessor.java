package yondoko.util;

public class ArgumentProcessor {
    String[] args;
    int currentIndex = 0;

    public ArgumentProcessor(String[] args) {
        this.args = args;
    }

    public String getString() {
        currentIndex++;
        return args[currentIndex-1];
    }

    public int getInt() {
        currentIndex++;
        return Integer.valueOf(args[currentIndex - 1]);
    }

    public float getFloat() {
        currentIndex++;
        return Float.valueOf(args[currentIndex - 1]);
    }

    public double getDouble() {
        currentIndex++;
        return Double.valueOf(args[currentIndex - 1]);
    }

    public long getLong() {
        currentIndex++;
        return Long.valueOf(args[currentIndex - 1]);
    }

    public boolean getBoolean() {
        currentIndex++;
        return Boolean.valueOf(args[currentIndex - 1]);
    }

    public int getNumRemainingArguments() {
        return args.length - currentIndex;
    }

    public boolean hasArguments() {
        return getNumRemainingArguments() >= 1;
    }
}
