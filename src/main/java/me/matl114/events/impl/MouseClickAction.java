package me.matl114.events.impl;

import net.minecraft.client.MouseHandler;

public record MouseClickAction(MouseHandler mouse, int eventButton, int action, int mode) {}
