package it.hurts.sskirillss.relics.entities;

import it.hurts.sskirillss.relics.client.particles.circle.CircleTintData;
import it.hurts.sskirillss.relics.init.EffectRegistry;
import it.hurts.sskirillss.relics.init.EntityRegistry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.awt.*;

public class StalactiteEntity extends ThrowableProjectile {
    @Getter
    @Setter
    private float damage;

    @Getter
    @Setter
    private float stun;

    public StalactiteEntity(EntityType<? extends ThrowableProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public StalactiteEntity(Level level, float damage, float stun) {
        super(EntityRegistry.STALACTITE.get(), level);

        this.damage = damage;
        this.stun = stun;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 200)
            this.discard();

        Level level = level();

        if (level.isClientSide())
            return;

        ((ServerLevel) level).sendParticles(new CircleTintData(new Color(100, 0, 255), 0.1F, 40, 0.9F, false),
                this.xo, this.yo, this.zo, 1, 0.025D, 0.025D, 0.025D, 0.01F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockPos pos = result.getBlockPos();
        BlockState state = level().getBlockState(pos);

        if (!state.blocksMotion())
            return;

        level().playSound(null, pos, SoundEvents.BASALT_BREAK, SoundSource.MASTER, 0.75F, 1.75F);

        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide() || !(result.getEntity() instanceof LivingEntity entity)
                || (this.getOwner() != null && entity.getStringUUID().equals(this.getOwner().getStringUUID())))
            return;

        entity.hurt(level().damageSources().thrown(this, this.getOwner()), damage);

        entity.addEffect(new MobEffectInstance(EffectRegistry.STUN.get(), Math.round(stun), 0, true, false));

        this.discard();
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.damage = compound.getFloat("Damage");
        this.stun = compound.getFloat("Stun");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Damage", this.damage);
        compound.putFloat("Stun", this.stun);
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