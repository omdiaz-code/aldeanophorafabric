package aldeanoforaflut.aldeanoforaflut;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Aldeanoforaflut implements ModInitializer {
    public static final String MODID = "aldeanoforaflut";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final CreativeModeTab PHORA_TAB = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModBlocks.MERCHANT_BLOCK_ITEM))
            .title(Component.translatable("itemGroup.aldeanoforaflut.phora_tab"))
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.MERCHANT_BLOCK_ITEM);
                output.accept(ModItems.DILITIO);
                output.accept(ModItems.PHORA_TABLET);
                output.accept(ModItems.COPPER_KEY);
            })
            .build();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Aldeano Phora (Fabric)");

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, new ResourceLocation(MODID, "phora_tab"), PHORA_TAB);
        ModItems.register();
        ModBlocks.register();
        ModEntities.register();
        ModSounds.register();
        ModNetworking.registerServerMessages();
    }
}
