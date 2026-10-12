package aldeanoforaflut.aldeanoforaflut;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PhoraRenderer extends GeoEntityRenderer<TransformableMerchantEntity> {
    public PhoraRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PhoraModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(TransformableMerchantEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
