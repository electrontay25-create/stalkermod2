package com.stalkermod.client;

import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModSounds;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Sonido de terror mientras el Stalker te observa: suena en bucle, baja mientras el Stalker se esconde
 * y se corta del todo cuando desaparece. El volumen maximo se ajusta en la configuracion.
 */
public class StalkerAmbientSound extends AbstractTickableSoundInstance {

    private final StalkerEntity stalker;
    private int age;

    public StalkerAmbientSound(StalkerEntity stalker) {
        super(ModSounds.STALKER_STARE.get(), SoundSource.AMBIENT, RandomSource.create());
        this.stalker = stalker;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (this.stalker.isRemoved() || !this.stalker.isAlive()) {
            this.stop();
            return;
        }
        this.age++;
        float rise = Math.min(1.0F, this.age / 20.0F);          // entra rapido (1 s)
        float fade = Math.max(0.0F, this.stalker.getReveal());
        float max = StalkerConfig.AMBIENT_VOLUME_PERCENT.get() / 100.0F;
        this.volume = Math.max(0.001F, rise * fade * max);
    }
}
