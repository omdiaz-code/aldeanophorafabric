package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PhoraBlockRenderer extends GeoBlockRenderer<PhoraBlockEntity> {
    public PhoraBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new PhoraBlockModel());
    }

    @Override
    public boolean shouldRenderOffScreen(PhoraBlockEntity blockEntity) {
        return true;
    }
}
