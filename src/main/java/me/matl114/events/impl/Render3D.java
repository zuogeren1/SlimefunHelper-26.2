package me.matl114.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;

public record Render3D(PoseStack stack, float partialTicks) {}
