package com.josebieco.talentsvocations.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Cálculos da GUI que não dependem do Minecraft. */
public final class TalentScreenModel {

    /** {@code freezesSecondary}: trocar a principal congela a secundária até o capstone da nova principal. */
    public record RespecPreview(int feeLevels, int spent, int refund, boolean freezesSecondary) {}

    public record GridBounds(int minX, int maxX, int minY, int maxY) {}

    /** Resumo de uma árvore para o jogador: pontos gastos, nós no máximo e totais. */
    public record TreeSummary(int spent, int maxedNodes, int nodeCount, int totalPoints) {}

    /** Resumo estático de uma classe: nó raiz, capstone e totais. */
    public record ClassSummary(String rootId, String capstoneId, int nodeCount, int totalPoints) {}

    private TalentScreenModel() {}

    /** Chave de tradução do motivo de bloqueio, ou null se a compra é possível. */
    public static String blockReasonKey(PurchaseResult r) {
        return switch (r) {
            case OK -> null;
            case UNKNOWN_NODE -> "gui.talentsvocations.reason.unknown";
            case NO_CLASS_SELECTED -> "gui.talentsvocations.reason.no_class";
            case WRONG_CLASS -> "gui.talentsvocations.reason.wrong_class";
            case MAXED -> "gui.talentsvocations.reason.maxed";
            case NOT_ENOUGH_POINTS -> "gui.talentsvocations.reason.no_points";
            case PREREQUISITE_NOT_MET -> "gui.talentsvocations.reason.prerequisite";
            case SECONDARY_LOCKED -> "gui.talentsvocations.reason.secondary_locked";
            case CAPSTONE_PRIMARY_ONLY -> "gui.talentsvocations.reason.capstone_primary_only";
            case PRIMARY_CAPSTONE_REQUIRED -> "gui.talentsvocations.reason.primary_capstone";
            case MULTICLASS_DISABLED -> "gui.talentsvocations.reason.multiclass_disabled";
        };
    }

    /** {@code tree} é a classe secundária e ela está congelada (escolhida, liberada, mas sem efeito). */
    public static boolean frozenSecondary(SkillView v, TalentRegistry r, TreeCategory tree) {
        return tree.isClass() && tree.id().equals(v.secondaryClass()) && secondaryTab(v, r) == SecondaryTab.FROZEN;
    }

    /**
     * Nível mostrado na GUI: o efetivo, exceto na secundária congelada, onde aparece o nível salvo (limitado ao
     * máximo) para o jogador ver o progresso guardado. O capstone da secundária nunca conta e segue o efetivo.
     */
    public static int displayLevel(SkillView v, TalentRegistry r, TalentNode n) {
        if (!n.capstone() && frozenSecondary(v, r, n.tree())) {
            return Math.max(0, Math.min(v.rawLevel(n.id()), n.maxLevel()));
        }
        return TalentRules.effectiveLevel(v, r, n.id());
    }

    /** Resumo para exibição: usa {@link #displayLevel} (níveis salvos na secundária congelada). */
    public static TreeSummary treeSummary(SkillView v, TalentRegistry r, TreeCategory tree) {
        int spent = 0, maxed = 0, total = 0;
        List<TalentNode> nodes = r.tree(tree);
        for (TalentNode n : nodes) {
            int level = displayLevel(v, r, n);
            spent += level * n.cost();
            if (level >= n.maxLevel()) maxed++;
            total += n.maxLevel() * n.cost();
        }
        return new TreeSummary(spent, maxed, nodes.size(), total);
    }

    public static ClassSummary classSummary(TalentRegistry r, TreeCategory tree) {
        List<TalentNode> nodes = r.tree(tree);
        if (nodes.isEmpty()) return new ClassSummary(null, null, 0, 0);
        String root = null;
        int total = 0;
        for (TalentNode n : nodes) {
            if (root == null && n.prerequisites().isEmpty()) root = n.id();
            total += n.maxLevel() * n.cost();
        }
        String capstone = r.capstone(tree).map(TalentNode::id).orElse(nodes.get(nodes.size() - 1).id());
        return new ClassSummary(root, capstone, nodes.size(), total);
    }

    /** Limita o deslocamento do arrastar a [viewSize - contentSize, 0]; conteúdo menor que a vista fica em 0. */
    public static int clampScroll(int contentSize, int viewSize, int scroll) {
        if (contentSize <= viewSize) return 0;
        return Math.max(viewSize - contentSize, Math.min(0, scroll));
    }

    /** Pixels rolados por "dente" da roda do mouse nos painéis de texto. */
    public static final int WHEEL_STEP = 10;
    private static final int MIN_THUMB = 8;

    /** Posição e altura (em px, relativas ao topo do trilho) da barra de rolagem. */
    public record ScrollThumb(int top, int height) {}

    /** Nova rolagem (≤ 0) de um painel de texto depois de girar a roda; {@code wheel > 0} sobe. */
    public static int wheelScroll(int contentH, int viewH, int current, double wheel) {
        return clampScroll(contentH, viewH, current + (int) Math.round(wheel * WHEEL_STEP));
    }

    /** Nova rolagem (≤ 0) de uma lista de {@code rows} linhas de {@code rowH} px numa vista de {@code viewH} px. */
    public static int listScroll(int rows, int rowH, int viewH, int current, double wheel) {
        return wheelScroll(rows * rowH, viewH, current, wheel);
    }

    /** Barra de rolagem do painel, ou null quando o texto cabe. */
    public static ScrollThumb scrollThumb(int contentH, int viewH, int scroll, int trackH) {
        if (contentH <= viewH) return null;
        int height = Math.max(MIN_THUMB, trackH * viewH / contentH);
        int top = (int) Math.round((double) -scroll / (contentH - viewH) * (trackH - height));
        return new ScrollThumb(top, height);
    }

    /** Textura de bloco (ladrilhada) usada como fundo da árvore. */
    public static String backgroundTexture(TreeCategory tree) {
        return switch (tree) {
            case COMMON -> "textures/block/stone.png";
            case MINER -> "textures/block/deepslate.png";
            case FARMER -> "textures/block/farmland.png";
            case EXPLORER -> "textures/block/grass_block_side.png";
            case WARRIOR -> "textures/block/polished_blackstone.png";
            case ARCHER -> "textures/block/oak_planks.png";
            case ANGLER -> "textures/block/sand.png";
            case TAMER -> "textures/block/hay_block_side.png";
            case BUILDER -> "textures/block/bricks.png";
            case ARTISAN -> "textures/block/bookshelf.png";
        };
    }

    public static List<Prerequisite> unmetPrerequisites(SkillView v, TalentRegistry r, TalentNode n) {
        List<Prerequisite> unmet = new ArrayList<>();
        for (Prerequisite p : n.prerequisites()) {
            if (TalentRules.effectiveLevel(v, r, p.nodeId()) < p.level()) unmet.add(p);
        }
        return unmet;
    }

    /**
     * Prévia de trocar a classe do espaço {@code slot}. {@code primary}/{@code secondary} são as classes hoje nos dois
     * espaços; a trocada é a do espaço {@code slot} (espaço vazio = primeira escolha, grátis). {@code freezesSecondary}
     * só vale ao trocar a principal tendo secundária escolhida.
     */
    public static RespecPreview respecPreview(Map<String, Integer> levels, TalentRegistry registry, ClassSlot slot,
                                              String primary, String secondary, EconomySettings settings) {
        String current = classInSlot(slot, primary, secondary);
        boolean freezes = slot == ClassSlot.PRIMARY && !TalentRules.NO_CLASS.equals(secondary);
        if (TalentRules.NO_CLASS.equals(current)) return new RespecPreview(0, 0, 0, freezes);
        int spent = RespecRules.spentClassPoints(levels, registry, current);
        return new RespecPreview(RespecRules.feeFor(spent, settings.respecFeeLevels()), spent,
                RespecRules.refund(spent, settings.respecRefundPercent()), freezes);
    }

    /** Estado da aba Secundária. */
    public enum SecondaryTab {
        /** Multiclasse desligado no servidor ({@code maxClasses} = 1). */
        DISABLED,
        /** Segunda Vocação não comprada. */
        LOCKED,
        /** Espaço liberado, sem classe escolhida. */
        CHOOSE,
        /** Classe escolhida, mas sem efeito (falta o capstone da principal). */
        FROZEN,
        ACTIVE
    }

    public static SecondaryTab secondaryTab(SkillView v, TalentRegistry r) {
        if (v.maxClasses() < 2) return SecondaryTab.DISABLED;
        if (!RespecRules.slotUnlocked(ClassSlot.SECONDARY, v)) return SecondaryTab.LOCKED;
        boolean chosen = TreeCategory.byId(v.secondaryClass()).map(TreeCategory::isClass).orElse(false);
        if (!chosen) return SecondaryTab.CHOOSE;
        return TalentRules.secondaryActive(v, r) ? SecondaryTab.ACTIVE : SecondaryTab.FROZEN;
    }

    /** Id do capstone da árvore principal, ou null sem principal ou sem capstone marcado. */
    public static String primaryCapstoneId(SkillView v, TalentRegistry r) {
        return TreeCategory.byId(v.primaryClass()).filter(TreeCategory::isClass)
                .flatMap(r::capstone).map(TalentNode::id).orElse(null);
    }

    /** Como uma classe aparece na lista de escolha de um espaço. */
    public enum ClassChoice {
        /** Já é a classe deste espaço. */
        CURRENT,
        /** Está no outro espaço: não pode ser escolhida aqui. */
        OTHER_SLOT,
        AVAILABLE
    }

    public static String classInSlot(ClassSlot slot, String primary, String secondary) {
        return slot == ClassSlot.PRIMARY ? primary : secondary;
    }

    public static ClassChoice classChoice(ClassSlot slot, String classId, String primary, String secondary) {
        if (classId.equals(classInSlot(slot, primary, secondary))) return ClassChoice.CURRENT;
        String other = slot == ClassSlot.PRIMARY ? secondary : primary;
        if (classId.equals(other)) return ClassChoice.OTHER_SLOT;
        return ClassChoice.AVAILABLE;
    }

    public static GridBounds gridBounds(List<TalentNode> nodes) {
        if (nodes.isEmpty()) return new GridBounds(0, 0, 0, 0);
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (TalentNode n : nodes) {
            minX = Math.min(minX, n.position().x());
            maxX = Math.max(maxX, n.position().x());
            minY = Math.min(minY, n.position().y());
            maxY = Math.max(maxY, n.position().y());
        }
        return new GridBounds(minX, maxX, minY, maxY);
    }
}
