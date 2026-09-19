package me.matl114.events.impl;

import net.minecraft.client.MouseHandler;

public record MouseMoveAction(MouseHandler mouse, double mouseX, double mouseY) {}
