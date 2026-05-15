package cat.omada.meowhack.modules.player;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class InventoryFix extends Module {
    public InventoryFix() {
        super(Categories.Player, "inventory-fix", "Fixes inventory issues like ghost items and disappearing items");
    }
}
