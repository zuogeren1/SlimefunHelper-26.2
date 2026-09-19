package me.matl114.accessors.access;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * 直接改写当前 Screen 字段，不触发任何屏幕生命周期。
 *
 * <p>26.2 的 {@code Gui.setScreen} 不是简单赋值：每次调用都会对旧 screen 调
 * {@code removed()}、对新 screen 调 {@code added()/init(...)}、释放并重新抓取鼠标、
 * {@code KeyMapping.releaseAll()}、恢复切换态、{@code soundManager.resume()}；
 * {@code setScreen(null)} 还可能按 level/玩家存活状态替换成 TitleScreen/DeathScreen，
 * 极端情况直接抛 IllegalStateException。
 *
 * <p>需要"只是换个引用、不要副作用"的场景（例如渲染热路径里临时摘掉 Screen）
 * 必须走这里，不能调 {@code setScreen}。
 */
public interface GuiScreenAccess {

    void setScreenRaw(@Nullable Screen screen);
}
