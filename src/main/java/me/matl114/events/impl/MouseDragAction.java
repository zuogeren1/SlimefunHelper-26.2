package me.matl114.events.impl;

import net.minecraft.client.MouseHandler;

public record MouseDragAction(MouseHandler mouse, double mouseX, double mouseY, double deltaX, double deltaY) {}
