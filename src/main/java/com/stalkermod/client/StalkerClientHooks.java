package com.stalkermod.client;

import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModSounds;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/** Puente cliente <-> resto del mod (clase separada para que el servidor nunca cargue codigo de cliente). */
public final class StalkerClientHooks {

    private StalkerClientHooks() {}

    /**
     * Cuando algo empieza a observarte: suena el CHILLIDO de presentimiento (una vez) y, si es el evento
     * de acechar, ademas la musica de terror en bucle. Solo para el jugador al que acecha.
     */
    public static void startAudio(StalkerEntity stalker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != stalker.getTargetId()) {
            return;
        }
        int percent = StalkerConfig.AMBIENT_VOLUME_PERCENT.get();
        if (percent <= 0) {
            return;
        }
        float screechVolume = Math.min(1.0F, percent / 100.0F * 1.5F);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.STALKER_SCREECH.get(), 1.0F, screechVolume));
        if (stalker.getMode() == StalkerEntity.MODE_STALK) {
            mc.getSoundManager().play(new StalkerAmbientSound(stalker));
        }
    }

    /** Abre la pantalla de ajustes con los valores actuales del servidor. */
    public static void openSettings(int minutes, int volume, int notice) {
        Minecraft.getInstance().setScreen(new StalkerSettingsScreen(minutes, volume, notice));
    }
}
