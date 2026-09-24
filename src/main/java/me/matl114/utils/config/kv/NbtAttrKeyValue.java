package me.matl114.utils.config.kv;

import java.util.function.Consumer;
import java.util.function.Function;
import lombok.experimental.Accessors;
import me.matl114.gui.McWidgetHelpers;
import me.matl114.gui.basic.ContentDelegateWidget;
import me.matl114.utils.config.BaseAttrKeyValue;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;

@Accessors(chain = true)
public class NbtAttrKeyValue<W> extends BaseAttrKeyValue<Tag> {
    protected final Function<Tag, W> nbtParser;

    public NbtAttrKeyValue<W> setEnableNull(boolean val) {
        this.enableNull = val;
        return this;
    }

    protected boolean enableNull = false;

    public NbtAttrKeyValue(String key, Tag value, Function<Tag, W> function) {
        super(key, value == null ? null : value.copy(), AttrKeyValues.NBT_FACTORY);
        this.nbtParser = function;
        addValidator(s -> {
            if (s != null) {
                return extraParse(s);
            } else {
                return enableNull;
            }
        });
    }

    private boolean extraParse(Tag element) {
        try {
            nbtParser.apply(element);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public void applyFormatting(Consumer<String> callback) {
        if (validate) {
            try {
                setInput(new SnbtPrinterTagVisitor().visit(this.get()));
                callback.accept(this.getInput());
            } catch (Throwable e) {
            }
        }
    }

    public ContentDelegateWidget<MultiLineEditBox> generateEditBox(int x, int y, int dx, int dy) {
        //            EditBoxWidget widget = new EditBoxWidget(Minecraft.getInstance().textRenderer, x,y,
        // dx,dy, Text.empty(), Text.empty());
        //            widget.setText(this.value);
        //            widget.setChangeListener((val)->setInput(val));
        //            TextFieldAccess.of(widget).setBorderColorProvider();
        return McWidgetHelpers.createMultiLineEditBox(
                x,
                y,
                dx,
                dy,
                (val) -> setInput(val),
                this.getInput(),
                McWidgetHelpers.getWrongRedTextBoxColorProvider(() -> validate));
        //            return widget;
    }
}
