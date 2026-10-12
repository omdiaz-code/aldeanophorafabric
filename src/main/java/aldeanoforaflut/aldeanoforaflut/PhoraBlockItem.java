package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class PhoraBlockItem extends BlockItem {
    public PhoraBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.description_line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.description_line2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.description_line3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.description_line4").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.description_line5").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.requires_prefix").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.aldeanoforaflut.copper_key").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.requires_line2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.requires_line3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip.aldeanoforaflut.phora.reset_prefix").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("tooltip.aldeanoforaflut.phora.reset_action").withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.reset_line2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.reset_line3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.warning_title").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip.aldeanoforaflut.phora.warning_line1_prefix").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("tooltip.aldeanoforaflut.phora.warning_no").withStyle(ChatFormatting.RED)));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.warning_line2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip.aldeanoforaflut.phora.warning_line3_prefix").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("tooltip.aldeanoforaflut.phora.warning_reset").withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.warning_line4").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aldeanoforaflut.phora.warning_line5").withStyle(ChatFormatting.GRAY));
    }
}
