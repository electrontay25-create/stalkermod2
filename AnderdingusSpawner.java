package com.stalkermod.event;

import com.stalkermod.entity.AnderdingusEntity;
import com.stalkermod.init.ModEntities;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Hace aparecer a Anderdingus "dormido" a 14-24 bloques, un poco hacia un lado de donde miras (para que lo notes),
 * en terreno abierto (con cielo encima) y con linea de vision libre hacia el jugador.
 */
public final class AnderdingusSpawner {

    private AnderdingusSpawner() {}

    public static boolean trySpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        RandomSource rnd = level.random;
        Vec3 eye = player.getEyePosition();

        // Direccion en la que miras (horizontal).
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0F);
        double fx = -Mth.sin(yawRad);
        double fz = Mth.cos(yawRad);

        for (int attempt = 0; attempt < 120; attempt++) {
            // 25-60 grados a un lado de tu mirada: aparece donde lo veras al girar un poco
            double off = Math.toRadians((25 + rnd.nextInt(36)) * (rnd.nextBoolean() ? 1 : -1));
            double dx = fx * Math.cos(off) - fz * Math.sin(off);
            double dz = fx * Math.sin(off) + fz * Math.cos(off);
            double dist = 14.0D + rnd.nextDouble() * 10.0D;
            int x = Mth.floor(player.getX() + dx * dist);
            int z = Mth.floor(player.getZ() + dz * dist);

            BlockPos feet = findFeet(level, x, z, player.getBlockY());
            if (feet == null) {
                continue;
            }
            if (!level.canSeeSky(feet.above(2))) {
                continue;   // terreno abierto, no bajo arboles/techos
            }
            Vec3 body = new Vec3(x + 0.5D, feet.getY() + 1.6D, z + 0.5D);
            if (Vis.blocked(level, eye, body) || Vis.blocked(level, eye, body.add(0.0D, -0.9D, 0.0D))) {
                continue;
            }

            AnderdingusEntity e = ModEntities.ANDERDINGUS.get().create(level);
            if (e == null) {
                return false;
            }
            float yaw = (float) (Mth.atan2(player.getZ() - (z + 0.5D), player.getX() - (x + 0.5D)) * (180.0D / Math.PI)) - 90.0F;
            e.moveTo(x + 0.5D, feet.getY(), z + 0.5D, yaw, 0.0F);
            e.init(player, StalkerConfig.ANDERDINGUS_NOTICE_SECONDS.get() * 20);
            level.addFreshEntity(e);
            return true;
        }
        return false;
    }

    /** Celda "de pie": suelo firme (no hojas ni agua) y 3 bloques de aire encima. */
    private static BlockPos findFeet(ServerLevel level, int x, int z, int py) {
        for (int i = 0; i <= 12; i++) {
            int dy = ((i + 1) / 2) * ((i % 2 == 1) ? -1 : 1);   // 0, -1, +1, -2, +2 ...
            BlockPos feet = new BlockPos(x, py + dy, z);
            if (!level.hasChunkAt(feet)) {
                return null;
            }
            BlockPos below = feet.below();
            BlockState ground = level.getBlockState(below);
            if (!ground.isFaceSturdy(level, below, Direction.UP)
                    || ground.is(BlockTags.LEAVES)
                    || !ground.getFluidState().isEmpty()) {
                continue;
            }
            if (StalkSpawner.isPassable(level, feet)
                    && StalkSpawner.isPassable(level, feet.above())
                    && StalkSpawner.isPassable(level, feet.above(2))) {
                return feet;
            }
        }
        return null;
    }
}
