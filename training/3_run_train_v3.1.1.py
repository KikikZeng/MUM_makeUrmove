import json
import csv
import os
import re
import time
import requests
import uuid
import argparse
from datetime import datetime

# ==================== 配置区 ====================
API_URL = "https://api-ai.vivo.com.cn/v1/chat/completions"
API_KEY = "sk-xuanji-2026006675-cnl6U3V3QlVsTVBCZUlBeA=="
MODEL_NAME = "Volc-DeepSeek-V3.2"

TRAIN_SET_FILE = "data/train_set.json"
PROMPT_FILE = "prompts/current_prompt.txt"
PROGRESS_FILE = "logs/progress_train.csv"
PROGRESS_FILE_LIMIT = "logs/progress_train_limit.csv"
LOG_FILE = "logs/experiment_log.csv"
DIAGNOSTIC_DIR = "logs"

SLEEP_INTERVAL = 0.5
MAX_RETRIES = 3          # 大模型调用重试次数（网络层面）
RETRY_ON_FAIL = True     # 是否启用内容重试
RETRY_TEMPERATURE = 0.0  # 重试时的温度

# 可重试的错误类型（内容问题，非格式问题）
RETRYABLE_ERROR_TYPES = [
    "阵营.*没有分配任何卡牌",
    "title.*重复",
    "card缺少字段",
    "node.*缺少阵营",
    "nodeIndex不连续",
    "totalNodes与nodes数量不一致"
]

# ==================== 前置审查 ====================
def pre_check(text):
    """
    审查文本是否具备基本的解析条件。
    返回 (pass, reason)  - pass: bool, reason: str
    """
    # 规则1：文本长度至少20个字符
    if len(text.strip()) < 20:
        return False, "文本过短（少于20个字符）"

    # 规则2：包含至少一个历史/事件关键词（避免纯抒情或描述）
    keywords = [
        "年", "时期", "朝代", "政权", "战争", "改革", "起义", "灭亡", "建立",
        "确立", "推行", "废除", "爆发", "签订", "设立", "制度", "政策",
        "联盟", "入侵", "投降", "称帝", "开国", "变法", "运动", "革命",
        "会战", "战役", "攻占", "撤退", "联合", "镇压", "退位", "掌权"
    ]
    if not any(kw in text for kw in keywords):
        return False, "文本未包含明显的历史事件关键词"

    # 简单检查是否有至少两个不同的主体（通过捕获常见称呼）
    # 这里用简单启发式：出现两个不同的人名、地名或政权名
    # 可以使用更复杂的NER，但为了轻量级，我们匹配“XX国”“XX军”“XX王朝”等模式
    pattern = r'([\u4e00-\u9fa5]{2,8}(?:王朝|帝国|王国|政府|政权|军|派|党|盟|部|族|集团|势力|力量|阵营|阶层|阶级|组织|机构|团体))'
    matches = re.findall(pattern, text)
    unique_matches = set(matches)
    if len(unique_matches) < 2:
        # 尝试匹配“XX人”“XX部”等更宽松
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

# ==================== 硬检查函数 ====================
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
def load_train_set():
    with open(TRAIN_SET_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise ValueError("训练集格式错误")
    return data

def load_prompt():
    if not os.path.exists(PROMPT_FILE):
        raise FileNotFoundError(f"未找到 Prompt 文件：{PROMPT_FILE}")
    with open(PROMPT_FILE, "r", encoding="utf-8") as f:
        return f.read().strip()

def load_progress(progress_file):
    if not os.path.exists(progress_file):
        return set()
    with open(progress_file, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader, None)
        return {row[0] for row in reader if row}

def save_progress(progress_file, index, raw_output, json_ok, time_ok, is_empty, error_type, retried=False, final_error_type=None):
    file_exists = os.path.exists(progress_file)
    with open(progress_file, "a", encoding="utf-8", newline='') as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow(["index", "raw_output", "json_ok", "time_ok", "is_empty", "error_type", "retried", "final_error_type"])
        writer.writerow([index, raw_output, json_ok, time_ok, is_empty, error_type, retried, final_error_type if final_error_type else error_type])

# ==================== 生成诊断报告 ====================
def generate_diagnostic_report(version, limit, total_processed, validation_pass_rate, time_error_count, empty_rate, error_details, prompt_preview, precheck_stats, retry_stats):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_path = os.path.join(DIAGNOSTIC_DIR, f"diagnosis_{timestamp}.txt")

    with open(report_path, "w", encoding="utf-8") as f:
        f.write("=" * 60 + "\n")
        f.write("            诊断报告\n")
        f.write("=" * 60 + "\n\n")
        f.write(f"生成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"版本号：{version}\n")
        f.write(f"运行限制：{'前 ' + str(limit) + ' 条' if limit else '全量'}\n")
        f.write(f"处理总数：{total_processed}\n\n")

        # 前置审查统计
        f.write("-" * 60 + "\n")
        f.write("【前置审查统计】\n")
        f.write(f"  总审查数：{precheck_stats['total']}\n")
        f.write(f"  通过数：{precheck_stats['passed']}\n")
        f.write(f"  不通过数：{precheck_stats['failed']}\n")
        if precheck_stats['failed'] > 0:
            f.write("  不通过原因分布：\n")
            for reason, count in precheck_stats['reasons'].items():
                f.write(f"    - {reason}：{count} 条\n")
        f.write("\n")

        # 重试统计
        f.write("-" * 60 + "\n")
        f.write("【重试统计】\n")
        f.write(f"  总解析数（通过审查后）：{retry_stats['total']}\n")
        f.write(f"  初始成功数：{retry_stats['initial_success']}\n")
        f.write(f"  触发重试数：{retry_stats['retried']}\n")
        f.write(f"  重试成功数：{retry_stats['retry_success']}\n")
        if retry_stats['retried'] > 0:
            f.write(f"  重试成功率：{retry_stats['retry_success']/retry_stats['retried']:.2%}\n")
        f.write("\n")

        f.write("-" * 60 + "\n")
        f.write("【总体指标】\n")
        f.write(f"整体验证通过率：{validation_pass_rate:.2%}  （目标 >95%）\n")
        f.write(f"时间顺序错误条数：{time_error_count}  （目标 0）\n")
        f.write(f"空输出率：{empty_rate:.2%}  （目标 <20%）\n\n")

        f.write("-" * 60 + "\n")
        f.write("【错误分类统计】\n")
        error_counts = {}
        for detail in error_details:
            err_type = detail["error_type"]
            error_counts[err_type] = error_counts.get(err_type, 0) + 1
        for err_type, count in sorted(error_counts.items(), key=lambda x: -x[1]):
            f.write(f"  {err_type}：{count} 条\n")

        f.write("\n" + "-" * 60 + "\n")
        f.write("【失败案例明细（最多显示10条）】\n\n")
        failed = [d for d in error_details if d["error_type"] != "通过" and d["error_type"] != "前置审查未通过"]
        for idx, detail in enumerate(failed[:10]):
            f.write(f"--- 案例 {idx+1} ---\n")
            f.write(f"错误类型：{detail['error_type']}\n")
            f.write(f"输入文本：{detail['text'][:150]}...\n")
            f.write(f"模型输出：{detail['raw_output'][:200]}...\n\n")
        if len(failed) > 10:
            f.write(f"... 还有 {len(failed) - 10} 条失败案例未显示。\n\n")

        f.write("-" * 60 + "\n")
        f.write("【当前使用的 Prompt】\n")
        f.write(prompt_preview + "\n\n")

        f.write("-" * 60 + "\n")
        f.write("【改进建议（根据错误类型）】\n")
        if "JSON解析失败" in error_counts:
            f.write("  - JSON解析失败率高 → 检查 max_tokens 是否足够，或 Prompt 是否要求输出过长的内容\n")
        if any("阵营" in k for k in error_counts.keys()):
            f.write("  - 阵营无卡牌 → 考虑优化 Prompt 中的阵营识别逻辑，或增加事件牌提取先于阵营确认的步骤\n")
        if any("重复" in k for k in error_counts.keys()):
            f.write("  - title重复 → 在 Prompt 中强调 title/summary/sourceHint 互不相同\n")
        if any("缺少字段" in k for k in error_counts.keys()):
            f.write("  - card缺少字段 → 检查 Prompt 中关于 card 字段的完整性约束\n")
        if any("node" in k for k in error_counts.keys()):
            f.write("  - node结构问题 → 确保 node 包含所有必要字段且引用正确\n")
        f.write("\n" + "=" * 60 + "\n")
        f.write("报告结束\n")

    print(f"\n📄 诊断报告已生成：{report_path}")
    return report_path

# ==================== 主程序 ====================
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="训练集批量解析脚本（带前置审查与重试）")
    parser.add_argument("--limit", type=int, default=0, help="只处理前 N 条数据（用于快速诊断），0 表示全量")
    args = parser.parse_args()
    limit = args.limit if args.limit > 0 else None

    print("=" * 50)
    print("训练集批量解析脚本（前置审查 + 重试机制）")
    print("=" * 50)

    try:
        all_texts = load_train_set()
    except FileNotFoundError:
        print(f"错误：未找到 {TRAIN_SET_FILE}，请先运行 2_split_data.py。")
        exit(1)

    total_available = len(all_texts)
    print(f"训练集总条数：{total_available}")
    texts = all_texts[:limit] if limit else all_texts
    total = len(texts)
    print(f"实际处理条数：{total}")

    try:
        system_prompt = load_prompt()
        print(f"当前 Prompt 文件：{PROMPT_FILE}")
        print("Prompt 预览（前100字符）：")
        print(system_prompt[:100] + "..." if len(system_prompt) > 100 else system_prompt)
        print("-" * 50)
    except FileNotFoundError as e:
        print(e)
        print("请先在 prompts/ 文件夹下创建 current_prompt.txt 文件。")
        exit(1)

    version = input("请输入本次实验的 Prompt 版本号（如 v1.0）：").strip()
    if not version:
        version = "unknown"
    print(f"版本号：{version}\n")

    os.makedirs("logs", exist_ok=True)
    progress_file = PROGRESS_FILE_LIMIT if limit else PROGRESS_FILE
    processed_indices = load_progress(progress_file)
    start_index = len(processed_indices)
    if start_index > 0:
        print(f"检测到已处理 {start_index} 条，将从第 {start_index+1} 条继续。")
    else:
        print("从头开始处理。")

    total_valid = 0
    total_invalid = 0
    total_time_ok = 0
    total_time_error = 0
    total_empty = 0
    error_details = []

    # 前置审查统计
    precheck_stats = {"total": 0, "passed": 0, "failed": 0, "reasons": {}}
    # 重试统计
    retry_stats = {"total": 0, "initial_success": 0, "retried": 0, "retry_success": 0}

    for i in range(start_index, total):
        text = texts[i]
        print(f"正在处理第 {i+1}/{total} 条...", end="")

        # ---------- 前置审查 ----------
        precheck_stats["total"] += 1
        passed, reason = pre_check(text)
        if not passed:
            precheck_stats["failed"] += 1
            precheck_stats["reasons"][reason] = precheck_stats["reasons"].get(reason, 0) + 1
            print(f" ⛔ 前置审查未通过：{reason}")
            # 记录为前置审查未通过
            save_progress(progress_file, i, "PRECHECK_FAIL", False, False, False, "前置审查未通过", retried=False, final_error_type="前置审查未通过")
            total_invalid += 1
            error_details.append({"index": i, "text": text[:100], "raw_output": "PRECHECK_FAIL", "error_type": "前置审查未通过"})
            continue
        precheck_stats["passed"] += 1

        # ---------- 首次调用 ----------
        raw_output = None
        for _ in range(MAX_RETRIES):  # 网络重试
            raw_output = call_lanxin(system_prompt, text, temperature=0.1)
            if raw_output is not None:
                break
            time.sleep(1)
        if raw_output is None:
            print(" ❌ 大模型调用失败，跳过")
            save_progress(progress_file, i, "ERROR", False, False, False, "大模型调用失败", retried=False, final_error_type="大模型调用失败")
            total_invalid += 1
            error_details.append({"index": i, "text": text[:100], "raw_output": "ERROR", "error_type": "大模型调用失败"})
            time.sleep(SLEEP_INTERVAL)
            continue

        json_ok, time_ok, is_empty, cards, error_type = parse_and_check(raw_output)
        retried = False
        final_error_type = error_type

        retry_stats["total"] += 1
        if error_type == "通过":
            retry_stats["initial_success"] += 1
        else:
            # 检查是否可重试（匹配错误类型）
            should_retry = False
            for pattern in RETRYABLE_ERROR_TYPES:
                if re.search(pattern, error_type):
                    should_retry = True
                    break
            if RETRY_ON_FAIL and should_retry:
                print(f" ⚠️ 失败（{error_type}），自动重试 (temperature=0.0)...", end="")
                retried = True
                retry_stats["retried"] += 1
                # 重试调用
                retry_raw = None
                for _ in range(MAX_RETRIES):
                    retry_raw = call_lanxin(system_prompt, text, temperature=RETRY_TEMPERATURE)
                    if retry_raw is not None:
                        break
                    time.sleep(1)
                if retry_raw is not None:
                    retry_json_ok, retry_time_ok, retry_is_empty, retry_cards, retry_error_type = parse_and_check(retry_raw)
                    if retry_error_type == "通过":
                        # 重试成功，使用新结果
                        json_ok, time_ok, is_empty, cards, error_type = retry_json_ok, retry_time_ok, retry_is_empty, retry_cards, retry_error_type
                        final_error_type = error_type
                        retry_stats["retry_success"] += 1
                        print(" ✅ 重试成功")
                    else:
                        # 重试失败，保留原错误
                        print(f" ❌ 重试失败（{retry_error_type}），保留原错误")
                        final_error_type = error_type  # 保留原始错误
                else:
                    print(" ❌ 重试调用失败")
            else:
                print(f" ❌ 失败（{error_type}），不重试")

        # 更新统计
        if error_type == "通过":
            total_valid += 1
        else:
            total_invalid += 1

        if time_ok:
            total_time_ok += 1
        else:
            # 只有通过验证的数据才计入时间错误
            if error_type == "通过" and not time_ok:
                total_time_error += 1

        if is_empty:
            total_empty += 1

        save_progress(progress_file, i, raw_output if not retried else raw_output, json_ok, time_ok, is_empty, error_type, retried=retried, final_error_type=final_error_type)
        error_details.append({"index": i, "text": text, "raw_output": raw_output if not retried else raw_output, "error_type": error_type})

        # 打印结果
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

    # 计算指标（仅针对通过审查并进入解析的样本）
    processed = total_valid + total_invalid
    if processed == 0:
        print("没有处理任何条目。")
        exit(0)

    validation_pass_rate = total_valid / processed if processed > 0 else 0
    empty_rate = total_empty / processed if processed > 0 else 0

    print("\n" + "=" * 50)
    print("本次运行统计：")
    print(f"处理条数：{processed}")
    print(f"整体验证通过率：{validation_pass_rate:.2%} （目标 >95%）")
    print(f"时间顺序错误条数：{total_time_error} （目标 0）")
    print(f"空输出率：{empty_rate:.2%} （目标 <20%）")
    print(f"前置审查不通过数：{precheck_stats['failed']}")
    print(f"重试次数：{retry_stats['retried']}，重试成功：{retry_stats['retry_success']}")

    generate_diagnostic_report(
        version=version,
        limit=limit,
        total_processed=processed,
        validation_pass_rate=validation_pass_rate,
        time_error_count=total_time_error,
        empty_rate=empty_rate,
        error_details=error_details,
        prompt_preview=system_prompt[:500] + ("..." if len(system_prompt) > 500 else ""),
        precheck_stats=precheck_stats,
        retry_stats=retry_stats
    )

    log_exists = os.path.exists(LOG_FILE)
    with open(LOG_FILE, "a", encoding="utf-8", newline='') as f:
        writer = csv.writer(f)
        if not log_exists:
            writer.writerow(["version", "total_processed", "limit", "validation_pass_rate", "time_error_count", "empty_rate", "precheck_failed", "retry_count", "retry_success"])
        writer.writerow([version, processed, limit if limit else "full", f"{validation_pass_rate:.4f}", total_time_error, f"{empty_rate:.4f}", precheck_stats['failed'], retry_stats['retried'], retry_stats['retry_success']])

    print(f"\n实验结果已追加至 {LOG_FILE}")
    print("\n👉 请查看生成的诊断报告，根据错误分类修改 Prompt，然后重新运行。")