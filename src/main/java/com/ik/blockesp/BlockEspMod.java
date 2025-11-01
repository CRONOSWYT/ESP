package com.ik.blockesp;

import com.ik.blockesp.client.ClientEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@Mod(BlockEspMod.MOD_ID)
public class BlockEspMod {
    public static final String MOD_ID = "blockesp";

    // NeoForge pasa IEventBus al constructor del mod
    public BlockEspMod(IEventBus modEventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener((RegisterKeyMappingsEvent e) -> ClientEvents.onRegisterKeys(e));
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientEvents.ForgeBusClient.onClientTick(e));
            NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterBlockEntities e) -> ClientEvents.ForgeBusClient.onRenderLevelStage(e));
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load e) -> ClientEvents.ForgeBusClient.onChunkLoad(e));
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> ClientEvents.ForgeBusClient.onEntityJoinLevel(e));
        }
    }
}
