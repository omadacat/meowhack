package cat.omada.meowhack.modules.player;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Printer extends Module {
    public Printer() {
        super(Categories.Player, "printer", "Prints schematics in the world");
    }
}
