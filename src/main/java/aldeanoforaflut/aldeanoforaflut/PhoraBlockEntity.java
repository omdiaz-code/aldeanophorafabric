package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PhoraBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.phora.sleep");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PhoraBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHORA, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<PhoraBlockEntity> state) {
        state.getController().setAnimation(SLEEP);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
