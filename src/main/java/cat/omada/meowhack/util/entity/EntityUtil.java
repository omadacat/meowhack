package cat.omada.meowhack.util.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.passive.PassiveEntity;

public class EntityUtil {
    public static float getHealth(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.getHealth() + living.getAbsorptionAmount();
        }
        return 0;
    }

    public static boolean isMonster(Entity e) {
        return e instanceof HostileEntity;
    }

    public static boolean isNeutral(Entity e) {
        return e instanceof Angerable && !(e instanceof HostileEntity);
    }

    public static boolean isPassive(Entity e) {
        return e instanceof PassiveEntity;
    }
}
