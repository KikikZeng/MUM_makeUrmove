package com.example.cardgame.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardgame.CardGameApplication;
import com.example.cardgame.R;
import com.example.cardgame.dto.narrative.PreviewViewData;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.NarrativeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NarrativePreviewActivity extends AppCompatActivity {
    private LinearLayout factionContainer;
    private LinearLayout handPreviewContainer;
    private LinearLayout timelineContainer;
    private TextView summaryText;
    private TextView timeText;
    private EditText sourceText;
    private Button startButton;
    private Button reparseButton;
    private HorizontalScrollView handPreviewScroll;
    private String selectedFactionId;
    private PreviewViewData currentPreview;
    private String originalText;
    private boolean sourceExpanded;
    private boolean handExpanded;
    private boolean timelineExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_narrative_preview);

        summaryText = findViewById(R.id.tv_preview_summary);
        timeText = findViewById(R.id.tv_preview_time);
        sourceText = findViewById(R.id.et_preview_source_text);
        factionContainer = findViewById(R.id.layout_faction_list);
        handPreviewContainer = findViewById(R.id.layout_hand_preview);
        timelineContainer = findViewById(R.id.layout_timeline);
        handPreviewScroll = findViewById(R.id.scroll_hand_preview);
        Button btnToggleSource = findViewById(R.id.btn_toggle_source_text);
        Button btnToggleHand = findViewById(R.id.btn_toggle_hand_preview);
        Button btnToggleTimeline = findViewById(R.id.btn_toggle_timeline);
        Button btnReparse = findViewById(R.id.btn_reparse);
        startButton = findViewById(R.id.btn_start_narrative_game);
        reparseButton = btnReparse;
        ImageButton btnBack = findViewById(R.id.btn_preview_back);
        ImageButton btnHelp = findViewById(R.id.btn_help);

        btnToggleSource.setOnClickListener(v -> toggleSourceText());
        btnToggleHand.setOnClickListener(v -> toggleHandPreview());
        btnToggleTimeline.setOnClickListener(v -> toggleTimeline());
        btnReparse.setOnClickListener(v -> confirmReparseIfNeeded());
        startButton.setOnClickListener(v -> startGame());
        btnBack.setOnClickListener(v -> finish());
        btnHelp.setOnClickListener(v -> showHelpDialog());

        sourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateReparseButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        renderPreview();
    }

    private void renderPreview() {
        currentPreview = CardGameApplication.getNarrativeActionHandler().getPreviewViewData();
        if (currentPreview == null) {
            Toast.makeText(this, "暂无解析结果", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int totalCards = currentPreview.getCards().size();
        summaryText.setText(currentPreview.getFactions().size() + "个阵营 · 共 "
                + totalCards + " 张牌 · "
                + currentPreview.getTotalNodes() + "个历史节点"
                + (currentPreview.isFallbackUsed() ? " · 预设数据" : ""));
        timeText.setText("预计对局时间 " + estimatePlayTime(currentPreview.getTotalNodes()));
        factionContainer.removeAllViews();

        selectedFactionId = null;
        originalText = currentPreview.getRawText() != null ? currentPreview.getRawText() : "";
        sourceText.setText(originalText);
        updateReparseButton();
        updateStartButton();

        for (Faction faction : currentPreview.getFactions()) {
            factionContainer.addView(createFactionView(faction, currentPreview.getCardCountByFaction()));
        }
        renderSelectedFactionCards();
        renderTimeline();
    }

    private View createFactionView(Faction faction, Map<String, Integer> cardCounts) {
        Button button = new Button(this);
        int count = cardCounts.containsKey(faction.getId()) ? cardCounts.get(faction.getId()) : 0;
        button.setText(faction.getName() + "  " + count + "张\n" + faction.getDescription());
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(0xFFFFFFFF);
        applyFactionButtonState(button, faction.getId().equals(selectedFactionId));
        button.setPadding(16, 12, 16, 12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 14);
        button.setLayoutParams(params);

        button.setOnClickListener(v -> {
            selectedFactionId = faction.getId();
            refreshFactionButtonStates();
            updateStartButton();
            renderSelectedFactionCards();
            Toast.makeText(this, "已选择：" + faction.getName(), Toast.LENGTH_SHORT).show();
        });
        return button;
    }

    private void refreshFactionButtonStates() {
        if (currentPreview == null) {
            return;
        }
        for (int i = 0; i < factionContainer.getChildCount(); i++) {
            View child = factionContainer.getChildAt(i);
            Faction faction = currentPreview.getFactions().get(i);
            applyFactionButtonState(child, faction.getId().equals(selectedFactionId));
        }
    }

    private void applyFactionButtonState(View view, boolean selected) {
        view.setBackgroundColor(selected ? 0xFF9E2F1D : 0xFF4E342E);
        view.setAlpha(selected ? 1.0f : 0.82f);
    }

    private void toggleSourceText() {
        sourceExpanded = !sourceExpanded;
        sourceText.setVisibility(sourceExpanded ? View.VISIBLE : View.GONE);
    }

    private void toggleHandPreview() {
        handExpanded = !handExpanded;
        handPreviewScroll.setVisibility(handExpanded ? View.VISIBLE : View.GONE);
        renderSelectedFactionCards();
    }

    private void toggleTimeline() {
        timelineExpanded = !timelineExpanded;
        timelineContainer.setVisibility(timelineExpanded ? View.VISIBLE : View.GONE);
        renderTimeline();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("使用说明")
                .setMessage("在此页面可以查看解析结果：选择左侧阵营查看其手牌，展开「历史节点」查看推演流程。\n\n确认无误后选择要扮演的阵营，点击「开始推演」进入对局。如需修改文本，可展开原文编辑后重新解析。")
                .setPositiveButton("知道了", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmReparseIfNeeded() {
        if (!isSourceTextModified()) {
            Toast.makeText(this, "当前文本未修改，无需重新解析", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("重新解析")
                .setMessage("确认基于修改后的文本重新生成阵营和卡牌？")
                .setPositiveButton("确认", (dialog, which) -> reparseSourceText())
                .setNegativeButton("取消", null)
                .show();
    }

    private void reparseSourceText() {
        String text = sourceText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "文本不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        reparseButton.setEnabled(false);
        reparseButton.setText("解析中...");
        new Thread(() -> {
            CardGameApplication.getNarrativeActionHandler().parseText(text);
            runOnUiThread(() -> {
                reparseButton.setText("重新解析");
                sourceExpanded = true;
                handExpanded = false;
                timelineExpanded = false;
                handPreviewScroll.setVisibility(View.GONE);
                timelineContainer.setVisibility(View.GONE);
                renderPreview();
                Toast.makeText(this, "已基于修改文本重新解析", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void updateReparseButton() {
        boolean modified = isSourceTextModified();
        reparseButton.setEnabled(true);
        reparseButton.setAlpha(modified ? 1.0f : 0.45f);
    }

    private boolean isSourceTextModified() {
        if (originalText == null) {
            return false;
        }
        return !originalText.equals(sourceText.getText().toString());
    }

    private void updateStartButton() {
        boolean selected = selectedFactionId != null;
        startButton.setAlpha(selected ? 1.0f : 0.45f);
    }

    private void renderSelectedFactionCards() {
        handPreviewContainer.removeAllViews();
        if (currentPreview == null || selectedFactionId == null) {
            addHintText(handPreviewContainer, "请选择阵营后查看手牌");
            return;
        }
        for (EventCard card : currentPreview.getCards()) {
            if (selectedFactionId.equals(card.getFactionId())) {
                handPreviewContainer.addView(createEventCardView(card, findFactionName(card.getFactionId())));
            }
        }
    }

    private void renderTimeline() {
        timelineContainer.removeAllViews();
        if (currentPreview == null) {
            return;
        }
        Map<String, EventCard> cardsById = new HashMap<>();
        for (EventCard card : currentPreview.getCards()) {
            cardsById.put(card.getId(), card);
        }
        for (NarrativeNode node : currentPreview.getNodes()) {
            TextView title = new TextView(this);
            title.setText((node.getNodeIndex() + 1) + ". " + node.getStageTitle());
            title.setTextColor(0xFFFFFFFF);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextSize(16);
            title.setPadding(0, 12, 0, 6);
            timelineContainer.addView(title);

            HorizontalScrollView rowScroll = new HorizontalScrollView(this);
            rowScroll.setHorizontalScrollBarEnabled(true);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (Map.Entry<String, List<String>> entry : node.getFactionCardIds().entrySet()) {
                for (String cardId : entry.getValue()) {
                    EventCard card = cardsById.get(cardId);
                    if (card != null) {
                        row.addView(createEventCardView(card, findFactionName(card.getFactionId())));
                    }
                }
            }
            if (row.getChildCount() == 0) {
                addHintText(row, "本节点为空推进");
            }
            rowScroll.addView(row);
            timelineContainer.addView(rowScroll);
        }
    }

    private View createEventCardView(EventCard card, String factionName) {
        TextView cardView = new TextView(this);
        cardView.setText(card.getTitle() + "\n" + card.getSourceHint() + "\n" + factionName);
        cardView.setTextSize(12);
        cardView.setTextColor(0xFF2B1A12);
        cardView.setGravity(Gravity.CENTER);
        cardView.setIncludeFontPadding(false);
        cardView.setLineSpacing(0, 1.08f);
        cardView.setPadding(8, 8, 8, 8);
        cardView.setBackgroundColor(colorForFaction(card.getFactionId()));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(104), dp(104));
        params.setMargins(0, 0, 8, 8);
        cardView.setLayoutParams(params);
        return cardView;
    }

    private void addHintText(LinearLayout container, String text) {
        TextView hint = new TextView(this);
        hint.setText(text);
        hint.setTextColor(0xFFE8FFFF);
        hint.setTextSize(14);
        hint.setPadding(8, 12, 8, 12);
        container.addView(hint);
    }

    private int colorForFaction(String factionId) {
        int index = 0;
        if (currentPreview != null) {
            for (int i = 0; i < currentPreview.getFactions().size(); i++) {
                if (currentPreview.getFactions().get(i).getId().equals(factionId)) {
                    index = i;
                    break;
                }
            }
        }
        int[] colors = {0xFFFFF4DE, 0xFFE7F0FF, 0xFFE9F8DD, 0xFFFFE4E4};
        return colors[index % colors.length];
    }

    private String findFactionName(String factionId) {
        if (currentPreview == null) {
            return "";
        }
        for (Faction faction : currentPreview.getFactions()) {
            if (faction.getId().equals(factionId)) {
                return faction.getName();
            }
        }
        return factionId;
    }

    private String estimatePlayTime(int nodes) {
        int min = Math.max(2, nodes / 2);
        int max = Math.max(min + 1, nodes);
        return min + "-" + max + " 分钟";
    }

    private void startGame() {
        if (selectedFactionId == null) {
            Toast.makeText(this, "请选择阵营", Toast.LENGTH_SHORT).show();
            return;
        }
        CardGameApplication.getNarrativeActionHandler().startNarrativeGame(selectedFactionId);
        startActivity(new Intent(this, NarrativeGameActivity.class));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}