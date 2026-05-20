package cat.omada.meowhack.util.player;

import net.minecraft.item.Item;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InventoryUtil {
    public static int count(Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }
}
