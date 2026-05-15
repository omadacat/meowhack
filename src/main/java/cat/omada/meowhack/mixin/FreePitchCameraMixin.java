package cat.omada.meowhack.mixin;

//import cat.omada.meowhack.modules.movement.ElytraBounce;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.Camera;
import net.minecraft.world.BlockView;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreePitchCameraMixin {
/*/
    @Shadow private float yaw;
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean invertedView, float tickDelta, CallbackInfo ci) {
        ElytraBounce eb = Modules.get().get(ElytraBounce.class);
        if (eb != null && eb.isFreePitchEnabled()) {
            setRotation(this.yaw, eb.cameraPitch);
        }
    } */
}
