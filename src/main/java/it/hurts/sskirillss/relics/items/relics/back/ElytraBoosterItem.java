package it.hurts.sskirillss.relics.items.relics.back;

import it.hurts.sskirillss.relics.api.events.common.ContainerSlotClickEvent;
import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.base.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.AbilityCastPredicate;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.AbilityCastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.AbilityCastType;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.data.PredicateInfo;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityStat;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicLevelingData;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.items.relics.base.utils.LevelingUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ElytraBoosterItem extends RelicItem {
    public static final String TAG_FUEL = "fuel";
    public static final String TAG_SPEED = "speed";

    @Override
    public RelicData constructRelicData() {
        return RelicData.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("boost", RelicAbilityEntry.builder()
                                .maxLevel(10)
                                .active(AbilityCastType.CYCLICAL, AbilityCastPredicate.builder()
                                        .predicate("fuel", data -> PredicateInfo.builder()
                                                .condition(NBTUtils.getInt(data.getStack(), TAG_FUEL, 0) > 0)
                                                .build()
                                        )
                                        .predicate("elytra", data -> PredicateInfo.builder()
                                                .condition(data.getPlayer().isFallFlying())
                                                .build()
                                        )
                                )
                                .stat("capacity", RelicAbilityStat.builder()
                                        .initialValue(50, 100)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.25)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat("speed", RelicAbilityStat.builder()
                                        .initialValue(1.1D, 1.5D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value * 16, 1))
                                        .build())
                                .build())
                        .build())
                .levelingData(new RelicLevelingData(100, 10, 100))
                .styleData(RelicStyleData.builder()
                        .borders("#b8b8d6", "#6e6e8f")
                        .build())
                .build();
    }

    public int getBreathCapacity(ItemStack stack) {
        return (int) Math.round(AbilityUtils.getAbilityValue(stack, "boost", "capacity"));
    }

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, AbilityCastType type, AbilityCastStage stage) {
        if (ability.equals("boost")) {
            if (stage == AbilityCastStage.TICK) {
                int fuel = NBTUtils.getInt(stack, TAG_FUEL, 0);

                if (fuel > 0 && player.tickCount % 20 == 0)
                    NBTUtils.setInt(stack, TAG_FUEL, --fuel);

                double speed = NBTUtils.getDouble(stack, TAG_SPEED, 1D);

                if (player.tickCount % 3 == 0) {
                    double maxSpeed = AbilityUtils.getAbilityValue(stack, "boost", "speed");

                    if (speed < maxSpeed) {
                        speed = Math.min(maxSpeed, speed + ((maxSpeed - 1D) / 100D));

                        NBTUtils.setDouble(stack, TAG_SPEED, speed);
                    } else {
                        player.startAutoSpinAttack(5);
                    }
                }

                Vec3 look = player.getLookAngle();
                Vec3 motion = player.getDeltaMovement();
                Level world = player.getCommandSenderWorld();
                RandomSource random = world.getRandom();

                player.setDeltaMovement(motion.add(look.x * 0.1D + (look.x * speed - motion.x) * 0.5D,
                        look.y * 0.1D + (look.y * speed - motion.y) * 0.5D,
                        look.z * 0.1D + (look.z * speed - motion.z) * 0.5D));

                for (int i = 0; i < speed * 3; i++)
                    world.addParticle(ParticleTypes.SMOKE,
                            player.getX() + (MathUtils.randomFloat(random) * 0.4F),
                            player.getY() + (MathUtils.randomFloat(random) * 0.4F),
                            player.getZ() + (MathUtils.randomFloat(random) * 0.4F),
                            0, 0, 0);

                if (player.tickCount % Math.max(1, (int) Math.round((10 - speed * 2) / (player.isInWaterOrRain() ? 2 : 1))) == 0)
                    NBTUtils.setInt(stack, TAG_FUEL, --fuel);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slot, isSelected);

        if (!(entity instanceof Player player))
            return;

        double speed = NBTUtils.getDouble(stack, TAG_SPEED, 1D);
        int fuel = NBTUtils.getInt(stack, TAG_FUEL, 0);

        if (speed > 1 && (fuel <= 0 || !player.isFallFlying()))
            NBTUtils.setDouble(stack, TAG_SPEED, 1D);
    }

    @SubscribeEvent
    public static void onSlotClick(ContainerSlotClickEvent event) {
        if (event.getAction() != ClickAction.PRIMARY)
            return;

        Player player = event.getEntity();

        ItemStack heldStack = event.getHeldStack();
        ItemStack slotStack = event.getSlotStack();

        if (!(slotStack.getItem() instanceof ElytraBoosterItem booster))
            return;

        int time = ForgeHooks.getBurnTime(heldStack, RecipeType.SMELTING) / 20;
        int amount = NBTUtils.getInt(slotStack, TAG_FUEL, 0);
        int capacity = booster.getBreathCapacity(slotStack);
        int sum = amount + time;

        if (time <= 0)
            return;

        NBTUtils.setInt(slotStack, TAG_FUEL, Math.min(capacity, sum));

        int left = sum > capacity ? time - (sum - capacity) : time;

        LevelingUtils.addExperience(player, slotStack, (int) Math.floor(left / 10F));

        ItemStack result = heldStack.getCraftingRemainingItem();

        heldStack.shrink(1);

        if (!result.isEmpty()) {
            if (heldStack.isEmpty())
                player.containerMenu.setCarried(result);
            else
                player.getInventory().add(result);
        }

        player.playSound(SoundEvents.BLAZE_SHOOT, 0.75F, 2F / (time * 0.025F));

        event.setCanceled(true);
    }
}