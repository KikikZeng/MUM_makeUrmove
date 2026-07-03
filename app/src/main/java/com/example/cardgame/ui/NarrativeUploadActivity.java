package com.example.cardgame.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardgame.CardGameApplication;
import com.example.cardgame.R;

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

        inputText = findViewById(R.id.rv_camp_list);
        Button btnSample = findViewById(R.id.btn_hand_preview);
        parseButton = findViewById(R.id.btn_timeline_preview);
        previewButton = findViewById(R.id.btn_reparse);
        counterText = findViewById(R.id.tv_game_duration);
        ImageButton btnBack = findViewById(R.id.btn_back);
        Button btnClear = findViewById(R.id.btn_enter_game);

        inputText.setText(SAMPLE_TEXT);
        updateCounter();
        updatePreviewButton(false);

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

        btnSample.setOnClickListener(v -> inputText.setText(SAMPLE_TEXT));
        btnClear.setOnClickListener(v -> inputText.setText(""));
        btnBack.setOnClickListener(v -> finish());
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
            CardGameApplication.getNarrativeActionHandler().parseText(rawText);
            runOnUiThread(() -> {
                parseButton.setEnabled(true);
                parseButton.setText("开始解析");
                updatePreviewButton(true);
                Toast.makeText(this, "解析完成，可进入结果预览", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void openPreview() {
        if (!previewButton.isEnabled()) {
            Toast.makeText(this, "请先完成解析", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, NarrativePreviewActivity.class));
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
}