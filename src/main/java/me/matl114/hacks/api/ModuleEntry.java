package me.matl114.hacks.api;

import lombok.Getter;
import me.matl114.managers.config.Config;
import me.matl114.managers.config.FlagRef;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ModuleEntry {
    Config config;

    @Getter
    String[] path;

    @Getter
    String[] hotkeyPath;

    FlagRef flagRef;

    @Getter
    String translationKey;

    public ModuleEntry(Config config, String[] path, String[] hotkeyPath) {
        this.config = config;
        this.path = path;
        this.hotkeyPath = hotkeyPath;
        this.translationKey = "module-toggle." + String.join(".", this.path);
    }

    public FlagRef getFlagRef() {
        if (flagRef == null) {
            flagRef = config.getBoolean(path);
        }
        return flagRef;
    }

    public boolean getActiveState() {
        FlagRef flagRef = getFlagRef();
        return flagRef != null && flagRef.get();
    }

    public MutableComponent getDisplay() {
        return Component.translatableWithFallback(this.translationKey, this.translationKey);
    }

    public MutableComponent getMetaData() {
        return null;
    }
}
