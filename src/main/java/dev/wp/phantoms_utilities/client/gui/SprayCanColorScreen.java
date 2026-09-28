package dev.wp.phantoms_utilities.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wp.phantoms_utilities.items.SprayCan;
import dev.wp.phantoms_utilities.network.server.SprayCanColorSelectPacket;
import dev.wp.phantoms_utilities.util.PUColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.List;

// Radial picker: 16 dye wedges (enum order, clockwise from the top) around a "Clear" hub, each
// with its dye item drawn on top of the flat color.
// Outside the hub, selection is by angle alone, so the cursor need not stay on the ring.
public class SprayCanColorScreen extends Screen {
    private static final List<PUColor> RING = Arrays.stream(PUColor.values()).filter(c -> c != PUColor.CLEAR).toList();
    private static final float STEP = Mth.TWO_PI / RING.size();
    private static final float INNER_RADIUS = 28;
    private static final float OUTER_RADIUS = 80;
    private static final float HOVER_GROW = 6;
    private static final float GAP = (float) Math.toRadians(1.2);
    private static final int ARC_SEGMENTS = 6;

    private final List<ItemStack> dyes = RING.stream().map(c -> new ItemStack(DyeItem.byColor(c.dye))).toList();
    private int centerX;
    private int centerY;

    public SprayCanColorScreen() {
        super(Component.translatable("gui.phantoms_utilities.spray_can_color.title"));
    }

    @Override
    protected void init() {
        this.centerX = this.width / 2;
        this.centerY = this.height / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        PUColor hovered = colorAt(mouseX, mouseY);
        PUColor current = currentColor();
        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer buf = graphics.bufferSource().getBuffer(RenderType.gui());

        for (int i = 0; i < RING.size(); i++) {
            PUColor color = RING.get(i);
            float mid = -Mth.HALF_PI + i * STEP;
            float a0 = mid - STEP / 2 + GAP;
            float a1 = mid + STEP / 2 - GAP;
            float outer = OUTER_RADIUS + (color == hovered ? HOVER_GROW : 0);
            int argb = 0xFF000000 | color.dye.getTextureDiffuseColor();

            if (color == current) sector(buf, pose, outer, outer + 3, a0, a1, 0xFFFFFFFF);
            sector(buf, pose, INNER_RADIUS + 3, outer, a0, a1, argb);
            if (color == hovered) sector(buf, pose, INNER_RADIUS + 3, outer, a0, a1, 0x50FFFFFF);
        }

        sector(buf, pose, 0, INNER_RADIUS, 0, Mth.TWO_PI, hovered == PUColor.CLEAR ? 0xD0606060 : 0xC0202020);
        if (current == PUColor.CLEAR) sector(buf, pose, INNER_RADIUS - 2, INNER_RADIUS, 0, Mth.TWO_PI, 0xFFFFFFFF);
        graphics.flush();

        for (int i = 0; i < RING.size(); i++) {
            float mid = -Mth.HALF_PI + i * STEP;
            float r = (INNER_RADIUS + 3 + OUTER_RADIUS) / 2 + (RING.get(i) == hovered ? HOVER_GROW / 2 : 0);
            int x = Math.round(centerX + Mth.cos(mid) * r) - 8;
            int y = Math.round(centerY + Mth.sin(mid) * r) - 8;
            graphics.renderItem(dyes.get(i), x, y);
        }

        int titleY = (int) (centerY - OUTER_RADIUS - HOVER_GROW - 16);
        graphics.drawCenteredString(this.font, this.title, centerX, titleY, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable(PUColor.CLEAR.translationKey),
                centerX, centerY - this.font.lineHeight / 2 + 1, hovered == PUColor.CLEAR ? 0xFFFF55 : 0xFFFFFF);

        if (hovered != null && hovered != PUColor.CLEAR) {
            graphics.renderTooltip(this.font, Component.translatable(hovered.translationKey), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            PUColor color = colorAt(mouseX, mouseY);
            if (color != null) {
                PacketDistributor.sendToServer(new SprayCanColorSelectPacket(color));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private PUColor colorAt(double mouseX, double mouseY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        if (dx * dx + dy * dy <= INNER_RADIUS * INNER_RADIUS) return PUColor.CLEAR;

        // Shift so wedge 0 (centered straight up) starts at angle 0, then bucket.
        double angle = Math.atan2(dy, dx) + Mth.HALF_PI + STEP / 2;
        angle = ((angle % Mth.TWO_PI) + Mth.TWO_PI) % Mth.TWO_PI;
        return RING.get((int) (angle / STEP) % RING.size());
    }

    private @Nullable PUColor currentColor() {
        if (this.minecraft == null || this.minecraft.player == null) return null;
        ItemStack stack = this.minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        return stack.getItem() instanceof SprayCan can ? can.getColor(stack) : null;
    }

    // Annular sector as a strip of quads, wound like GuiGraphics.fill so culling keeps them.
    private void sector(VertexConsumer buf, Matrix4f pose, float r0, float r1, float a0, float a1, int argb) {
        int segments = Math.max(1, Mth.ceil(ARC_SEGMENTS * (a1 - a0) / STEP));
        for (int s = 0; s < segments; s++) {
            float b0 = a0 + (a1 - a0) * s / segments;
            float b1 = a0 + (a1 - a0) * (s + 1) / segments;
            float c0 = Mth.cos(b0), s0 = Mth.sin(b0), c1 = Mth.cos(b1), s1 = Mth.sin(b1);
            buf.addVertex(pose, centerX + c0 * r0, centerY + s0 * r0, 0).setColor(argb);
            buf.addVertex(pose, centerX + c1 * r0, centerY + s1 * r0, 0).setColor(argb);
            buf.addVertex(pose, centerX + c1 * r1, centerY + s1 * r1, 0).setColor(argb);
            buf.addVertex(pose, centerX + c0 * r1, centerY + s0 * r1, 0).setColor(argb);
        }
    }
}
