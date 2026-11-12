package com.stalkermod.init;

import com.stalkermod.StalkerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sonidos propios del mod (assets/stalkermod/sounds/*.ogg). Reemplazables por los tuyos conservando el nombre.
 * Los toques y golpes de la puerta usan los sonidos ORIGINALES de Minecraft (ver DoorEvent).
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StalkerMod.MODID);

    /** Musica de terror en bucle mientras te acecha. */
    public static final RegistryObject<SoundEvent> STALKER_AMBIENT = register("stalker_ambient");
    /** Chillido de presentimiento: suena una vez cuando algo empieza a observarte. */
    public static final RegistryObject<SoundEvent> STALKER_SCREECH = register("stalker_screech");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StalkerMod.MODID, name)));
    }
}
