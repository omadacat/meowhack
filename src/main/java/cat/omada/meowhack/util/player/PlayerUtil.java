package cat.omada.meowhack.util.player;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerUtil {
    public static boolean isHotbarKeysPressed() {
        if (mc.options == null) return false;
        for (var key : mc.options.hotbarKeys) {
            if (key.isPressed()) return true;
        }
        return false;
    }
}
