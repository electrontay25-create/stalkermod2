package com.stalkermod.client;

import com.stalkermod.entity.StalkerEntity;
import net.minecraft.client.Minecraft;

/** Puente cliente <-> entidad (clase separada para que el servidor nunca cargue codigo de cliente). */
public final class StalkerClientHooks {

    private StalkerClientHooks() {}

    /** Arranca la musica de terror solo para el jugador al que acecha. */
    public static void startAmbient(StalkerEntity stalker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != stalker.getTargetId()) {
            return;
        }
        mc.getSoundManager().play(new StalkerAmbientSound(stalker));
    }
}
