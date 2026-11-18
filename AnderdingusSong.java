package com.stalkermod.client;

import com.stalkermod.entity.AnderdingusEntity;
import com.stalkermod.init.ModSounds;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * La cancion del video. Empieza a sonar cuando Anderdingus despierta (dormido no suena) y se corta en el
 * instante en que desaparece (te alcanza, termina la cancion o se va).
 */
public class AnderdingusSong extends AbstractTickableSoundInstance {

    private final AnderdingusEntity entity;

    public AnderdingusSong(AnderdingusEntity entity) {
        super(ModSounds.ANDERDINGUS_SONG.get(), SoundSource.HOSTILE, RandomSource.create());
        this.entity = entity;
        this.looping = false;
        this.delay = 0;
        this.relative = true;                                   // se oye igual en cualquier lugar
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = Math.max(0.01F, StalkerConfig.ANDERDINGUS_VOLUME_PERCENT.get() / 100.0F);
    }

    @Override
    public void tick() {
        if (this.entity.isRemoved() || !this.entity.isAlive()) {
            this.stop();
        }
    }
}
