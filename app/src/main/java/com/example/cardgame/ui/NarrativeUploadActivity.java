package com.example.cardgame.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardgame.CardGameApplication;
import com.example.cardgame.R;
import com.example.cardgame.dto.narrative.ParseResult;

public class NarrativeUploadActivity extends AppCompatActivity {
    private static final int MAX_TEXT_LENGTH = 5000;
    private static final String SAMPLE_TEXT = "安史之乱是唐朝由盛转衰的重要事件。边镇节度使权力集中，安禄山势力坐大，并在范阳起兵反唐。叛乱迅速扩大，唐廷仓促组织平叛，百姓遭受战乱。局势经历逃亡、兵变、反攻与重建，最终留下藩镇坐大和社会创伤。";

    private EditText inputText;
    private Button parseButton;
    private Button previewButton;
    private TextView counterText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_narrative_upload);
        hideSystemUI();

        inputText = findViewById(R.id.rv_camp_list);
        inputText.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        Button btnSample = findViewById(R.id.btn_hand_preview);
        parseButton = findViewById(R.id.btn_timeline_preview);
        previewButton = findViewById(R.id.btn_reparse);
        counterText = findViewById(R.id.tv_game_duration);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnHelp = findViewById(R.id.btn_help);
        Button btnClear = findViewById(R.id.btn_enter_game);

        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCounter();
                updatePreviewButton(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        updateCounter();
        updatePreviewButton(false);

        btnSample.setOnClickListener(v -> inputText.setText(SAMPLE_TEXT));
        btnClear.setOnClickListener(v -> inputText.setText(""));
        btnBack.setOnClickListener(v -> finish());
        btnHelp.setOnClickListener(v -> showHelpDialog());
        parseButton.setOnClickListener(v -> parseAndPreview());
        previewButton.setOnClickListener(v -> openPreview());
    }

    private void parseAndPreview() {
        String rawText = inputText.getText().toString().trim();
        if (rawText.isEmpty()) {
            Toast.makeText(this, "请输入一段叙事文本", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rawText.length() > MAX_TEXT_LENGTH) {
            Toast.makeText(this, "文本超过 5000 字，请先精简", Toast.LENGTH_SHORT).show();
            return;
        }
        parseButton.setEnabled(false);
        parseButton.setText("解析中...");
        updatePreviewButton(false);
        new Thread(() -> {
            ParseResult result = CardGameApplication.getNarrativeActionHandler().parseText(rawText);
            runOnUiThread(() -> {
                parseButton.setEnabled(true);
                parseButton.setText("开始解析");
                handleParseResult(result);
            });
        }).start();
    }

    private void handleParseResult(ParseResult result) {
        if (result == null) {
            showErrorDialog("解析异常，请重试", false);
            return;
        }

        switch (result.getParseStatus()) {
            case ParseResult.STATUS_SUCCESS:
                updatePreviewButton(true);
                Toast.makeText(this, "解析成功，查看预览结果", Toast.LENGTH_SHORT).show();
                break;

            case ParseResult.STATUS_FACTION_COUNT_INVALID:
                showErrorDialog("当前文本只提取到一个阵营或者缺少明确阵营对立关系，请换一个文本试试吧", true);
                break;

            case ParseResult.STATUS_PARSE_ERROR:
                showErrorDialog("解析异常，请重试", false);
                break;

            case ParseResult.STATUS_MISSING_ACTION:
                showErrorDialog("有一方阵营没有具体行动，请补充后重试", true);
                break;

            default:
                showErrorDialog("解析异常，请重试", false);
                break;
        }
    }

    private void showErrorDialog(String message, boolean clearText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage(message)
                .setPositiveButton("知道了", (dialog, which) -> {
                    dialog.dismiss();
                    if (clearText) {
                        inputText.setText("");
                    }
                })
                .setCancelable(false);
        builder.show();
    }

    private void openPreview() {
        if (!previewButton.isEnabled()) {
            Toast.makeText(this, "请先完成解析", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, NarrativePreviewActivity.class));
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("使用说明")
                .setMessage("在此页面输入或粘贴一段叙事文本（如历史事件、小说情节等），点击「开始解析」后系统将自动分析文本中的阵营、事件和卡牌。\n\n您也可以点击「示例文本」查看样例，或点击「结果预览」查看解析结果。")
                .setPositiveButton("知道了", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateCounter() {
        int length = inputText.getText().toString().length();
        int remaining = Math.max(0, MAX_TEXT_LENGTH - length);
        counterText.setText("最多 5000 字 · 剩余 " + remaining + " 字");
        counterText.setTextColor(length > MAX_TEXT_LENGTH ? 0xFFE8D5A3 : 0xFF8B7355);
    }

    private void updatePreviewButton(boolean enabled) {
        previewButton.setEnabled(enabled);
        previewButton.setAlpha(enabled ? 1.0f : 0.45f);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}