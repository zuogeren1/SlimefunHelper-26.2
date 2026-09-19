package me.matl114.hacks.api;

import java.util.function.Supplier;
import me.matl114.managers.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class MetaDataModuleEntry extends ModuleEntry {
    Supplier<Component> metaData;

    public MetaDataModuleEntry(Config config, String[] path, String[] hotkeyPath, Supplier<Component> provider) {
        super(config, path, hotkeyPath);
        metaData = provider;
    }

    @Override
    public MutableComponent getMetaData() {
        return (MutableComponent) metaData.get();
    }
}
