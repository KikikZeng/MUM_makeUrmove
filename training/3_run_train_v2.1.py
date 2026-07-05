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
PROGRESS_FILE_LIMIT = "logs/progress_train_limit.csv"  # 带 --limit 时用
LOG_FILE = "logs/experiment_log.csv"
DIAGNOSTIC_DIR = "logs"

SLEEP_INTERVAL = 0.5
MAX_RETRIES = 3

# ==================== 调用大模型 ====================
def call_lanxin(system_prompt, user_text):
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
        "temperature": 0.1,
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
    - json_ok: JSON是否成功解析（True/False），仅代表格式层
    - time_ok: 时间顺序是否正确（仅当整体验证通过时有意义）
    - is_empty: 是否为空输出
    - data: 解析后的数据
    - error_type: 错误类型描述
    """
    cleaned = raw_output.strip()
    cleaned = re.sub(r'^```json\s*', '', cleaned)
    cleaned = re.sub(r'^```\s*', '', cleaned)
    cleaned = re.sub(r'\s*```$', '', cleaned)

    # 1. JSON 解析
    try:
        data = json.loads(cleaned)
    except json.JSONDecodeError:
        return False, False, False, None, "JSON解析失败"

    # 2. 根结构检查
    required_root_fields = ["factions", "cards", "nodes", "totalNodes", "fallbackUsed"]
    for field in required_root_fields:
        if field not in data:
            return True, False, False, data, f"缺少根字段: {field}"

    # 3. 空输出检查
    if len(data["factions"]) == 0 and len(data["cards"]) == 0 and len(data["nodes"]) == 0:
        return True, True, True, data, "空输出"

    # 4. factions 检查
    if not isinstance(data["factions"], list) or len(data["factions"]) == 0:
        return True, False, False, data, "factions为空或不是数组"

    faction_ids = set()
    for f in data["factions"]:
        if "id" not in f or "name" not in f:
            return True, False, False, data, "faction缺少id或name"
        faction_ids.add(f["id"])

    if len(faction_ids) < 2 or len(faction_ids) > 4:
        return True, False, False, data, f"阵营数量为{len(faction_ids)}，应在2-4之间"

    # 5. cards 检查
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

    # 检查每个阵营是否至少有1张卡牌
    for fid in faction_ids:
        has_card = any(c["factionId"] == fid for c in data["cards"])
        if not has_card:
            return True, False, False, data, f"阵营{fid}没有分配任何卡牌"

    # 6. nodes 检查
    if not isinstance(data["nodes"], list) or len(data["nodes"]) == 0:
        return True, False, False, data, "nodes为空或不是数组"

    for n in data["nodes"]:
        if "nodeIndex" not in n or "stageTitle" not in n or "factionCardIds" not in n:
            return True, False, False, data, f"node缺少必要字段"
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

    # 检查 nodeIndex 连续性
    node_indices = [n["nodeIndex"] for n in data["nodes"]]
    if sorted(node_indices) != list(range(len(data["nodes"]))):
        return True, False, False, data, "nodeIndex不连续或未从0开始"

    if data["totalNodes"] != len(data["nodes"]):
        return True, False, False, data, f"totalNodes与nodes数量不一致"

    if not isinstance(data["fallbackUsed"], bool):
        return True, False, False, data, "fallbackUsed不是布尔值"

    # 所有检查通过
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


def save_progress(progress_file, index, raw_output, json_ok, time_ok, is_empty, error_type):
    file_exists = os.path.exists(progress_file)
    with open(progress_file, "a", encoding="utf-8", newline='') as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow(["index", "raw_output", "json_ok", "time_ok", "is_empty", "error_type"])
        writer.writerow([index, raw_output, json_ok, time_ok, is_empty, error_type])


# ==================== 生成诊断报告 ====================
def generate_diagnostic_report(version, limit, total_processed, validation_pass_rate, time_error_count, empty_rate, error_details, prompt_preview):
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
        failed = [d for d in error_details if d["error_type"] != "通过"]
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
            f.write("  - JSON解析失败率高 → 在 Prompt 中强调「只输出JSON数组，不要附加任何解释或Markdown」\n")
        if "阵营" in "".join(error_counts.keys()) and "没有分配任何卡牌" in "".join(error_counts.keys()):
            f.write("  - 阵营无卡牌 → 检查 Prompt 规则3，确保每个阵营至少有1张卡牌\n")
        if "title" in "".join(error_counts.keys()) and "重复" in "".join(error_counts.keys()):
            f.write("  - title与summary/sourceHint重复 → 强调「title必须与summary、sourceHint不同」\n")
        if "card缺少字段" in "".join(error_counts.keys()):
            f.write("  - card缺少字段 → 检查 Schema 中 card 是否包含 id, factionId, title, summary, sourceHint\n")
        if "node" in "".join(error_counts.keys()):
            f.write("  - node结构不完整 → 检查 node 是否包含 nodeIndex, stageTitle, factionCardIds\n")
        f.write("\n" + "=" * 60 + "\n")
        f.write("报告结束\n")

    print(f"\n📄 诊断报告已生成：{report_path}")
    return report_path


# ==================== 主程序 ====================
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="训练集批量解析脚本（带快速诊断模式）")
    parser.add_argument("--limit", type=int, default=0, help="只处理前 N 条数据（用于快速诊断），0 表示全量")
    args = parser.parse_args()
    limit = args.limit if args.limit > 0 else None

    print("=" * 50)
    print("训练集批量解析脚本（快速诊断模式）")
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

    for i in range(start_index, total):
        text = texts[i]
        print(f"正在处理第 {i+1}/{total} 条...", end="")

        raw_output = None
        for _ in range(MAX_RETRIES):
            raw_output = call_lanxin(system_prompt, text)
            if raw_output is not None:
                break
            time.sleep(1)
        if raw_output is None:
            print(" ❌ 大模型调用失败，跳过")
            save_progress(progress_file, i, "ERROR", False, False, False, "大模型调用失败")
            total_invalid += 1
            error_details.append({"index": i, "text": text[:100], "raw_output": "ERROR", "error_type": "大模型调用失败"})
            time.sleep(SLEEP_INTERVAL)
            continue

        json_ok, time_ok, is_empty, cards, error_type = parse_and_check(raw_output)

        # 统计：整体验证是否通过（以 error_type == "通过" 为准）
        if error_type == "通过":
            total_valid += 1
        else:
            total_invalid += 1

        # 统计：时间顺序是否正确（只统计整体验证通过的数据）
        if error_type == "通过" and time_ok:
            total_time_ok += 1
        elif error_type == "通过" and not time_ok:
            total_time_error += 1
        # 如果 error_type != "通过"，不纳入时间统计（无法检查顺序）

        if is_empty:
            total_empty += 1

        save_progress(progress_file, i, raw_output, json_ok, time_ok, is_empty, error_type)
        error_details.append({"index": i, "text": text, "raw_output": raw_output, "error_type": error_type if error_type else "未知"})

        # 打印结果
        if error_type == "通过":
            status_icon = "✅"
            status_text = "通过"
        elif json_ok:
            status_icon = "⚠️"
            status_text = "验证失败"
        else:
            status_icon = "❌"
            status_text = "JSON解析失败"

        time_status = "顺序正确" if (error_type == "通过" and time_ok) else ("顺序错误" if (error_type == "通过" and not time_ok) else "无法检查")
        empty_status = "空输出" if is_empty else "有卡牌"
        print(f" {status_icon} {status_text} | JSON:{json_ok} | {time_status} | {empty_status} | {error_type}")
        time.sleep(SLEEP_INTERVAL)

    processed = total_valid + total_invalid
    if processed == 0:
        print("没有处理任何条目。")
        exit(0)

    validation_pass_rate = total_valid / processed
    empty_rate = total_empty / processed

    print("\n" + "=" * 50)
    print("本次运行统计：")
    print(f"处理条数：{processed}")
    print(f"整体验证通过率：{validation_pass_rate:.2%} （目标 >95%）")
    print(f"时间顺序错误条数：{total_time_error} （目标 0）")
    print(f"空输出率：{empty_rate:.2%} （目标 <20%）")

    generate_diagnostic_report(
        version=version,
        limit=limit,
        total_processed=processed,
        validation_pass_rate=validation_pass_rate,
        time_error_count=total_time_error,
        empty_rate=empty_rate,
        error_details=error_details,
        prompt_preview=system_prompt[:500] + ("..." if len(system_prompt) > 500 else "")
    )

    log_exists = os.path.exists(LOG_FILE)
    with open(LOG_FILE, "a", encoding="utf-8", newline='') as f:
        writer = csv.writer(f)
        if not log_exists:
            writer.writerow(["version", "total_processed", "limit", "validation_pass_rate", "time_error_count", "empty_rate"])
        writer.writerow([version, processed, limit if limit else "full", f"{validation_pass_rate:.4f}", total_time_error, f"{empty_rate:.4f}"])

    print(f"\n实验结果已追加至 {LOG_FILE}")
    print("\n👉 请查看生成的诊断报告，根据错误分类修改 Prompt，然后重新运行。")