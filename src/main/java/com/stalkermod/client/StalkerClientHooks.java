package com.stalkermod.client;

import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.client.Minecraft;

/** Puente cliente <-> resto del mod (clase separada para que el servidor nunca cargue codigo de cliente). */
public final class StalkerClientHooks {

    private StalkerClientHooks() {}

    /** Arranca la musica de terror solo para el jugador al que acecha (si el volumen no es 0). */
    public static void startAmbient(StalkerEntity stalker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != stalker.getTargetId()) {
            return;
        }
        if (StalkerConfig.AMBIENT_VOLUME_PERCENT.get() <= 0) {
            return;
        }
        mc.getSoundManager().play(new StalkerAmbientSound(stalker));
    }

    /** Abre la pantalla de ajustes con los valores actuales del servidor. */
    public static void openSettings(int minutes, int volume, int notice) {
        Minecraft.getInstance().setScreen(new StalkerSettingsScreen(minutes, volume, notice));
    }
}
