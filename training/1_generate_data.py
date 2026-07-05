import requests
import json
import uuid
import time
import random
import os
import re

# ==================== 配置区 ====================
API_URL = "https://api-ai.vivo.com.cn/v1/chat/completions"
API_KEY = "sk-xuanji-2026006675-cnl6U3V3QlVsTVBCZUlBeA=="
MODEL_NAME = "Volc-DeepSeek-V3.2"

TOTAL_COUNT = 200          # 目标生成总数（首次建议设为10测试）
SLEEP_INTERVAL = 0.5       # 请求间隔（秒），防止限流
MAX_RETRIES = 3            # 单条失败重试次数

# ==================== 维度列表 ====================
COUNTRIES = [
    "中国",
    "欧洲（含英法德意俄）",
    "中亚/西亚（含阿拉伯、蒙古、奥斯曼）",
    "东亚（日本/朝鲜）",
    "南亚（印度/巴基斯坦）",
    "美洲（含美国/拉美）"
]

TIMES = [
    "上古时期（公元前3000年-公元前200年）",
    "古典时期（公元前200年-公元500年）",
    "中古时期（公元500年-公元1500年）",
    "近代早期（公元1500年-公元1840年）",
    "近代（1840年-1949年）",
    "现代（1949年至今）"
]

FIELDS = ["军事冲突", "政治改革", "制度变革", "外交事件", "社会运动", "经济改革"]

# ==================== 核心调用函数 ====================
def call_lanxin(system_prompt, user_text):
    """调用蓝心大模型，返回生成的文本内容"""
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
        "temperature": 0.7,
        "reasoning_effort": "minimal",
        "max_tokens": 2048
    }
    try:
        resp = requests.post(API_URL, headers=headers, json=payload, timeout=30)
        if resp.status_code == 200:
            result = resp.json()
            return result["choices"][0]["message"]["content"]
        else:
            print(f"接口报错：{resp.status_code}，{resp.text}")
            return None
    except Exception as e:
        print(f"网络异常：{e}")
        return None

# ==================== 生成单条历史课文 ====================
def generate_one_passage():
    """随机组合维度，生成一条符合要求的文本"""
    country = random.choice(COUNTRIES)
    time_period = random.choice(TIMES)
    field = random.choice(FIELDS)

    system_prompt = (
        "你是一位历史教科书编写专家。请用平实清晰、客观中性的语言，描述一段真实历史。\n"
        "你的描述必须严格满足以下结构要求：\n"
        "1. 涉及至少两个明确的阵营/群体（如国家、政治派别、阶级、宗教团体等），它们之间存在直接的竞争、对抗或利益冲突。\n"
        "2. 按时间顺序叙述至少3个关键事件节点（例如：起因/导火索 → 发展/转折 → 结果/影响），每个节点都必须体现双方的冲突或博弈。\n"
        "3. 事件必须基于真实历史，不得虚构。\n"
        "4. 语言风格符合中国历史课文标准，流畅连贯，包含具体年份（公元或公元前）。\n"
        "请直接输出一段完整的叙述文字，不要分点或标注序号。"
    )

    user_text = (
        f"请从历史中选取符合上述条件的真实事件，要求：\n"
        f"- 发生地区或涉及的主要政权：{country}\n"
        f"- 所属时代：{time_period}\n"
        f"- 主要领域：{field}\n"
        f"请确保叙述中至少有2个对立阵营，并包含至少3个事件节点，字数200-300字。"
    )

    for attempt in range(MAX_RETRIES):
        raw = call_lanxin(system_prompt, user_text)
        if raw is None:
            continue
        text = raw.strip()
        if len(text) < 50:
            print(f"  文本过短，重试...")
            continue

        # 年份检查：支持“公元”、“公元前”或四位数字年份（如1979年）
        year_pattern = r'(\d{4}\s*年|公元前\s*\d+\s*年|公元\s*\d+\s*年)'
        if not re.search(year_pattern, text):
            print(f"  未检测到明确年份，重试...")
            continue

        # 冲突词汇检查（确保不是纯流水账）
        conflict_keywords = ["阵营", "对抗", "战争", "改革", "条约", "联盟", "起义", "冲突", "势力", "较量", "抵制", "镇压"]
        if any(kw in text for kw in conflict_keywords):
            return text
        else:
            print(f"  可能缺乏冲突叙事，重试...")
            continue

    return None

# ==================== 获取已生成条数（断点续传） ====================
def count_existing_lines(filepath):
    if not os.path.exists(filepath):
        return 0
    with open(filepath, "r", encoding="utf-8") as f:
        return sum(1 for _ in f)

# ==================== 主程序 ====================
if __name__ == "__main__":
    os.makedirs("data", exist_ok=True)
    temp_file = "data/raw_data_raw.txt"      # 中间临时文件（每行一条）
    final_json = "data/raw_data.json"        # 最终JSON文件

    # 检查已有进度
    already_done = count_existing_lines(temp_file)
    print(f"检测到已生成 {already_done} 条数据（来自上次中断的保存）")

    if already_done >= TOTAL_COUNT:
        print("已完成目标数量，无需重新生成。")
        if not os.path.exists(final_json):
            with open(temp_file, "r", encoding="utf-8") as f:
                lines = [line.strip() for line in f if line.strip()]
            with open(final_json, "w", encoding="utf-8") as f:
                json.dump(lines, f, ensure_ascii=False, indent=2)
            print("已从临时文件重建 JSON 文件。")
        exit(0)

    need_to_generate = TOTAL_COUNT - already_done
    print(f"本次需新生成 {need_to_generate} 条...\n")

    # 加载已有数据到内存（用于最终JSON）
    passages = []
    if os.path.exists(temp_file):
        with open(temp_file, "r", encoding="utf-8") as f:
            passages = [line.strip() for line in f if line.strip()]

    fail_count = 0
    for i in range(already_done + 1, TOTAL_COUNT + 1):
        print(f"正在生成第 {i}/{TOTAL_COUNT} 条...", end="")
        p = generate_one_passage()
        if p:
            # 立即追加到临时文件
            with open(temp_file, "a", encoding="utf-8") as f:
                f.write(p + "\n")
            passages.append(p)
            print(" ✅ 成功 (已保存)")
        else:
            fail_count += 1
            print(" ❌ 失败（跳过）")
        time.sleep(SLEEP_INTERVAL)

    # 全部生成完成后，生成最终的JSON文件
    with open(final_json, "w", encoding="utf-8") as f:
        json.dump(passages, f, ensure_ascii=False, indent=2)

    print("\n" + "=" * 50)
    print(f"生成完成！成功：{len(passages)}条，失败：{fail_count}条")
    print(f"中间备份文件：{temp_file}")
    print(f"最终JSON文件：{final_json}")
    print("\n预览前5条样本：")
    print("-" * 50)
    for idx, p in enumerate(passages[:5]):
        print(f"样本{idx+1}：{p[:120]}...")
    print("\n下一步：运行 2_split_data.py 拆分为训练集和测试集")