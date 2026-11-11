package com.stalkermod.event;

import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModEntities;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Busca un lugar valido para el evento "acechar".
 *
 * Reglas (para que NUNCA aparezca flotando ni en campo abierto):
 *  1. Suelo solido debajo (no hojas, no agua) y 2 bloques de aire para el cuerpo.
 *  2. Pegado a un bloque "de cobertura" (tronco, piedra, pared...) de 2 de alto que quede ENTRE el
 *     Stalker y el jugador. El Stalker se coloca al borde del bloque, asi se asoma por el costado.
 *  3. Debe haber linea de vision libre entre los ojos del jugador y la parte de la cabeza que sobresale.
 *  4. (Salvo que se ignore) debe estar FUERA del campo de vision del jugador: aparece a tu espalda
 *     o a los lados, y lo ves cuando te das la vuelta.
 */
public final class StalkSpawner {

    private StalkSpawner() {}

    public static boolean trySpawn(ServerPlayer player, boolean ignoreView) {
        ServerLevel level = (ServerLevel) player.level();
        RandomSource rnd = level.random;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        int min = StalkerConfig.STALK_MIN_DISTANCE.get();
        int max = Math.max(min + 1, StalkerConfig.STALK_MAX_DISTANCE.get());

        for (int attempt = 0; attempt < 150; attempt++) {
            double dist = min + rnd.nextDouble() * (max - min);
            double ang = rnd.nextDouble() * Math.PI * 2.0D;
            int bx = Mth.floor(player.getX() + Math.cos(ang) * dist);
            int bz = Mth.floor(player.getZ() + Math.sin(ang) * dist);

            BlockPos stand = findStandPos(level, bx, bz, player.getBlockY());
            if (stand == null) {
                continue;
            }

            // Direccion horizontal desde el Stalker hacia el jugador.
            double vx = player.getX() - (stand.getX() + 0.5D);
            double vz = player.getZ() - (stand.getZ() + 0.5D);
            double len = Math.sqrt(vx * vx + vz * vz);
            if (len < 1.0E-3D) {
                continue;
            }
            vx /= len;
            vz /= len;

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                // La cobertura debe estar del lado del jugador (entre ambos).
                if (dir.getStepX() * vx + dir.getStepZ() * vz < 0.55D) {
                    continue;
                }
                BlockPos cover = stand.relative(dir);
                if (!isCover(level, cover) || !isCover(level, cover.above())) {
                    continue;
                }

                Direction first = dir.getClockWise();
                Direction second = dir.getCounterClockWise();
                if (rnd.nextBoolean()) {
                    Direction tmp = first;
                    first = second;
                    second = tmp;
                }

                for (Direction side : new Direction[]{first, second}) {
                    BlockPos sideCell = stand.relative(side);
                    if (!isPassable(level, sideCell) || !isPassable(level, sideCell.above())) {
                        continue;
                    }

                    // Posicion: al borde del bloque, para que solo se asome por un costado.
                    double sx = stand.getX() + 0.5D + side.getStepX() * 0.5D;
                    double sz = stand.getZ() + 0.5D + side.getStepZ() * 0.5D;
                    double sy = stand.getY();

                    // Punto de la cabeza que sobresale (hacia el lado libre).
                    Vec3 headPoint = new Vec3(sx + side.getStepX() * 0.12D, sy + 2.5D, sz + side.getStepZ() * 0.12D);

                    if (!ignoreView) {
                        Vec3 toHead = headPoint.subtract(eye).normalize();
                        if (toHead.dot(look) > 0.30D) {
                            continue; // el jugador lo veria aparecer
                        }
                    }

                    BlockHitResult hit = level.clip(new ClipContext(eye, headPoint,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                    if (hit.getType() != HitResult.Type.MISS) {
                        continue; // algo tapa la vista: no se veria
                    }

                    // --- Valido: crear la entidad ---
                    StalkerEntity stalker = ModEntities.STALKER.get().create(level);
                    if (stalker == null) {
                        return false;
                    }
                    float yaw = (float) (Mth.atan2(player.getZ() - sz, player.getX() - sx) * (180.0D / Math.PI)) - 90.0F;
                    // "izquierda" del Stalker segun su yaw:
                    float leftX = Mth.cos(yaw * ((float) Math.PI / 180.0F));
                    float leftZ = Mth.sin(yaw * ((float) Math.PI / 180.0F));
                    int lean = (side.getStepX() * leftX + side.getStepZ() * leftZ) > 0.0F ? 1 : -1;

                    stalker.moveTo(sx, sy, sz, yaw, 0.0F);
                    stalker.initStalk(player, yaw, lean);
                    level.addFreshEntity(stalker);
                    return true;
                }
            }
        }
        return false;
    }

    /** Busca una celda "de pie" cerca de la altura del jugador (0, -1, +1, -2, +2 ... hasta 8). */
    private static BlockPos findStandPos(ServerLevel level, int x, int z, int py) {
        for (int i = 0; i <= 16; i++) {
            int dy = ((i + 1) / 2) * ((i % 2 == 1) ? -1 : 1);
            BlockPos feet = new BlockPos(x, py + dy, z);
            if (!level.hasChunkAt(feet)) {
                return null; // no cargamos chunks nuevos
            }
            if (isStandable(level, feet)) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP)
                || ground.is(BlockTags.LEAVES)
                || !ground.getFluidState().isEmpty()) {
            return false;
        }
        return isPassable(level, feet) && isPassable(level, feet.above());
    }

    static boolean isPassable(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        return st.getCollisionShape(level, pos).isEmpty() && st.getFluidState().isEmpty();
    }

    /** Cobertura valida: bloque opaco y completo (tronco, piedra, madera...). Las hojas y el vidrio no cuentan. */
    private static boolean isCover(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        return st.canOcclude() && st.isCollisionShapeFullBlock(level, pos);
    }
}
