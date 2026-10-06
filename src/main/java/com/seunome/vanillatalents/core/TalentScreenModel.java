package com.seunome.vanillatalents.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Cálculos da GUI que não dependem do Minecraft. */
public final class TalentScreenModel {

    public record RespecPreview(int feeLevels, int spent, int refund) {}

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
            case UNKNOWN_NODE -> "gui.vanillatalents.reason.unknown";
            case NO_CLASS_SELECTED -> "gui.vanillatalents.reason.no_class";
            case WRONG_CLASS -> "gui.vanillatalents.reason.wrong_class";
            case MAXED -> "gui.vanillatalents.reason.maxed";
            case NOT_ENOUGH_POINTS -> "gui.vanillatalents.reason.no_points";
            case PREREQUISITE_NOT_MET -> "gui.vanillatalents.reason.prerequisite";
            case SECONDARY_LOCKED -> "gui.vanillatalents.reason.secondary_locked";
            case CAPSTONE_PRIMARY_ONLY -> "gui.vanillatalents.reason.capstone_primary_only";
            case PRIMARY_CAPSTONE_REQUIRED -> "gui.vanillatalents.reason.primary_capstone";
            case MULTICLASS_DISABLED -> "gui.vanillatalents.reason.multiclass_disabled";
        };
    }

    public static TreeSummary treeSummary(SkillView v, TalentRegistry r, TreeCategory tree) {
        int spent = 0, maxed = 0, total = 0;
        List<TalentNode> nodes = r.tree(tree);
        for (TalentNode n : nodes) {
            int level = TalentRules.effectiveLevel(v, r, n.id());
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
        };
    }

    public static List<Prerequisite> unmetPrerequisites(SkillView v, TalentRegistry r, TalentNode n) {
        List<Prerequisite> unmet = new ArrayList<>();
        for (Prerequisite p : n.prerequisites()) {
            if (TalentRules.effectiveLevel(v, r, p.nodeId()) < p.level()) unmet.add(p);
        }
        return unmet;
    }

    public static RespecPreview respecPreview(Map<String, Integer> levels, String currentClass, EconomySettings settings) {
        if (TalentRules.NO_CLASS.equals(currentClass)) return new RespecPreview(0, 0, 0);
        int spent = RespecRules.spentClassPoints(levels, currentClass);
        return new RespecPreview(RespecRules.feeFor(spent, settings.respecFeeLevels()), spent,
                RespecRules.refund(spent, settings.respecRefundPercent()));
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
