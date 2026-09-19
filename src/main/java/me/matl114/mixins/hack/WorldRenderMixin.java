package me.matl114.mixins.hack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 本 mixin 当前为空 —— 原本挂在 {@code LevelRenderer} 上的两个钩子已迁走：
 * <ul>
 *   <li>「无天气」({@code NoRender.noWeather}) → {@code hack.WeatherEffectRendererMixin}
 *       （26.2 世界渲染提取搬到 {@code LevelExtractor} / {@code WeatherEffectRenderer}，
 *       {@code renderLevel} 里已无该调用点）。
 *   <li>「屏蔽黑暗 / 失明」→ {@code events.CameraEvents}
 *       （26.2 在 {@code Camera.extractRenderState} 里读 BLINDNESS / DARKNESS）。
 * </ul>
 *
 * <p>另外「隐藏实体」({@code NoRender.entity.force-no}) 在两层版本里都是网络包层面的实现
 * （取消 {@code ClientboundAddEntityPacket}），不在这里做。
 *
 * <p>保留本类是为了让 {@code LevelRenderer} 的注入点有明确的归属说明位置；
 * 若以后需要在 {@code LevelRenderer} 上挂新钩子，直接加在这里。
 */
@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class WorldRenderMixin {}
