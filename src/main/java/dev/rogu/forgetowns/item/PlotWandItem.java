package dev.rogu.forgetowns.item;

import dev.rogu.forgetowns.data.ClaimManager;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class PlotWandItem extends Item {

    public PlotWandItem() {
        super(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    // Add the custom NBT tag to every instance
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        // Use components to store custom data
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, cd -> {
            CompoundTag tag = cd.copyTag(); // Get a mutable copy
            tag.putBoolean("ForgeTownsPlotWand", true);
            return CustomData.of(tag);
        });
        return stack;
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
            ItemStack usedItem = player.getItemInHand(hand);
            ClaimManager.selectPlotCorner(
                serverPlayer,
                serverPlayer.blockPosition(),
                usedItem // Pass the ItemStack
            );
            return InteractionResultHolder.success(usedItem);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    // Add custom name and lore
    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        // Optional: Check for the NBT tag before adding lore, though usually not necessary if getDefaultInstance adds it.
        // CustomData customData = pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        // if (customData.copyTag().getBoolean("ForgeTownsPlotWand")) {
            pTooltipComponents.add(Component.translatable("item.forgetowns.plot_wand.tooltip1").setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GRAY)));
            pTooltipComponents.add(Component.translatable("item.forgetowns.plot_wand.tooltip2").setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GRAY)));
        // }
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }

    @Override
    public Component getName(ItemStack pStack) {
        // Optional: Check NBT if you want the name to depend on the tag
        // CustomData customData = pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        // if (customData.copyTag().getBoolean("ForgeTownsPlotWand")) {
            return Component.translatable("item.forgetowns.plot_wand").setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.AQUA));
        // } 
        // return super.getName(pStack); // Fallback to default name if tag is missing
    }
}
