package it.hurts.sskirillss.relics.items.relics.belt;

import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttributeModifier;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicSlotModifier;
import it.hurts.sskirillss.relics.items.relics.base.data.base.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityStat;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicLevelingData;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.items.relics.base.utils.LevelingUtils;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.Reference;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;
import top.theillusivec4.curios.api.SlotContext;

public class DrownedBeltItem extends RelicItem {
    @Override
    public RelicData constructRelicData() {
        return RelicData.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("slots", RelicAbilityEntry.builder()
                                .requiredPoints(2)
                                .stat("talisman", RelicAbilityStat.builder()
                                        .initialValue(0D, 2D)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, 1D)
                                        .formatValue(value -> (int) (MathUtils.round(value, 0)))
                                        .build())
                                .build())
                        .ability("anchor", RelicAbilityEntry.builder()
                                .stat("slowness", RelicAbilityStat.builder()
                                        .initialValue(0.5D, 0.25D)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, -0.05D)
                                        .formatValue(value -> (int) (MathUtils.round(value, 2) * 100))
                                        .build())
                                .stat("sinking", RelicAbilityStat.builder()
                                        .initialValue(5D, 3D)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, -0.1D)
                                        .formatValue(value -> (int) (MathUtils.round(value, 2) * 100))
                                        .build())
                                .build())
                        .ability("pressure", RelicAbilityEntry.builder()
                                .stat("damage", RelicAbilityStat.builder()
                                        .initialValue(1.25D, 2D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) (MathUtils.round(value, 2) * 100))
                                        .build())
                                .build())
                        .ability("riptide", RelicAbilityEntry.builder()
                                .stat("cooldown", RelicAbilityStat.builder()
                                        .initialValue(10D, 5D)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, -0.5D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .levelingData(new RelicLevelingData(100, 10, 100))
                .styleData(RelicStyleData.builder()
                        .borders("#7889b8", "#25374e")
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        if (player.isEyeInFluid(FluidTags.WATER) && !player.onGround())
            EntityUtils.applyAttribute(player, stack, ForgeMod.ENTITY_GRAVITY.get(), (float) AbilityUtils.getAbilityValue(stack, "anchor", "sinking"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        else
            EntityUtils.removeAttribute(player, stack, ForgeMod.ENTITY_GRAVITY.get(), AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        EntityUtils.removeAttribute(slotContext.entity(), stack, ForgeMod.ENTITY_GRAVITY.get(), AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public RelicSlotModifier getSlotModifiers(ItemStack stack) {
        return RelicSlotModifier.builder()
                .entry(Pair.of("talisman", (int) Math.round(AbilityUtils.getAbilityValue(stack, "slots", "talisman"))))
                .build();
    }

    @Override
    public RelicAttributeModifier getAttributeModifiers(ItemStack stack) {
        return RelicAttributeModifier.builder()
                .attribute(new RelicAttributeModifier.Modifier(ForgeMod.SWIM_SPEED.get(), (float) -AbilityUtils.getAbilityValue(stack, "anchor", "slowness")))
                .build();
    }

    @Mod.EventBusSubscriber(modid = Reference.MODID)
    public static class Events {
        @SubscribeEvent
        public static void onEntityHurt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)
                    || !player.isUnderWater() || !event.getEntity().isUnderWater())
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.DROWNED_BELT.get());

            if (stack.isEmpty())
                return;

            event.setAmount((float) (event.getAmount() * AbilityUtils.getAbilityValue(stack, "pressure", "damage")));
        }

        @SubscribeEvent
        public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
            ItemStack stack = event.getItem();

            if (!(event.getEntity() instanceof Player player) || stack.getItem() != Items.TRIDENT || !player.getCooldowns().isOnCooldown(stack.getItem()))
                return;

            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onItemUseFinish(LivingEntityUseItemEvent.Stop event) {
            ItemStack stack = event.getItem();

            if (!(event.getEntity() instanceof Player player) || stack.getItem() != Items.TRIDENT)
                return;

            ItemStack relic = EntityUtils.findEquippedCurio(player, ItemRegistry.DROWNED_BELT.get());

            if (relic.isEmpty())
                return;

            int duration = stack.getItem().getUseDuration(stack) - event.getDuration();
            int enchantment = EnchantmentHelper.getRiptide(stack);

            if (duration < 10 || enchantment <= 0)
                return;

            LevelingUtils.addExperience(player, relic, enchantment);

            player.getCooldowns().addCooldown(stack.getItem(), (int) Math.round(AbilityUtils.getAbilityValue(relic, "riptide", "cooldown") * enchantment * 20));
        }
    }
}