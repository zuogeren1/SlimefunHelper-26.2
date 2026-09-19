package me.matl114.events.impl;

import net.minecraft.world.inventory.ContainerInput;

public record SlotClickAction(ContainerInput actionType, int syncId, int slotId, int button) {}
