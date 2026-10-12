package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CopperKeyItem extends Item {
    public CopperKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip.aldeanoforaflut.copper_key.line1_prefix").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("tooltip.aldeanoforaflut.copper_key.line1_action").withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip.aldeanoforaflut.copper_key.line2_prefix").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("tooltip.aldeanoforaflut.copper_key.line2_phora").withStyle(ChatFormatting.LIGHT_PURPLE)));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.copper_key.line3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.copper_key.consumed")
                .withStyle(ChatFormatting.GRAY));
    }
}
