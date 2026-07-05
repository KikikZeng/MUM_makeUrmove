import json
import csv
import os
import re
import time
import requests
import uuid
from datetime import datetime

# ==================== 配置区 ====================
API_URL = "https://api-ai.vivo.com.cn/v1/chat/completions"
API_KEY = "sk-xuanji-2026006675-cnl6U3V3QlVsTVBCZUlBeA=="
MODEL_NAME = "Volc-DeepSeek-V3.2"

TEST_SET_FILE = "data/test_set_locked.json"
PROMPT_FILE = "prompts/current_prompt.txt"

REPORT_FILE = "logs/final_report.txt"
DETAIL_FILE = "logs/final_details.csv"

SLEEP_INTERVAL = 0.5
MAX_RETRIES = 3          # 网络调用重试次数

# 可重试的错误类型（与3号保持一致）
RETRYABLE_ERROR_TYPES = [
    "阵营.*没有分配任何卡牌",
    "title.*重复",
    "card缺少字段",
    "node.*缺少阵营",
    "nodeIndex不连续",
    "totalNodes与nodes数量不一致"
]


# ==================== 前置审查（与3号完全一致） ====================
def pre_check(text):
    """
    审查文本是否具备基本的解析条件。
    返回 (pass, reason)  - pass: bool, reason: str
    """
    if len(text.strip()) < 20:
        return False, "文本过短（少于20个字符）"

    keywords = [
        "年", "时期", "朝代", "政权", "战争", "改革", "起义", "灭亡", "建立",
        "确立", "推行", "废除", "爆发", "签订", "设立", "制度", "政策",
        "联盟", "入侵", "投降", "称帝", "开国", "变法", "运动", "革命",
        "会战", "战役", "攻占", "撤退", "联合", "镇压", "退位", "掌权"
    ]
    if not any(kw in text for kw in keywords):
        return False, "文本未包含明显的历史事件关键词"

    pattern = r'([\u4e00-\u9fa5]{2,8}(?:王朝|帝国|王国|政府|政权|军|派|党|盟|部|族|集团|势力|力量|阵营|阶层|阶级|组织|机构|团体))'
    matches = re.findall(pattern, text)
    unique_matches = set(matches)
    if len(unique_matches) < 2:
        pattern2 = r'([\u4e00-\u9fa5]{1,4}(?:人|部|族|系|派))'
        matches2 = re.findall(pattern2, text)
        all_matches = set(matches) | set(matches2)
        if len(all_matches) < 2:
            return False, "文本中未检测到至少两个不同阵营或行动主体"

    return True, "通过审查"


# ==================== 调用大模型 ====================
def call_lanxin(system_prompt, user_text, temperature=0.1):
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }
    payload = {
        "requestId": uuid.uuid4().hex,
        "model": MODEL_NAME,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_text}
        ],
        "stream": False,
        "temperature": temperature,
        "reasoning_effort": "minimal",
        "max_tokens": 2048
    }
    try:
        resp = requests.post(API_URL, headers=headers, json=payload, timeout=60)
        if resp.status_code == 200:
            result = resp.json()
            return result["choices"][0]["message"]["content"]
        else:
            print(f"  接口报错：{resp.status_code}")
            return None
    except Exception as e:
        print(f"  网络异常：{e}")
        return None


# ==================== 硬检查函数（与3号完全一致） ====================
def parse_and_check(raw_output):
    """
    返回： (json_ok, time_ok, is_empty, data, error_type)
    """
    cleaned = raw_output.strip()
    cleaned = re.sub(r'^```json\s*', '', cleaned)
    cleaned = re.sub(r'^```\s*', '', cleaned)
    cleaned = re.sub(r'\s*```$', '', cleaned)

    try:
        data = json.loads(cleaned)
    except json.JSONDecodeError:
        return False, False, False, None, "JSON解析失败"

    required_root_fields = ["factions", "cards", "nodes", "totalNodes", "fallbackUsed"]
    for field in required_root_fields:
        if field not in data:
            return True, False, False, data, f"缺少根字段: {field}"

    if len(data["factions"]) == 0 and len(data["cards"]) == 0 and len(data["nodes"]) == 0:
        return True, True, True, data, "空输出"

    if not isinstance(data["factions"], list) or len(data["factions"]) == 0:
        return True, False, False, data, "factions为空或不是数组"

    faction_ids = set()
    for f in data["factions"]:
        if "id" not in f or "name" not in f:
            return True, False, False, data, "faction缺少id或name"
        faction_ids.add(f["id"])

    if len(faction_ids) < 2 or len(faction_ids) > 4:
        return True, False, False, data, f"阵营数量为{len(faction_ids)}，应在2-4之间"

    if not isinstance(data["cards"], list):
        return True, False, False, data, "cards不是数组"

    card_ids = set()
    for c in data["cards"]:
        required_card_fields = ["id", "factionId", "title", "summary", "sourceHint"]
        for field in required_card_fields:
            if field not in c:
                return True, False, False, data, f"card缺少字段: {field}"
        if c["factionId"] not in faction_ids:
            return True, False, False, data, f"card引用了不存在的factionId: {c['factionId']}"
        if c["title"] == c["summary"] or c["title"] == c["sourceHint"]:
            return True, False, False, data, f"title与summary/sourceHint重复: {c['title']}"
        card_ids.add(c["id"])

    for fid in faction_ids:
        has_card = any(c["factionId"] == fid for c in data["cards"])
        if not has_card:
            return True, False, False, data, f"阵营{fid}没有分配任何卡牌"

    if not isinstance(data["nodes"], list) or len(data["nodes"]) == 0:
        return True, False, False, data, "nodes为空或不是数组"

    for n in data["nodes"]:
        if "nodeIndex" not in n or "stageTitle" not in n or "factionCardIds" not in n:
            return True, False, False, data, "node缺少必要字段"
        if not isinstance(n["factionCardIds"], dict):
            return True, False, False, data, "factionCardIds必须是对象"
        for fid in faction_ids:
            if fid not in n["factionCardIds"]:
                return True, False, False, data, f"node {n['nodeIndex']} 缺少阵营 {fid} 的cardIds"
            if not isinstance(n["factionCardIds"][fid], list):
                return True, False, False, data, f"node中{fid}的cardIds不是数组"
            for cid in n["factionCardIds"][fid]:
                if cid not in card_ids:
                    return True, False, False, data, f"node引用了不存在的cardId: {cid}"

    node_indices = [n["nodeIndex"] for n in data["nodes"]]
    if sorted(node_indices) != list(range(len(data["nodes"]))):
        return True, False, False, data, "nodeIndex不连续或未从0开始"

    if data["totalNodes"] != len(data["nodes"]):
        return True, False, False, data, f"totalNodes与nodes数量不一致"

    if not isinstance(data["fallbackUsed"], bool):
        return True, False, False, data, "fallbackUsed不是布尔值"

    return True, True, False, data, "通过"


# ==================== 加载数据 ====================
def load_test_set():
    if not os.path.exists(TEST_SET_FILE):
        raise FileNotFoundError(f"未找到测试集文件：{TEST_SET_FILE}")
    with open(TEST_SET_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise ValueError("测试集格式错误")
    return data


def load_prompt():
    if not os.path.exists(PROMPT_FILE):
        raise FileNotFoundError(f"未找到 Prompt 文件：{PROMPT_FILE}")
    with open(PROMPT_FILE, "r", encoding="utf-8") as f:
        return f.read().strip()


# ==================== 主程序 ====================
if __name__ == "__main__":
    print("=" * 60)
    print("            ⚠️  最终验收脚本（仅运行一次）")
    print("=" * 60)
    print("\n⚠️  警告：此脚本将使用当前 Prompt 对锁死的测试集进行最终评估。")
    print("   评估结果将作为最终验收依据。")
    print("   如果未达标，请回到训练集重新调优，然后重新验收。")
    print("   严禁根据本次结果直接修改 Prompt 重新运行！\n")
    print("=" * 60)

    confirm = input("确认要继续吗？(输入 yes 继续): ")
    if confirm.lower() != "yes":
        print("已取消。")
        exit(0)

    # 1. 加载测试集
    try:
        texts = load_test_set()
    except FileNotFoundError as e:
        print(f"错误：{e}")
        print("请先运行 2_split_data.py 生成测试集。")
        exit(1)

    total = len(texts)
    print(f"测试集总条数：{total}")

    # 2. 加载 Prompt
    try:
        system_prompt = load_prompt()
        print("当前 Prompt 预览（前100字符）：")
        print(system_prompt[:100] + "..." if len(system_prompt) > 100 else system_prompt)
        print("-" * 60)
    except FileNotFoundError as e:
        print(f"错误：{e}")
        exit(1)

    # 3. 记录开始时间
    start_time = datetime.now()
    print(f"\n开始时间：{start_time.strftime('%Y-%m-%d %H:%M:%S')}\n")

    # 4. 逐条处理
    results = []
    total_valid = 0
    total_invalid = 0
    total_time_ok = 0
    total_time_error = 0
    total_empty = 0
    total_precheck_fail = 0
    total_retry_triggered = 0
    total_retry_success = 0
    error_counts = {}

    for i, text in enumerate(texts):
        print(f"正在处理第 {i+1}/{total} 条...", end="")

        # ---------- 前置审查 ----------
        passed, reason = pre_check(text)
        if not passed:
            total_precheck_fail += 1
            error_type = "前置审查未通过"
            error_counts[error_type] = error_counts.get(error_type, 0) + 1
            total_invalid += 1
            results.append({
                "index": i,
                "text": text,
                "raw_output": "PRECHECK_FAIL",
                "json_ok": False,
                "time_ok": False,
                "is_empty": False,
                "error_type": error_type,
                "retried": False,
                "final_error_type": error_type
            })
            print(f" ⛔ {reason}")
            continue

        # ---------- 首次调用 ----------
        raw_output = None
        for attempt in range(MAX_RETRIES):
            raw_output = call_lanxin(system_prompt, text, temperature=0.1)
            if raw_output is not None:
                break
            time.sleep(1)

        if raw_output is None:
            error_type = "大模型调用失败"
            error_counts[error_type] = error_counts.get(error_type, 0) + 1
            total_invalid += 1
            results.append({
                "index": i,
                "text": text,
                "raw_output": "ERROR",
                "json_ok": False,
                "time_ok": False,
                "is_empty": False,
                "error_type": error_type,
                "retried": False,
                "final_error_type": error_type
            })
            print(" ❌ 大模型调用失败，跳过")
            time.sleep(SLEEP_INTERVAL)
            continue

        # ---------- 首次解析 ----------
        json_ok, time_ok, is_empty, cards, error_type = parse_and_check(raw_output)
        retried = False
        final_error_type = error_type

        # ---------- 自动重试逻辑（与3号完全一致） ----------
        if error_type != "通过":
            should_retry = False
            for pattern in RETRYABLE_ERROR_TYPES:
                if re.search(pattern, error_type):
                    should_retry = True
                    break
            if should_retry:
                print(f" ⚠️ 失败（{error_type}），自动重试 (temperature=0.0)...", end="")
                retried = True
                total_retry_triggered += 1
                retry_raw = None
                for attempt in range(MAX_RETRIES):
                    retry_raw = call_lanxin(system_prompt, text, temperature=0.0)
                    if retry_raw is not None:
                        break
                    time.sleep(1)
                if retry_raw is not None:
                    retry_json_ok, retry_time_ok, retry_is_empty, retry_cards, retry_error_type = parse_and_check(retry_raw)
                    if retry_error_type == "通过":
                        json_ok, time_ok, is_empty, cards, error_type = retry_json_ok, retry_time_ok, retry_is_empty, retry_cards, retry_error_type
                        final_error_type = error_type
                        total_retry_success += 1
                        print(" ✅ 重试成功")
                    else:
                        print(f" ❌ 重试失败（{retry_error_type}），保留原错误")
                        final_error_type = error_type
                else:
                    print(" ❌ 重试调用失败")
                    final_error_type = error_type

        # ---------- 更新统计 ----------
        if error_type == "通过":
            total_valid += 1
        else:
            total_invalid += 1

        if time_ok:
            total_time_ok += 1
        else:
            if error_type == "通过" and not time_ok:
                total_time_error += 1

        if is_empty:
            total_empty += 1

        error_counts[error_type] = error_counts.get(error_type, 0) + 1

        results.append({
            "index": i,
            "text": text,
            "raw_output": raw_output,
            "json_ok": json_ok,
            "time_ok": time_ok,
            "is_empty": is_empty,
            "error_type": error_type,
            "retried": retried,
            "final_error_type": final_error_type
        })

        # ---------- 打印状态 ----------
        if error_type == "通过":
            status_icon = "✅"
            status_text = "通过"
        elif error_type == "前置审查未通过":
            status_icon = "⛔"
            status_text = "前置审查未通过"
        elif json_ok:
            status_icon = "⚠️"
            status_text = "验证失败"
        else:
            status_icon = "❌"
            status_text = "JSON解析失败"

        time_status = "顺序正确" if (error_type == "通过" and time_ok) else ("顺序错误" if (error_type == "通过" and not time_ok) else "无法检查")
        empty_status = "空输出" if is_empty else "有卡牌"
        retry_mark = " 🔄重试" if retried else ""
        print(f" {status_icon} {status_text}{retry_mark} | JSON:{json_ok} | {time_status} | {empty_status} | {error_type}")
        time.sleep(SLEEP_INTERVAL)

    # 5. 计算指标
    processed = total_valid + total_invalid
    if processed == 0:
        print("没有处理任何条目。")
        exit(0)

    validation_pass_rate = total_valid / processed if processed > 0 else 0
    empty_rate = total_empty / processed if processed > 0 else 0

    passed = (
        validation_pass_rate >= 0.95 and
        total_time_error == 0 and
        empty_rate < 0.20
    )

    # 6. 生成最终报告
    end_time = datetime.now()
    duration = (end_time - start_time).total_seconds()

    os.makedirs("logs", exist_ok=True)

    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write("=" * 70 + "\n")
        f.write("                最终验收报告\n")
        f.write("=" * 70 + "\n\n")
        f.write(f"验收时间：{end_time.strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"耗时：{duration:.1f} 秒\n")
        f.write(f"测试集规模：{processed} 条\n")
        f.write(f"前置审查拦截：{total_precheck_fail} 条\n")
        f.write(f"触发重试：{total_retry_triggered} 条\n")
        f.write(f"重试成功：{total_retry_success} 条\n")
        if total_retry_triggered > 0:
            f.write(f"重试成功率：{total_retry_success/total_retry_triggered:.2%}\n")
        f.write("\n")

        f.write("-" * 70 + "\n")
        f.write("【核心指标】\n")
        f.write(f"整体验证通过率：{validation_pass_rate:.2%}  （目标：≥95%）\n")
        f.write(f"时间顺序错误条数：{total_time_error}  （目标：0）\n")
        f.write(f"空输出率：{empty_rate:.2%}  （目标：<20%）\n\n")

        f.write("-" * 70 + "\n")
        f.write("【验收结论】\n")
        if passed:
            f.write("🎉 验收通过！可以部署上线。\n")
        else:
            f.write("❌ 验收未通过。请回到训练集继续调优，然后重新验收。\n")
            f.write("\n原因分析：\n")
            if validation_pass_rate < 0.95:
                f.write(f"  - 整体验证通过率 {validation_pass_rate:.2%} 低于目标 95%\n")
            if total_time_error > 0:
                f.write(f"  - 存在 {total_time_error} 条时间顺序错误\n")
            if empty_rate >= 0.20:
                f.write(f"  - 空输出率 {empty_rate:.2%} 高于目标 20%\n")

        f.write("\n" + "-" * 70 + "\n")
        f.write("【错误分类统计】\n")
        for err_type, count in sorted(error_counts.items(), key=lambda x: -x[1]):
            f.write(f"  {err_type}：{count} 条\n")

        f.write("\n" + "-" * 70 + "\n")
        f.write("【失败案例明细（最多显示10条）】\n\n")
        failed = [r for r in results if r["error_type"] != "通过"]
        for idx, r in enumerate(failed[:10]):
            f.write(f"--- 案例 {idx+1} ---\n")
            f.write(f"错误类型：{r['error_type']}\n")
            if r.get("retried", False):
                f.write(f"（已重试，仍失败）\n")
            f.write(f"输入文本：{r['text'][:150]}...\n")
            f.write(f"模型输出：{r['raw_output'][:200]}...\n\n")
        if len(failed) > 10:
            f.write(f"... 还有 {len(failed) - 10} 条失败案例，请查看 {DETAIL_FILE}\n")

        f.write("=" * 70 + "\n")

    # 7. 保存详细数据
    with open(DETAIL_FILE, "w", encoding="utf-8", newline='') as f:
        writer = csv.writer(f)
        writer.writerow(["index", "json_ok", "time_ok", "is_empty", "error_type", "retried", "final_error_type", "text_preview", "raw_output_preview"])
        for r in results:
            writer.writerow([
                r["index"],
                r["json_ok"],
                r["time_ok"],
                r["is_empty"],
                r["error_type"],
                r.get("retried", False),
                r.get("final_error_type", r["error_type"]),
                r["text"][:100],
                r["raw_output"][:200]
            ])

    # 8. 控制台输出结论
    print("\n" + "=" * 60)
    print("           最终验收结果")
    print("=" * 60)
    print(f"处理条数：{processed}")
    print(f"整体验证通过率：{validation_pass_rate:.2%}  {'✅' if validation_pass_rate >= 0.95 else '❌'}")
    print(f"时间顺序错误条数：{total_time_error}  {'✅' if total_time_error == 0 else '❌'}")
    print(f"空输出率：{empty_rate:.2%}  {'✅' if empty_rate < 0.20 else '❌'}")

    print("\n" + "-" * 60)
    if passed:
        print("🎉 验收通过！可以部署上线。")
        print(f"   报告已保存：{REPORT_FILE}")
        print(f"   详情已保存：{DETAIL_FILE}")
    else:
        print("❌ 验收未通过。")
        print(f"   请查看报告：{REPORT_FILE}")
        print(f"   根据失败案例修改 Prompt，在训练集上重新调优后再次验收。")
        print("\n   严禁根据本次结果直接修改 Prompt 重新运行验收！")

    print("=" * 60)