package com.stalkermod.init;

import com.stalkermod.StalkerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sonido propio del mod: assets/stalkermod/sounds/stalker_stare.ogg
 * Suena en bucle SOLO mientras el Stalker te esta observando y se va cuando se esconde.
 * (Reemplazable por tu audio conservando el nombre del archivo.)
 * Los toques y golpes de la puerta usan los sonidos ORIGINALES de Minecraft (ver DoorEvent).
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StalkerMod.MODID);

    public static final RegistryObject<SoundEvent> STALKER_STARE = register("stalker_stare");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StalkerMod.MODID, name)));
    }
}
