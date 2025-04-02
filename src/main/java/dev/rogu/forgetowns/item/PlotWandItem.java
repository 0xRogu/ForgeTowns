package dev.rogu.forgetowns.item;

import dev.rogu.forgetowns.data.ClaimManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PlotWandItem extends Item {

    public PlotWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand hand
    ) {
        if (
            !level.isClientSide && player instanceof ServerPlayer serverPlayer
        ) {
            ClaimManager.selectPlotCorner(
                serverPlayer,
                serverPlayer.blockPosition()
            );
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
