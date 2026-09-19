package me.matl114.events.impl;

import net.minecraft.client.KeyboardHandler;

public record KeyboardAction(KeyboardHandler keyboard, int keyCode, int scannCode, int action, int modifier) {}
