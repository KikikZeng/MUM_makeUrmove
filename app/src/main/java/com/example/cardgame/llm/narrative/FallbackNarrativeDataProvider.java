package com.example.cardgame.llm.narrative;

import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.NarrativeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FallbackNarrativeDataProvider {

    public ParseResult getFallbackData() {
        List<Faction> factions = Arrays.asList(
                new Faction("tang", "唐廷", "代表中央朝廷及平叛力量"),
                new Faction("rebel", "叛军", "以安史集团为核心的反叛势力"),
                new Faction("local", "藩镇", "地方节度使与观望势力"),
                new Faction("people", "民众", "战争中受影响的普通民众")
        );

        List<EventCard> cards = Arrays.asList(
                new EventCard("t1", "tang", "边镇失衡", "朝廷倚重节度使", "安史之乱背景"),
                new EventCard("r1", "rebel", "势力坐大", "安禄山兼领重镇", "安史之乱背景"),
                new EventCard("r2", "rebel", "范阳起兵", "安禄山起兵反唐", "叛乱爆发"),
                new EventCard("l1", "local", "地方观望", "地方势力观望局势", "叛乱爆发"),
                new EventCard("t2", "tang", "仓促应对", "唐廷组织平叛", "战局扩大"),
                new EventCard("r3", "rebel", "攻陷两京", "叛军威胁中枢", "战局扩大"),
                new EventCard("p1", "people", "流离失所", "百姓遭受战乱", "战局扩大"),
                new EventCard("t3", "tang", "马嵬兵变", "逃亡途中军心激变", "局势转折"),
                new EventCard("p2", "people", "民生凋敝", "战乱加重民困", "局势转折"),
                new EventCard("t4", "tang", "收复两京", "唐军逐步反攻", "平叛推进"),
                new EventCard("r4", "rebel", "内部分裂", "叛军阵营分化", "平叛推进"),
                new EventCard("l2", "local", "协助平叛", "部分藩镇转向朝廷", "平叛推进"),
                new EventCard("t5", "tang", "重建秩序", "朝廷恢复统治", "乱后重建"),
                new EventCard("l3", "local", "藩镇坐大", "地方权力继续扩大", "乱后重建"),
                new EventCard("p3", "people", "社会创伤", "人口经济受重创", "乱后重建")
        );

        List<NarrativeNode> nodes = Arrays.asList(
                node(0, "矛盾积累", "边镇权力不断集中，局势暗中失衡。",
                        "帝国边镇的权力逐渐集中，风暴在表面平静中酝酿。",
                        "朝廷对边镇的倚重与叛军势力的坐大同时发生，危机开始成形。",
                        mapOf("tang", list("t1"), "rebel", list("r1"), "local", list(), "people", list())),
                node(1, "叛乱爆发", "局势由潜在矛盾转为公开冲突。",
                        "范阳的战火点燃了叛乱，观望者开始判断局势走向。",
                        "叛军率先打破平衡，地方势力进入观望，唐廷尚未形成有效反应。",
                        mapOf("tang", list(), "rebel", list("r2"), "local", list("l1"), "people", list())),
                node(2, "战局扩大", "冲突迅速扩大，并开始冲击王朝中枢。",
                        "叛乱不再只是边地震动，它开始威胁王朝的核心秩序。",
                        "唐廷仓促组织平叛，叛军攻势扩大，民众开始承受战乱代价。",
                        mapOf("tang", list("t2"), "rebel", list("r3"), "local", list(), "people", list("p1"))),
                node(3, "局势转折", "朝廷危机与民间苦难同时加深。",
                        "逃亡、兵变与民生困顿交织在一起，局势进入艰难转折。",
                        "唐廷内部震荡，民间苦难加深，战争的代价逐渐显露。",
                        mapOf("tang", list("t3"), "rebel", list(), "local", list(), "people", list("p2"))),
                node(4, "平叛推进", "战争形势逐渐从扩张转向反攻。",
                        "反攻开始推进，叛军内部也不再像最初那样稳固。",
                        "唐军逐步收复要地，叛军分裂，部分地方势力转向协助平叛。",
                        mapOf("tang", list("t4"), "rebel", list("r4"), "local", list("l2"), "people", list())),
                node(5, "乱后重建", "叛乱结束后，王朝秩序与社会结构留下长期影响。",
                        "战火渐息，但乱后的唐帝国已经不再回到原来的样子。",
                        "朝廷重建秩序，藩镇权力延续扩大，社会创伤成为长期阴影。",
                        mapOf("tang", list("t5"), "rebel", list(), "local", list("l3"), "people", list("p3")))
        );

        return new ParseResult(factions, cards, nodes, nodes.size(), true);
    }

    private NarrativeNode node(int index, String title, String hint,
                               String openingNarration, String resultNarration,
                               Map<String, List<String>> factionCardIds) {
        return new NarrativeNode(
                index,
                title,
                hint,
                "安史之乱预设材料",
                openingNarration,
                resultNarration,
                factionCardIds
        );
    }

    private List<String> list(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private Map<String, List<String>> mapOf(String key1, List<String> value1,
                                            String key2, List<String> value2,
                                            String key3, List<String> value3,
                                            String key4, List<String> value4) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return map;
    }
}
