package cat.omada.meowhack.mixin;

import cat.omada.meowhack.modules.movement.ElytraBounce;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class FreePitchCameraMixin {

    @ModifyArgs(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V")
    )
    private void modifyCameraRotation(Args args) {
        ElytraBounce eb = Modules.get().get(ElytraBounce.class);
        if (eb == null || !eb.isFreePitchEnabled()) return;

        float entityPitch = (float) args.get(1);
        eb.cameraPitch += entityPitch - eb.prevEntityPitch;
        eb.cameraPitch = Math.max(-90f, Math.min(90f, eb.cameraPitch));
        eb.prevEntityPitch = entityPitch;
        args.set(1, eb.cameraPitch);
    }
}
