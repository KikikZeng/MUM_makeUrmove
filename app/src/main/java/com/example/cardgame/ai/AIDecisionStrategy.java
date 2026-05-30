package com.example.cardgame.ai;

import com.example.cardgame.model.Card;
import com.example.cardgame.model.GameState;
import com.example.cardgame.model.Player;
import java.util.List;

public interface AIDecisionStrategy {
    /**
     * 决定出牌或过牌
     * @param aiPlayer 当前 AI 玩家
     * @param gameState 当前游戏状态（包含上家出的牌、是否首轮首出等）
     * @return 要出的牌列表，null 或空列表表示过牌
     */
    List<Card> decidePlay(Player aiPlayer, GameState gameState);

    /**
     * 记录出牌失败（由外部调用）
     */
    void recordPlayFailure();

    /**
     * 重置失败计数（成功出牌后调用）
     */
    void resetFailCount();
}