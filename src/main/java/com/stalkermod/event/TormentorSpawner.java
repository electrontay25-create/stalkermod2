package com.stalkermod.event;

import com.stalkermod.entity.TormentorEntity;
import com.stalkermod.init.ModEntities;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/**
 * Busca la ventana que atacara el Tormentor.
 *
 * Reglas:
 *  - El jugador debe estar DENTRO de una casa (con techo encima).
 *  - "Ventana" = vidrio, panel de vidrio, VALLA o barrotes de hierro. Da igual el material: el Tormentor
 *    la destruye igual (los jugadores a veces usan vallas como ventanas "a prueba de monstruos").
 *  - La ventana esta en un muro: libre por los dos lados (adentro y afuera) y con el muro continuando a los costados.
 *  - Se ve desde donde estas (linea de vision libre).
 */
public final class TormentorSpawner {

    private TormentorSpawner() {}

    public static boolean isWindowBlock(BlockState st) {
        return st.is(Tags.Blocks.GLASS)
                || st.is(Tags.Blocks.GLASS_PANES)
                || st.is(BlockTags.FENCES)
                || st.is(Blocks.IRON_BARS);
    }

    public static boolean trySpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pp = player.blockPosition();
        if (level.canSeeSky(pp.above())) {
            return false;   // no estas dentro de una casa (no hay techo)
        }
        int r = StalkerConfig.TORMENTOR_SEARCH_RADIUS.get();
        Vec3 eye = player.getEyePosition();

        BlockPos best = null;
        Direction bestOut = null;
        double bestD = Double.MAX_VALUE;

        for (BlockPos p : BlockPos.betweenClosed(pp.offset(-r, -2, -r), pp.offset(r, 3, r))) {
            if (!isWindowBlock(level.getBlockState(p))) {
                continue;
            }
            BlockPos w = p.immutable();
            Direction out = orientation(level, w, player);
            if (out == null) {
                continue;
            }
            Vec3 wc = Vec3.atCenterOf(w);
            double d = wc.distanceToSqr(player.position());
            if (d < 4.0D || d > (double) r * r) {
                continue;
            }
            // El jugador debe estar del lado de ADENTRO y esa cara debe estar techada.
            double toOut = (player.getX() - wc.x) * out.getStepX() + (player.getZ() - wc.z) * out.getStepZ();
            if (toOut > 0.2D) {
                continue;
            }
            if (level.canSeeSky(w.relative(out.getOpposite()))) {
                continue;
            }
            if (!Vis.clear(level, eye, wc)) {
                continue;
            }
            if (d < bestD) {
                bestD = d;
                best = w;
                bestOut = out;
            }
        }
        if (best == null) {
            return false;
        }

        // Ventanas pegadas (3x3 en el plano del muro) que tambien se romperan.
        List<BlockPos> windows = new ArrayList<>();
        Direction wallDir = bestOut.getClockWise();
        for (int da = -1; da <= 1; da++) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos q = best.relative(wallDir, da).above(dy);
                if (isWindowBlock(level.getBlockState(q))) {
                    windows.add(q);
                }
            }
        }

        TormentorEntity t = ModEntities.TORMENTOR.get().create(level);
        if (t == null) {
            return false;
        }
        double x = best.getX() + 0.5D + bestOut.getStepX() * 0.55D;
        double y = best.getY() + 0.5D;
        double z = best.getZ() + 0.5D + bestOut.getStepZ() * 0.55D;
        float yaw = bestOut.getOpposite().toYRot();   // mirando hacia adentro de la casa

        t.moveTo(x, y, z, yaw, 0.0F);
        t.init(player, yaw, windows);
        level.addFreshEntity(t);
        return true;
    }

    /**
     * Direccion "hacia afuera" si el bloque esta en un muro (libre por los dos lados y con muro a los costados);
     * si no es una ventana de muro, null.
     */
    private static Direction orientation(ServerLevel level, BlockPos w, ServerPlayer player) {
        Direction[][] axes = {{Direction.NORTH, Direction.SOUTH}, {Direction.EAST, Direction.WEST}};
        for (Direction[] ax : axes) {
            Direction a = ax[0];
            Direction b = ax[1];
            if (!StalkSpawner.isPassable(level, w.relative(a)) || !StalkSpawner.isPassable(level, w.relative(b))) {
                continue;
            }
            Direction c1 = a.getClockWise();
            Direction c2 = c1.getOpposite();
            if (StalkSpawner.isPassable(level, w.relative(c1)) || StalkSpawner.isPassable(level, w.relative(c2))) {
                continue;   // el muro no sigue a los costados: no es una ventana de muro
            }
            boolean skyA = level.canSeeSky(w.relative(a));
            boolean skyB = level.canSeeSky(w.relative(b));
            if (skyA != skyB) {
                return skyA ? a : b;
            }
            Vec3 wc = Vec3.atCenterOf(w);
            double dA = wc.add(a.getStepX(), 0.0D, a.getStepZ()).distanceToSqr(player.position());
            double dB = wc.add(b.getStepX(), 0.0D, b.getStepZ()).distanceToSqr(player.position());
            return dA > dB ? a : b;   // el lado mas lejano del jugador
        }
        return null;
    }
}
