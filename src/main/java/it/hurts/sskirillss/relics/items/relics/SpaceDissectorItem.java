package it.hurts.sskirillss.relics.items.relics;

import it.hurts.sskirillss.relics.client.particles.circle.CircleTintData;
import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.entities.DissectionEntity;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.base.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityStat;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicLevelingData;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.items.relics.base.utils.LevelingUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;

import java.awt.*;
import java.util.UUID;

public class SpaceDissectorItem extends RelicItem {
    public static final String TAG_PORTAL = "portal";

    @Override
    public RelicData constructRelicData() {
        return RelicData.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("dissection", RelicAbilityEntry.builder()
                                .stat("distance", RelicAbilityStat.builder()
                                        .initialValue(16D, 32D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.25D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat("time", RelicAbilityStat.builder()
                                        .initialValue(5D, 10D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.5D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .levelingData(new RelicLevelingData(100, 10, 200))
                .styleData(RelicStyleData.builder()
                        .borders("#008cd7", "#0a3484")
                        .build())
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand handIn) {
        ItemStack stack = player.getItemInHand(handIn);

        String stringUUID = NBTUtils.getString(stack, TAG_PORTAL, "");

        if (!stringUUID.isEmpty()) {
            UUID uuid = UUID.fromString(stringUUID);

            if (!world.isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) world;

                DissectionEntity startPortal = (DissectionEntity) serverLevel.getEntity(uuid);

                if (startPortal != null)
                    startPortal.setLifeTime(20);
                else
                    LevelingUtils.addExperience(player, stack, 1);
            }
        } else
            LevelingUtils.addExperience(player, stack, 1);

        Vec3 view = player.getViewVector(0);
        Vec3 eyeVec = player.getEyePosition(0);

        float distance = Math.round(AbilityUtils.getAbilityValue(stack, "dissection", "distance"));

        BlockHitResult ray = world.clip(new ClipContext(eyeVec, eyeVec.add(view.x * distance, view.y * distance,
                view.z * distance), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        DissectionEntity portal = new DissectionEntity(world);

        int time = (int) Math.round(AbilityUtils.getAbilityValue(stack, "dissection", "time")) * 20;

        portal.setPos(ray.getLocation());
        portal.setMaxLifeTime(time);
        portal.setLifeTime(time);
        portal.setMaster(true);

        world.addFreshEntity(portal);

        NBTUtils.setString(stack, TAG_PORTAL, portal.getStringUUID());

        player.startUsingItem(handIn);

        return super.use(world, player, handIn);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity pLivingEntity, int pTimeCharged) {
        if (!(pLivingEntity instanceof Player player))
            return;

        Vec3 view = player.getViewVector(0);
        Vec3 eyeVec = player.getEyePosition(0);

        float distance = Math.round(AbilityUtils.getAbilityValue(stack, "dissection", "distance"));

        BlockHitResult ray = world.clip(new ClipContext(eyeVec, eyeVec.add(view.x * distance, view.y * distance,
                view.z * distance), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 targetVec = ray.getLocation();

        String stringUUID = NBTUtils.getString(stack, TAG_PORTAL, "");

        if (stringUUID.isEmpty())
            return;

        UUID uuid = UUID.fromString(stringUUID);

        if (world.isClientSide())
            return;

        ServerLevel serverLevel = (ServerLevel) world;

        DissectionEntity startPortal = (DissectionEntity) serverLevel.getEntity(uuid);

        if (startPortal == null)
            return;

        DissectionEntity endPortal = new DissectionEntity(world);

        endPortal.setPos(targetVec);
        endPortal.setMaster(false);

        world.addFreshEntity(endPortal);

        startPortal.setPair(endPortal);
        endPortal.setPair(startPortal);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        RandomSource random = level.getRandom();

        Vec3 view = entity.getViewVector(0);
        Vec3 eyeVec = entity.getEyePosition(0);

        float distance = Math.round(AbilityUtils.getAbilityValue(stack, "dissection", "distance"));

        BlockHitResult ray = level.clip(new ClipContext(eyeVec, eyeVec.add(view.x * distance, view.y * distance,
                view.z * distance), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
        Vec3 targetVec = ray.getLocation();

        ParticleUtils.createBall(new CircleTintData(new Color(150 + random.nextInt(100), 100, 0),
                0.2F + random.nextFloat() * 0.1F, 10 + random.nextInt(20), 0.9F, true), targetVec, level, 1, 0.25F);

        String stringUUID = NBTUtils.getString(stack, TAG_PORTAL, "");

        if (stringUUID.isEmpty())
            return;

        UUID uuid = UUID.fromString(stringUUID);

        if (level.isClientSide())
            return;

        ServerLevel serverLevel = (ServerLevel) level;

        DissectionEntity startPortal = (DissectionEntity) serverLevel.getEntity(uuid);

        if (startPortal == null)
            return;

        startPortal.lookAt(EntityAnchorArgument.Anchor.FEET, targetVec);
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}