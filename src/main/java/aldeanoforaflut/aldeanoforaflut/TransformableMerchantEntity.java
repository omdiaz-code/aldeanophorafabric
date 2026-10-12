package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.UseItemGoal;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransformableMerchantEntity extends WanderingTrader implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 1 = Turning On (animacion de encendido)
    // 2 = Active Villager (camina y tradea)
    // 3 = Turning Off (animacion de apagado, luego vuelve a ser bloque)
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(TransformableMerchantEntity.class, EntityDataSerializers.INT);

    private static final int TURN_OFF_DURATION = 32;
    private static final int TRADE_SOUND_COOLDOWN = 3;

    private int turnOnTicks = 0;
    private int turnOffTicks = 0;
    private int lastTradeSoundTick = -TRADE_SOUND_COOLDOWN;
    private long lastRestockDay = Long.MIN_VALUE;

    // Niveles de XP estilo aldeano vanilla
    private int phoraLevel = 1;
    private int phoraXp = 0;

    public static final String TITLE_PREFIX = "Aldeano Phora de Recursos";

    // Dueño y acceso
    private UUID ownerUUID = null;
    private String ownerName = null;
    // 0 = Solo yo, 1 = Mi Hermandad, 2 = Todos
    private int accessMode = 2;

    // Barra pasiva (ticks acumulados para progreso lento)
    private long passiveTicks = 0;

    // Nivel requerido de cada oferta, alineado por índice con this.offers.
    // Se guarda aquí y no en el ítem resultado para que el Dilitio siempre se apile.
    private final List<Integer> offerLevels = new ArrayList<>();
    private static final String[] LEGACY_RESULT_TAGS = {
            "RequiredLevel", "PhoraPassiveTicks", "PhoraAccessMode", "PhoraIsOwner", "PhoraEntityId"
    };

    // --- NAMETAG: NUNCA mostrar sobre la cabeza ---
    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean isCustomNameVisible() {
        return false;
    }

    public UUID getOwnerUUID() { return this.ownerUUID; }
    public void setOwnerUUID(UUID uuid) { this.ownerUUID = uuid; }

    public String getOwnerName() { return this.ownerName; }
    public void setOwnerName(String name) { this.ownerName = name; }

    /** Título del GUI de comercio. Empieza siempre con TITLE_PREFIX, que el cliente usa para detectar la pantalla Phora. */
    public Component getTradeTitle() {
        if (this.ownerName != null && !this.ownerName.isEmpty()) {
            return Component.literal(TITLE_PREFIX + " de " + this.ownerName);
        }
        // Phoras anteriores a este cambio: el nombre del dueño estaba en el nombre personalizado
        if (this.hasCustomName() && this.getCustomName().getString().startsWith(TITLE_PREFIX)) {
            return this.getCustomName();
        }
        return Component.literal(TITLE_PREFIX);
    }

    public int getAccessMode() { return this.accessMode; }
    public void setAccessMode(int mode) { this.accessMode = mode % 3; }
    public void cycleAccessMode() { this.accessMode = (this.accessMode + 1) % 3; }
    public String getAccessModeText() {
        return switch (this.accessMode) {
            case 0 -> "Solo yo";
            case 1 -> "Mi Hermandad";
            default -> "Todos";
        };
    }

    public boolean canPlayerAccess(Player player) {
        if (this.ownerUUID == null) return true;
        if (this.accessMode == 2) return true; // Todos
        if (this.accessMode == 0) return player.getUUID().equals(this.ownerUUID); // Solo yo
        // accessMode == 1: Mi Hermandad - por ahora dejar pasar a todos (integrar mod de hermandades despues)
        return true;
    }

    public boolean isOwner(Player player) {
        return this.ownerUUID != null && player.getUUID().equals(this.ownerUUID);
    }

    public int getPhoraLevel() { return this.phoraLevel; }

    public long getPassiveTicks() { return this.passiveTicks; }
    public long getLastRestockDay() { return this.lastRestockDay; }
    public void setLastRestockDay(long day) { this.lastRestockDay = day; }

    public int getOfferLevel(int index) {
        return index >= 0 && index < this.offerLevels.size() ? this.offerLevels.get(index) : 0;
    }

    public boolean isOfferUnlocked(int index) {
        return getOfferLevel(index) <= this.phoraLevel;
    }

    public TransformableMerchantEntity(EntityType<? extends WanderingTrader> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true); // Empieza sin IA (encendiendose)
        this.updateTrades(); // Inicializar tradeos de Nivel 1
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> goal instanceof UseItemGoal<?>);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 1); // Empieza en Estado 1 (Encendiendose)
    }

    public int getEntityState() {
        return this.entityData.get(STATE);
    }

    public void setEntityState(int state) {
        this.entityData.set(STATE, state);
    }

    public boolean isActive() {
        return getEntityState() == 2;
    }

    @Override
    public void tick() {
        MerchantAiHandler.clearLegacyInvisibility(this);
        super.tick();
        if (!this.level().isClientSide) {
            DilitioRestockHandler.tick(this);
            if (getEntityState() == 1) {
                turnOnTicks++;
                if (turnOnTicks == 1) {
                    this.playSound(ModSounds.PHORA_POWER_ON, ModSounds.PHORA_VOLUME, 1.0f);
                }
                if (turnOnTicks >= 40) {
                    setEntityState(2);
                    this.setNoAi(false);
                    this.playSound(ModSounds.PHORA_BOOT_READY, ModSounds.PHORA_VOLUME, 1.0f);
                }
            } else if (getEntityState() == 2) {
                // Barra pasiva: incrementar cada tick (1% cada 3 horas de juego = 3000 ticks)
                this.passiveTicks++;
            } else if (getEntityState() == 3) {
                turnOffTicks++;
                if (turnOffTicks >= TURN_OFF_DURATION) {
                    finishTurnOff();
                }
            }
        }
    }

    public boolean isTurningOff() {
        return getEntityState() == 3;
    }

    // --- INMORTAL: solo se quita reiniciándolo (vuelve a ser bloque) ---
    // Daño que ignora invulnerabilidad (/kill, caer al vacío) sí aplica, para que los admins puedan limpiar
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    /**
     * Apaga el Phora: cierra el comercio y reproduce la animación de apagado.
     * Al terminar se vuelve a colocar como bloque en su posición (o se suelta como ítem
     * si el lugar está ocupado) y se elimina la entidad.
     * Al encenderlo de nuevo se crea una entidad nueva con tradeos distintos.
     */
    public void resetToBlock() {
        if (this.level().isClientSide || this.isRemoved() || isTurningOff()) return;

        Player trader = this.getTradingPlayer();
        if (trader != null) {
            if (trader instanceof ServerPlayer sp) { sp.closeContainer(); }
            this.setTradingPlayer(null);
        }

        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        this.setNoAi(true);
        this.turnOffTicks = 0;
        setEntityState(3);
        this.playSound(ModSounds.PHORA_POWER_OFF, ModSounds.PHORA_VOLUME, 1.0f);
    }

    private void finishTurnOff() {
        BlockPos pos = this.blockPosition();
        BlockState blockState = ModBlocks.MERCHANT_BLOCK.defaultBlockState()
                .setValue(TransformableMerchantBlock.FACING, Direction.fromYRot(this.getYRot()));
        if (this.level().getBlockState(pos).canBeReplaced()) {
            this.level().setBlockAndUpdate(pos, blockState);
        } else {
            this.spawnAtLocation(new ItemStack(ModBlocks.MERCHANT_BLOCK_ITEM));
        }

        this.discard();
    }

    // --- SONIDOS: computadora de ciencia ficción en lugar de la voz del aldeano ---
    @Override
    protected SoundEvent getAmbientSound() {
        if (!isActive()) return null;
        return this.isTrading() ? ModSounds.PHORA_PROCESSING : ModSounds.PHORA_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null; // Es invulnerable: no hay quejido
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PHORA_POWER_OFF;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean hasResult) {
        return hasResult ? ModSounds.PHORA_ACCEPT : ModSounds.PHORA_DENY;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.PHORA_TRADE;
    }

    @Override
    public float getVoicePitch() {
        return 1.0f; // Sin la variación aleatoria de la voz de aldeano
    }

    @Override
    protected float getSoundVolume() {
        return ModSounds.PHORA_VOLUME;
    }

    @Override
    public boolean isPushable() {
        return getEntityState() == 2;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entityIn) {
        if (getEntityState() == 2) {
            super.doPush(entityIn);
        }
    }

    @Override
    public void push(double x, double y, double z) {
        if (getEntityState() == 2) {
            super.push(x, y, z);
        }
    }

    // --- LOGICA DE TRADEO Y NIVELES ---
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.sidedSuccess(true);

        if (player.getItemInHand(hand).is(ModItems.COPPER_KEY)) {
            if (!this.isOwner(player) && !player.hasPermissions(2)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cSolo el dueño o un administrador puede apagar este Aldeano Phora."));
                return InteractionResult.FAIL;
            }
            if (this.isActive()) {
                if (!ModItems.consumeCopperKey(player, hand)) return InteractionResult.FAIL;
                this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                this.resetToBlock();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (getEntityState() == 2) {
            // Verificar permisos de acceso
            if (!this.canPlayerAccess(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNo tienes acceso a este Aldeano Phora."));
                return InteractionResult.FAIL;
            }
            if (this.getTradingPlayer() != null) {
                return InteractionResult.FAIL;
            }
            if (this.offers != null && !this.offers.isEmpty()) {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getTradeTitle(), this.phoraLevel);

                // Datos extra para la pantalla Phora (van por paquete, no en el NBT del resultado)
                if (player instanceof ServerPlayer serverPlayer) {
                    int[] levels = new int[this.offers.size()];
                    for (int i = 0; i < levels.length; i++) {
                        levels[i] = getOfferLevel(i);
                    }
                    ModNetworking.sendTradeInfo(serverPlayer, serverPlayer.containerMenu.containerId,
                            this.getId(), this.passiveTicks, this.accessMode, this.isOwner(player), levels);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void updateTrades() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            // Generar todos los trades hasta el nivel 5 desde el inicio
            this.offerLevels.clear();
            if (!this.level().isClientSide) {
                for (int lvl = 1; lvl <= 5; lvl++) {
                    int added = DilitioTrades.addTradesForLevel(this.offers, this.random, lvl, this.phoraLevel, (ServerLevel) this.level());
                    for (int i = 0; i < added; i++) {
                        this.offerLevels.add(lvl);
                    }
                }
            }
        } else {
            // Desbloquear tradeos del nuevo nivel alcanzado
            for (int i = 0; i < this.offers.size(); i++) {
                int reqLvl = getOfferLevel(i);
                if (reqLvl > 0 && reqLvl <= this.phoraLevel) {
                    this.offers.get(i).resetUses(); // Desbloqueado!
                }
            }
        }
    }

    @Override
    public int getVillagerXp() {
        return this.phoraXp;
    }

    @Override
    public void overrideXp(int xp) {
        this.phoraXp = xp;
    }

    @Override
    public boolean showProgressBar() {
        return true; // Mostrar barra de XP en la UI
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer); // Esto internamente llama a rewardTradeXp
        // Shift-click hace varios tradeos en el mismo tick: un solo sonido para no saturar
        if (this.tickCount - this.lastTradeSoundTick >= TRADE_SOUND_COOLDOWN) {
            this.lastTradeSoundTick = this.tickCount;
            this.playSound(ModSounds.PHORA_TRADE, ModSounds.PHORA_VOLUME, 0.95f + this.random.nextFloat() * 0.1f);
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        int i = 3 + this.random.nextInt(4);
        this.phoraXp += offer.getXp();
        
        if (this.canLevelUp()) {
            this.phoraLevel++;
            this.updateTrades(); // Desbloquea los tradeos de este nuevo nivel
            i += 5;
        }

        if (offer.shouldRewardExp()) {
            this.level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), i));
        }
    }

    private boolean canLevelUp() {
        int targetXp = getXpForLevel(this.phoraLevel + 1);
        return this.phoraLevel < 5 && this.phoraXp >= targetXp;
    }

    /** XP acumulada necesaria para alcanzar cada nivel. El coste en intercambios
     * crece de forma marcada: 20, 30, 50 y 75 intercambios, usando la recompensa
     * de XP de los trades de cada nivel. */
    public static int getXpForLevel(int lvl) {
        return switch (lvl) {
            case 2 -> 40;
            case 3 -> 340;
            case 4 -> 1340;
            case 5 -> 3590;
            default -> 0;
        };
    }


    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("PhoraState", getEntityState());
        compound.putInt("PhoraLevel", this.phoraLevel);
        compound.putInt("PhoraXp", this.phoraXp);
        compound.putInt("PhoraAccessMode", this.accessMode);
        compound.putLong("PhoraPassiveTicks", this.passiveTicks);
        compound.putLong("PhoraLastRestockDay", this.lastRestockDay);
        compound.putIntArray("PhoraOfferLevels", this.offerLevels);
        if (this.ownerUUID != null) {
            compound.putUUID("PhoraOwner", this.ownerUUID);
        }
        if (this.ownerName != null) {
            compound.putString("PhoraOwnerName", this.ownerName);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("PhoraState")) {
            int savedState = compound.getInt("PhoraState");
            setEntityState(savedState);
            this.setNoAi(savedState != 2);
        }
        if (compound.contains("PhoraLevel")) {
            this.phoraLevel = compound.getInt("PhoraLevel");
        }
        if (compound.contains("PhoraXp")) {
            this.phoraXp = compound.getInt("PhoraXp");
        }
        if (compound.contains("PhoraAccessMode")) {
            this.accessMode = compound.getInt("PhoraAccessMode");
        }
        if (compound.contains("PhoraPassiveTicks")) {
            this.passiveTicks = compound.getLong("PhoraPassiveTicks");
        }
        this.lastRestockDay = compound.contains("PhoraLastRestockDay")
                ? compound.getLong("PhoraLastRestockDay") : Long.MIN_VALUE;
        if (compound.hasUUID("PhoraOwner")) {
            this.ownerUUID = compound.getUUID("PhoraOwner");
        }
        if (compound.contains("PhoraOwnerName")) {
            this.ownerName = compound.getString("PhoraOwnerName");
        }
        // Migrar Phoras antiguos que usaban el nombre personalizado como título.
        // El dueño ya se guarda aparte, así que quitamos el nombre que flotaba encima.
        if (this.hasCustomName()) {
            String savedName = this.getCustomName().getString();
            String ownerPrefix = TITLE_PREFIX + " de ";
            if ((this.ownerName == null || this.ownerName.isEmpty()) && savedName.startsWith(ownerPrefix)) {
                this.ownerName = savedName.substring(ownerPrefix.length());
            }
            if (savedName.startsWith(TITLE_PREFIX)) {
                this.setCustomName(null);
                this.setCustomNameVisible(false);
            }
        }
        loadOfferLevels(compound);
    }

    private void loadOfferLevels(CompoundTag compound) {
        // Sin ofertas guardadas se conservan las generadas en el constructor junto con sus niveles
        if (!compound.contains("Offers")) return;
        this.offerLevels.clear();
        boolean hasSavedLevels = compound.contains("PhoraOfferLevels");
        if (hasSavedLevels) {
            for (int lvl : compound.getIntArray("PhoraOfferLevels")) {
                this.offerLevels.add(lvl);
            }
        }
        if (this.offers == null) return;

        // Mundos guardados antes de este cambio: el nivel venía en el NBT del resultado.
        // Se recupera y se limpia el resultado para que el Dilitio vuelva a apilarse.
        for (MerchantOffer offer : this.offers) {
            ItemStack result = offer.getResult();
            if (!result.hasTag()) {
                if (!hasSavedLevels) this.offerLevels.add(1);
                continue;
            }
            CompoundTag tag = result.getTag();
            if (!hasSavedLevels) {
                this.offerLevels.add(tag.contains("RequiredLevel") ? tag.getInt("RequiredLevel") : 1);
            }
            for (String key : LEGACY_RESULT_TAGS) {
                tag.remove(key);
            }
            if (tag.isEmpty()) {
                result.setTag(null);
            }
        }
    }

    // --- GECKOLIB ANIMATIONS ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<TransformableMerchantEntity> state) {
        int entityState = getEntityState();

        if (entityState == 1) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.phora.turn_on"));
            return PlayState.CONTINUE;
        } else if (entityState == 3) {
            state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("animation.phora.turn_off"));
            return PlayState.CONTINUE;
        } else {
            // Estado 2: Activo
            if (state.isMoving()) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.phora.float_move"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.phora.idle"));
            }
            return PlayState.CONTINUE;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}


