package com.smashingmods.alchemistry.common.network.jei;

import com.smashingmods.alchemylib.api.item.IngredientStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TransferUtils {

    public static int getMaxOperations(ItemStack pRecipeInput, boolean pMaxTransfer) {
        return getMaxOperations(pRecipeInput, ItemStack.EMPTY, pMaxTransfer, true);
    }

    public static int getMaxOperations(List<ItemStack> pRecipeInputList, boolean pMaxTransfer) {
        return getMaxOperations(pRecipeInputList, new ArrayList<>(), pMaxTransfer, true);
    }

    public static int getMaxOperations(ItemStack pRecipeInput, ItemStack pInventoryInput, boolean pMaxTransfer, boolean pCreative) {
        int maxCount;
        if (pMaxTransfer) {
            if (pCreative) {
                maxCount = pRecipeInput.getMaxStackSize();
            } else {
                maxCount = pInventoryInput.getCount();
            }
        } else {
            maxCount = pRecipeInput.getCount();
        }
        return maxCount / pRecipeInput.getCount();
    }

    public static int getMaxOperations(List<ItemStack> pRecipeInputList, @Nonnull List<ItemStack> pInventoryItems, boolean pMaxTransfer, boolean pCreative) {

        List<Integer> ints = new ArrayList<>();
        for (int i = 0; i < pRecipeInputList.size(); i++) {
            if (pRecipeInputList.get(i).getCount() < 1) {
                ints.add(0);
                break;
            }
            int maxCount;
            if (pMaxTransfer) {
                if (pCreative) {
                    maxCount = pRecipeInputList.get(i).getMaxStackSize();
                } else {
                    maxCount = pInventoryItems.get(i).getCount();
                }
            } else {
                maxCount = pRecipeInputList.get(i).getCount();
            }
            ints.add(maxCount / pRecipeInputList.get(i).getCount());
        }
        return Collections.min(ints);
    }

    public static ItemStack matchIngredientToItemStack(NonNullList<ItemStack> pItems, IngredientStack pIngredientStack) {
        AtomicReference<ItemStack> atomicItem = new AtomicReference<>();
        boolean test = pItems.stream().anyMatch(itemStack -> {
            boolean matches = pIngredientStack.matches(itemStack);
            if (matches && itemStack.getCount() >= pIngredientStack.getCount()) {
                atomicItem.set(itemStack);
                return true;
            }
            return false;
        });
        return test ? atomicItem.get() : ItemStack.EMPTY;
    }

    public static List<ItemStack> matchIngredientListToItemStack(NonNullList<ItemStack> pItems, List<IngredientStack> pIngredientStackList) {
        List<ItemStack> toReturn = new ArrayList<>();
        for (IngredientStack ingredientStack : pIngredientStackList) {
            ItemStack itemStack = matchIngredientToItemStack(pItems, ingredientStack);
            if (!itemStack.isEmpty()) {
                toReturn.add(itemStack);
            }
        }
        return toReturn;
    }
}
