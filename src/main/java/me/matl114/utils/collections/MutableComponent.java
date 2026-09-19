package me.matl114.utils.collections;

import java.util.Optional;
import lombok.AllArgsConstructor;
import net.minecraft.core.component.DataComponentType;

@AllArgsConstructor
public class MutableComponent<T> {

    public DataComponentType<T> type;
    public Optional<T> value;
}
