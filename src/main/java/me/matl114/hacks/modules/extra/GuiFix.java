package me.matl114.hacks.modules.extra;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.managers.Configs;
import me.matl114.managers.config.FlagRef;
import me.matl114.utils.Debug;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public class GuiFix extends BaseModule {
    public final ModulePath guiFix = makePath(Configs.EXTRA_CONFIG, "other.gui-fix");

    public GuiFix() {
        super("GuiFix");
    }

    public final FlagRef noTerrain =
            flagBuilder(guiFix.add("disable-terrain-load-screen")).build();

    public final FlagRef optimizeGameMenu = builder(guiFix.add("optimize-game-menu"), FlagRef.TYPE)
            .defaultValue(true)
            .build();

    public final FlagRef optimizeServerScreen = builder(guiFix.add("optimize-server-screen"), FlagRef.TYPE)
            .defaultValue(true)
            .build();

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPreSetScreen().getChannel(LevelLoadingScreen.class), this::onTerrainScreenLoad);
        registerListener(
                Listener.getPostInitializeScreen().getChannel(PauseScreen.class),
                this::onGameMenuScreenRelocateWurstButton,
                Integer.MAX_VALUE - 1);
        registerListener(
                Listener.getPostInitializeScreen().getChannel(JoinMultiplayerScreen.class),
                this::onServerListMenuRelocateButtons,
                Integer.MAX_VALUE - 1);
    }

    public void onTerrainScreenLoad(Event<Screen> event) {
        if (noTerrain.get()) {
            event.context(null);
            // event.cancel();
        }
    }

    private static final Set<Component> VANILLA_BUTTON_TEXT;

    static {
        Set<Component> texts = new LinkedHashSet<>();
        Field[] fields = PauseScreen.class.getDeclaredFields();
        for (var re : fields) {
            try {
                if (Modifier.isStatic(re.getModifiers()) && Component.class.isAssignableFrom(re.getType())) {
                    re.setAccessible(true);
                    Component text = (Component) re.get(null);
                    if (text instanceof MutableComponent text0
                            && text0.getContents() instanceof TranslatableContents translate) {
                        texts.add(text);
                    }
                }
            } catch (Throwable e) {
            }
        }

        VANILLA_BUTTON_TEXT = texts;
    }

    public void onGameMenuScreenRelocateWurstButton(Event<PauseScreen> screenEvent) {
        if (optimizeGameMenu.get()) {
            var screen = screenEvent.context;
            List<? extends GuiEventListener> elements = screen.children();
            int extraButtons = 0;
            int lastLineY = 0;
            List<Button> extraElements = new ArrayList<>();
            for (var el : elements) {
                if (el instanceof Button button) {
                    Component text = button.getMessage();
                    if (VANILLA_BUTTON_TEXT.contains(text)) {
                        if (!button.visible) {
                            button.visible = true;
                        }
                        lastLineY = Math.max(lastLineY, button.getY());
                    } else {
                        extraElements.add(button);
                    }
                }
            }
            if (lastLineY > 0 && !extraElements.isEmpty()) {
                for (var entry : extraElements) {
                    if (entry.getWidth() > 100) {
                        extraButtons += 1;
                        entry.setY(lastLineY + 24 * extraButtons);
                    }
                }
            }
        }
    }

    public void onServerListMenuRelocateButtons(Event<JoinMultiplayerScreen> screenEvent) {
        if (optimizeServerScreen.get()) {
            var screen = screenEvent.context;
            // reschedule top buttons

            try {
                // filter title, only buttons
                List<LayoutElement> widgets = screen.children().stream()
                        .filter(s -> s instanceof LayoutElement
                                && s instanceof NarratableEntry
                                && !(s instanceof StringWidget))
                        .map(LayoutElement.class::cast)
                        .filter(s -> s.getY() < 20)
                        .collect(Collectors.toCollection(ArrayList::new));
                onResize(widgets);
            } catch (Throwable e) {
                Debug.info(e, "Error while rescheduling server list screen");
            }
        }
    }

    private void onResize(List<LayoutElement> widgets) {
        widgets.sort(Comparator.comparingInt(s -> -(s.getX() + s.getWidth())));
        int width = Integer.MAX_VALUE;
        Deque<LayoutElement> forReschedule = new ArrayDeque<>();
        for (LayoutElement widget : widgets) {
            // same height
            if (widget.getY() < 5) {
                widget.setY(5);
            }
            int xR = widget.getX() + widget.getWidth();
            if (xR < width) {
                // choice whether to place the reschedule
                if (forReschedule.isEmpty()) {
                    // no reschedule, keep the button
                    width = widget.getX();
                } else {
                    while (!forReschedule.isEmpty()) {
                        LayoutElement tryWidget = forReschedule.peekFirst();
                        int tryWidth = tryWidget.getWidth();
                        if (xR < width - tryWidth) {
                            // we should just put it here
                            // update current placed border
                            width = width - tryWidth;
                            tryWidget.setX(width);
                            forReschedule.removeFirst();
                        } else if (widget.getX() < width - tryWidth) {
                            width = width - tryWidth;
                            tryWidget.setX(width);
                            forReschedule.removeFirst();
                            forReschedule.addLast(widget);
                            break;
                        } else {
                            // keep widget here,
                            // move next
                            width = widget.getX();
                            break;
                        }
                    }
                }
            } else {
                // must reschedule
                forReschedule.addLast(widget);
            }
        }
        while (!forReschedule.isEmpty()) {
            LayoutElement widget = forReschedule.removeFirst();
            int tryWidth = widget.getWidth();
            width = width - tryWidth;
            widget.setX(width);
        }
    }
}
