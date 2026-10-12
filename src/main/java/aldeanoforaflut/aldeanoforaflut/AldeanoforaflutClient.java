package aldeanoforaflut.aldeanoforaflut;

import aldeanoforaflut.aldeanoforaflut.entity.client.ClientNetworking;
import aldeanoforaflut.aldeanoforaflut.entity.client.PhoraMerchantScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;

public class AldeanoforaflutClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.TRANSFORMABLE_MERCHANT, PhoraRenderer::new);
        ClientNetworking.register();

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof MerchantScreen merchantScreen
                    && client.player != null
                    && merchantScreen.getTitle().getString().startsWith(TransformableMerchantEntity.TITLE_PREFIX)) {
                client.setScreen(new PhoraMerchantScreen(
                        merchantScreen.getMenu(),
                        client.player.getInventory(),
                        merchantScreen.getTitle()));
            }
        });
    }
}