package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PhoraBlockModel extends GeoModel<PhoraBlockEntity> {
    @Override
    public ResourceLocation getModelResource(PhoraBlockEntity object) {
        return new ResourceLocation(Aldeanoforaflut.MODID, "geo/phora.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PhoraBlockEntity object) {
        return new ResourceLocation(Aldeanoforaflut.MODID, "textures/entity/phora_versionpro.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PhoraBlockEntity animatable) {
        return new ResourceLocation(Aldeanoforaflut.MODID, "animations/phora.animation.json");
    }
}
