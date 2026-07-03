package com.example.cardgame.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
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
    private FrameLayout factionSeatContainer;
    private Button submitButton;
    private boolean settlementOpened;
    private final List<String> selectedCardIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_narrative_game);

        stageTitleText = findViewById(R.id.tv_stage_title);
        stageHintText = findViewById(R.id.tv_stage_hint);
        narrationText = findViewById(R.id.tv_narration);
        aiProgressText = findViewById(R.id.tv_ai_progress);
        nodeActionsText = findViewById(R.id.tv_node_actions);
        userFactionText = findViewById(R.id.tv_user_faction);
        progressNodeContainer = findViewById(R.id.layout_progress_nodes);
        heartContainer = findViewById(R.id.layout_heart_container);
        handContainer = findViewById(R.id.layout_hand_cards);
        factionSeatContainer = findViewById(R.id.layout_faction_seats);
        submitButton = findViewById(R.id.btn_submit_event_cards);
        Button btnAbandon = findViewById(R.id.btn_abandon_narrative);

        submitButton.setOnClickListener(v -> submitCards());
        btnAbandon.setOnClickListener(v -> confirmAbandon());

        renderGame();
    }

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
        nodeActionsText.setText(formatLastResolvedNode(viewData));
        submitButton.setText(viewData.isCurrentUserNodeEmpty() ? "继续推进" : "出牌");
        submitButton.setEnabled(!viewData.isGameOver());

        selectedCardIds.clear();
        renderHandCards(viewData.getHandCards());

        if (viewData.isGameOver()) {
            openSettlement();
        }
    }

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
        factionSeatContainer.removeAllViews();
        List<Faction> aiFactions = new ArrayList<>();
        Faction userFaction = null;
        for (Faction faction : viewData.getFactions()) {
            if (faction.getId().equals(viewData.getUserFactionId())) {
                userFaction = faction;
            } else {
                aiFactions.add(faction);
            }
        }

        int[] aiGravities = {Gravity.TOP | Gravity.START, Gravity.TOP | Gravity.END, Gravity.BOTTOM | Gravity.START};
        for (int i = 0; i < aiFactions.size(); i++) {
            int gravity = aiGravities[Math.min(i, aiGravities.length - 1)];
            factionSeatContainer.addView(createSeatView(aiFactions.get(i).getName(), false), createSeatParams(gravity));
        }
        if (userFaction != null) {
            factionSeatContainer.addView(createSeatView(userFaction.getName() + "（你）", true),
                    createSeatParams(Gravity.BOTTOM | Gravity.END));
        }
    }

    private TextView createSeatView(String text, boolean user) {
        TextView seat = new TextView(this);
        seat.setText(text);
        seat.setGravity(Gravity.CENTER);
        seat.setTextColor(0xFFFFFFFF);
        seat.setTextSize(11);
        seat.setTypeface(null, android.graphics.Typeface.BOLD);
        seat.setBackgroundColor(user ? 0xCC9E2F1D : 0x884E342E);
        seat.setPadding(6, 2, 6, 2);
        return seat;
    }

    private FrameLayout.LayoutParams createSeatParams(int gravity) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(90), dp(28));
        params.gravity = gravity;
        return params;
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
            cardView.setBackgroundColor(0xFFFFF4DE);
            cardView.setGravity(Gravity.CENTER);
            cardView.setIncludeFontPadding(false);
            cardView.setLineSpacing(0, 1.02f);
            cardView.setPadding(6, 6, 6, 6);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(104), dp(88));
            params.setMargins(0, 0, 8, 0);
            cardView.setLayoutParams(params);

            cardView.setOnClickListener(v -> toggleCard(cardView, card.getId()));
            handContainer.addView(cardView);
        }
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

    private void toggleCard(TextView cardView, String cardId) {
        if (selectedCardIds.contains(cardId)) {
            selectedCardIds.remove(cardId);
            cardView.setBackgroundColor(0xFFFFF4DE);
        } else {
            selectedCardIds.add(cardId);
            cardView.setBackgroundColor(0xFFFFD48A);
        }
    }

    private void submitCards() {
        NarrativePlayResult result = CardGameApplication.getNarrativeActionHandler()
                .submitEventCards(new ArrayList<>(selectedCardIds));
        if (result.getPlayType() == NarrativePlayType.WRONG) {
            Toast.makeText(this, "选择不符合当前历史节点", Toast.LENGTH_SHORT).show();
        } else if (result.getPlayType() == NarrativePlayType.INCOMPLETE) {
            Toast.makeText(this, "没选全，历史仍向前推进", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "推演推进", Toast.LENGTH_SHORT).show();
        }
        renderGame();
    }

    private void confirmAbandon() {
        new AlertDialog.Builder(this)
                .setTitle("结束推演")
                .setMessage("是否放弃当前历史推演？")
                .setPositiveButton("放弃", (dialog, which) -> {
                    CardGameApplication.getNarrativeActionHandler().abandonGame();
                    renderGame();
                })
                .setNegativeButton("继续", null)
                .show();
    }

    private void showSettlement(NarrativeGameViewData viewData) {
        openSettlement();
    }

    private void openSettlement() {
        if (settlementOpened) {
            return;
        }
        settlementOpened = true;
        startActivity(new Intent(this, NarrativeSettlementActivity.class));
    }

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
            return "上一轮推进：等待首次推进";
        }

        StringBuilder builder = new StringBuilder("上一轮推进\n");
        Map<String, List<EventCard>> resolvedEvents = viewData.getLastResolvedNodeEvents();
        for (Faction faction : viewData.getFactions()) {
            builder.append(faction.getName());
            if (faction.getId().equals(viewData.getUserFactionId())) {
                builder.append("（你）");
            }
            builder.append("：");

            List<EventCard> cards = resolvedEvents.get(faction.getId());
            if (cards == null || cards.isEmpty()) {
                builder.append("空推进");
            } else {
                for (int i = 0; i < cards.size(); i++) {
                    if (i > 0) {
                        builder.append("、");
                    }
                    builder.append(cards.get(i).getTitle());
                }
            }
            builder.append("\n");
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
}