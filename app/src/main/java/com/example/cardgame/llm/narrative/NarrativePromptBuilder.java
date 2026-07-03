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
                + "任务：从用户文本中提取阵营、事件牌、全局历史节点。"
                + "本产品仅解析历史与文学叙事文本，忽略现代商业或科技内容。"
                + "阵营数量：根据文本中的立场对立程度，自动识别 2 到 5 个阵营。若对立双方明显，则 2 个；若多方角力，则适当增加，但不超过 5 个。"
                + "规则："
                + "1. 先按历史/叙事进程切分全局 nodes，再在每个 node 中分配各阵营事件。"
                + "2. 某阵营在某节点没有明确行动时，factionCardIds 中该阵营返回空数组 []，禁止编造行动。"
                + "3. cards 只包含事件牌，不包含原因牌、立场牌、旁白牌。优先提取具有冲突性、转折性的关键事件，避免平淡描述。"
                + "4. 每张事件牌 id 全局唯一，factionId 必须属于 factions。"
                + "5. nodes 中引用的 cardId 必须存在于 cards，且 card.factionId 必须与所属阵营一致。"
                + "6. stageHint 是给玩家的模糊提示，不能直接泄露应出的牌名。"
                + "7. openingNarration 和 resultNarration 是旁白展示，不参与判定。"
                + "8. 摘要尽量短，title 不超过 8 个汉字，summary 不超过 15 个汉字。"
                + "9. JSON 中所有标点必须使用英文半角符号（冒号用: 逗号用, 引号用英文双引号），严禁使用中文全角标点。"
                + "正确：id\":\"wei"
                + "错误：id\"：\"wei"
                + "返回 JSON schema："
                + "{"
                + "\"factions\":[{\"id\":\"tang\",\"name\":\"唐廷\",\"description\":\"...\"}],"
                + "\"cards\":[{\"id\":\"c1\",\"factionId\":\"tang\",\"title\":\"...\",\"summary\":\"...\",\"sourceHint\":\"...\"}],"
                + "\"nodes\":[{\"nodeIndex\":0,\"stageTitle\":\"...\",\"stageHint\":\"...\",\"sourceHint\":\"...\","
                + "\"openingNarration\":\"...\",\"resultNarration\":\"...\","
                + "\"factionCardIds\":{\"tang\":[\"c1\"],\"rebel\":[]}}],"
                + "\"totalNodes\":1,"
                + "\"fallbackUsed\":false"
                + "}"
                + "示例："
                + "示例输入："
                + "\"755年，安禄山在范阳起兵叛唐，迅速攻占洛阳。唐玄宗命哥舒翰守潼关，却因杨国忠谗言逼迫哥舒翰出战，导致唐军大败。叛军攻入长安，玄宗逃往蜀中。太子李亨在灵武即位，是为唐肃宗，开始组织反击。\""
                + "示例输出："
                + "{"
                + "\"factions\":["
                + "{\"id\":\"tang\",\"name\":\"唐廷\",\"description\":\"中央政权，初期昏聩，后期组织反击\"},"
                + "{\"id\":\"rebel\",\"name\":\"叛军\",\"description\":\"安禄山领导的叛乱势力，攻势凶猛\"}"
                + "],"
                + "\"cards\":["
                + "{\"id\":\"c1\",\"factionId\":\"rebel\",\"title\":\"范阳起兵\",\"summary\":\"安禄山在范阳发动叛乱\",\"sourceHint\":\"安禄山起兵\"},"
                + "{\"id\":\"c2\",\"factionId\":\"tang\",\"title\":\"哥舒翰败亡\",\"summary\":\"杨国忠逼迫出战，唐军大败\",\"sourceHint\":\"潼关之战\"},"
                + "{\"id\":\"c3\",\"factionId\":\"tang\",\"title\":\"玄宗西逃\",\"summary\":\"叛军入长安，玄宗逃蜀\",\"sourceHint\":\"长安陷落\"}"
                + "],"
                + "\"nodes\":["
                + "{\"nodeIndex\":0,\"stageTitle\":\"叛乱爆发\",\"stageHint\":\"边镇势力开始挑战中央权威\",\"sourceHint\":\"安禄山起兵\",\"openingNarration\":\"安禄山在范阳举起叛旗，大唐震动。\",\"resultNarration\":\"叛军势如破竹，朝廷措手不及。\",\"factionCardIds\":{\"rebel\":[\"c1\"],\"tang\":[]}},"
                + "{\"nodeIndex\":1,\"stageTitle\":\"潼关失守\",\"stageHint\":\"内部猜忌导致防线崩溃\",\"sourceHint\":\"哥舒翰出战\",\"openingNarration\":\"玄宗命哥舒翰固守潼关，但杨国忠屡进谗言。\",\"resultNarration\":\"唐军惨败，叛军直逼长安。\",\"factionCardIds\":{\"tang\":[\"c2\"],\"rebel\":[]}},"
                + "{\"nodeIndex\":2,\"stageTitle\":\"肃宗即位\",\"stageHint\":\"新的领导者开始组织反击\",\"sourceHint\":\"灵武即位\",\"openingNarration\":\"太子李亨在灵武登基，扛起平叛大旗。\",\"resultNarration\":\"唐廷开始重整旗鼓，局势出现转机。\",\"factionCardIds\":{\"tang\":[\"c3\"],\"rebel\":[]}}"
                + "],"
                + "\"totalNodes\":3,"
                + "\"fallbackUsed\":false"
                + "}";
    }
}
