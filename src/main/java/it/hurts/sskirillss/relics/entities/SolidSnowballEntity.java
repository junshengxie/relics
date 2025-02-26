package it.hurts.sskirillss.relics.entities;

import it.hurts.sskirillss.relics.init.EffectRegistry;
import it.hurts.sskirillss.relics.init.EntityRegistry;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.items.relics.base.utils.LevelingUtils;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;

public class SolidSnowballEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(SolidSnowballEntity.class, EntityDataSerializers.INT);

    public void setSize(int amount) {
        this.getEntityData().set(SIZE, amount);
    }

    public int getSize() {
        return this.getEntityData().get(SIZE);
    }

    public SolidSnowballEntity(EntityType<? extends ThrowableProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public SolidSnowballEntity(Level level) {
        super(EntityRegistry.SOLID_SNOWBALL.get(), level);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 300)
            this.discard();

        Level level = level();

        if (level.isClientSide())
            return;

        float scale = getSize() * 0.0035F;

        ((ServerLevel) level).sendParticles(ParticleTypes.SNOWFLAKE, this.xo, this.yo + this.getBbHeight() / 2F,
                this.zo, 3, scale, scale, scale, 0.025F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockPos pos = result.getBlockPos();
        BlockState state = level().getBlockState(pos);

        if (!state.blocksMotion())
            return;

        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide() || !(result.getEntity() instanceof LivingEntity entity)
                || (this.getOwner() != null && entity.getStringUUID().equals(this.getOwner().getStringUUID())))
            return;

        ItemStack stack = EntityUtils.findEquippedCurio(this.getOwner(), ItemRegistry.WOOL_MITTEN.get());

        if (!stack.isEmpty()) {
            entity.hurt(level().damageSources().thrown(this, this.getOwner()), (float) (getSize() * AbilityUtils.getAbilityValue(stack, "mold", "damage")));
            entity.addEffect(new MobEffectInstance(EffectRegistry.STUN.get(), (int) Math.round(getSize() * AbilityUtils.getAbilityValue(stack, "mold", "stun")) * 20, 0, true, false));
        }

        this.discard();
    }

    @Override
    public void onRemovedFromWorld() {
        ParticleUtils.createBall(ParticleTypes.SNOWFLAKE, this.position(), level(), 1 + (getSize() / 10), 0.1F + getSize() * 0.005F);

        if (level().isClientSide())
            return;

        Entity owner = this.getOwner();

        if (owner == null)
            return;

        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(getSize() / 15F))) {
            if (!entity.getStringUUID().equals(owner.getStringUUID()))
                entity.setTicksFrozen((int) (100 + Math.round(getSize() * AbilityUtils.getAbilityValue(EntityUtils.findEquippedCurio(owner, ItemRegistry.WOOL_MITTEN.get()), "mold", "freeze"))));
        }

        ItemStack stack = EntityUtils.findEquippedCurio(owner, ItemRegistry.WOOL_MITTEN.get());

        if (!stack.isEmpty())
            LevelingUtils.addExperience(owner, stack, (int) Math.floor(getSize() / 5F));

        level().playSound(null, this.blockPosition(), SoundEvents.SNOW_BREAK, SoundSource.MASTER, 1F, 0.5F);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SIZE, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        setSize(compound.getInt("Size"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Size", getSize());
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Nonnull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}