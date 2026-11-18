package dev.agra.gui.ui;

import org.lwjgl.glfw.GLFW;

public final class KeyNames {
    private KeyNames() {}

    public static String of(int key) {
        if (key < 0) return "none";
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "space";
            case GLFW.GLFW_KEY_ESCAPE: return "esc";
            case GLFW.GLFW_KEY_ENTER: return "enter";
            case GLFW.GLFW_KEY_TAB: return "tab";
            case GLFW.GLFW_KEY_BACKSPACE: return "backspace";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "l-shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "r-shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "l-ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "r-ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "l-alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "r-alt";
            case GLFW.GLFW_KEY_UP: return "up";
            case GLFW.GLFW_KEY_DOWN: return "down";
            case GLFW.GLFW_KEY_LEFT: return "left";
            case GLFW.GLFW_KEY_RIGHT: return "right";
            case GLFW.GLFW_KEY_END: return "end";
            case GLFW.GLFW_KEY_HOME: return "home";
            case GLFW.GLFW_KEY_INSERT: return "insert";
            case GLFW.GLFW_KEY_DELETE: return "delete";
            default:
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) return "f" + (key - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(key, 0);
        return n != null ? n : "key " + key;
    }
}
