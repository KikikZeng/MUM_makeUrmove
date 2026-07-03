package com.example.cardgame.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardgame.CardGameApplication;
import com.example.cardgame.R;
import com.example.cardgame.dto.narrative.NarrativeGameViewData;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.GameStatus;

import java.util.List;

public class NarrativeSettlementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_narrative_settlement);

        TextView titleText = findViewById(R.id.tv_settlement_title);
        TextView historyResultText = findViewById(R.id.tv_history_result);
        TextView historySentenceText = findViewById(R.id.tv_history_sentence);
        TextView ratingText = findViewById(R.id.tv_settlement_rating);
        TextView statsText = findViewById(R.id.tv_settlement_stats);
        Button btnHome = findViewById(R.id.btn_settlement_home);
        Button btnReplay = findViewById(R.id.btn_settlement_replay);

        NarrativeGameViewData viewData = CardGameApplication.getNarrativeActionHandler().getNarrativeGameViewData();
        if (viewData == null) {
            titleText.setText("推演结束");
            historyResultText.setText("历史结果：暂无");
            historySentenceText.setText("未获取到本局历史推演数据。");
            ratingText.setText("未完成");
            statsText.setText("暂无对局数据");
        } else {
            titleText.setText(viewData.getStatus() == GameStatus.ABANDONED ? "中途放弃" : "推演完成");
            historyResultText.setText(buildHistoryResult(viewData));
            historySentenceText.setText(buildHistorySentence(viewData));
            ratingText.setText(buildRating(viewData));
            statsText.setText(buildStats(viewData));
        }

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        btnReplay.setOnClickListener(v -> {
            Intent intent = new Intent(this, NarrativeUploadActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private String buildRating(NarrativeGameViewData viewData) {
        if (viewData.getStatus() == GameStatus.ABANDONED) {
            return "未完成";
        }
        if (viewData.getHearts() <= 0.0 || viewData.getSevereErrorCount() > 0) {
            return "失败";
        }
        if (viewData.getWrongCount() == 0 && viewData.getIncompleteCount() == 0) {
            return "优秀";
        }
        return "合格";
    }

    private String buildHistoryResult(NarrativeGameViewData viewData) {
        if (viewData.getStatus() == GameStatus.ABANDONED) {
            return "历史结果：未完成";
        }
        return "历史结果：全阵营完成本段历史推演";
    }

    private String buildHistorySentence(NarrativeGameViewData viewData) {
        if (viewData.getLastResolvedNodeEvents().isEmpty()) {
            return "本局尚未推进到完整历史节点。";
        }

        StringBuilder builder = new StringBuilder("最后一轮：");
        boolean firstFaction = true;
        for (Faction faction : viewData.getFactions()) {
            List<EventCard> cards = viewData.getLastResolvedNodeEvents().get(faction.getId());
            if (cards == null || cards.isEmpty()) {
                continue;
            }
            if (!firstFaction) {
                builder.append("；");
            }
            builder.append(faction.getName()).append("完成");
            for (int i = 0; i < cards.size(); i++) {
                if (i > 0) {
                    builder.append("、");
                }
                builder.append(cards.get(i).getTitle());
            }
            firstFaction = false;
        }
        return firstFaction ? "本局历史线已推进完成。" : builder.toString();
    }

    private String buildStats(NarrativeGameViewData viewData) {
        int attempts = viewData.getCorrectCount()
                + viewData.getWrongCount()
                + viewData.getIncompleteCount()
                + viewData.getSevereErrorCount();
        int accuracy = attempts == 0 ? 0 : Math.round(viewData.getCorrectCount() * 100f / attempts);
        return "个人表现评估"
                + "\n正确率：" + accuracy + "%"
                + "\n历史进度：" + viewData.getGlobalProgress() + "/" + viewData.getTotalNodes()
                + "\n剩余生命：" + formatHearts(viewData.getHearts())
                + "\n完全正确：" + viewData.getCorrectCount()
                + "  选错：" + viewData.getWrongCount()
                + "  没选全：" + viewData.getIncompleteCount()
                + "  严重错误：" + viewData.getSevereErrorCount()
                + "\n用时：暂未统计";
    }

    private String formatHearts(double hearts) {
        return hearts == (int) hearts ? String.valueOf((int) hearts) : String.valueOf(hearts);
    }
}
