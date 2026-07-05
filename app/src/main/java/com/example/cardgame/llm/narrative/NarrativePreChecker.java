package com.example.cardgame.llm.narrative;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NarrativePreChecker {
    private static final String[] EVENT_KEYWORDS = {
            "年", "时期", "朝代", "政权", "战争", "改革", "起义", "灭亡", "建立",
            "确立", "推行", "废除", "爆发", "签订", "设立", "制度", "政策",
            "联盟", "入侵", "投降", "称帝", "开国", "变法", "运动", "革命",
            "会战", "战役", "攻占", "撤退", "联合", "镇压", "退位", "掌权"
    };

    private static final Pattern FACTION_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5]{2,8}(?:王朝|帝国|王国|政府|政权|军|派|党|盟|部|族|集团|势力|力量|阵营|阶层|阶级|组织|机构|团体))"
    );
    private static final Pattern LOOSE_FACTION_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5]{1,4}(?:人|军|部|族|系|派))"
    );

    public CheckResult check(String text) {
        String value = text != null ? text.trim() : "";
        if (value.length() < 20) {
            return CheckResult.failed("文本过短（少于20个字符）");
        }
        if (!containsEventKeyword(value)) {
            return CheckResult.failed("文本未包含明显的历史事件关键词");
        }
        if (countActorLikeNames(value) < 2) {
            return CheckResult.failed("文本中未检测到至少两个不同阵营或行动主体");
        }
        return CheckResult.passed();
    }

    private boolean containsEventKeyword(String text) {
        for (String keyword : EVENT_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int countActorLikeNames(String text) {
        java.util.Set<String> names = new java.util.HashSet<>();
        addMatches(names, FACTION_PATTERN.matcher(text));
        if (names.size() < 2) {
            addMatches(names, LOOSE_FACTION_PATTERN.matcher(text));
        }
        return names.size();
    }

    private void addMatches(java.util.Set<String> names, Matcher matcher) {
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
    }

    public static class CheckResult {
        private final boolean passed;
        private final String reason;

        private CheckResult(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }

        public static CheckResult passed() {
            return new CheckResult(true, "通过审查");
        }

        public static CheckResult failed(String reason) {
            return new CheckResult(false, reason);
        }

        public boolean isPassed() {
            return passed;
        }

        public String getReason() {
            return reason;
        }
    }
}
