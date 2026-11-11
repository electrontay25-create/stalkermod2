package com.stalkermod.init;

import com.stalkermod.StalkerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sonidos del mod. Los archivos .ogg estan en assets/stalkermod/sounds/ y son PLACEHOLDERS:
 * reemplazalos por los tuyos conservando el mismo nombre.
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StalkerMod.MODID);

    public static final RegistryObject<SoundEvent> STALKER_AMBIENT = register("stalker_ambient");
    public static final RegistryObject<SoundEvent> STALKER_KNOCK = register("stalker_knock");
    public static final RegistryObject<SoundEvent> STALKER_HEAVY_KNOCK = register("stalker_heavy_knock");
    public static final RegistryObject<SoundEvent> STALKER_DOOR_BREAK = register("stalker_door_break");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StalkerMod.MODID, name)));
    }
}
