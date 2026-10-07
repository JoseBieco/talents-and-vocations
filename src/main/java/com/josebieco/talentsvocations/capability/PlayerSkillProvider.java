package com.josebieco.talentsvocations.capability;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerSkillProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerSkillData> PLAYER_SKILL = CapabilityManager.get(new CapabilityToken<PlayerSkillData>() {
    });

    private PlayerSkillData skillData = null;
    private final LazyOptional<PlayerSkillData> optional = LazyOptional.of(this::createPlayerSkill);

    private PlayerSkillData createPlayerSkill() {
        if (this.skillData == null) {
            this.skillData = new PlayerSkillData();
        }
        return this.skillData;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_SKILL) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        return createPlayerSkill().serializeNBT(registryAccess);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
        createPlayerSkill().deserializeNBT(registryAccess, nbt);
    }
}
