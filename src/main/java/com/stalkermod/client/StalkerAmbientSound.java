package com.stalkermod.client;

import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Musica de terror: sube de volumen poco a poco al aparecer el Stalker, baja mientras
 * se desvanece y se corta cuando desaparece.
 */
public class StalkerAmbientSound extends AbstractTickableSoundInstance {

    private final StalkerEntity stalker;
    private int age;

    public StalkerAmbientSound(StalkerEntity stalker) {
        super(ModSounds.STALKER_AMBIENT.get(), SoundSource.AMBIENT, RandomSource.create());
        this.stalker = stalker;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001F;
        this.relative = true;                                   // no depende de la posicion
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (this.stalker.isRemoved() || !this.stalker.isAlive()) {
            this.stop();
            return;
        }
        this.age++;
        float rise = Math.min(1.0F, this.age / 100.0F);          // sube durante ~5 segundos
        float fade = Math.max(0.0F, this.stalker.getAlpha());    // baja mientras se desvanece
        this.volume = Math.max(0.001F, rise * fade);
    }
}
