package me.matl114.gui.basic;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import me.matl114.versioned.api.VDrawContext;
import net.minecraft.network.chat.Component;

public class TooltipHandler implements RenderHandler {
    final TooltipProvider provider;

    public static TooltipHandler of(List<Component> list) {
        return new TooltipHandler(list);
    }

    public static TooltipHandler of(TooltipProvider provider) {
        return new TooltipHandler(provider);
    }

    public static TooltipHandler of(Supplier<List<Component>> listSupplier) {
        return new TooltipHandler(TooltipProvider.of(listSupplier));
    }

    public TooltipHandler(List<Component> provider) {
        this(TooltipProvider.of(provider));
    }

    public TooltipHandler(TooltipProvider provider) {
        this.provider = provider;
    }

    @Override
    public final void renderAtCentered(
            DrawableWidget element,
            VDrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha,
            boolean shouldHighlight) {}

    public void renderExtraAbsoluteCoord(
            DrawableWidget element,
            VDrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha,
            boolean shouldHighlight) {

        if (shouldHighlight) {
            if (provider != null) {
                List<Component> texts = provider.getTooltips(element);
                if (texts != null && !texts.isEmpty()) {
                    context.drawTooltip(mc.font, texts, Optional.empty(), mouseX, mouseY);
                }
            }
        }
    }

    public interface TooltipProvider {
        List<Component> getTooltips(DrawableWidget element);

        static TooltipProvider of(List<Component> a) {
            return (e) -> a;
        }

        static TooltipProvider of(Supplier<List<Component>> t) {
            return (e) -> t.get();
        }
    }
}
