package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Block MERCHANT_BLOCK = new TransformableMerchantBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());
    public static final BlockItem MERCHANT_BLOCK_ITEM = new PhoraBlockItem(MERCHANT_BLOCK, new Item.Properties());

    private ModBlocks() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK,
                new ResourceLocation(Aldeanoforaflut.MODID, "transformable_merchant_block"), MERCHANT_BLOCK);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(Aldeanoforaflut.MODID, "transformable_merchant_block"), MERCHANT_BLOCK_ITEM);
    }
}
