package com.example.cardgame.ai;

/**
 * AI玩家配置文件 - 用于设置不同AI的聪明程度和策略倾向
 */
public class AIPlayerProfile {
    public static final int LEVEL_WEAK = 0;
    public static final int LEVEL_NORMAL = 1;
    public static final int LEVEL_STRONG = 2;

    private int level;
    private boolean keepBigPattern;      // 是否优先保留五张牌型
    private double earlyConservative;    // 开局保守程度 0-1（1=最保守）
    private double midAggression;        // 中盘进攻性 0-1（1=最激进）
    private double lateFinishBonus;      // 残局直接出完奖励倍数

    public AIPlayerProfile(int level) {
        this.level = level;
        switch (level) {
            case LEVEL_WEAK:
                keepBigPattern = false;
                earlyConservative = 0.9;   // 非常保守
                midAggression = 0.2;       // 进攻性低
                lateFinishBonus = 1.0;     // 普通奖励
                break;
            case LEVEL_NORMAL:
                keepBigPattern = true;
                earlyConservative = 0.6;   // 适度保守
                midAggression = 0.6;       // 平衡
                lateFinishBonus = 3.0;     // 较高奖励
                break;
            case LEVEL_STRONG:
                keepBigPattern = true;
                earlyConservative = 0.3;   // 不太保守
                midAggression = 0.9;       // 高度进攻
                lateFinishBonus = 6.0;     // 高额奖励
                break;
            default:
                keepBigPattern = true;
                earlyConservative = 0.5;
                midAggression = 0.5;
                lateFinishBonus = 3.0;
        }
    }

    // Getters
    public int getLevel() {
        return level;
    }

    public boolean isKeepBigPattern() {
        return keepBigPattern;
    }

    public double getEarlyConservative() {
        return earlyConservative;
    }

    public double getMidAggression() {
        return midAggression;
    }

    public double getLateFinishBonus() {
        return lateFinishBonus;
    }
}