package aldeanoforaflut.aldeanoforaflut;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<PhoraBlockEntity> PHORA = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            new ResourceLocation(Aldeanoforaflut.MODID, "phora"),
            FabricBlockEntityTypeBuilder.create(PhoraBlockEntity::new, ModBlocks.MERCHANT_BLOCK).build()
    );

    private ModBlockEntities() {
    }

    public static void register() {
        // Loading this class registers the Phora block entity type.
    }
}
