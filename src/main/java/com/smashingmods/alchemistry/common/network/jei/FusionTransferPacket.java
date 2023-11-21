package com.smashingmods.alchemistry.common.network.jei;

import com.smashingmods.alchemistry.Alchemistry;
import com.smashingmods.alchemistry.client.jei.RecipeTypes;
import com.smashingmods.alchemistry.common.block.fusion.FusionControllerBlockEntity;
import com.smashingmods.alchemistry.common.block.fusion.FusionControllerMenu;
import com.smashingmods.alchemistry.common.recipe.fusion.FusionRecipe;
import com.smashingmods.alchemistry.registry.MenuRegistry;
import com.smashingmods.alchemistry.registry.RecipeRegistry;
import com.smashingmods.alchemylib.api.network.AlchemyPacket;
import com.smashingmods.alchemylib.api.storage.ProcessingSlotHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FusionTransferPacket implements AlchemyPacket {

    private final BlockPos blockPos;
    private final ItemStack input1;
    private final ItemStack input2;
    private final boolean maxTransfer;

    public FusionTransferPacket(BlockPos pBlockPos, ItemStack pInput1, ItemStack pInput2, boolean pMaxTransfer) {
        this.blockPos = pBlockPos;
        this.input1 = pInput1;
        this.input2 = pInput2;
        this.maxTransfer = pMaxTransfer;
    }

    public FusionTransferPacket(FriendlyByteBuf pBuffer) {
        this.blockPos = pBuffer.readBlockPos();
        this.input1 = pBuffer.readItem();
        this.input2 = pBuffer.readItem();
        this.maxTransfer = pBuffer.readBoolean();
    }

    public void encode(FriendlyByteBuf pBuffer) {
        pBuffer.writeBlockPos(blockPos);
        pBuffer.writeItem(input1);
        pBuffer.writeItem(input2);
        pBuffer.writeBoolean(maxTransfer);
    }

    public void handle(NetworkEvent.Context pContext) {
        ServerPlayer player = pContext.getSender();
        Objects.requireNonNull(player);

        FusionControllerBlockEntity blockEntity = (FusionControllerBlockEntity) player.getLevel().getBlockEntity(blockPos);
        Objects.requireNonNull(blockEntity);

        ProcessingSlotHandler inputHandler = blockEntity.getInputHandler();
        ProcessingSlotHandler outputHandler = blockEntity.getOutputHandler();
        Inventory inventory = player.getInventory();

        RecipeRegistry.getFusionRecipe(recipe -> ItemStack.isSameItemSameTags(recipe.getInput1(), input1) && ItemStack.isSameItemSameTags(recipe.getInput2(), input2), player.getLevel())
            .ifPresent(recipe -> {

                FusionRecipe recipeCopy = recipe.copy();

                inputHandler.emptyToInventory(inventory);
                outputHandler.emptyToInventory(inventory);

                boolean creative = player.gameMode.isCreative();
                boolean inventoryContains = inventory.contains(input1) && inventory.contains(input2);
                boolean canTransfer = (inventoryContains || creative) && inputHandler.isEmpty() && outputHandler.isEmpty();

                if (canTransfer) {
                    List<ItemStack> recipeInputs = List.of(recipeCopy.getInput1(), recipeCopy.getInput2());
                    if (creative) {
                        int maxOperations = TransferUtils.getMaxOperations(recipeInputs, maxTransfer);

                        inputHandler.setOrIncrement(0, new ItemStack(recipeCopy.getInput1().getItem(), recipeCopy.getInput1().getCount() * maxOperations));
                        inputHandler.setOrIncrement(1, new ItemStack(recipeCopy.getInput2().getItem(), recipeCopy.getInput2().getCount() * maxOperations));
                    } else {
                        int slot1 = inventory.findSlotMatchingItem(recipeCopy.getInput1());
                        int slot2 = inventory.findSlotMatchingItem(recipeCopy.getInput2());
                        List<ItemStack> inventoryInputs = List.of(inventory.getItem(slot1), inventory.getItem(slot2));

                        int maxOperations = TransferUtils.getMaxOperations(recipeInputs, inventoryInputs, maxTransfer, false);

                        inventory.removeItem(slot1, recipeCopy.getInput1().getCount() * maxOperations);
                        inventory.removeItem(slot1, recipeCopy.getInput2().getCount() * maxOperations);

                        inputHandler.setOrIncrement(0, new ItemStack(recipeCopy.getInput1().getItem(), recipeCopy.getInput1().getCount() * maxOperations));
                        inputHandler.setOrIncrement(1, new ItemStack(recipeCopy.getInput2().getItem(), recipeCopy.getInput2().getCount() * maxOperations));
                    }
                    blockEntity.setProgress(0);
                    blockEntity.setRecipe(recipe);
                }
            });
    }

    public static class TransferHandler implements IRecipeTransferHandler<FusionControllerMenu, FusionRecipe> {

        public TransferHandler() {}

        @Override
        public Class<FusionControllerMenu> getContainerClass() {
            return FusionControllerMenu.class;
        }

        @Override
        public Optional<MenuType<FusionControllerMenu>> getMenuType() {
            return Optional.of(MenuRegistry.FUSION_CONTROLLER_MENU.get());
        }

        @Override
        public RecipeType<FusionRecipe> getRecipeType() {
            return RecipeTypes.FUSION;
        }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(FusionControllerMenu pContainer, FusionRecipe pRecipe, IRecipeSlotsView pRecipeSlots, Player pPlayer, boolean pMaxTransfer, boolean pDoTransfer) {
            if (pDoTransfer) {
                pContainer.getBlockEntity().setRecipe(pRecipe);
                Alchemistry.PACKET_HANDLER.sendToServer(new FusionTransferPacket(pContainer.getBlockEntity().getBlockPos(), pRecipe.getInput1(), pRecipe.getInput2(), pMaxTransfer));
            }
            return null;
        }
    }
}