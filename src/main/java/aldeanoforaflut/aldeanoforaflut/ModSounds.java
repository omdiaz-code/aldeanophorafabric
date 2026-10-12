package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final float PHORA_VOLUME = 0.25f;

    public static final SoundEvent PHORA_POWER_ON = register("phora.power_on");
    public static final SoundEvent PHORA_BOOT_READY = register("phora.boot_ready");
    public static final SoundEvent PHORA_POWER_OFF = register("phora.power_off");
    public static final SoundEvent PHORA_AMBIENT = register("phora.ambient");
    public static final SoundEvent PHORA_PROCESSING = register("phora.processing");
    public static final SoundEvent PHORA_ACCEPT = register("phora.accept");
    public static final SoundEvent PHORA_DENY = register("phora.deny");
    public static final SoundEvent PHORA_TRADE = register("phora.trade");
    public static final SoundEvent PHORA_RESTOCK = register("phora.restock");

    private static SoundEvent register(String name) {
        ResourceLocation id = new ResourceLocation(Aldeanoforaflut.MODID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void register() {
        // Dummy method to trigger static initialization
    }
}
