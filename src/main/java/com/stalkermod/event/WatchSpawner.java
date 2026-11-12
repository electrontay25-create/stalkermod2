package com.stalkermod.event;

import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModEntities;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Evento "media cabeza". Busca un BORDE DE OCLUSION: un lugar donde, mirando desde tus ojos, un muro/esquina/
 * tronco/techo de cueva tapa justo la mitad de donde estaria una cabeza.
 *
 * Como funciona:
 *  - Lanza un rayo desde tus ojos en una direccion al azar. Si choca con algo a 5-18 bloques, gira el rayo de a
 *    0.6 grados (hacia un lado, o hacia arriba) hasta que la distancia "salta": ahi termina el muro (borde).
 *  - Pone el CENTRO de la cabeza justo en ese borde, un poco detras del muro.
 *  - Comprueba con rayos que la mitad hacia el lado libre se ve y la otra mitad queda TAPADA por el muro.
 *    Si no se cumple exactamente, descarta el lugar.
 * Funciona en cuevas, tuneles con curvas, casas, arboles, acantilados...
 */
public final class WatchSpawner {

    private static final double STEP_DEG = 0.6D;
    private static final int MAX_STEPS = 14;

    private WatchSpawner() {}

    public static boolean trySpawn(ServerPlayer player, boolean ignoreView, int attempts) {
        ServerLevel level = (ServerLevel) player.level();
        RandomSource rnd = level.random;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        int min = StalkerConfig.WATCH_MIN_DISTANCE.get();
        int max = Math.max(min + 2, StalkerConfig.WATCH_MAX_DISTANCE.get());

        for (int attempt = 0; attempt < attempts; attempt++) {
            double yaw0 = rnd.nextDouble() * 360.0D;
            double pitch0 = (rnd.nextDouble() * 2.0D - 1.0D) * 20.0D;
            boolean top = rnd.nextInt(3) == 0;                         // 1/3 por arriba, 2/3 por el costado
            int sign = top ? -1 : (rnd.nextBoolean() ? 1 : -1);        // arriba = pitch negativo

            double d1 = Vis.distance(level, eye, dir(yaw0, pitch0), max + 6.0D);
            if (d1 < min || d1 > max) {
                continue;
            }

            // Buscar el borde del muro.
            double prev = d1;
            double wall = -1.0D;
            int edgeK = -1;
            for (int k = 1; k <= MAX_STEPS; k++) {
                double yaw = top ? yaw0 : yaw0 + sign * k * STEP_DEG;
                double pitch = top ? pitch0 + sign * k * STEP_DEG : pitch0;
                double dk = Vis.distance(level, eye, dir(yaw, pitch), max + 8.0D);
                if (dk > prev + 1.8D) {
                    wall = prev;
                    edgeK = k;
                    break;
                }
                prev = dk;
            }
            if (edgeK < 0 || wall < min) {
                continue;
            }

            // Centro de la cabeza: justo en el borde, un poco detras del muro.
            double yawE = top ? yaw0 : yaw0 + sign * (edgeK - 0.5D) * STEP_DEG;
            double pitchE = top ? pitch0 + sign * (edgeK - 0.5D) * STEP_DEG : pitch0;
            Vec3 dirE = dir(yawE, pitchE);
            Vec3 hc = eye.add(dirE.scale(wall + 0.7D));

            // Vectores auxiliares (horizontales): "derecha" de la direccion de vista.
            double yr = Math.toRadians(yawE);
            Vec3 right = new Vec3(-Math.cos(yr), 0.0D, -Math.sin(yr));
            Vec3 lateral = right.scale(sign);                          // hacia el lado libre (solo variante costado)

            boolean ok;
            if (top) {
                Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
                ok = Vis.clear(level, eye, hc.add(up.scale(0.22D)))
                        && Vis.clear(level, eye, hc.add(up.scale(0.22D)).add(right.scale(0.3D)))
                        && Vis.clear(level, eye, hc.add(up.scale(0.22D)).subtract(right.scale(0.3D)))
                        && Vis.clear(level, eye, hc.add(up.scale(0.34D)))
                        && Vis.blocked(level, eye, hc.subtract(up.scale(0.22D)))
                        && Vis.blocked(level, eye, hc.subtract(up.scale(0.32D)));
            } else {
                Vec3 pv = hc.add(lateral.scale(0.22D));
                ok = Vis.clear(level, eye, pv)
                        && Vis.clear(level, eye, pv.add(0.0D, 0.3D, 0.0D))
                        && Vis.clear(level, eye, pv.subtract(0.0D, 0.3D, 0.0D))
                        && Vis.clear(level, eye, hc.add(lateral.scale(0.3D)))
                        && Vis.blocked(level, eye, hc.subtract(lateral.scale(0.22D)))
                        && Vis.blocked(level, eye, hc.subtract(lateral.scale(0.3D)));
            }
            if (!ok) {
                continue;
            }

            double distH = Math.sqrt(hc.distanceToSqr(eye));
            if (distH < min) {
                continue;
            }
            if (!ignoreView) {
                if (hc.subtract(eye).normalize().dot(look) > 0.30D) {
                    continue; // lo veria aparecer
                }
            }

            // ---- Valido ----
            StalkerEntity st = ModEntities.STALKER.get().create(level);
            if (st == null) {
                return false;
            }
            float yawToPlayer = (float) (Mth.atan2(player.getZ() - hc.z, player.getX() - hc.x) * (180.0D / Math.PI)) - 90.0F;
            float leftX = Mth.cos(yawToPlayer * ((float) Math.PI / 180.0F));
            float leftZ = Mth.sin(yawToPlayer * ((float) Math.PI / 180.0F));
            int lean = top ? 1 : ((lateral.x * leftX + lateral.z * leftZ) > 0.0D ? 1 : -1);
            int noticeTicks = StalkerConfig.WATCH_STARE_SECONDS.get() * 20;

            st.moveTo(hc.x, hc.y, hc.z, yawToPlayer, 0.0F);
            st.initWatch(player, yawToPlayer, lean, top ? StalkerEntity.WATCH_TOP : StalkerEntity.WATCH_SIDE, noticeTicks);
            level.addFreshEntity(st);
            return true;
        }
        return false;
    }

    /** Direccion a partir de yaw/pitch en grados (convencion de Minecraft: pitch positivo = mirar abajo). */
    private static Vec3 dir(double yawDeg, double pitchDeg) {
        double y = Math.toRadians(yawDeg);
        double p = Math.toRadians(pitchDeg);
        double cp = Math.cos(p);
        return new Vec3(-Math.sin(y) * cp, -Math.sin(p), Math.cos(y) * cp);
    }
}
