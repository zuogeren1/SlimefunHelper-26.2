# SlimefunHelper GUI 组件文档

本库基于 Minecraft 原版 GUI 系统扩展,提供自绘组件:自定义渲染、事件处理、布局、滚动、拖拽。
核心类继承自 `DrawableWidget`,可添加到 `Screen` 或 `SubScreenWidget` 中。

> 对应代码:`src/main/java/me/matl114/gui/`
> 版本:**Minecraft 26.1.2 / 26.2**(mojmap 官方名)。组件库本身两版一致。
> 版本相关类名注意:`Component`(旧 yarn `Text`)、`Identifier`(旧 mojmap `ResourceLocation`)、
> `GuiGraphicsExtractor`(旧 yarn `DrawContext`)、`PoseStack`(旧 yarn `MatrixStack`)。

## 核心基类

### `DrawableWidget`

所有组件的基类,统一实现绘制与鼠标事件。

| 成员 | 说明 |
|------|------|
| `DrawableWidget(int x, int y, int dx, int dy)` | 构造:位置与大小 |
| `setTextureScale(float)` | 纹理缩放比例 |
| `setAlpha(float)` | 透明度(0~1) |
| `setPriority(int depth)` | 层级(影响渲染与点击顺序) |
| `setRenderHandler(RenderHandler)` / `updateRenderHandler(UnaryOperator)` | 设置/变换渲染器 |
| `addTo(Screen)` | 添加到原版屏幕 |
| `addToSub(SubScreenWidget)` / `addToSub(SubScreenWidget, int priority)` | 添加到子屏幕容器(可指定层级) |
| `extractRenderState(GuiGraphicsExtractor, int mouseX, int mouseY, float delta)` | 渲染入口(26.1 起的 extract 阶段) |
| `renderInDefaultMatrix(...)` / `renderAbsolute(...)` | 手动控制矩阵 / 绝对坐标渲染 |
| `mouseClicked(double, double, int)` / `mouseReleased(double, double, int)` | 抽象方法,子类实现 |
| `isMouseOver(double, double)` | 命中测试 |
| `setFocused(boolean)` / `isFocused()` | 焦点 |
| `setX(int)` / `setY(int)` | 位置 |

### `SubScreenWidget`

子屏幕容器,管理内部子组件的位置、渲染顺序与事件分发。

- `SubScreenWidget.instance(int x, int y, int dx, int dy)` 创建。
- `addDrawableChild(DrawableWidget)` 添加子组件。
- 自动处理坐标变换(子组件用相对坐标)。
- `childrenRenderOrder()`(按优先级升序)/ `childrenInteractOrder()`(按优先级降序),
  保证高优先级组件优先拿到点击。
- `setSelected(DrawableWidget)` / `getSelected()` 管理焦点。

```java
SubScreenWidget panel = SubScreenWidget.instance(10, 10, 200, 300);
panel.addDrawableChild(new ExecutableWidget(0, 0, 50, 20));
panel.addTo(screen);
```

### `ScrollableListWidget`

可滚动列表容器,内部自动生成滚动条并裁剪可视区域。

- 构造:`new ScrollableListWidget(int x, int y, int dx, int dy)`
- `addScrollingWidget(DrawableWidget widget)` 添加可滚动子组件
- 自动按子组件总高度计算是否需要滚动条
- 支持鼠标滚轮滚动,滚动条可拖拽
- 内部使用 `AdvancedScrollElement` 实现滚动条

```java
ScrollableListWidget list = new ScrollableListWidget(10, 10, 180, 200);
list.addScrollingWidget(new ExecutableWidget(0, 0, 160, 20));
list.addScrollingWidget(new ExecutableWidget(0, 30, 160, 20));
list.addTo(screen);
```

### `ContentDelegateWidget<W>`

包装任意原版组件(如 `Button`、`EditBox`),把原版组件当成一个 `DrawableWidget` 嵌入。

- 构造:`new ContentDelegateWidget<>(int x, int y, int dx, int dy)`
- `setContentDelegate(W delegate)` 设置被包装的组件
- 自动处理坐标缩放与事件转发

```java
ContentDelegateWidget<Button> wrapper = new ContentDelegateWidget<>(10, 10, 100, 20);
wrapper.setContentDelegate(Button.builder(Component.literal("Click"), button -> {}).build());
```

### `DelegateWidget`

包装另一个 `DrawableWidget`,所有方法与属性都委托给内部组件,适合需要动态替换内容的场景。

### `ExecutableWidget`

可交互组件,通过 `ElementHandler` 处理鼠标、键盘、滚动事件。

- `ExecutableWidget.instance(int x, int y, int dx, int dy)`
- `setElementHandler(ElementHandler)`
- 内置拖拽支持

```java
ExecutableWidget btn = ExecutableWidget.instance(10, 10, 100, 30);
btn.setElementHandler(new AbstractElement() {
    @Override
    public boolean onClick(ExecutableWidget element, double mouseX, double mouseY, int button) {
        return true;
    }
});
```

### `DisplayWidget`

空组件,不响应任何事件,仅用于占位或纯渲染(`new DisplayWidget(x, y, dx, dy)`)。

## 渲染与事件接口

### `RenderHandler`

负责在组件的局部坐标系里绘制内容。

| 回调 | 说明 |
|------|------|
| `renderAtCentered(...)` | 组件变换后的坐标系(原点为组件左上角) |
| `renderExtraAbsoluteCoord(...)` | 屏幕绝对坐标(常用于工具提示) |
| `canBeSelected(DrawableWidget)` | 是否可被高亮选中 |

内置工厂方法:

| 方法 | 说明 |
|------|------|
| `ofMatchingElement(Identifier)` | 按组件当前尺寸自动适配 UV |
| `ofMatchingElement(Identifier, u0, v0, uheight, vheight)` | 指定 UV 区域 |
| `ofElementTextureSize(Identifier[, float scaler])` | 按纹理原始大小 |
| `ofResource(Identifier[, float scaler])` | 按固定像素大小绘制 |
| `ofResource(Identifier, u0, v0, uheight, vheight)` | 指定 UV |
| `ofPositionResource(Identifier, x, y, xheight, yheight)` | 指定位图区域 |
| `ofGuiTextures(Identifier[, startX, startY, sizeX, sizeY])` | 按 1.20.2+ 的 GUI 贴图规范 |
| `ofColorQuad(int color)` | 纯色填充 |
| `ofScrollableText(Component, int color)` | 可滚动文本(过长自动滚动) |
| `ofAutoScaleText(Component, int color)` | 自动缩放以适配组件大小 |
| `ofSingleItem(Supplier<ItemStack>, int x, int y, boolean inSlot)` | 绘制单个物品 |

```java
widget.setRenderHandler(
        RenderHandler.ofMatchingElement(Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container.png"), 0, 0, 256, 256));
```

### `TooltipHandler`

继承 `RenderHandler`,在 `renderExtraAbsoluteCoord` 里根据鼠标悬停绘制提示。

```java
TooltipHandler tip = TooltipHandler.of(List.of(Component.literal("这是一个提示")));
widget.setRenderHandler(tip);
// 也可以:TooltipHandler.of(Supplier<List<Component>>) / TooltipHandler.of(TooltipProvider)
```

### `ElementHandler`

在 `RenderHandler` 的基础上增加输入处理能力。

| 回调 | 说明 |
|------|------|
| `onClick(ExecutableWidget, double mouseX, double mouseY, int button)` | 点击 |
| `onAction(ExecutableWidget, double, double, int, Type)` | 动作(按下/释放等) |
| `onScroll(...)` | 滚轮 |
| `onKey(ExecutableWidget, int keyCode, int scanCode, int modifiers, boolean isPress)` | 键盘 |
| `onTyped(ExecutableWidget, char chr, int modifiers)` | 字符输入 |
| `withTooltips(TooltipHandler)` / `withPresentCondition(Predicate)` / `withActiveActionCondition(Predicate)` | 组合装饰 |

### `AbstractElement`

`ElementHandler` 的简易实现,支持组合多个 `RenderHandler` / `InputHandler` / 工具提示。

| 方法 | 说明 |
|------|------|
| `combineRender(RenderHandler)` | 追加局部坐标渲染器 |
| `combineAbsoluteRender(RenderHandler)` | 追加绝对坐标渲染器 |
| `withInputHandler(InputHandler)` | 追加输入处理器 |
| `withTooltips(TooltipHandler)` | 追加工具提示 |
| `renderCentered0(...)` / `renderExtra0(...)` | 子类覆写的绘制入口 |

```java
ElementHandler handler = new AbstractElement() {
    @Override
    public boolean onClick(ExecutableWidget element, double mouseX, double mouseY, int button) {
        return true;
    }
};
btn.setElementHandler(handler);
```

### `VDrawContext`(绘制上下文)

版本适配层的绘制接口,组件渲染时拿到的就是它。

| 方法 | 说明 |
|------|------|
| `drawText(Font, String, x, y, color, shadow)` / `drawText(Font, FormattedCharSequence, ...)` | 画文本 |
| `drawTextWithShadow(Font, String, x, y, color)` | 带阴影 |
| `drawCenteredTextWithShadow(Font, String, centerX, y, color)` | 居中 |
| `drawTexture(...)` / `drawGuiTexture(Identifier, x, y, w, h)` / `drawSprite(...)` | 画贴图 |
| `setShaderColor(int rgba)` / `setShaderColor(r, g, b, a)` / `setShaderAlpha(float)` | 着色 |
| `pushLayer(int depth)` / `popLayer()` | 层级栈 |

## 内置组件

### `SlotElement`

物品槽组件。

```java
SlotElement slot = SlotElement.instance(new ItemStack(Items.DIAMOND))
        .setSlotFrame(true)          // 显示槽位边框
        .setInSlot(true)             // 显示物品数量
        .setShowItemTooltips(true);  // 显示物品提示
slot.addToSub(panel);
```

带点击回调:`SlotElement.instance(ItemStack, SlotClickCallback)`。

### `PlateElement`

带边框的空白面板。

```java
PlateElement plate = PlateElement.catchInteract(); // 拦截点击,不穿透到下层
PlateElement plain = PlateElement.instance();      // 不拦截
```

### `RawTextElement`

单行文本,颜色与对齐通过构造函数传入(没有 `setColor` / `setAlignment` 这样的 setter)。

```java
// instance / 构造二选一
RawTextElement a = RawTextElement.instance(Component.literal("Hello"));
RawTextElement b = new RawTextElement(Component.literal("Hello"), 0xFFFF5555, -1);
// alignment: -1 左对齐, 0 居中, 1 右对齐
```

### `LabelElement`

带背景框的文本标签。

```java
LabelElement.instance(Component.literal("标题"));
new LabelElement(Component.literal("标题"), 0xFFFFFFFF, 0);
```

### `MultiLineTextElement`

自动换行的多行文本。

```java
new MultiLineTextElement(Component.literal("很长的一段话..."), 0xFFFFFFFF, -1);
```

### `IconElement`

图标按钮,按状态切换纹理。

```java
IconElement icon = IconElement.statedGui(
        Identifier.fromNamespaceAndPath("mod", "active"),
        Identifier.fromNamespaceAndPath("mod", "inactive"),
        (btn, x, y, b) -> true);
icon.setActive(true);
```

其他工厂:`fixed(Identifier, ButtonAction)`、`stated(Identifier, Identifier, ButtonAction)`、
`statePredicate(...)`、`fixedGui(Identifier, ButtonAction)`、`statedGuiPredicate(...)`。

### `ButtonElement`

Minecraft 风格按钮(继承 `IconElement.SimpleIconElement`),带文本标签。

```java
ButtonElement btn = new ButtonElement(TextProvider.of(Component.literal("确认")), (e, x, y, b) -> true);
```

### `AdvancedScrollElement`

滚动条组件,通常由 `ScrollableListWidget` 内部使用,也可单独使用。

### 其他可用组件

`BoxElement`、`ColorBoxElement`、`ColorLabelTextElement`、`ColorSplitterElement`、
`OutputSlotElement`、`PageButtonElement`、`ResetButtonElement`、`TextFieldElement`、
`DynamicContentWidget`、`DynamicListWidget`、`DynamicSubScreenWidget`、
`GridSubScreen`、`PageSwitchSubScreen`。

## 组合使用示例

```java
SubScreenWidget panel = SubScreenWidget.instance(50, 50, 200, 300);
panel.setTextureScale(2.0f);

ExecutableWidget btn = ExecutableWidget.instance(10, 10, 80, 20);
btn.setElementHandler(new AbstractElement() {
    @Override
    public void renderCentered0(DrawableWidget element, VDrawContext context, int mouseX, int mouseY,
                                float delta, float alpha, boolean shouldHighlight) {
        context.drawCenteredTextWithShadow(mc.font, "Click", 40, 5, 0xFFFFFFFF);
    }

    @Override
    public boolean onClick(ExecutableWidget element, double mouseX, double mouseY, int button) {
        return true;
    }
});

SlotElement slot = SlotElement.instance(new ItemStack(Items.EMERALD))
        .withTooltips(TooltipHandler.of(List.of(Component.literal("绿宝石"))));

panel.addDrawableChild(btn);
panel.addDrawableChild(slot);
panel.addTo(screen);
```

## 注意事项

- 所有组件坐标都是相对父容器的,`SubScreenWidget` 会自动做坐标变换。
- `textureScale` 会影响子组件的渲染大小与鼠标点击坐标换算,谨慎使用。
- 组件 `priority` 越高,渲染越靠后(越在上层),点击越优先。
- 拖拽需要组件实现 `Draggable`(`DrawableWidget` 已实现),参考 `gui/basic/Draggable.java`。
- 26.2 的渲染拆成了 extract / draw 两阶段,`DrawableWidget` 的入口是
  `extractRenderState(GuiGraphicsExtractor, mouseX, mouseY, delta)`,不要再找旧版的 `render0(...)`。