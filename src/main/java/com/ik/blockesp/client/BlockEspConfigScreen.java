package com.ik.blockesp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

public class BlockEspConfigScreen extends Screen {
    private EditBox searchBox;
    private EditBox colorBox;
    private AbstractSliderButton opacitySlider;
    private AbstractSliderButton radiusSlider;
    private Button toggleButton;
    private Button saveButton;
    private Button nextButton;
    private Button prevButton;
    private Button tabBlocksButton;
    private Button tabItemsButton;
    private Button seeThroughButton;

    private boolean itemsMode = false;
    private List<Block> blockMatches = new ArrayList<>();
    private List<net.minecraft.world.item.Item> itemMatches = new ArrayList<>();
    private int index = 0;

    public BlockEspConfigScreen() {
        super(Component.literal("Block ESP Config"));
    }

    @Override
    protected void init() {
        int midX = this.width / 2;
        int y = 30;
        // Tabs Bloques/Items
        this.tabBlocksButton = Button.builder(Component.literal("Bloques"), b -> {
            itemsMode = false; index = 0; rebuildMatches(); loadCurrentIntoFields();
        }).bounds(midX - 150, y, 140, 20).build();
        this.addRenderableWidget(tabBlocksButton);
        this.tabItemsButton = Button.builder(Component.literal("Items"), b -> {
            itemsMode = true; index = 0; rebuildMatches(); loadCurrentIntoFields();
        }).bounds(midX + 10, y, 140, 20).build();
        this.addRenderableWidget(tabItemsButton);

        y += 25;
        this.searchBox = new EditBox(this.font, midX - 150, y, 300, 20, Component.literal("Buscar bloque"));
        // Restaurar la última búsqueda si existe
        if (ClientESP.lastQuery != null) {
            this.searchBox.setValue(ClientESP.lastQuery);
        }
        this.addRenderableWidget(searchBox);
        y += 30;

        this.colorBox = new EditBox(this.font, midX - 150, y, 140, 20, Component.literal("#AARRGGBB"));
        this.addRenderableWidget(colorBox);

        this.toggleButton = Button.builder(Component.literal("Activar/Desactivar"), b -> {
            if (!itemsMode) {
                Block current = getCurrentBlock();
                if (current != null) {
                    var st = ClientESP.getStyle(current);
                    if (st != null) st.enabled = !st.enabled;
                }
            } else {
                var current = getCurrentItem();
                if (current != null) {
                    var st = ClientESP.getItemStyle(current);
                    if (st != null) st.enabled = !st.enabled;
                }
            }
        }).bounds(midX + 10, y, 140, 20).build();
        this.addRenderableWidget(toggleButton);

        y += 30;
        this.opacitySlider = new AbstractSliderButton(midX - 150, y, 300, 20, Component.literal("Opacidad"), 1.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal("Opacidad: " + String.format("%.2f", this.value)));
            }

            @Override
            protected void applyValue() {
                if (!itemsMode) {
                    Block current = getCurrentBlock();
                    if (current != null) {
                        var st = ClientESP.getStyle(current);
                        if (st != null) st.opacity = (float) this.value;
                    }
                } else {
                    var current = getCurrentItem();
                    if (current != null) {
                        var st = ClientESP.getItemStyle(current);
                        if (st != null) st.opacity = (float) this.value;
                    }
                }
            }
        };
        this.addRenderableWidget(opacitySlider);

        y += 30;
        // Slider de radio global
        this.radiusSlider = new AbstractSliderButton(midX - 150, y, 300, 20, Component.literal("Radio"), clampToSlider(ClientESP.radius)) {
            @Override
            protected void updateMessage() {
                int r = (int) (this.value * 128); // 0..128
                this.setMessage(Component.literal("Radio: " + r));
            }
            @Override
            protected void applyValue() {
                ClientESP.radius = Math.max(8, (int) (this.value * 128));
            }
        };
        this.addRenderableWidget(radiusSlider);

        y += 30;
        this.prevButton = Button.builder(Component.literal("Prev"), b -> prev()).bounds(midX - 150, y, 70, 20).build();
        this.addRenderableWidget(prevButton);
        this.nextButton = Button.builder(Component.literal("Next"), b -> next()).bounds(midX - 75, y, 70, 20).build();
        this.addRenderableWidget(nextButton);

        this.seeThroughButton = Button.builder(Component.literal("Ver a través: "+ (ClientESP.seeThroughEnabled?"ON":"OFF")), b -> {
            ClientESP.seeThroughEnabled = !ClientESP.seeThroughEnabled;
            this.seeThroughButton.setMessage(Component.literal("Ver a través: "+ (ClientESP.seeThroughEnabled?"ON":"OFF")));
        }).bounds(midX + 10, y, 140, 20).build();
        this.addRenderableWidget(seeThroughButton);

        y += 25;
        this.saveButton = Button.builder(Component.literal("Guardar"), b -> save()).bounds(midX - 150, y, 300, 20).build();
        this.addRenderableWidget(saveButton);

        // Inicializar
        rebuildMatches();
        // Restaurar selección previa si existe dentro de los matches
        if (!itemsMode && ClientESP.lastSelectedBlockId != null) {
            for (int i = 0; i < blockMatches.size(); i++) {
                var id = BuiltInRegistries.BLOCK.getKey(blockMatches.get(i));
                if (ClientESP.lastSelectedBlockId.equals(id)) { index = i; break; }
            }
        }
        loadCurrentIntoFields();
    }

    private void prev() { if ((!itemsMode && !blockMatches.isEmpty()) || (itemsMode && !itemMatches.isEmpty())) { index = (index - 1 + getMatchesSize()) % getMatchesSize(); loadCurrentIntoFields(); } }
    private void next() { if ((!itemsMode && !blockMatches.isEmpty()) || (itemsMode && !itemMatches.isEmpty())) { index = (index + 1) % getMatchesSize(); loadCurrentIntoFields(); } }

    private void rebuildMatches() {
        blockMatches.clear();
        itemMatches.clear();
        String q = searchBox.getValue().trim().toLowerCase();
        ClientESP.lastQuery = searchBox.getValue();
        if (!itemsMode) {
            for (Block b : BuiltInRegistries.BLOCK) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
                String s = id.toString().toLowerCase();
                if (q.isEmpty() || s.contains(q)) {
                    blockMatches.add(b);
                }
            }
            if (index >= blockMatches.size()) index = 0;
        } else {
            for (var it : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(it);
                String s = id.toString().toLowerCase();
                if (q.isEmpty() || s.contains(q)) {
                    itemMatches.add(it);
                }
            }
            if (index >= itemMatches.size()) index = 0;
        }
    }

    private int getMatchesSize() { return itemsMode ? itemMatches.size() : blockMatches.size(); }

    private Block getCurrentBlock() {
        if (blockMatches.isEmpty()) return null; return blockMatches.get(index);
    }

    private net.minecraft.world.item.Item getCurrentItem() {
        if (itemMatches.isEmpty()) return null; return itemMatches.get(index);
    }

    private void loadCurrentIntoFields() {
        ClientConfig.BlockStyle st;
        if (!itemsMode) {
            Block b = getCurrentBlock();
            if (b == null) return;
            st = ClientESP.getStyle(b);
            if (st == null) return;
            ClientESP.lastSelectedBlockId = BuiltInRegistries.BLOCK.getKey(b);
        } else {
            var it = getCurrentItem();
            if (it == null) return;
            st = ClientESP.getItemStyle(it);
            if (st == null) return;
        }
        this.colorBox.setValue(String.format("#%08X", st.colorArgb));
        // Reconstruir slider para evitar métodos protegidos
        if (this.opacitySlider != null) {
            this.removeWidget(this.opacitySlider);
        }
        int midX = this.width / 2;
        int sliderY = 100; // coincide con init()
        this.opacitySlider = new AbstractSliderButton(midX - 150, sliderY, 300, 20, Component.literal("Opacidad"), st.opacity) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal("Opacidad: " + String.format("%.2f", this.value)));
            }

            @Override
            protected void applyValue() {
                if (!itemsMode) {
                    Block current = getCurrentBlock();
                    if (current != null) {
                        var st2 = ClientESP.getStyle(current);
                        if (st2 != null) st2.opacity = (float) this.value;
                    }
                } else {
                    var current = getCurrentItem();
                    if (current != null) {
                        var st2 = ClientESP.getItemStyle(current);
                        if (st2 != null) st2.opacity = (float) this.value;
                    }
                }
            }
        };
        this.opacitySlider.setMessage(Component.literal("Opacidad: " + String.format("%.2f", st.opacity)));
        this.addRenderableWidget(this.opacitySlider);
    }

    @Override
    public void tick() {
        super.tick();
        if (searchBox.isFocused()) {
            rebuildMatches();
        }
    }

    private void save() {
        // Aplicar color desde textfield
        String hex = colorBox.getValue();
        try { if (hex.startsWith("#")) hex = hex.substring(1); } catch (Exception ignored) {}
        // Establecer color en el elemento actual
        if (!itemsMode) {
            Block current = getCurrentBlock();
            if (current != null) {
                var st = ClientESP.getStyle(current);
                try {
                    long v = Long.parseLong(hex, 16);
                    if (hex.length() == 6) { st.colorArgb = (int)(0xFF000000L | v); }
                    else if (hex.length() == 8) { st.colorArgb = (int)v; }
                } catch (Exception ignored) {}
                ClientESP.lastSelectedBlockId = BuiltInRegistries.BLOCK.getKey(current);
            }
        } else {
            var current = getCurrentItem();
            if (current != null) {
                var st = ClientESP.getItemStyle(current);
                try {
                    long v = Long.parseLong(hex, 16);
                    if (hex.length() == 6) { st.colorArgb = (int)(0xFF000000L | v); }
                    else if (hex.length() == 8) { st.colorArgb = (int)v; }
                } catch (Exception ignored) {}
            }
        }
        ClientESP.lastQuery = searchBox.getValue();
        // Construir ConfigData desde estilos actuales
        ClientConfig.ConfigData data = new ClientConfig.ConfigData();
        data.radius = ClientESP.radius;
        data.seeThrough = ClientESP.seeThroughEnabled;
        for (var e : ClientESP.getStyles().entrySet()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(e.getKey());
            if (id != null) {
                data.blocks.put(id.toString(), e.getValue());
            }
        }
        for (var e : ClientESP.getItemStyles().entrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(e.getKey());
            if (id != null) {
                data.items.put(id.toString(), e.getValue());
            }
        }
        ClientConfig.saveConfig(data);
        // Reaplicar en memoria
        ClientESP.reloadTargets();
        // Cerrar pantalla
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static double clampToSlider(int r) {
        // Mapea 8..128 a 0..1
        int clamped = Math.max(8, Math.min(128, r));
        return clamped / 128.0;
    }
}
