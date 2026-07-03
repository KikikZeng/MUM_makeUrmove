package com.example.cardgame.llm.narrative;

import com.example.cardgame.llm.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class NarrativePromptBuilder {

    public List<ChatMessage> buildMessages(String rawText) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", buildSystemPrompt()));
        messages.add(new ChatMessage("user", "文本内容：\n" + rawText));
        return messages;
    }

    private String buildSystemPrompt() {
        return "你是一个历史/文学叙事文本解析器。"
                + "请只返回合法 JSON，不要使用 Markdown，不要解释。"
                + "任务：从用户文本中提取 2 到 4 个阵营、事件牌、全局历史节点。"
                + "规则："
                + "1. 先按历史/叙事进程切分全局 nodes，再在每个 node 中分配各阵营事件。"
                + "2. 某阵营在某节点没有明确行动时，factionCardIds 中该阵营返回空数组 []，禁止编造行动。"
                + "3. cards 只包含事件牌，不包含原因牌、立场牌、旁白牌。"
                + "4. 每张事件牌 id 全局唯一，factionId 必须属于 factions。"
                + "5. nodes 中引用的 cardId 必须存在于 cards，且 card.factionId 必须与所属阵营一致。"
                + "6. stageHint 是给玩家的模糊提示，不能直接泄露应出的牌名。"
                + "7. openingNarration 和 resultNarration 是旁白展示，不参与判定。"
                + "8. 摘要尽量短，title 不超过 8 个汉字，summary 不超过 15 个汉字。"
                + "返回 JSON schema："
                + "{"
                + "\"factions\":[{\"id\":\"tang\",\"name\":\"唐廷\",\"description\":\"...\"}],"
                + "\"cards\":[{\"id\":\"c1\",\"factionId\":\"tang\",\"title\":\"...\",\"summary\":\"...\",\"sourceHint\":\"...\"}],"
                + "\"nodes\":[{\"nodeIndex\":0,\"stageTitle\":\"...\",\"stageHint\":\"...\",\"sourceHint\":\"...\","
                + "\"openingNarration\":\"...\",\"resultNarration\":\"...\","
                + "\"factionCardIds\":{\"tang\":[\"c1\"],\"rebel\":[]}}],"
                + "\"totalNodes\":1,"
                + "\"fallbackUsed\":false"
                + "}";
    }
}
