package com.stalkermod.item;

import com.stalkermod.init.StalkerConfig;
import com.stalkermod.network.ModNetwork;
import com.stalkermod.network.OpenSettingsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/**
 * Reloj del Stalker: manten presionado el clic derecho ~1.5 segundos y se abre la ventana de ajustes.
 */
public class StalkerWatchItem extends Item {

    public StalkerWatchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 30; // 1.5 segundos
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Se llama en el servidor al completar el tiempo de "mantener": le pedimos al cliente abrir la ventana. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new OpenSettingsPacket(
                            StalkerConfig.STALK_INTERVAL_MINUTES.get(),
                            StalkerConfig.AMBIENT_VOLUME_PERCENT.get(),
                            StalkerConfig.STALK_NOTICE_SECONDS.get()));
        }
        return stack;
    }
}
