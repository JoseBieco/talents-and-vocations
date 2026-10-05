package com.seunome.vanillatalents.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seunome.vanillatalents.core.GridPos;
import com.seunome.vanillatalents.core.Prerequisite;
import com.seunome.vanillatalents.core.TalentNode;
import com.seunome.vanillatalents.core.TreeCategory;

import java.util.List;
import java.util.Map;

/** Esquema JSON de um nó em {@code data/<ns>/skills/<arvore>/<id>.json}. */
public final class TalentNodeCodec {

    private TalentNodeCodec() {}

    public static final Codec<TreeCategory> TREE_CATEGORY = Codec.STRING.comapFlatMap(
            id -> TreeCategory.byId(id).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown treeCategory: " + id)),
            TreeCategory::id);

    public static final Codec<Prerequisite> PREREQUISITE = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Prerequisite::nodeId),
            Codec.INT.fieldOf("level").forGetter(Prerequisite::level)
    ).apply(i, Prerequisite::new));

    public static final Codec<GridPos> GRID_POS = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("x").forGetter(GridPos::x),
            Codec.INT.fieldOf("y").forGetter(GridPos::y)
    ).apply(i, GridPos::new));

    public static final Codec<TalentNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(TalentNode::id),
            TREE_CATEGORY.fieldOf("treeCategory").forGetter(TalentNode::tree),
            Codec.STRING.fieldOf("name").forGetter(TalentNode::nameKey),
            Codec.STRING.fieldOf("description").forGetter(TalentNode::descKey),
            Codec.STRING.fieldOf("icon").forGetter(TalentNode::icon),
            Codec.INT.fieldOf("maxLevel").forGetter(TalentNode::maxLevel),
            PREREQUISITE.listOf().optionalFieldOf("prerequisites", List.of()).forGetter(TalentNode::prerequisites),
            GRID_POS.fieldOf("position").forGetter(TalentNode::position),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("values", Map.of()).forGetter(TalentNode::values)
    ).apply(i, TalentNode::new));
}
