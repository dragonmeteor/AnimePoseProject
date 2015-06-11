package yumyai.gfx.ver01;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BindingStack {
    protected final int maxDepth;

    protected final List<Map<String, Object>> stack =
            new ArrayList<Map<String, Object>>();

    protected int stackTop;

    public BindingStack() {
        this(128);
    }

    public BindingStack(int maxDepth) {
        this.maxDepth = maxDepth;

        if (maxDepth < 1) {
            throw new RuntimeException("maximum binding stack depth must be "
                    + "more than 0.");
        }

        for (int i = 0; i < maxDepth; i++) {
            stack.add(new HashMap<String, Object>());
        }

        stackTop = 0;
    }

    public void pushFrame() {
        if (stackTop == maxDepth) {
            throw new RuntimeException("frame count has reached maximum!"
                    + " no more frame to push!");
        } else {
            stackTop++;
            stack.get(stackTop).clear();
        }
    }

    public void popFrame() {
        if (stackTop == 0) {
            throw new RuntimeException("attempt to pop the bottommost frame");
        } else {
            stackTop--;
        }
    }

    public Object get(String name) {
        int position = stackTop;
        while (position >= 0) {
            Map<String, Object> bindings = stack.get(position);
            if (bindings.containsKey(name)) {
                return bindings.get(name);
            }
            position--;
        }
        throw new RuntimeException("binding for variable '" + name
                + "' not found.");
    }

    public void set(String name, Object value) {
        if (stackTop < 0) {
            throw new RuntimeException("no frame in the stack");
        } else {
            Map<String, Object> bindings = stack.get(stackTop);
            bindings.put(name, value);
        }
    }
}
