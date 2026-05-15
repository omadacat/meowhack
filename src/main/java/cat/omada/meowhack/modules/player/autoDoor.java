package cat.omada.meowhack.modules.player;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class autoDoor extends Module {
    public autoDoor() {
        super(Categories.Player, "auto-door", "Automatically opens doors when you approach them");
    }
}
