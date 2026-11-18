package dev.agra.gui.module.exploits;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hold left click on a block with any pickaxe: the blocks around it (3x3 or 5x5, in the plane you are facing)
 * are mined right after it, one after the other, using the normal mining progress of your tool.
 */
public class Drill extends Module {
    private static Drill I;

    private final Setting.Num size = num("size", "1 = 3x3, 2 = 5x5.", 1, 1, 2, 1);
    private final Setting.Bool pickaxeOnly = bool("pickaxe only", "Only drill while holding a pickaxe.", true);
    private final Setting.Num reach = num("reach", "Max distance to blocks that get drilled.", 5.0, 3.0, 6.0, 0.5);

    private final Deque<BlockPos> queue = new ArrayDeque<>();
    private Direction face = Direction.NORTH;
    private boolean active;
    private int stuck;

    public Drill() {
        super("drill", "Mines 3x3 around the block you break with any pickaxe.", Category.EXPLOITS);
        I = this;
    }

    /** Read by the mixin: while true, vanilla block breaking is skipped and the drill drives it. */
    public static boolean busy() { return I != null && I.isEnabled() && I.active; }

    @Override
    public void onDisable() {
        queue.clear();
        if (active && mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
        active = false;
    }

    @Override
    public void onTick() {
        if (mc.interactionManager == null) return;
        boolean held = mc.currentScreen == null
                && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean tool = !pickaxeOnly.get() || Registries.ITEM.getId(mc.player.getMainHandStack().getItem()).getPath().endsWith("_pickaxe");

        if (!held || !tool) {
            if (active) mc.interactionManager.cancelBlockBreaking();
            active = false;
            queue.clear();
            stuck = 0;
            return;
        }
        active = true;

        if (queue.isEmpty() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK
                && mc.crosshairTarget instanceof BlockHitResult bhr) {
            face = bhr.getSide();
            build(bhr.getBlockPos());
            stuck = 0;
        }

        BlockPos head = queue.peek();
        if (head == null) return;
        BlockState st = mc.world.getBlockState(head);
        if (st.isAir() || st.getHardness(mc.world, head) < 0 || ++stuck > 200) {
            queue.poll();
            stuck = 0;
            return;
        }
        mc.interactionManager.updateBlockBreakingProgress(head, face);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void build(BlockPos center) {
        int r = size.i();
        double max = reach.get() * reach.get();
        queue.add(center);
        for (int a = -r; a <= r; a++) {
            for (int b = -r; b <= r; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos p = switch (face.getAxis()) {
                    case X -> center.add(0, a, b);
                    case Y -> center.add(a, 0, b);
                    default -> center.add(a, b, 0);
                };
                BlockState st = mc.world.getBlockState(p);
                if (st.isAir() || !st.getFluidState().isEmpty() || st.getHardness(mc.world, p) < 0) continue;
                if (mc.player.getEyePos().squaredDistanceTo(p.toCenterPos()) > max) continue;
                queue.add(p);
            }
        }
    }
}
