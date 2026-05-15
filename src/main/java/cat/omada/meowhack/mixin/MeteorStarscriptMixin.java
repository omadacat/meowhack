package cat.omada.meowhack.mixin;

import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MeteorStarscript.class, remap = false)
public class MeteorStarscriptMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private static void onInit(CallbackInfo ci) {
        MeteorStarscript.ss.getGlobals().get("meteor").get().getMap().set("name", "GNU/Meteor Client");
    }
}
