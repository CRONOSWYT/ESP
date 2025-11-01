package com.ik.blockesp.client;

import com.ik.blockesp.BlockEspMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

public class ClientEvents {
    private static KeyMapping toggleKey;
    private static KeyMapping reloadKey;
    private static KeyMapping openUiKey;

    @SuppressWarnings("unused")
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        toggleKey = new KeyMapping("key.blockesp.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.blockesp");
        reloadKey = new KeyMapping("key.blockesp.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.blockesp");
        openUiKey = new KeyMapping("key.blockesp.openui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, "key.categories.blockesp");
        event.register(toggleKey);
        event.register(reloadKey);
        event.register(openUiKey);
        ClientESP.reloadTargets();
    }

    public static class ForgeBusClient {

        @SuppressWarnings("unused")
        public static void onClientTick(ClientTickEvent.Post event) {
            {
                if (toggleKey != null) {
                    while (toggleKey.consumeClick()) {
                        ClientESP.enabled = !ClientESP.enabled;
                        toast(I18n.get("blockesp.status"), ClientESP.enabled ? I18n.get("blockesp.on") : I18n.get("blockesp.off"));
                    }
                }
                if (reloadKey != null) {
                    while (reloadKey.consumeClick()) {
                        ClientESP.reloadTargets();
                        toast(I18n.get("blockesp.reloaded"), "");
                    }
                }
                if (openUiKey != null) {
                    while (openUiKey.consumeClick()) {
                        Minecraft.getInstance().setScreen(new BlockEspConfigScreen());
                    }
                }
                ClientESP.onClientTick();
            }
        }

        @SuppressWarnings("unused")
        public static void onChunkLoad(ChunkEvent.Load event) {
            // Cuando el cliente recibe un chunk nuevo, reescaneamos para detectar bloques nuevos sin reiniciar
            if (ClientESP.enabled) {
                ClientESP.onChunkLoaded();
            }
        }

        @SuppressWarnings("unused")
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (!ClientESP.enabled) return;
            if (event.getLevel() == null || !event.getLevel().isClientSide()) return;
            Entity entity = event.getEntity();
            if (!(entity instanceof ItemEntity ie)) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            // Dentro del radio actual
            AABB aabb = new AABB(mc.player.blockPosition()).inflate(ClientESP.radius);
            if (!aabb.intersects(ie.getBoundingBox())) return;
            var style = ClientESP.getItemStyle(ie.getItem().getItem());
            if (style != null && style.enabled) {
                ClientESP.targetItems.add(ie);
            }
        }

        @SuppressWarnings("unused")
        public static void onRenderLevelStage(RenderLevelStageEvent.AfterBlockEntities event) {
            if (!ClientESP.enabled) return;

            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null) return;

            PoseStack poseStack = event.getPoseStack();
            Vec3 camera = event.getCamera().getPosition();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

            for (BlockPos pos : ClientESP.targetPositions) {
                BlockState state = level.getBlockState(pos);
                var style = ClientESP.getStyle(state.getBlock());
                if (style == null || !style.enabled) continue;

                int argb = style.colorArgb;
                float a = Math.max(0.05f, Math.min(1.0f, style.opacity));
                float r = ((argb >> 16) & 0xFF) / 255f;
                float g = ((argb >> 8) & 0xFF) / 255f;
                float b = (argb & 0xFF) / 255f;

                Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(camera);
                poseStack.pushPose();
                poseStack.translate(offset.x, offset.y, offset.z);

                // Caja del bloque [0,0,0] a [1,1,1]
                // Importante: obtener el buffer dentro del bucle para evitar reutilizar
                // un BufferBuilder que haya sido finalizado al pedir otro RenderType.
                VertexConsumer lineConsumer = buffers.getBuffer(ClientESP.seeThroughEnabled ? EspRenderTypes.LINES_SEE_THROUGH : RenderType.lines());
                ShapeRenderer.renderLineBox(poseStack, lineConsumer, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, r, g, b, a);

                // Relleno semitransparente con RenderType sin depth-test para ver a través de bloques
                if (ClientESP.seeThroughEnabled) {
                    VertexConsumer fillConsumer = buffers.getBuffer(EspRenderTypes.FILLED_BOX_SEE_THROUGH);
                    ShapeRenderer.addChainedFilledBoxVertices(poseStack, fillConsumer, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, r, g, b, a * 0.15f);
                }

                poseStack.popPose();
            }

            // Render de ítems (ItemEntity) con su bounding box
            for (ItemEntity ie : ClientESP.targetItems) {
                var st = ClientESP.getItemStyle(ie.getItem().getItem());
                if (st == null || !st.enabled) continue;
                int argb = st.colorArgb;
                float a = Math.max(0.05f, Math.min(1.0f, st.opacity));
                float r = ((argb >> 16) & 0xFF) / 255f;
                float g = ((argb >> 8) & 0xFF) / 255f;
                float b = (argb & 0xFF) / 255f;

                var bb = ie.getBoundingBox();
                double x0 = bb.minX - camera.x;
                double y0 = bb.minY - camera.y;
                double z0 = bb.minZ - camera.z;
                double x1 = bb.maxX - camera.x;
                double y1 = bb.maxY - camera.y;
                double z1 = bb.maxZ - camera.z;

                VertexConsumer lineConsumer = buffers.getBuffer(ClientESP.seeThroughEnabled ? EspRenderTypes.LINES_SEE_THROUGH : RenderType.lines());
                ShapeRenderer.renderLineBox(poseStack, lineConsumer, x0, y0, z0, x1, y1, z1, r, g, b, a);
                if (ClientESP.seeThroughEnabled) {
                    VertexConsumer fillConsumer = buffers.getBuffer(EspRenderTypes.FILLED_BOX_SEE_THROUGH);
                    ShapeRenderer.addChainedFilledBoxVertices(poseStack, fillConsumer, x0, y0, z0, x1, y1, z1, r, g, b, a * 0.15f);
                }
            }

            // Asegurar el envío del batch de líneas si quedó pendiente.
            mc.renderBuffers().bufferSource().endBatch(ClientESP.seeThroughEnabled ? EspRenderTypes.LINES_SEE_THROUGH : RenderType.lines());
            if (ClientESP.seeThroughEnabled) {
                mc.renderBuffers().bufferSource().endBatch(EspRenderTypes.FILLED_BOX_SEE_THROUGH);
            }
        }
    }

    private static void toast(String title, String message) {
        // Simple salida por chat; reemplazar por toast si se desea
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(title + (message.isEmpty()?"":" "+message)), true);
        }
    }
}
