package it.hurts.sskirillss.relics.client.screen.description;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.sskirillss.relics.client.screen.description.widgets.ability.AbilityRerollButtonWidget;
import it.hurts.sskirillss.relics.client.screen.description.widgets.ability.AbilityResetButtonWidget;
import it.hurts.sskirillss.relics.client.screen.description.widgets.ability.AbilityUpgradeButtonWidget;
import it.hurts.sskirillss.relics.indev.RelicAbilityEntry;
import it.hurts.sskirillss.relics.indev.RelicAbilityStat;
import it.hurts.sskirillss.relics.indev.RelicDataNew;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.tiles.ResearchingTableTile;
import it.hurts.sskirillss.relics.utils.Reference;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class AbilityDescriptionScreen extends Screen {
    private final Minecraft MC = Minecraft.getInstance();

    public static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MODID, "textures/gui/description/ability_background.png");

    public final BlockPos pos;
    public ItemStack stack;
    public final String ability;

    public int backgroundHeight = 177;
    public int backgroundWidth = 256;

    public AbilityDescriptionScreen(BlockPos pos, ItemStack stack, String ability) {
        super(TextComponent.EMPTY);

        this.pos = pos;
        this.stack = stack;
        this.ability = ability;
    }

    @Override
    protected void init() {
        if (!(stack.getItem() instanceof RelicItem<?> relic))
            return;

        TextureManager manager = MC.getTextureManager();

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        manager.bindForSetup(TEXTURE);

        this.addRenderableWidget(new AbilityUpgradeButtonWidget(((this.width - backgroundWidth) / 2) + 209, ((this.height - backgroundHeight) / 2) + 93, this, ability));
        this.addRenderableWidget(new AbilityRerollButtonWidget(((this.width - backgroundWidth) / 2) + 209, ((this.height - backgroundHeight) / 2) + 116, this, ability));
        this.addRenderableWidget(new AbilityResetButtonWidget(((this.width - backgroundWidth) / 2) + 209, ((this.height - backgroundHeight) / 2) + 139, this, ability));
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        ClientLevel world = MC.level;

        if (world == null || !(world.getBlockEntity(pos) instanceof ResearchingTableTile tile))
            return;

        stack = tile.getStack();

        if (!(stack.getItem() instanceof RelicItem<?> relic))
            return;

        RelicDataNew relicData = relic.getNewData();

        if (relicData == null)
            return;

        RelicAbilityEntry abilityData = RelicItem.getAbility(relic, ability);

        if (abilityData == null)
            return;

        TextureManager manager = MC.getTextureManager();

        this.renderBackground(pPoseStack);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        manager.bindForSetup(TEXTURE);

        int texWidth = 512;
        int texHeight = 512;

        int x = (this.width - backgroundWidth) / 2;
        int y = (this.height - backgroundHeight) / 2;

        blit(pPoseStack, x, y, 0, 0, backgroundWidth, backgroundHeight, texWidth, texHeight);

        int level = RelicItem.getAbilityPoints(stack, ability);
        int maxLevel = abilityData.getMaxLevel() == -1 ? ((relicData.getLevelingData().getMaxLevel() - abilityData.getRequiredLevel()) / abilityData.getRequiredPoints()) : abilityData.getMaxLevel();

        MutableComponent name = new TranslatableComponent("tooltip.relics." + relic.getRegistryName().getPath() + ".ability." + ability)
                .append(new TranslatableComponent("tooltip.relics.relic.ability.level", level, maxLevel == -1 ? "∞" : maxLevel));

        MC.font.drawShadow(pPoseStack, name, x + ((backgroundWidth - MC.font.width(name)) / 2F), y + 6, 0xFFFFFF);

        List<FormattedCharSequence> lines = MC.font.split(new TranslatableComponent("tooltip.relics." + stack.getItem().getRegistryName().getPath() + ".ability." + ability + ".description"), 320);

        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);

            pPoseStack.pushPose();

            pPoseStack.scale(0.5F, 0.5F, 0.5F);

            MC.font.draw(pPoseStack, line, x * 2 + 65 * 2, y * 2 + i * 9 + 33 * 2, 0x412708);

            pPoseStack.popPose();
        }

        boolean isHoveringUpgrade = (pMouseX >= x + 209
                && pMouseY >= y + 93
                && pMouseX < x + 209 + 22
                && pMouseY < y + 93 + 22);

        boolean isHoveringReroll = (pMouseX >= x + 209
                && pMouseY >= y + 116
                && pMouseX < x + 209 + 22
                && pMouseY < y + 116 + 22);

        boolean isHoveringReset = (pMouseX >= x + 209
                && pMouseY >= y + 139
                && pMouseX < x + 209 + 22
                && pMouseY < y + 139 + 22);

        Set<String> stats = RelicItem.getAbilityInitialValues(stack, ability).keySet();

        int yOff = 0;

        for (int i = 0; i < stats.size(); i++) {
            String stat = stats.stream().toList().get(i);

            RelicAbilityStat statData = RelicItem.getAbilityStat(relic, ability, stat);

            if (statData != null) {
                TextComponent cost = new TextComponent(statData.getFormatValue().apply(RelicItem.getAbilityValue(stack, ability, stat)));

                if (isHoveringUpgrade && level < maxLevel) {
                    cost.append(" -> " + statData.getFormatValue().apply(RelicItem.getAbilityValue(stack, ability, stat, level + 1)));
                }

                if (isHoveringReroll) {
                    cost.append(" -> ").append(new TextComponent("X.XXX").withStyle(ChatFormatting.OBFUSCATED));
                }

                if (isHoveringReset && level > 0) {
                    cost.append(" -> " + statData.getFormatValue().apply(RelicItem.getAbilityValue(stack, ability, stat, 0)));
                }

                TranslatableComponent start = new TranslatableComponent("tooltip.relics." + stack.getItem().getRegistryName().getPath() + ".ability." + ability + ".stat." + stat, cost);

                pPoseStack.pushPose();

                pPoseStack.scale(0.5F, 0.5F, 0.5F);

                MC.font.draw(pPoseStack, start, x * 2 + 35 * 2, y * 2 + yOff + 103 * 2, 0x412708);

                pPoseStack.popPose();

                yOff += 9;
            }
        }

        boolean showInfo = false;
        String info = "";
        int cost = 0;

        if (isHoveringUpgrade) {
            info = "upgrade";

            showInfo = !RelicItem.isAbilityMaxLevel(stack, ability);

            cost = RelicItem.getUpgradeRequiredExperience(stack, ability);
        }

        if (isHoveringReroll) {
            info = "reroll";

            showInfo = RelicItem.mayReroll(stack, ability);

            cost = RelicItem.getRerollRequiredExperience(stack, ability);
        }

        if (isHoveringReset) {
            info = "reset";

            showInfo = RelicItem.mayReset(stack, ability);

            cost = RelicItem.getResetRequiredExperience(stack, ability);
        }

        if (!info.isEmpty()) {
            pPoseStack.pushPose();

            pPoseStack.scale(0.5F, 0.5F, 0.5F);

            yOff += 9;

            MC.font.draw(pPoseStack, new TranslatableComponent("tooltip.relics.relic." + info + ".description"), x * 2 + 35 * 2, y * 2 + 103 * 2 + yOff, 0x412708);

            yOff += 9;

            MC.font.draw(pPoseStack, new TranslatableComponent("tooltip.relics.relic." + info + ".cost", showInfo ? abilityData.getRequiredPoints() : "∞", showInfo ? cost : "∞"), x * 2 + 35 * 2, y * 2 + 103 * 2 + yOff, 0x412708);

            pPoseStack.popPose();
        }

        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void onClose() {
        MC.setScreen(new RelicDescriptionScreen(pos, stack));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}