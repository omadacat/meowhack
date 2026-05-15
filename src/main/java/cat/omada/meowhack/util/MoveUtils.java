package cat.omada.meowhack.util;

import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public final class MoveUtils {
    private MoveUtils() {}

    /** Fires a firework rocket. Returns -1 on failure, 200 on success, or the hotbar slot
     *  of the temporarily swapped elytra if {@code elytraRequired} forced a swap. */
    public static int firework(MinecraftClient mc, boolean elytraRequired) {
        int elytraSwapSlot = -1;
        if (elytraRequired && !mc.player.getInventory().getStack(SlotUtils.ARMOR_START + 2).isOf(Items.ELYTRA)) {
            FindItemResult itemResult = InvUtils.findInHotbar(Items.ELYTRA);
            if (!itemResult.found()) return -1;

            elytraSwapSlot = itemResult.slot();
            InvUtils.swap(itemResult.slot(), true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swapBack();
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }

        FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!itemResult.found()) return -1;

        if (itemResult.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            mc.player.swingHand(Hand.OFF_HAND);
        } else {
            InvUtils.swap(itemResult.slot(), true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            InvUtils.swapBack();
        }
        return elytraSwapSlot != -1 ? elytraSwapSlot : 200;
    }

    public static void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    public static int emptyInvSlots(MinecraftClient mc) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.AIR) count++;
        }
        return count;
    }

    public static int totalInvCount(MinecraftClient mc, Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    /** Converts a Minecraft yaw (degrees) to a horizontal unit direction vector. */
    public static Vec3d yawToDirection(double yaw) {
        double rad = Math.toRadians(yaw);
        return new Vec3d(-Math.sin(rad), 0, Math.cos(rad));
    }

    /** Moves {@code pos} by {@code distance} in the direction of {@code yaw}. */
    public static Vec3d positionInDirection(Vec3d pos, double yaw, double distance) {
        return pos.add(yawToDirection(yaw).multiply(distance));
    }

    /** Perpendicular distance from {@code point} to the line through {@code start} along {@code direction} (XZ only). */
    public static double distancePointToDirection(Vec3d point, Vec3d direction, @Nullable Vec3d start) {
        if (start == null) start = Vec3d.ZERO;
        Vec3d rel = point.subtract(start).multiply(1, 0, 1);
        Vec3d dir = direction.multiply(1, 0, 1);
        double proj = rel.dotProduct(dir) / dir.lengthSquared();
        return rel.subtract(dir.multiply(proj)).length();
    }

    /** Rounds {@code yaw} to the nearest 45° axis (returns 0–315). */
    public static double angleOnAxis(double yaw) {
        if (yaw < 0) yaw += 360;
        return Math.round(yaw / 45.0) * 45 % 360;
    }

    /** Smooth angle interpolation towards {@code target} from {@code current}. */
    public static float smoothRotation(double current, double target, double scale) {
        return (float) (current + angleDifference(target, current) * scale);
    }

    /** Signed shortest difference from {@code current} to {@code target} (degrees). */
    public static double angleDifference(double target, double current) {
        double diff = (target - current + 180) % 360 - 180;
        return diff < -180 ? diff + 360 : diff;
    }
}
