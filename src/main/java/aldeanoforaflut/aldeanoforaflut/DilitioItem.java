package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DilitioItem extends Item {

    private static final String[] LEGACY_TAGS = {
            "RequiredLevel", "PhoraPassiveTicks", "PhoraAccessMode", "PhoraIsOwner", "PhoraEntityId"
    };

    public DilitioItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !stack.hasTag()) return;

        CompoundTag tag = stack.getTag();
        for (String key : LEGACY_TAGS) {
            tag.remove(key);
        }
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }
}
