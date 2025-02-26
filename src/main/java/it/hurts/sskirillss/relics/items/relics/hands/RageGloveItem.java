package it.hurts.sskirillss.relics.items.relics.hands;

import it.hurts.sskirillss.relics.client.particles.circle.CircleTintData;
import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.init.EffectRegistry;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.init.SoundRegistry;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.base.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.AbilityCastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.AbilityCastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityStat;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicLevelingData;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.items.relics.base.utils.LevelingUtils;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.PacketPlayerMotion;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.SlotContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RageGloveItem extends RelicItem {
    public static final String TAG_STACKS = "stacks";
    public static final String TAG_TIME = "time";

    @Override
    public RelicData constructRelicData() {
        return RelicData.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("rage", RelicAbilityEntry.builder()
                                .maxLevel(10)
                                .stat("incoming_damage", RelicAbilityStat.builder()
                                        .initialValue(0.05D, 0.025D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.05D)
                                        .formatValue(value -> MathUtils.round(MathUtils.round(value, 3) * 100, 3))
                                        .build())
                                .stat("dealt_damage", RelicAbilityStat.builder()
                                        .initialValue(0.025D, 0.075D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(MathUtils.round(value, 3) * 100, 3))
                                        .build())
                                .stat("duration", RelicAbilityStat.builder()
                                        .initialValue(2D, 4D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .ability("phlebotomy", RelicAbilityEntry.builder()
                                .requiredLevel(5)
                                .maxLevel(10)
                                .stat("heal", RelicAbilityStat.builder()
                                        .initialValue(0.0001D, 0.00025D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(MathUtils.round(value, 5) * 20, 5))
                                        .build())
                                .stat("movement_speed", RelicAbilityStat.builder()
                                        .initialValue(0.01D, 0.025D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(MathUtils.round(value, 3) * 100, 3))
                                        .build())
                                .stat("attack_speed", RelicAbilityStat.builder()
                                        .initialValue(0.005D, 0.01D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.05D)
                                        .formatValue(value -> MathUtils.round(MathUtils.round(value, 3) * 100, 3))
                                        .build())
                                .build())
                        .ability("spurt", RelicAbilityEntry.builder()
                                .requiredLevel(10)
                                .maxLevel(10)
                                .active(AbilityCastType.INSTANTANEOUS)
                                .stat("damage", RelicAbilityStat.builder()
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat("distance", RelicAbilityStat.builder()
                                        .initialValue(3D, 8D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.3D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat("cooldown", RelicAbilityStat.builder()
                                        .initialValue(20, 15)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_TOTAL, -0.075)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .levelingData(new RelicLevelingData(100, 20, 100))
                .styleData(RelicStyleData.builder()
                        .borders("#eed551", "#dcbe1d")
                        .build())
                .build();
    }

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, AbilityCastType type, AbilityCastStage stage) {
        Level level = player.getCommandSenderWorld();
        RandomSource random = level.getRandom();

        if (ability.equals("spurt")) {
            int stacks = NBTUtils.getInt(stack, TAG_STACKS, 0);

            double maxDistance = AbilityUtils.getAbilityValue(stack, "spurt", "distance");

            Vec3 view = player.getViewVector(0);
            Vec3 eyeVec = player.getEyePosition(0);

            BlockHitResult ray = level.clip(new ClipContext(eyeVec, eyeVec.add(view.x * maxDistance, view.y * maxDistance,
                    view.z * maxDistance), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            Vec3 current = player.position();
            Vec3 target = ray.getLocation();

            int distance = (int) Math.ceil(current.distanceTo(target));

            if (distance <= 0)
                return;

            Vec3 motion = player.getDeltaMovement().add(target.subtract(current).normalize());

            player.teleportTo(target.x, target.y, target.z);

            if (!level.isClientSide()) {
                NetworkHandler.sendToClient(new PacketPlayerMotion(motion.x, motion.y, motion.z), (ServerPlayer) player);

                AbilityUtils.setAbilityCooldown(stack, "spurt", (int) Math.round(AbilityUtils.getAbilityValue(stack, "spurt", "cooldown") * 20));
            }

            player.fallDistance = 0F;

            level.playSound(null, player.blockPosition(), SoundRegistry.SPURT.get(), SoundSource.MASTER, 1F, 0.75F + random.nextFloat() * 0.5F);

            Vec3 start = current.add(0, 1, 0);
            Vec3 end = target.add(0, 1, 0);

            Vec3 delta = end.subtract(start);
            Vec3 dir = delta.normalize();

            for (int i = 0; i < distance * 10; ++i) {
                double progress = i * delta.length() / (distance * 10);

                level.addParticle(new CircleTintData(new Color(255, 60 + random.nextInt(60), 0), 0.2F + random.nextFloat() * 0.5F,
                                60 + random.nextInt(60), 0.95F, true),
                        start.x + dir.x * progress, start.y + dir.y * progress,
                        start.z + dir.z * progress, 0, MathUtils.randomFloat(random) * 0.075F, 0);
            }

            List<LivingEntity> targets = new ArrayList<>();

            for (int i = 0; i < distance; ++i) {
                double progress = i * delta.length() / distance;

                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(new BlockPos((int) (start.x + dir.x * progress),
                        (int) (start.y + dir.y * progress), (int) (start.z + dir.z * progress))).inflate(0.5, 1, 0.5))) {
                    if (entity.getStringUUID().equals(player.getStringUUID())
                            || entity.isDeadOrDying())
                        continue;

                    targets.add(entity);
                }
            }

            if (!targets.isEmpty()) {
                EntityUtils.resetAttribute(player, stack, Attributes.ATTACK_SPEED, Integer.MAX_VALUE, AttributeModifier.Operation.MULTIPLY_BASE);
                EntityUtils.resetAttribute(player, stack, Attributes.ATTACK_DAMAGE, (float) (AbilityUtils.getAbilityValue(stack, "spurt", "damage") * stacks), AttributeModifier.Operation.ADDITION);

                for (LivingEntity entity : targets) {
                    player.attack(entity);

                    LevelingUtils.addExperience(player, stack, 1);

                    entity.addEffect(new MobEffectInstance(EffectRegistry.BLEEDING.get(), 100, 0));
                    entity.setSecondsOnFire(5);
                }

                EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION);
                EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_BASE);
            }

            NBTUtils.clearTag(stack, TAG_STACKS);
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        if (AbilityUtils.canUseAbility(stack, "phlebotomy")) {
            float percentage = 100F - (player.getHealth() / player.getMaxHealth() * 100F);

            player.heal((float) AbilityUtils.getAbilityValue(stack, "phlebotomy", "heal") * percentage);

            EntityUtils.resetAttribute(player, stack, Attributes.ATTACK_SPEED, (float) (AbilityUtils.getAbilityValue(stack, "phlebotomy", "attack_speed") * percentage), AttributeModifier.Operation.MULTIPLY_TOTAL);
            EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) (AbilityUtils.getAbilityValue(stack, "phlebotomy", "movement_speed") * percentage), AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        if (AbilityUtils.canUseAbility(stack, "rage")) {
            int stacks = NBTUtils.getInt(stack, TAG_STACKS, 0);

            if (stacks > 0) {
                int time = NBTUtils.getInt(stack, TAG_TIME, 0);

                if (time > 0)
                    NBTUtils.setInt(stack, TAG_TIME, --time);
                else {
                    NBTUtils.setInt(stack, TAG_STACKS, 0);

                    LevelingUtils.addExperience(player, stack, (int) Math.floor(stacks / 3F));
                }
            }
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)
                || stack.getItem() == newStack.getItem())
            return;

        EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);
        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);

        NBTUtils.clearTag(stack, TAG_STACKS);
        NBTUtils.clearTag(stack, TAG_TIME);
    }

    @Mod.EventBusSubscriber(modid = Reference.MODID)
    public static class Events {
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            Entity source = event.getSource().getDirectEntity();

            if (source instanceof Player player) {
                if (!(event.getSource().getEntity() instanceof Player))
                    return;

                ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.RAGE_GLOVE.get());

                if (stack.isEmpty())
                    return;

                if (AbilityUtils.canUseAbility(stack, "rage")) {
                    int stacks = NBTUtils.getInt(stack, TAG_STACKS, 0);

                    NBTUtils.setInt(stack, TAG_STACKS, ++stacks);
                    NBTUtils.setInt(stack, TAG_TIME, (int) Math.round(AbilityUtils.getAbilityValue(stack, "rage", "duration") * 20));

                    event.setAmount((float) (event.getAmount() + (event.getAmount() * (stacks * AbilityUtils.getAbilityValue(stack, "rage", "dealt_damage")))));
                }
            } else if (event.getEntity() instanceof Player player) {
                ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.RAGE_GLOVE.get());

                if (stack.isEmpty())
                    return;

                if (AbilityUtils.canUseAbility(stack, "rage")) {
                    int stacks = NBTUtils.getInt(stack, TAG_STACKS, 0);

                    if (stacks <= 0)
                        return;

                    event.setAmount((float) (event.getAmount() + (event.getAmount() * (stacks * AbilityUtils.getAbilityValue(stack, "rage", "incoming_damage")))));
                }
            }
        }
    }
}