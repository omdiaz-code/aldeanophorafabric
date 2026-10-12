package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class TransformableMerchantBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public TransformableMerchantBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhoraBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.COPPER_KEY)) {
            if (!level.isClientSide) {
                float yRot = state.getValue(FACING).toYRot();
                level.removeBlock(pos, false);

                TransformableMerchantEntity merchant = ModEntities.TRANSFORMABLE_MERCHANT.create(level);
                if (merchant != null) {
                    merchant.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yRot, 0);
                    merchant.setYHeadRot(yRot);
                    merchant.setYBodyRot(yRot);
                    merchant.setEntityState(1); // Empieza en Estado 1: Encendiendose
                    
                    // Guardar el dueño por separado; el título del comercio usa este campo
                    // y no necesita un nombre visible flotando sobre el Phora.
                    merchant.setOwnerUUID(player.getUUID());
                    merchant.setOwnerName(player.getName().getString());

                    level.addFreshEntity(merchant);
                    if (ModItems.consumeCopperKey(player, hand)) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
