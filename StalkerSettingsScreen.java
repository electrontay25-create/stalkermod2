package com.stalkermod.client;

import com.stalkermod.init.StalkerConfig;
import com.stalkermod.network.ModNetwork;
import com.stalkermod.network.SaveSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ventana de ajustes del Stalker (se abre manteniendo presionado el Reloj del Stalker).
 */
public class StalkerSettingsScreen extends Screen {

    private static final int PANEL_W = 250;
    private static final int PANEL_H = 188;

    private final int minutes;
    private final int volume;
    private final int notice;

    private EditBox minutesBox;
    private EditBox noticeBox;
    private VolumeSlider slider;
    private int left;
    private int top;

    public StalkerSettingsScreen(int minutes, int volume, int notice) {
        super(Component.literal("Stalker - Ajustes"));
        this.minutes = minutes;
        this.volume = volume;
        this.notice = notice;
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;
        int fx = this.left + 12;
        int fw = PANEL_W - 24;

        this.minutesBox = new EditBox(this.font, fx, this.top + 44, fw, 20, Component.literal("minutos"));
        this.minutesBox.setMaxLength(3);
        this.minutesBox.setFilter(s -> s.matches("\\d*"));
        this.minutesBox.setValue(String.valueOf(this.minutes));
        this.addRenderableWidget(this.minutesBox);

        this.slider = new VolumeSlider(fx, this.top + 76, fw, 20, this.volume);
        this.addRenderableWidget(this.slider);

        this.noticeBox = new EditBox(this.font, fx, this.top + 118, fw, 20, Component.literal("segundos"));
        this.noticeBox.setMaxLength(2);
        this.noticeBox.setFilter(s -> s.matches("\\d*"));
        this.noticeBox.setValue(String.valueOf(this.notice));
        this.addRenderableWidget(this.noticeBox);

        this.addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> this.save())
                .bounds(fx, this.top + 150, fw, 22).build());
    }

    private void save() {
        int m = parse(this.minutesBox.getValue(), this.minutes);
        int n = parse(this.noticeBox.getValue(), this.notice);
        m = Math.max(1, Math.min(240, m));
        n = Math.max(1, Math.min(30, n));
        int v = this.slider.getPercent();

        StalkerConfig.AMBIENT_VOLUME_PERCENT.set(v);          // volumen: lo lee este mismo cliente
        ModNetwork.CHANNEL.sendToServer(new SaveSettingsPacket(m, v, n));
        this.onClose();
    }

    private static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.fill(this.left - 2, this.top - 2, this.left + PANEL_W + 2, this.top + PANEL_H + 2, 0xFFB5B5B5); // borde
        g.fill(this.left, this.top, this.left + PANEL_W, this.top + PANEL_H, 0xFF2D2D2F);                 // fondo

        g.drawCenteredString(this.font, this.title, this.width / 2, this.top + 10, 0xFFFFFFFF);
        int fx = this.left + 12;
        g.drawString(this.font, "Minutos entre apariciones (min 1)", fx, this.top + 30, 0xFFFFFFFF);
        g.drawString(this.font, "Segundos que lo miras antes de que se esconda", fx, this.top + 104, 0xFFFFFFFF);

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Barra deslizante 0-100 % para el volumen de la musica. */
    private static class VolumeSlider extends AbstractSliderButton {

        VolumeSlider(int x, int y, int w, int h, int percent) {
            super(x, y, w, h, Component.empty(), percent / 100.0D);
            this.updateMessage();
        }

        int getPercent() {
            return (int) Math.round(this.value * 100.0D);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal("Volumen de la musica: " + this.getPercent() + "%"));
        }

        @Override
        protected void applyValue() {
            // el valor se lee al guardar
        }
    }
}
