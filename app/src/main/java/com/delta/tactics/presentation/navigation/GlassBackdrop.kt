package com.delta.tactics.presentation.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/** Records only the page, never the dock or dialogs, to avoid recursive feedback. */
class GlassBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier =
    onGloballyPositioned { backdrop.origin = it.positionInRoot() }
        .drawWithContent {
            backdrop.layer.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.layer)
        }

/** The effect processes a translated copy of the live page; labels are drawn separately. */
@Composable
fun Modifier.glassBackdrop(backdrop: GlassBackdrop?, radius: Float): Modifier {
    if (backdrop == null) return this
    var origin by remember { mutableStateOf(Offset.Zero) }
    val lens = if (Build.VERSION.SDK_INT >= 33) remember { GlassLens() } else null
    return onGloballyPositioned { origin = it.positionInRoot() }
        .graphicsLayer {
            if (Build.VERSION.SDK_INT >= 33 && lens != null) {
                renderEffect = lens.effect(size.width, size.height, radius, density)
            } else if (Build.VERSION.SDK_INT >= 31) {
                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                    8f * density, 8f * density, android.graphics.Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            }
        }
        .drawWithContent {
            val delta = backdrop.origin - origin
            translate(delta.x, delta.y) { drawLayer(backdrop.layer) }
        }
}

@RequiresApi(33)
private class GlassLens {
    private val shader = android.graphics.RuntimeShader("""
        uniform shader page;
        uniform float2 resolution;
        uniform float radius;
        uniform float depth;
        half4 main(float2 p) {
            float2 halfSize = resolution * 0.5;
            float r = min(radius, min(halfSize.x, halfSize.y));
            float2 local = p - halfSize;
            float2 nearest = clamp(local, -halfSize + r, halfSize - r);
            float2 v = local - nearest;
            float distanceToEdge = r - length(v);
            float edge = 1.0 - smoothstep(0.0, depth * 2.0, distanceToEdge);
            float2 normal = v / max(length(v), 0.001);
            float2 samplePos = p - normal * edge * depth;
            samplePos = clamp(samplePos, float2(1.0), resolution - 1.0);
            half4 color = page.eval(samplePos);
            float light = dot(normal, normalize(float2(-0.5, -1.0)));
            color.rgb += half3(light * edge * 0.12);
            return color;
        }
    """.trimIndent())

    fun effect(width: Float, height: Float, corner: Float, density: Float): androidx.compose.ui.graphics.RenderEffect {
        shader.setFloatUniform("resolution", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
        shader.setFloatUniform("radius", corner * density)
        shader.setFloatUniform("depth", 7f * density)
        val blur = android.graphics.RenderEffect.createBlurEffect(
            6f * density, 6f * density, android.graphics.Shader.TileMode.CLAMP
        )
        return android.graphics.RenderEffect.createChainEffect(
            android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "page"), blur
        ).asComposeRenderEffect()
    }
}
