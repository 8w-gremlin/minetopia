package com.minetopia.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record VillageStructure(VillageStructureType type, BlockPos pos) {

    public static final Codec<VillageStructure> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    VillageStructureType.CODEC.fieldOf("type").forGetter(VillageStructure::type),
                    BlockPos.CODEC.fieldOf("pos").forGetter(VillageStructure::pos)
            ).apply(instance, VillageStructure::new)
    );
}
