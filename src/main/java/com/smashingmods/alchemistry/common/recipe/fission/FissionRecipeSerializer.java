package com.smashingmods.alchemistry.common.recipe.fission;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class FissionRecipeSerializer<T extends FissionRecipe> implements RecipeSerializer<T> {

    private final FissionRecipeSerializer.IFactory<T> factory;

    public FissionRecipeSerializer(FissionRecipeSerializer.IFactory<T> pFactory) {
        this.factory = pFactory;
    }

    @Override
    public T fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
        String group = pSerializedRecipe.get("group").getAsString();
        ItemStack input = ShapedRecipe.itemStackFromJson(pSerializedRecipe.getAsJsonObject("input"));
        ItemStack output1 = ShapedRecipe.itemStackFromJson(pSerializedRecipe.getAsJsonObject("output1"));
        ItemStack output2 = ShapedRecipe.itemStackFromJson(pSerializedRecipe.getAsJsonObject("output2"));
        return factory.create(pRecipeId, group, input, output1, output2);
    }

    @Override
    public T fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        String group = pBuffer.readUtf(Short.MAX_VALUE);
        ItemStack input = pBuffer.readItem();
        ItemStack output1 = pBuffer.readItem();
        ItemStack output2 = pBuffer.readItem();
        return factory.create(pRecipeId, group, input, output1, output2);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, T pRecipe) {
        pBuffer.writeUtf(pRecipe.getGroup());
        pBuffer.writeItem(pRecipe.getInput());
        pBuffer.writeItem(pRecipe.getOutput1());
        pBuffer.writeItem(pRecipe.getOutput2());
    }

    public interface IFactory<T extends Recipe<Inventory>> {
        T create(ResourceLocation pId, String pGroup, ItemStack pInput, ItemStack pOutput1, ItemStack pOutput2);
    }
}