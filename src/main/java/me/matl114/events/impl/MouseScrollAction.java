package me.matl114.events.impl;

import net.minecraft.client.MouseHandler;

public record MouseScrollAction(MouseHandler mouse, double horizontal, double vertical) {}
