package com.ik.blockesp.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;

/**
 * RenderTypes personalizados para ver a través de bloques (sin prueba de profundidad y sin escritura de profundidad).
 */
public final class EspRenderTypes {
    private EspRenderTypes() {}

    // Líneas sin depth-test para contornos del ESP
    public static final RenderType LINES_SEE_THROUGH = RenderType.create(
            "blockesp_lines_see_through",
            1536,
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/blockesp_lines_see_through")
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build(),
            RenderType.CompositeState.builder()
                    .setLineState(new RenderStateShard.LineStateShard(java.util.OptionalDouble.empty()))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false)
    );

    // Relleno sin depth-test para cajas del ESP
    public static final RenderType FILLED_BOX_SEE_THROUGH = RenderType.create(
            "blockesp_filled_box_see_through",
            1536,
            false,
            true,
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/blockesp_filled_box_see_through")
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
                    .build(),
            RenderType.CompositeState.builder()
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false)
    );
}
