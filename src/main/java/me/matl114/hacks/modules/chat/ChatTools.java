package me.matl114.hacks.modules.chat;

import me.matl114.utils.ClientUtils;

import io.github.reserveword.imblocker.common.gui.FocusableObject;
import java.util.List;
import me.matl114.accessors.access.ChatScreenAccess;
import me.matl114.accessors.gui.ScreenAccess;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.gui.McWidgetHelpers;
import me.matl114.gui.basic.*;
import me.matl114.gui.elements.ButtonElement;
import me.matl114.gui.elements.IconElement;
import me.matl114.hacks.ChatTasks;
import me.matl114.hacks.MainTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.utils.HotKeyUtils;
import me.matl114.hooks.IMBlockerHooks;
import me.matl114.managers.Configs;
import me.matl114.managers.ScheduleService;
import me.matl114.managers.TaskManagers;
import me.matl114.managers.Tasks;
import me.matl114.managers.config.*;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.ChatUtils;
import me.matl114.utils.ScreenUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class ChatTools extends BaseModule {
    public final ModulePath chatTools = makePath(Configs.CHAT_CONFIG, "chat-screen-tools");

    public ChatTools() {
        super("ChatTools");
        bindFlag(enableChatScreenTools);
    }

    public final FlagRef enableChatScreenTools = flagBuilder(chatTools.add("enable-tools"))
            .updateListener(this::toggleBasicToolScreen)
            .build();

    public final FlagRef enableSpecialChars = flagBuilder(chatTools.add("enable-quick-chars"))
            .updateListener(this::toggleSpecialCharWidget)
            .build();

    public final StringRef specialChars = builder(chatTools.add("quick-chars"), String.class)
            .defaultValue("😡🤓🥵😭🤡😋🤤😊😄🥲😁👉👆🤔😎🐍😅♂♀")
            .updateListener(this::refreshSpecialChars)
            .build();

    public final StringRef chatCache =
            builder(chatTools.add("cached"), String.class).defaultValue("").build();

    public final FlagRef autoSend = flagBuilder(
                    Configs.CHAT_CONFIG, chatTools.add("auto-chat").toPath())
            .build();

    public final IntRef period =
            intBuilder(chatTools.add("auto-chat-period")).defaultValue(21).build();

    public final IntRef multiple =
            intBuilder(chatTools.add("auto-chat-multiple")).defaultValue(1).build();

    public final FlagRef keepChatInv = flagBuilder(
                    Configs.CHAT_CONFIG, chatTools.add("keep-chat-inv").toPath())
            .build();

    public final FlagRef obfLogin =
            flagBuilder(chatTools.add("obf-login-message")).build();

    public final KeyBindRef removeCmdKey = hotkey(
                    Configs.CHAT_CONFIG,
                    chatTools.add("remove-command-prefix-hotkey").toPath(),
                    new MultiKeyBind())
            .registerHotkey(HotKeyUtils.asHandler(this::onRemoveCommandPrefix))
            .build();

    @Override
    public void onDisableModule() {
        super.onDisableModule();
        counter = 0;
    }

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPostGameTick(), this::onTick);
        registerListener(Listener.getPostInitializeScreen().getChannel(ChatScreen.class), this::onChatScreenInitialize);
        registerListener(Listener.getPreSetScreen().getChannel(ChatScreen.class), this::onCloseChatScreen);
        registerListener(Listener.getPostInitializeScreen().getChannel(ChatScreen.class), this::fixIMBlockerStateError);
        TaskManagers.getToggleManager().register(TaskManagers.PREFIX_BUTTON_TOGGLE + "." + "auto-chat", autoSend);
        TaskManagers.getToggleManager()
                .register(TaskManagers.PREFIX_BUTTON_TOGGLE + "." + "keep-chat-inv", keepChatInv);
    }

    public void onRemoveCommandPrefix() {
        if (ClientUtils.getScreen(mc) instanceof ChatScreen chatScreen) {
            if (chatScreen.getFocused() instanceof EditBox widget
                    && widget.getValue().startsWith("/")) {
                widget.setValue(widget.getValue().substring(1));
            }
        }
    }

    public int counter = 0;

    public void onTick(Event<LocalPlayer> gt) {
        if (mc.getConnection() != null && autoSend.get()) {
            counter += 1;
            if (counter >= period.getValue()) {
                counter = 0;
                for (var i = 0; i < multiple.get(); ++i) {
                    sendCachedMessage();
                }
            }
        }
    }

    private static final Identifier LOCK_ENABLE_SPRITE = Identifier.tryParse("slimefunhelper:gui/lock_enable");
    private static final Identifier LOCK_DISABLE_SPRITE = Identifier.tryParse("slimefunhelper:gui/lock_disable");
    private static final List<Component> TOOLTIPS_CHAT_TOOLS = List.of(Component.literal("点击展开/关闭聊天框小工具栏"));
    private static final List<Component> TOOLTIPS_SEND_CACHE = List.of(Component.literal("发送缓存聊天框中的东西"));
    private static final List<Component> TOOLTIPS_AUTO_SEND =
            List.of(Component.literal("自动发送缓存聊天框中的东西"), Component.literal("查看配置界面以调整参数"));
    private static final List<Component> TOOLTIPS_KEEP_INV =
            List.of(Component.literal("切换是否keepChatInv"), Component.literal("若启用,回车发送文字后将仍保持在聊天界面"));
    private static final List<Component> TOOLTIPS_SEL_TO_UNICODE =
            List.of(Component.literal("点击将当前正在输入的输入框中"), Component.literal("输入的字符转为unicode字符"));
    private static final List<Component> TOOLTIPS_INT_TO_CHAR =
            List.of(Component.literal("可以将旁边的小输入框中的数字和字符进行ascii转换"));
    private static final List<Component> TOOLTIPS_ENCRYPT = List.of(
            Component.literal("左击切换是否进行消息加密"), Component.literal("右击以打开配置文件"), Component.literal("按住ctrl发送可以禁用加密"));
    private static final List<Component> TOOLTIPS_FORMAT =
            List.of(Component.literal("左击切换是否进行聊天格式化"), Component.literal("右击以打开配置文件"));
    private static final List<Component> TOOLTIPS_SPECIAL_CHARS =
            List.of(Component.literal("点击展开/关闭特殊字符快捷键"), Component.literal("可以在配置界面中配置特殊字符列表"));

    public void sendCachedMessage() {
        String val = chatCache.get();
        if (val != null) {
            ChatTasks.sayMessage(val, false);
        }
    }

    private String int2CharFieldContent = "";

    private void tranlateInt2char(EditBox int2CharInputField) {
        String value = int2CharInputField.getValue();
        if (value.isEmpty()) return;
        try {
            int val = Integer.parseInt(value);
            try {
                char ch = (char) val;
                int2CharInputField.setValue(String.valueOf(ch));
            } catch (Throwable e) {
                int2CharInputField.setValue("Error");
            }
        } catch (Throwable e) {
            char ch = value.charAt(0);
            int2CharInputField.setValue(String.valueOf(((int) ch)));
        }
    }

    SubScreenWidget basicSubScreenWidget;
    ContentDelegateWidget<SubScreenWidget> delegateToolScreen;
    ContentDelegateWidget<SubScreenWidget> delegateSpecialCharWidget;
    EditBox cacheWidget;
    EditBox int2CharInputField;

    @Nullable
    private EditBox findCurrentFocusing() {
        if (ClientUtils.getScreen(mc) instanceof ChatScreen chat && chat.getFocused() instanceof EditBox textField) {
            return textField;
        } else if (basicSubScreenWidget != null
                && basicSubScreenWidget.isFocused()
                && basicSubScreenWidget.getSelected() instanceof ContentDelegateWidget<?> contentDelegateWidget
                && contentDelegateWidget.getDelegate() instanceof EditBox text) {
            return text;
        } else return null;
    }

    private static final Component ENABLE_STATE = Component.literal("-").setStyle(Style.EMPTY.withBold(true));
    private static final Component DISABLE_STATE = Component.literal("+").setStyle(Style.EMPTY.withBold(true));

    private void initToolWidget() {
        int totalWith = 250; // -250 ~ 0
        int totalHeight = 68; //  -104 ~ -36
        SubScreenWidget basicSubScreenWidget = new SubScreenWidget(0, 0, 250, 68);
        // -56 -> -56 - (-104)
        ContentDelegateWidget<EditBox> helperWidgetWrapper = McWidgetHelpers.createTextFieldEditBox(
                0, 48, 120, 20, chatCache::set, chatCache.get());
        cacheWidget = helperWidgetWrapper.getDelegate();

        // todo： add translatable to buttons and everything
        ExecutableWidget.instance(120, 48, 60, 20)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.literal("send cache")),
                                ButtonAction.run(() -> ChatTasks.sayMessage(chatCache.get(), true)))
                        .withTooltips(TooltipHandler.of(TOOLTIPS_SEND_CACHE)))
                .addToSub(basicSubScreenWidget);

        Runnable toggle = HotKeyUtils.getToggleTask(
                Configs.CHAT_CONFIG, chatTools.add("auto-chat").toPath());
        ExecutableWidget.instance(180, 48, 50, 20)
                .setElementHandler(
                        new ButtonElement(TextProvider.of(Component.literal("auto-send")), ButtonAction.run(toggle))
                                .setActivePredicate(el -> autoSend.get())
                                .withTooltips(TooltipHandler.of(TOOLTIPS_AUTO_SEND)))
                .addToSub(basicSubScreenWidget);

        Runnable toggle2 = HotKeyUtils.getToggleTask(
                Configs.CHAT_CONFIG, chatTools.add("keep-chat-inv").toPath());
        ExecutableWidget.instance(180, 24, 70, 20)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.literal("keep-chat-inv")), ButtonAction.run(toggle2))
                        .setActivePredicate((el) -> keepChatInv.get())
                        .withTooltips(TooltipHandler.of(TOOLTIPS_KEEP_INV)))
                .addToSub(basicSubScreenWidget);

        ExecutableWidget.instance(120, 24, 60, 20)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.literal("to-unicode")), ButtonAction.run(() -> {
                                    EditBox widget = findCurrentFocusing();
                                    if (widget != null) {
                                        widget.setValue(ChatUtils.toUnicodedString(widget.getValue()));
                                    }
                                }))
                        .withTooltips(TooltipHandler.of(TOOLTIPS_SEL_TO_UNICODE)))
                .addToSub(basicSubScreenWidget);
        ExecutableWidget.instance(0, 24, 20, 20)
                .setElementHandler(IconElement.statedGuiPredicate(
                                LOCK_ENABLE_SPRITE,
                                LOCK_DISABLE_SPRITE,
                                ButtonAction.isLeft((i) -> {
                                    if (i) {
                                        ChatTasks.getEncryptChat().encrypt.toggle();
                                    } else {
                                        MainTasks.openModuleScreen(ChatTasks.getEncryptChat());
                                    }
                                }),
                                (el) -> ChatTasks.getEncryptChat().shouldEncryptSendMessage())
                        .withTooltips(TooltipHandler.of(TOOLTIPS_ENCRYPT)))
                .addToSub(basicSubScreenWidget);
        ExecutableWidget.instance(20, 24, 20, 20)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.literal("F").withStyle(ChatFormatting.BOLD)),
                                ButtonAction.isLeft((i) -> {
                                    if (i) {
                                        ChatTasks.getChatExtra().enableFormat.toggle();
                                    } else {
                                        MainTasks.openModuleScreen(ChatTasks.getChatExtra());
                                    }
                                }))
                        .setActivePredicate(
                                (el) -> ChatTasks.getChatExtra().enableFormat.get())
                        .withTooltips(TooltipHandler.of(TOOLTIPS_FORMAT)))
                .addToSub(basicSubScreenWidget);

        ContentDelegateWidget<EditBox> helperWidget = McWidgetHelpers.createTextFieldEditBox(
                40, 24, 40, 20, s -> int2CharFieldContent = s, int2CharFieldContent);
        int2CharInputField = helperWidget.getDelegate();

        ExecutableWidget.instance(80, 24, 40, 20)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.literal("int<->char")),
                                ButtonAction.run(() -> tranlateInt2char(int2CharInputField)))
                        .withTooltips(TooltipHandler.of(TOOLTIPS_INT_TO_CHAR)))
                .addToSub(basicSubScreenWidget);
        ContentDelegateWidget<SubScreenWidget> widgetQuickChars =
                new ContentDelegateWidget<>(250, 0, 0, 0).addToSub(basicSubScreenWidget);

        ExecutableWidget.instance(225, 0, 22, 20)
                .setElementHandler(new ButtonElement(
                                (el) -> enableSpecialChars.get() ? ENABLE_STATE : DISABLE_STATE,
                                ButtonAction.run(enableSpecialChars::toggle))
                        .withTooltips(TooltipHandler.of(TOOLTIPS_SPECIAL_CHARS)))
                .addToSub(basicSubScreenWidget);
        this.delegateSpecialCharWidget = widgetQuickChars;
        this.basicSubScreenWidget = basicSubScreenWidget;
        toggleSpecialCharWidget(this.enableSpecialChars.get());
        this.delegateToolScreen = new ContentDelegateWidget<>(-250, -68, 0, 0);
        toggleBasicToolScreen(this.enableChatScreenTools.get());
    }

    private void toggleSpecialCharWidget(boolean bl) {
        if (this.delegateSpecialCharWidget != null) {
            if (bl) {
                String specialChar = specialChars.getValue();
                int len = specialChar.length();
                int totalY = ((len + 1 - 1) / 4) + 1;
                int x = 1, xm = 4;
                int y = 0;
                SubScreenWidget specialCharWidgets = new SubScreenWidget(-100, -24 * totalY, 100, 24 * totalY);

                for (int i = 0; i < len; i++) {
                    x += 1;
                    char c = specialChar.charAt(i);
                    String value = String.valueOf(c);
                    if (!ChatUtils.isNormalCharacter(c)) {
                        ++i;
                        if (i < len) {
                            char d = specialChar.charAt(i);
                            value = new String(new char[] {c, d});
                        }
                    }
                    final String valueOfChar = value;
                    ExecutableWidget.instance(100 - 25 * x, (totalY - y) * 24, 22, 20)
                            .setElementHandler(new ButtonElement(
                                    TextProvider.of(Component.literal(valueOfChar)), ButtonAction.run(() -> {
                                        EditBox focused = findCurrentFocusing();
                                        if (focused != null) {
                                            focused.insertText(valueOfChar);
                                        }
                                    })))
                            .addToSub(specialCharWidgets);

                    if (x >= xm) {
                        x = 0;
                        y += 1;
                    }
                }
                this.delegateSpecialCharWidget.setContentDelegate(specialCharWidgets);
            } else {
                this.delegateSpecialCharWidget.setContentDelegate(null);
            }
        } else {
            // return
        }
    }

    private void refreshSpecialChars(String specialChars) {
        toggleSpecialCharWidget(enableSpecialChars.get());
    }

    private void toggleBasicToolScreen(boolean bl) {
        if (delegateToolScreen != null) {
            if (bl) {
                delegateToolScreen.setContentDelegate(basicSubScreenWidget);
            } else {
                delegateToolScreen.setContentDelegate(null);
            }
            if (ClientUtils.getScreen(mc) instanceof ChatScreen chat) {
                ScreenAccess access = ScreenAccess.of(chat);
                // do not make concurrent modification
                Tasks.scheduleDelayed(
                        () -> {
                            access.removeChildFrom(cacheWidget);
                            access.removeChildFrom(int2CharInputField);
                            if (bl) {
                                access.addDrawableChildTo(cacheWidget);
                                access.addDrawableChildTo(int2CharInputField);
                            }
                        },
                        0);
            }
        }
    }

    public void onChatScreenInitialize(Event<ChatScreen> event) {
        var chat0 = event.context;
        if (basicSubScreenWidget == null) {
            initToolWidget();
        }
        ExecutableWidget.instance(chat0.width - 20, chat0.height - 56, 20, 20)
                .setElementHandler(new ButtonElement(
                                (el) -> enableChatScreenTools.get() ? ENABLE_STATE : DISABLE_STATE,
                                ButtonAction.run(enableChatScreenTools::toggle))
                        .withTooltips(TooltipHandler.of(TOOLTIPS_CHAT_TOOLS)))
                .addTo(chat0);
        var content =
                new ContentDelegateWidget<ContentDelegateWidget<SubScreenWidget>>(chat0.width, chat0.height - 36, 0, 0);
        content.setContentDelegate(this.delegateToolScreen);
        content.addTo(chat0);
        var access = ScreenAccess.of(chat0);
        cacheWidget.setX(chat0.width - 250);
        cacheWidget.setY(chat0.height - 56);
        int2CharInputField.setX(chat0.width - 210);
        int2CharInputField.setY(chat0.height - 104 + 24);
        if (enableChatScreenTools.get()) {
            access.addDrawableChildTo(cacheWidget);
            access.addDrawableChildTo(int2CharInputField);
        }
    }

    public void onCloseChatScreen(Event<ChatScreen> event) {
        // do not consider subClasses
        if (keepChatInv.get()
                && mc.player != null
                && mc.level != null
                && ClientUtils.getScreen(mc) != null
                && ClientUtils.getScreen(mc).getClass() == ChatScreen.class
                && ScreenUtils.hasEnterDown()) {
            ChatScreenAccess access = ChatScreenAccess.of((ChatScreen) ClientUtils.getScreen(mc));
            access.resetMessageHistoryIndex();
            event.cancel();
        }
    }

    public void fixIMBlockerStateError(Event<ChatScreen> event) {
        if (IMBlockerHooks.getInstance().isEnabled()
                && event.context.getFocused() instanceof FocusableObject focusableObject) {
            var chat = event.context;
            ScheduleService.launchAsyncDelayedTask(
                    () -> {
                        if (chat.getFocused() == focusableObject) {
                            focusableObject.updateEnglishState();
                        }
                    },
                    100L);
        }
    }
}
