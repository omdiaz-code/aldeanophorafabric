package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.world.effect.MobEffects;

public final class MerchantAiHandler {
    private MerchantAiHandler() {
    }

    public static void clearLegacyInvisibility(TransformableMerchantEntity entity) {
        if (!entity.level().isClientSide
                && entity.hasEffect(MobEffects.INVISIBILITY)) {
            entity.removeEffect(MobEffects.INVISIBILITY);
        }
    }
}