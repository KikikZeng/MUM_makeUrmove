package com.example.cardgame.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardgame.CardGameApplication;
import com.example.cardgame.R;
import com.example.cardgame.dto.narrative.NarrativeGameViewData;
import com.example.cardgame.dto.narrative.NarrativePlayResult;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.rule.narrative.NarrativePlayType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NarrativeGameActivity extends AppCompatActivity {
    private TextView stageTitleText;
    private TextView stageHintText;
    private TextView narrationText;
    private TextView aiProgressText;
    private TextView nodeActionsText;
    private TextView userFactionText;
    private LinearLayout progressNodeContainer;
    private LinearLayout heartContainer;
    private LinearLayout handContainer;
    private LinearLayout lastRoundCardsContainer;
    private LinearLayout playerPlayedCardsContainer;
    private TextView lastRoundLabel;
    private TextView playInstructionText;
    private FrameLayout factionSeatContainer;
    private Button submitButton;
    private Button btnAbandon;
    private boolean settlementOpened;
    private final List<String> selectedCardIds = new ArrayList<>();
    private final List<EventCard> lastPlayedCards = new ArrayList<>();
    private final List<EventCard> playerRoundCards = new ArrayList<>();
    private int lastNodeIndex = -1;

    // 回合制相关
    private final Handler turnHandler = new Handler(Looper.getMainLooper());
    private List<String> turnOrder = new ArrayList<>();          // 当前节点的出牌顺序（factionId）
    private int currentTurnIndex = 0;                            // 当前轮到谁
    private final Map<String, List<EventCard>> nodeCardsByFaction = new HashMap<>(); // 当前节点每个阵营的牌
    private final Map<String, Boolean> factionPlayed = new HashMap<>(); // 每个阵营是否已出牌
    private boolean isPlayerTurn = false;
    private boolean turnLocked = false;                          // 防止重复触发

    // 三方 AI 阵营名 TextView
    private TextView tvNameTop;
    private TextView tvNameLeft;
    private TextView tvNameRight;
    // 三方 AI 阵营容器
    private LinearLayout opponentTop;
    private LinearLayout opponentLeft;
    private LinearLayout opponentRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_narrative_game_board);

        stageTitleText = findViewById(R.id.tv_stage_title);
        stageHintText = findViewById(R.id.tv_stage_hint);
        narrationText = findViewById(R.id.tv_narration);
        aiProgressText = findViewById(R.id.tv_ai_progress);
        nodeActionsText = findViewById(R.id.tv_node_actions);
        userFactionText = findViewById(R.id.tv_user_faction);
        progressNodeContainer = findViewById(R.id.layout_progress_nodes);
        heartContainer = findViewById(R.id.layout_heart_container);
        handContainer = findViewById(R.id.layout_hand_cards_inner);
        lastRoundCardsContainer = findViewById(R.id.layout_last_round_cards);
        playerPlayedCardsContainer = findViewById(R.id.layout_player_played_cards);
        lastRoundLabel = findViewById(R.id.tv_last_round_label);
        playInstructionText = findViewById(R.id.tv_play_instruction);
        tvNameTop = findViewById(R.id.tv_name_top);
        tvNameLeft = findViewById(R.id.tv_name_left);
        tvNameRight = findViewById(R.id.tv_name_right);
        opponentTop = findViewById(R.id.opponent_top);
        opponentLeft = findViewById(R.id.opponent_left);
        opponentRight = findViewById(R.id.opponent_right);
        factionSeatContainer = new FrameLayout(this);
        submitButton = findViewById(R.id.btn_submit_event_cards);
        btnAbandon = findViewById(R.id.btn_abandon_narrative);

        submitButton.setOnClickListener(v -> submitCards());
        btnAbandon.setOnClickListener(v -> confirmAbandon());
        findViewById(R.id.btn_game_back).setOnClickListener(v -> finish());

        renderGame();
    }

    // ========== 渲染入口 ==========

    private void renderGame() {
        NarrativeGameViewData viewData = CardGameApplication.getNarrativeActionHandler().getNarrativeGameViewData();
        if (viewData == null) {
            Toast.makeText(this, "对局未初始化", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        renderProgressNodes(viewData);
        renderFactionSeats(viewData);
        renderHearts(viewData.getHearts());
        userFactionText.setText(findFactionName(viewData, viewData.getUserFactionId()));
        stageTitleText.setText(viewData.getStageTitle() != null ? viewData.getStageTitle() : "推演完成");
        stageHintText.setText(viewData.getStageHint() != null ? viewData.getStageHint() : "");
        narrationText.setText(viewData.getOpeningNarration() != null ? viewData.getOpeningNarration() : "");
        aiProgressText.setText(formatAiProgress(viewData));
        aiProgressText.setVisibility(aiProgressText.getText().length() > 0 && !aiProgressText.getText().toString().equals("AI 阵营进度：") ? View.VISIBLE : View.GONE);
        nodeActionsText.setText(formatLastResolvedNode(viewData));
        submitButton.setEnabled(!viewData.isGameOver());

        if (viewData.isGameOver()) {
            openSettlement();
            return;
        }

        // 新节点：初始化回合制
        int currentNodeIndex = viewData.getGlobalProgress();
        if (currentNodeIndex != lastNodeIndex) {
            initTurnForNode(viewData);
            lastNodeIndex = currentNodeIndex;
        }

        selectedCardIds.clear();
        renderHandCards(viewData.getHandCards());
        renderLastRoundCards(viewData);
    }

    // ========== 回合制核心 ==========

    private void initTurnForNode(NarrativeGameViewData viewData) {
        // 取消所有残留的延迟任务，防止旧任务污染新状态
        turnHandler.removeCallbacksAndMessages(null);

        // 游戏已结束，不再启动回合
        if (viewData.isGameOver()) {
            openSettlement();
            return;
        }

        turnLocked = false;
        playerRoundCards.clear();
        factionPlayed.clear();
        nodeCardsByFaction.clear();

        // 从 currentNodeEvents 提取每个阵营的牌
        Map<String, List<EventCard>> nodeEvents = viewData.getCurrentNodeEvents();
        if (nodeEvents != null) {
            for (Map.Entry<String, List<EventCard>> entry : nodeEvents.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    nodeCardsByFaction.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
        }

        // 构建出牌顺序：按 factions 列表顺序，但排除当前节点无牌的阵营
        turnOrder.clear();
        for (Faction faction : viewData.getFactions()) {
            if (nodeCardsByFaction.containsKey(faction.getId())) {
                turnOrder.add(faction.getId());
                factionPlayed.put(faction.getId(), false);
            }
        }
        currentTurnIndex = 0;

        if (turnOrder.isEmpty()) {
            // 无人有牌，后端推进
            CardGameApplication.getNarrativeActionHandler().submitEventCards(new ArrayList<>());
            renderGame();
            return;
        }

        // 开始第一回合
        startCurrentTurn(viewData);
    }

    private void startCurrentTurn(NarrativeGameViewData viewData) {
        if (turnLocked || currentTurnIndex >= turnOrder.size()) {
            return;
        }

        // 跳过已经出过牌的阵营（容错）
        String currentFactionId = turnOrder.get(currentTurnIndex);
        if (factionPlayed.get(currentFactionId) == Boolean.TRUE) {
            currentTurnIndex++;
            if (currentTurnIndex >= turnOrder.size()) {
                advanceNode(viewData);
                return;
            }
            currentFactionId = turnOrder.get(currentTurnIndex);
        }

        turnLocked = true;
        String userFactionId = viewData.getUserFactionId();

        // 高亮当前阵营
        highlightCurrentFaction(viewData);

        if (currentFactionId.equals(userFactionId)) {
            // 玩家回合
            isPlayerTurn = true;
            submitButton.setEnabled(true);
            submitButton.setVisibility(View.VISIBLE);
            btnAbandon.setVisibility(View.VISIBLE);
            playInstructionText.setText("轮到你了，请根据阶段提示选择手牌：");
            turnLocked = false;
        } else {
            // AI 回合
            isPlayerTurn = false;
            submitButton.setEnabled(false);
            submitButton.setVisibility(View.GONE);
            btnAbandon.setVisibility(View.GONE);
            playInstructionText.setText("等待 " + findFactionName(viewData, currentFactionId) + " 出牌中...");

            // 延迟 1.5 秒后出牌
            final String aiId = currentFactionId;
            turnHandler.postDelayed(() -> executeAiTurn(aiId), 1500);
        }
    }

    private void executeAiTurn(String aiFactionId) {
        // 重新获取最新 viewData，避免使用过期引用
        NarrativeGameViewData viewData = CardGameApplication.getNarrativeActionHandler().getNarrativeGameViewData();
        List<EventCard> aiCards = nodeCardsByFaction.get(aiFactionId);
        if (aiCards == null || aiCards.isEmpty()) {
            aiCards = new ArrayList<>();
        }

        // 更新左侧框：显示该 AI 的牌
        renderAiPlayedCards(viewData, aiFactionId, aiCards);
        factionPlayed.put(aiFactionId, true);

        // 维持 3 秒后进入下一回合
        turnHandler.postDelayed(() -> advanceTurn(viewData), 3000);
    }

    private void advanceTurn(NarrativeGameViewData viewData) {
        currentTurnIndex++;
        // 跳过已出牌的阵营，找到下一个未出牌的
        while (currentTurnIndex < turnOrder.size()) {
            String nextFaction = turnOrder.get(currentTurnIndex);
            if (factionPlayed.get(nextFaction) != Boolean.TRUE) {
                turnLocked = false;
                startCurrentTurn(viewData);
                return;
            }
            currentTurnIndex++;
        }
        // 所有人都出完了
        advanceNode(viewData);
    }

    private void advanceNode(NarrativeGameViewData viewData) {
        // 游戏已结束
        if (viewData.isGameOver()) {
            openSettlement();
            return;
        }
        // 所有阵营已出完牌，后端推进到下一节点
        CardGameApplication.getNarrativeActionHandler().submitEventCards(new ArrayList<>());
        renderGame();
    }

    private void highlightCurrentFaction(NarrativeGameViewData viewData) {
        String currentId = turnOrder.get(currentTurnIndex);
        String userFactionId = viewData.getUserFactionId();

        // 重置所有名字颜色
        tvNameTop.setTextColor(0xFFF6F2E8);
        tvNameLeft.setTextColor(0xFFF6F2E8);
        tvNameRight.setTextColor(0xFFF6F2E8);
        userFactionText.setTextColor(0xFFF6F2E8);

        // 高亮当前阵营
        int highlightColor = 0xFFFFD700; // 金色

        if (currentId.equals(userFactionId)) {
            userFactionText.setTextColor(highlightColor);
        } else {
            // 找到对应的 AI 阵营名
            String aiName = findFactionName(viewData, currentId);
            if (aiName.equals(tvNameTop.getText().toString())) {
                tvNameTop.setTextColor(highlightColor);
            } else if (aiName.equals(tvNameLeft.getText().toString())) {
                tvNameLeft.setTextColor(highlightColor);
            } else if (aiName.equals(tvNameRight.getText().toString())) {
                tvNameRight.setTextColor(highlightColor);
            }
        }
    }

    private void renderAiPlayedCards(NarrativeGameViewData viewData, String aiFactionId, List<EventCard> cards) {
        lastRoundCardsContainer.removeAllViews();
        String aiName = findFactionName(viewData, aiFactionId);
        lastRoundLabel.setText("上一轮出牌：" + aiName);

        for (EventCard card : cards) {
            TextView cardView = new TextView(this);
            cardView.setText(buildCardText(card));
             cardView.setTextSize(9);
            cardView.setTextColor(0xFF2B1A12);
            cardView.setBackgroundResource(R.drawable.bg_card_played);
            cardView.setGravity(Gravity.CENTER);
            cardView.setIncludeFontPadding(false);
            cardView.setPadding(dp(3), dp(3), dp(3), dp(3));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(80), dp(66));
            params.setMargins(0, 0, 4, 0);
            cardView.setLayoutParams(params);
            lastRoundCardsContainer.addView(cardView);
        }
    }

    // ========== 渲染方法 ==========

    private void renderProgressNodes(NarrativeGameViewData viewData) {
        progressNodeContainer.removeAllViews();
        List<String> titles = viewData.getStageTitles();
        int total = Math.max(viewData.getTotalNodes(), titles.size());
        for (int i = 0; i < total; i++) {
            final int index = i;
            TextView node = new TextView(this);
            node.setText(String.valueOf(i + 1));
            node.setGravity(Gravity.CENTER);
            node.setTextSize(10);
            node.setTextColor(0xFFFFFFFF);
            node.setTypeface(null, android.graphics.Typeface.BOLD);
            node.setBackgroundColor(i <= viewData.getGlobalProgress() ? 0xFF3D523D : 0x663D523D);
            node.setOnClickListener(v -> {
                String title = index < titles.size() ? titles.get(index) : "历史节点 " + (index + 1);
                Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(18), 1);
            params.setMargins(3, 0, 3, 0);
            node.setLayoutParams(params);
            progressNodeContainer.addView(node);
        }
    }

    private void renderFactionSeats(NarrativeGameViewData viewData) {
        List<Faction> aiFactions = new ArrayList<>();
        for (Faction faction : viewData.getFactions()) {
            if (!faction.getId().equals(viewData.getUserFactionId())) {
                aiFactions.add(faction);
            }
        }

        if (aiFactions.size() > 0) {
            tvNameTop.setText(aiFactions.get(0).getName());
            tvNameTop.setVisibility(View.VISIBLE);
            opponentTop.setVisibility(View.VISIBLE);
        } else {
            tvNameTop.setVisibility(View.GONE);
            opponentTop.setVisibility(View.GONE);
        }
        if (aiFactions.size() > 1) {
            tvNameLeft.setText(aiFactions.get(1).getName());
            tvNameLeft.setVisibility(View.VISIBLE);
            opponentLeft.setVisibility(View.VISIBLE);
        } else {
            tvNameLeft.setVisibility(View.GONE);
            opponentLeft.setVisibility(View.GONE);
        }
        if (aiFactions.size() > 2) {
            tvNameRight.setText(aiFactions.get(2).getName());
            tvNameRight.setVisibility(View.VISIBLE);
            opponentRight.setVisibility(View.VISIBLE);
        } else {
            tvNameRight.setVisibility(View.GONE);
            opponentRight.setVisibility(View.GONE);
        }
    }

    private void renderHearts(double hearts) {
        heartContainer.removeAllViews();
        for (int i = 0; i < 3; i++) {
            TextView heart = new TextView(this);
            heart.setText("♥");
            heart.setTextColor(0xFFE53935);
            heart.setTextSize(16);
            heart.setGravity(Gravity.CENTER);
            heart.setAlpha(heartAlpha(hearts, i));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(20), dp(22));
            params.setMargins(0, 0, 4, 0);
            heart.setLayoutParams(params);
            heartContainer.addView(heart);
        }
    }

    private float heartAlpha(double hearts, int index) {
        if (hearts >= index + 1) {
            return 1.0f;
        }
        if (hearts > index) {
            return 0.45f;
        }
        return 0.16f;
    }

    private void renderHandCards(List<EventCard> handCards) {
        handContainer.removeAllViews();
        for (EventCard card : handCards) {
            TextView cardView = new TextView(this);
            cardView.setText(buildCardText(card));
            cardView.setTextSize(11);
            cardView.setTextColor(0xFF2B1A12);
            cardView.setBackgroundResource(R.drawable.bg_card_normal);
            cardView.setGravity(Gravity.CENTER);
            cardView.setIncludeFontPadding(false);
            cardView.setLineSpacing(0, 1.02f);
            cardView.setPadding(dp(6), dp(4), dp(6), dp(4));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(88), dp(88));
            params.setMargins(0, 0, 8, 0);
            cardView.setLayoutParams(params);

            cardView.setOnClickListener(v -> {
                if (isPlayerTurn) {
                    toggleCard(cardView, card.getId());
                }
            });
            handContainer.addView(cardView);
        }

        handContainer.post(() -> {
            FrameLayout wrapper = (FrameLayout) handContainer.getParent();
            HorizontalScrollView scrollView = (HorizontalScrollView) wrapper.getParent();
            wrapper.setMinimumWidth(scrollView.getWidth());
        });
    }

    private String buildCardText(EventCard card) {
        StringBuilder builder = new StringBuilder(card.getTitle());
        if (card.getSourceHint() != null && !card.getSourceHint().trim().isEmpty()) {
            builder.append("\n").append(card.getSourceHint());
        }
        if (card.getSummary() != null && !card.getSummary().trim().isEmpty()) {
            builder.append("\n").append(card.getSummary());
        }
        return builder.toString();
    }

    private void renderLastRoundCards(NarrativeGameViewData viewData) {
        // 不在这里渲染，由 executeAiTurn 动态更新
        if (lastRoundCardsContainer.getChildCount() == 0) {
            lastRoundLabel.setText("等待出牌...");
        }
    }

    private void renderPlayedCards() {
        playerPlayedCardsContainer.removeAllViews();
        if (playerRoundCards.isEmpty()) {
            return;
        }
        for (EventCard card : playerRoundCards) {
            TextView cardView = new TextView(this);
            cardView.setText(buildCardText(card));
            cardView.setTextSize(9);
            cardView.setTextColor(0xFF2B1A12);
            cardView.setBackgroundResource(R.drawable.bg_card_played);
            cardView.setGravity(Gravity.CENTER);
            cardView.setIncludeFontPadding(false);
            cardView.setPadding(dp(3), dp(3), dp(3), dp(3));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(80), dp(66));
            params.setMargins(0, 0, 6, 0);
            cardView.setLayoutParams(params);
            playerPlayedCardsContainer.addView(cardView);
        }
    }

    // ========== 交互 ==========

    private void toggleCard(TextView cardView, String cardId) {
        if (!isPlayerTurn) {
            Toast.makeText(this, "还没轮到你出牌", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCardIds.contains(cardId)) {
            selectedCardIds.remove(cardId);
            cardView.setBackgroundResource(R.drawable.bg_card_normal);
        } else {
            selectedCardIds.add(cardId);
            cardView.setBackgroundResource(R.drawable.bg_card_selected);
        }
    }

    private void submitCards() {
        if (!isPlayerTurn) {
            Toast.makeText(this, "还没轮到你出牌", Toast.LENGTH_SHORT).show();
            return;
        }

        List<EventCard> currentHand = CardGameApplication.getNarrativeActionHandler()
                .getNarrativeGameViewData().getHandCards();

        List<EventCard> justPlayed = new ArrayList<>();
        for (String cardId : selectedCardIds) {
            for (EventCard card : currentHand) {
                if (card.getId().equals(cardId)) {
                    justPlayed.add(card);
                    break;
                }
            }
        }

        NarrativePlayResult result = CardGameApplication.getNarrativeActionHandler()
                .submitEventCards(new ArrayList<>(selectedCardIds));

        if (result.getPlayType() == NarrativePlayType.WRONG) {
            Toast.makeText(this, "选择不符合当前历史节点，请重新选择", Toast.LENGTH_SHORT).show();
            selectedCardIds.clear();
            renderGame();
            return;
        }

        // 正确：牌上移到右侧框，后端已推进节点
        playerRoundCards.addAll(justPlayed);
        selectedCardIds.clear();
        factionPlayed.put(CardGameApplication.getNarrativeActionHandler()
                .getNarrativeGameViewData().getUserFactionId(), true);

        renderPlayedCards();
        playInstructionText.setText("出牌成功！等待其他玩家...");
        submitButton.setEnabled(false);
        submitButton.setVisibility(View.GONE);
        btnAbandon.setVisibility(View.GONE);
        isPlayerTurn = false;

        // 重置高亮
        tvNameTop.setTextColor(0xFFF6F2E8);
        tvNameLeft.setTextColor(0xFFF6F2E8);
        tvNameRight.setTextColor(0xFFF6F2E8);
        userFactionText.setTextColor(0xFFF6F2E8);

        // 玩家提交已经推进了后端节点，直接渲染新节点
        // 延迟一小段让玩家看到自己的出牌，然后进入下一节点
        turnHandler.postDelayed(() -> {
            NarrativeGameViewData updated = CardGameApplication.getNarrativeActionHandler().getNarrativeGameViewData();
            if (updated.isGameOver()) {
                openSettlement();
            } else {
                renderGame();
            }
        }, 2000);
    }

    private void confirmAbandon() {
        new AlertDialog.Builder(this)
                .setTitle("结束推演")
                .setMessage("是否放弃当前历史推演？")
                .setPositiveButton("放弃", (dialog, which) -> {
                    turnHandler.removeCallbacksAndMessages(null);
                    lastPlayedCards.clear();
                    playerRoundCards.clear();
                    CardGameApplication.getNarrativeActionHandler().abandonGame();
                    renderGame();
                })
                .setNegativeButton("继续", null)
                .show();
    }

    private void openSettlement() {
        if (settlementOpened) {
            return;
        }
        settlementOpened = true;
        turnHandler.removeCallbacksAndMessages(null);
        startActivity(new Intent(this, NarrativeSettlementActivity.class));
    }

    // ========== 格式化 ==========

    private String formatAiProgress(NarrativeGameViewData viewData) {
        StringBuilder builder = new StringBuilder("AI 阵营进度：");
        boolean first = true;
        for (Faction faction : viewData.getFactions()) {
            if (faction.getId().equals(viewData.getUserFactionId())) {
                continue;
            }
            Integer progress = viewData.getAiProgress().get(faction.getId());
            if (!first) {
                builder.append(" · ");
            }
            builder.append(faction.getName()).append(" ")
                    .append(progress != null ? progress : 0)
                    .append("/")
                    .append(viewData.getTotalNodes());
            first = false;
        }
        return builder.toString();
    }

    private String formatLastResolvedNode(NarrativeGameViewData viewData) {
        if (viewData.getLastResolvedNodeIndex() < 0) {
            return "等待首次推进";
        }
        StringBuilder builder = new StringBuilder();
        Map<String, List<EventCard>> resolvedEvents = viewData.getLastResolvedNodeEvents();
        for (Faction faction : viewData.getFactions()) {
            builder.append(faction.getName());
            if (faction.getId().equals(viewData.getUserFactionId())) {
                builder.append("(你)");
            }
            builder.append("：");

            List<EventCard> cards = resolvedEvents.get(faction.getId());
            if (cards == null || cards.isEmpty()) {
                builder.append("空\n");
            } else {
                for (int i = 0; i < cards.size(); i++) {
                    if (i > 0) {
                        builder.append("、");
                    }
                    builder.append(cards.get(i).getTitle());
                }
                builder.append("\n");
            }
        }
        return builder.toString().trim();
    }

    private String findFactionName(NarrativeGameViewData viewData, String factionId) {
        for (Faction faction : viewData.getFactions()) {
            if (faction.getId().equals(factionId)) {
                return faction.getName();
            }
        }
        return factionId;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        turnHandler.removeCallbacksAndMessages(null);
    }
}