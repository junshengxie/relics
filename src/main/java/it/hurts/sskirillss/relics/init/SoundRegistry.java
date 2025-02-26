package it.hurts.sskirillss.relics.init;

import it.hurts.sskirillss.relics.utils.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Reference.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Reference.MODID);

    public static final RegistryObject<SoundEvent> RICOCHET = SOUNDS.register("ricochet", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Reference.MODID, "ricochet")));
    public static final RegistryObject<SoundEvent> THROW = SOUNDS.register("throw", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Reference.MODID, "throw")));
    public static final RegistryObject<SoundEvent> ARROW_RAIN = SOUNDS.register("arrow_rain", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Reference.MODID, "arrow_rain")));
    public static final RegistryObject<SoundEvent> SPURT = SOUNDS.register("spurt", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Reference.MODID, "spurt")));
    public static final RegistryObject<SoundEvent> POWERED_ARROW = SOUNDS.register("powered_arrow", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Reference.MODID, "powered_arrow")));
    public static final RegistryObject<SoundEvent> LEAP = SOUNDS.register("leap", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Reference.MODID, "leap")));

    public static void register() {
        SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}