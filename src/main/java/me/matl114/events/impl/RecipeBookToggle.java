package me.matl114.events.impl;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;

public record RecipeBookToggle(
        RecipeUpdateListener provider, RecipeBookComponent recipeBookWidget, Button toggleWidget) {}
