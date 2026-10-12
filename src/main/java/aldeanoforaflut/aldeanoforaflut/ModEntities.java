package aldeanoforaflut.aldeanoforaflut;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.Villager;

public final class ModEntities {
    public static final EntityType<TransformableMerchantEntity> TRANSFORMABLE_MERCHANT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            new ResourceLocation(Aldeanoforaflut.MODID, "transformable_merchant"),
            FabricEntityTypeBuilder.create(MobCategory.MISC, TransformableMerchantEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0F, 1.0F))
                    .build()
    );

    private ModEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(TRANSFORMABLE_MERCHANT, Villager.createAttributes().build());
    }
}