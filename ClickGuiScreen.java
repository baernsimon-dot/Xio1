package dev.agra.gui.ui;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.ModuleManager;
import dev.agra.gui.core.Setting;
import dev.agra.gui.core.Sounds;
import dev.agra.gui.core.Theme;
import dev.agra.gui.module.hud.InterfaceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private static final Category[] TABS = Category.values();

    private static final int W = 470, H = 272, PAD = 10, SIDE_W = 86, BTN_H = 22, BTN_GAP = 6, TOP = 18;
    private static final int CARD_H = 26, CARD_GAP = 6;
    private static final int SW = 190;                 // settings panel width
    private static final int BIND_H = 18;
    private static final float OPEN_MS = 280f, CLOSE_MS = 190f, TAB_MS = 260f;

    // ---- state ----
    private final long openedAt = System.nanoTime();
    private long closingAt;
    private boolean closing;
    private long lastFrame = System.nanoTime();
    private float alpha = 1f;

    private boolean lastShift = true, lastEsc, lastLeft, lastRight;
    private final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];

    private int selected = 0;
    private long tabChangedAt = 0;
    private float indicatorY = Float.NaN;
    private final float[] hover = new float[TABS.length];
    private final float[] sel = new float[TABS.length];

    private Module settingsModule;
    private float settingsAnim;
    private float settingsScroll;
    private boolean listening;
    private Setting.Num dragging;
    private final Map<Setting, Float> sAnim = new HashMap<>();

    private Object tipTarget;
    private String tipText;
    private long tipSince;

    public ClickGuiScreen() {
        super(Text.literal("agra"));
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        lastLeft = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        lastRight = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        for (int k = 32; k < prevKeys.length; k++) prevKeys[k] = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
        sel[0] = 1f;
        for (Module m : ModuleManager.ALL) m.guiOn = m.isEnabled() ? 1f : 0f;
        Sounds.ui(1.4f, 0.8f);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* custom dim, no blur */ }
    @Override public void close() { requestClose(); }

    private void requestClose() {
        if (closing) return;
        closing = true;
        closingAt = System.nanoTime();
        listening = false;
        dragging = null;
        Sounds.ui(0.8f, 0.7f);
        ModuleManager.save();
    }

    // ---------------- helpers ----------------
    private static float clamp01(float v) { return v < 0 ? 0 : Math.min(v, 1); }
    private static float easeOutCubic(float t) { float f = 1 - t; return 1 - f * f * f; }
    private static float easeOutBack(float t) { float c1 = 1.3f, c3 = c1 + 1; float f = t - 1; return 1 + c3 * f * f * f + c1 * f * f; }

    private int c(int argb) { return Draw.alpha(argb, alpha); }
    private void rr(DrawContext ctx, int x, int y, int w, int h, int r, int argb) { Draw.rrect(ctx, x, y, w, h, r, c(argb)); }
    private void text(DrawContext ctx, String s, int x, int y, int argb) { Draw.text(ctx, s, x, y, c(argb), false); }
    private void textR(DrawContext ctx, String s, int right, int y, int argb) { text(ctx, s, right - Draw.width(s), y, argb); }
    private boolean over(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private float speed() { return (float) InterfaceModule.I.animSpeed.get(); }

    // ---------------- layout ----------------
    private int x0() { return (this.width - W) / 2; }
    private int y0() { return (this.height - H) / 2; }
    private int tabX() { return x0() + PAD; }
    private int tabY(int i) { return y0() + PAD + TOP + i * (BTN_H + BTN_GAP); }
    private int contentX() { return x0() + PAD + SIDE_W + PAD + 4; }
    private int contentW() { return x0() + W - PAD - contentX(); }
    private int cardW() { return (contentW() - CARD_GAP) / 2; }
    private int cardX(int i) { return contentX() + (i % 2) * (cardW() + CARD_GAP); }
    private int cardY(int i) { return y0() + PAD + 24 + (i / 2) * (CARD_H + CARD_GAP); }

    private int spX() { return x0() + W + 8; }
    private int spY() { return y0(); }
    private int rowsTop() { return spY() + PAD + 18 + BIND_H + 8; }
    private int rowsBottom() { return spY() + H - PAD; }
    private int rowH(Setting s) { return s instanceof Setting.Num ? 28 : 18; }
    private int rowsHeight() {
        int h = 0;
        if (settingsModule != null) for (Setting s : settingsModule.settings) h += rowH(s);
        return h;
    }
    private void clampScroll() {
        float max = Math.max(0, rowsHeight() - (rowsBottom() - rowsTop()));
        settingsScroll = Math.max(0, Math.min(max, settingsScroll));
    }

    private interface RowVisitor { void row(Setting s, int y, int h); }
    private void eachRow(RowVisitor v) {
        if (settingsModule == null) return;
        int y = rowsTop() - Math.round(settingsScroll);
        for (Setting s : settingsModule.settings) { int h = rowH(s); v.row(s, y, h); y += h; }
    }

    // ---------------- input (polled) ----------------
    private void pollInput(int mx, int my, float openProgress) {
        long h = MinecraftClient.getInstance().getWindow().getHandle();

        // key scan for keybind listening
        boolean captured = false;
        for (int k = 32; k < prevKeys.length; k++) {
            boolean down = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
            if (listening && down && !prevKeys[k] && settingsModule != null) {
                settingsModule.key = (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_BACKSPACE) ? -1 : k;
                listening = false;
                captured = true;
                Sounds.ui(1.5f, 0.6f);
            }
            prevKeys[k] = down;
        }

        boolean shift = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean esc = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
        if (!captured && !listening) {
            if (shift && !lastShift) requestClose();
            if (esc && !lastEsc) requestClose();
        }
        lastShift = shift;
        lastEsc = esc;

        boolean left = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean ready = !closing && openProgress > 0.7f;
        if (ready && left && !lastLeft) click(mx, my, false);
        if (ready && right && !lastRight) click(mx, my, true);
        lastLeft = left;
        lastRight = right;

        if (dragging != null) {
            if (left) setFromMouse(dragging, mx); else dragging = null;
        }
    }

    private void setFromMouse(Setting.Num n, int mx) {
        int barX = spX() + PAD, barW = SW - 2 * PAD;
        double f = Math.max(0, Math.min(1, (mx - barX) / (double) barW));
        n.set(n.min + f * (n.max - n.min));
    }

    private void click(int mx, int my, boolean right) {
        // settings panel
        if (settingsModule != null && over(mx, my, spX(), spY(), SW, H)) {
            int bindY = spY() + PAD + 18;
            if (over(mx, my, spX() + PAD, bindY, SW - 2 * PAD, BIND_H)) {
                if (right) { settingsModule.key = -1; listening = false; } else listening = !listening;
                Sounds.ui(1.2f, 0.5f);
                return;
            }
            if (my < rowsTop() || my >= rowsBottom()) return;
            eachRow((s, y, h) -> {
                if (my < y || my >= y + h) return;
                if (s instanceof Setting.Bool b) { b.value = !b.value; Sounds.ui(b.value ? 1.4f : 0.9f, 0.5f); }
                else if (s instanceof Setting.Mode m) { m.next(right ? -1 : 1); Sounds.ui(1.1f + m.index * 0.04f, 0.5f); }
                else if (s instanceof Setting.Num n && !right) { dragging = n; setFromMouse(n, mx); }
            });
            return;
        }

        // sidebar tabs
        for (int i = 0; i < TABS.length; i++) {
            if (!right && over(mx, my, tabX(), tabY(i), SIDE_W, BTN_H) && i != selected) {
                selected = i;
                tabChangedAt = System.nanoTime();
                Sounds.ui(0.9f + i * 0.12f, 0.6f);
                return;
            }
        }

        // module cards
        List<Module> mods = ModuleManager.in(TABS[selected]);
        for (int i = 0; i < mods.size(); i++) {
            if (!over(mx, my, cardX(i), cardY(i), cardW(), CARD_H)) continue;
            Module m = mods.get(i);
            if (right) {
                settingsModule = (settingsModule == m) ? null : m;
                settingsScroll = 0;
                listening = false;
                Sounds.ui(1.3f, 0.5f);
            } else if (m.toggleable) {
                m.toggle();
                Sounds.ui(m.isEnabled() ? 1.5f : 0.9f, 0.7f);
            } else {
                settingsModule = m;
                settingsScroll = 0;
                Sounds.ui(1.3f, 0.5f);
            }
            return;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (settingsModule != null && over((int) mouseX, (int) mouseY, spX(), spY(), SW, H)) {
            settingsScroll -= (float) verticalAmount * 14f;
            clampScroll();
            return true;
        }
        return false;
    }

    // ---------------- render ----------------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;

        float p = closing
                ? 1f - clamp01((now - closingAt) / 1e6f / (CLOSE_MS / speed()))
                : clamp01((now - openedAt) / 1e6f / (OPEN_MS / speed()));

        pollInput(mx, my, p);

        if (closing && p <= 0f) {
            MinecraftClient.getInstance().setScreen(null);
            return;
        }

        String style = InterfaceModule.I.openStyle.get();
        float eased = closing ? easeOutCubic(p) : (style.equals("pop") ? easeOutBack(p) : easeOutCubic(p));
        alpha = easeOutCubic(p);

        // background dim
        if (InterfaceModule.I.dim.get()) {
            ctx.fill(0, 0, this.width, this.height, Draw.alpha(0xFF050210, (float) (InterfaceModule.I.dimAmount.get() / 100.0) * alpha));
        }

        // animation smoothing
        float k = 1f - (float) Math.exp(-dt * 16f * speed());
        for (int i = 0; i < TABS.length; i++) {
            boolean hov = !closing && over(mx, my, tabX(), tabY(i), SIDE_W, BTN_H);
            hover[i] += ((hov ? 1f : 0f) - hover[i]) * k;
            sel[i] += ((i == selected ? 1f : 0f) - sel[i]) * k;
        }
        float targetY = tabY(selected);
        if (Float.isNaN(indicatorY)) indicatorY = targetY;
        indicatorY += (targetY - indicatorY) * (1f - (float) Math.exp(-dt * 20f * speed()));
        settingsAnim += ((settingsModule != null ? 1f : 0f) - settingsAnim) * (1f - (float) Math.exp(-dt * 14f * speed()));
        if (settingsAnim < 0.01f && settingsModule == null) settingsAnim = 0f;
        for (Module m : ModuleManager.ALL) {
            m.guiOn += ((m.isEnabled() ? 1f : 0f) - m.guiOn) * k;
            if (m.category != TABS[selected]) m.guiHover += (0f - m.guiHover) * k; // visible cards update in drawMain
        }

        // transform
        float cx = this.width / 2f, cy = this.height / 2f;
        ctx.getMatrices().pushMatrix();
        if (style.equals("pop")) {
            float sc = 0.88f + 0.12f * eased;
            ctx.getMatrices().translate(cx, cy);
            ctx.getMatrices().scale(sc, sc);
            ctx.getMatrices().translate(-cx, -cy);
        } else if (style.equals("slide")) {
            ctx.getMatrices().translate(0f, 22f * (1f - eased));
        }

        drawMain(ctx, mx, my, now, k);
        if (settingsAnim > 0.01f) drawSettings(ctx, mx, my, k);

        ctx.getMatrices().popMatrix();

        drawTooltip(ctx, mx, my, now);
    }

    private void drawMain(DrawContext ctx, int mx, int my, long now, float k) {
        int x0 = x0(), y0 = y0();
        Draw.glass(ctx, x0, y0, W, H, Theme.radius() + 2, Theme.bg(), Theme.edge(), alpha, InterfaceModule.I.gloss.get());

        // title
        for (int i = 0, x = tabX() + 2; i < 4; i++) {
            String ch = "agra".substring(i, i + 1);
            text(ctx, ch, x, y0 + PAD + 2, Theme.gradient(i * 0.3f));
            x += Draw.width(ch);
        }

        // sidebar
        int r = Theme.radius() - 1;
        for (int i = 0; i < TABS.length; i++) {
            rr(ctx, tabX(), tabY(i), SIDE_W, BTN_H, r, Draw.lerp(Theme.btn(), Theme.btnHover(), hover[i]));
        }
        rr(ctx, tabX(), Math.round(indicatorY), SIDE_W, BTN_H, r, Theme.selected());
        for (int i = 0; i < TABS.length; i++) {
            int tc = Draw.lerp(Theme.dim(), Theme.text(), Math.max(sel[i], hover[i] * 0.7f));
            text(ctx, TABS[i].label, tabX() + 9 + Math.round(3 * Math.max(sel[i], hover[i])), tabY(i) + (BTN_H - 8) / 2, tc);
        }

        // divider
        int dx = x0 + PAD + SIDE_W + PAD / 2;
        ctx.fill(dx, y0 + PAD, dx + 1, y0 + H - PAD, c(Theme.edge()));

        // content
        float tp = tabChangedAt == 0 ? 1f : clamp01((now - tabChangedAt) / 1e6f / (TAB_MS / speed()));
        float te = easeOutCubic(tp);
        float saved = alpha;
        alpha = saved * te;
        int slide = Math.round(10 * (1f - te));

        text(ctx, TABS[selected].label, contentX() + slide, y0 + PAD + 2, Theme.text());
        List<Module> mods = ModuleManager.in(TABS[selected]);
        if (mods.isEmpty()) {
            text(ctx, "empty", contentX() + slide, y0 + PAD + 26, Theme.dim());
        }
        tipCandidate = null;
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int x = cardX(i) + slide, y = cardY(i);
            boolean hov = !closing && over(mx, my, cardX(i), y, cardW(), CARD_H) && !(settingsModule != null && over(mx, my, spX(), spY(), SW, H));
            m.guiHover += ((hov ? 1f : 0f) - m.guiHover) * k * 2f;
            if (hov) tipCandidate = m;

            int base = Draw.lerp(Theme.btn(), Theme.selected(), m.guiOn);
            int col = Draw.lerp(base, Theme.btnHover(), m.guiHover * (1f - m.guiOn * 0.5f));
            rr(ctx, x, y, cardW(), CARD_H, r, col);
            if (InterfaceModule.I.gloss.get()) {
                ctx.fillGradient(x + r, y + 1, x + cardW() - r, y + CARD_H / 2, c(0x1CFFFFFF), c(0x00FFFFFF));
            }
            int tc = Draw.lerp(Theme.dim(), Theme.text(), Math.max(m.guiOn, m.guiHover * 0.8f));
            text(ctx, m.name, x + 9, y + (CARD_H - 8) / 2, tc);

            // three dots = "right click for settings"
            if (!m.settings.isEmpty()) {
                int dxp = x + cardW() - 10;
                int dc = Draw.lerp(Theme.dim(), Theme.text(), Math.max(m.guiHover, settingsModule == m ? 1f : 0f));
                for (int d = 0; d < 3; d++) rr(ctx, dxp, y + CARD_H / 2 - 5 + d * 4, 2, 2, 1, dc);
            }
            if (settingsModule == m) rr(ctx, x, y + CARD_H - 2, cardW(), 2, 1, Theme.gradient(0f));
        }
        if (!mods.isEmpty()) text(ctx, "left click: toggle   right click: settings", contentX() + slide, y0 + H - PAD - 8, Theme.dim());
        alpha = saved;
    }

    private Object tipCandidate;

    private void drawSettings(DrawContext ctx, int mx, int my, float k) {
        float saved = alpha;
        alpha = saved * settingsAnim;
        int off = Math.round(14 * (1f - settingsAnim));
        int px = spX() - off, py = spY();
        int r = Theme.radius() - 1;
        Module m = settingsModule;
        if (m == null) { alpha = saved; return; }

        Draw.glass(ctx, px, py, SW, H, Theme.radius() + 2, Theme.bg(), Theme.edge(), alpha, InterfaceModule.I.gloss.get());
        text(ctx, m.name, px + PAD, py + PAD + 1, Theme.gradient(0.2f));
        textR(ctx, "settings", px + SW - PAD, py + PAD + 1, Theme.dim());

        // keybind row
        int by = py + PAD + 18;
        boolean bindHover = over(mx, my, px + PAD, by, SW - 2 * PAD, BIND_H);
        rr(ctx, px + PAD, by, SW - 2 * PAD, BIND_H, r, listening ? Theme.selected() : (bindHover ? Theme.btnHover() : Theme.btn()));
        text(ctx, "keybind", px + PAD + 7, by + 5, Theme.text());
        textR(ctx, listening ? "press a key..." : KeyNames.of(m.key), px + SW - PAD - 7, by + 5, listening ? Theme.text() : Theme.dim());
        if (bindHover) tipCandidate = "Click, then press a key to bind it. Right click or ESC/Backspace clears the bind.";

        // rows (clipped + scrollable)
        clampScroll();
        ctx.enableScissor(px, rowsTop(), px + SW, rowsBottom());
        final int fpx = px;
        eachRow((s, y, h) -> {
            if (y + h < rowsTop() || y > rowsBottom()) return;
            int left = fpx + PAD, right = fpx + SW - PAD;
            boolean hov = over(mx, my, fpx, y, SW, h) && my >= rowsTop() && my < rowsBottom();
            if (hov) tipCandidate = s.description;
            float a = sAnim.getOrDefault(s, 0f);

            if (s instanceof Setting.Bool b) {
                a += ((b.value ? 1f : 0f) - a) * k * 1.5f;
                text(ctx, s.name, left, y + 5, hov ? Theme.text() : Theme.dim());
                int sx = right - 24;
                rr(ctx, sx, y + 4, 24, 11, 5, Draw.lerp(Theme.btn(), Theme.selected(), a));
                rr(ctx, sx + 2 + Math.round(a * 13), y + 6, 7, 7, 3, Draw.lerp(Theme.dim(), 0xFFFFFFFF, a));
            } else if (s instanceof Setting.Mode mo) {
                text(ctx, s.name, left, y + 5, hov ? Theme.text() : Theme.dim());
                String v = mo.get();
                int vw = Draw.width(v) + 10;
                rr(ctx, right - vw, y + 2, vw, 14, 4, hov ? Theme.btnHover() : Theme.btn());
                text(ctx, v, right - vw + 5, y + 5, Theme.text());
            } else if (s instanceof Setting.Num n) {
                text(ctx, s.name, left, y + 3, hov ? Theme.text() : Theme.dim());
                textR(ctx, n.display(), right, y + 3, Theme.text());
                int barW = SW - 2 * PAD;
                float f = (float) ((n.value - n.min) / (n.max - n.min));
                a += (f - a) * k * 2f;
                rr(ctx, left, y + 17, barW, 4, 2, Theme.btn());
                rr(ctx, left, y + 17, Math.max(3, Math.round(barW * a)), 4, 2, Theme.gradient(0.3f));
                rr(ctx, left + Math.round(barW * a) - 3, y + 15, 6, 8, 3, 0xFFFFFFFF);
            }
            sAnim.put(s, a);
        });
        ctx.disableScissor();

        // scroll hint
        if (rowsHeight() > rowsBottom() - rowsTop()) {
            float max = rowsHeight() - (rowsBottom() - rowsTop());
            int trackH = rowsBottom() - rowsTop();
            int thumb = Math.max(14, Math.round(trackH * (trackH / (float) rowsHeight())));
            int ty = rowsTop() + Math.round((trackH - thumb) * (settingsScroll / max));
            rr(ctx, px + SW - 4, ty, 2, thumb, 1, Theme.edge());
        }
        alpha = saved;
    }

    private void drawTooltip(DrawContext ctx, int mx, int my, long now) {
        Object target = null;
        String text = null;
        if (tipCandidate instanceof Module m) { target = m; text = m.description; }
        else if (tipCandidate instanceof String s) { target = s; text = s; }

        if (target != tipTarget) { tipTarget = target; tipSince = now; }
        tipText = text;
        if (closing || tipText == null || (now - tipSince) / 1e6f < 220f) return;

        float a = clamp01(((now - tipSince) / 1e6f - 220f) / 140f) * alpha;
        int maxW = 170;
        List<String> lines = wrap(tipText, maxW);
        int w = 0;
        for (String l : lines) w = Math.max(w, Draw.width(l));
        w += 12;
        int h = lines.size() * 11 + 8;
        int x = Math.min(mx + 12, this.width - w - 4);
        int y = Math.min(my + 14, this.height - h - 4);
        Draw.glass(ctx, x, y, w, h, 4, Theme.bg(), Theme.edge(), a, false);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, Draw.alpha(0xFF000000, a * 0.35f));
        for (int i = 0; i < lines.size(); i++) Draw.text(ctx, lines.get(i), x + 6, y + 5 + i * 11, Draw.alpha(Theme.text(), a), false);
    }

    private List<String> wrap(String s, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (Draw.width(test) > maxW && line.length() > 0) { out.add(line.toString()); line = new StringBuilder(word); }
            else line = new StringBuilder(test);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
}
