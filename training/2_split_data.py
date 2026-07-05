import json
import random
import os

# ==================== 配置区 ====================
# 输入文件（优先使用 raw_data.json，如果不存在则尝试 raw_data_raw.txt）
INPUT_JSON = "data/raw_data.json"
INPUT_TXT = "data/raw_data_raw.txt"

# 输出文件
TRAIN_SET = "data/train_set.json"           # 160条，用于日常调优
TEST_SET = "data/test_set_locked.json"      # 40条，锁死，最终验收前不可查看

# 拆分比例
TRAIN_RATIO = 0.8   # 80% 训练，20% 测试

# 随机种子（固定后每次拆分结果一致，保证可复现）
RANDOM_SEED = 42

# ==================== 读取数据 ====================
def load_data():
    """优先读取 JSON 文件，若不存在则读取 TXT 文件"""
    if os.path.exists(INPUT_JSON):
        with open(INPUT_JSON, "r", encoding="utf-8") as f:
            data = json.load(f)
        if isinstance(data, list):
            return data
        else:
            raise ValueError("raw_data.json 格式错误：根节点不是数组")
    elif os.path.exists(INPUT_TXT):
        with open(INPUT_TXT, "r", encoding="utf-8") as f:
            lines = [line.strip() for line in f if line.strip()]
        return lines
    else:
        raise FileNotFoundError(f"未找到数据文件，请检查 {INPUT_JSON} 或 {INPUT_TXT} 是否存在")

# ==================== 主程序 ====================
if __name__ == "__main__":
    print("=" * 50)
    print("开始拆分数据集...")

    # 1. 加载数据
    data = load_data()
    total = len(data)
    print(f"总数据量：{total} 条")

    if total == 0:
        print("错误：数据为空，请先运行 1_generate_data.py 生成数据。")
        exit(1)

    # 2. 随机打乱（固定种子保证可复现）
    random.seed(RANDOM_SEED)
    shuffled = data.copy()
    random.shuffle(shuffled)

    # 3. 按比例拆分
    split_idx = int(total * TRAIN_RATIO)
    train_set = shuffled[:split_idx]
    test_set = shuffled[split_idx:]

    print(f"训练集：{len(train_set)} 条")
    print(f"测试集：{len(test_set)} 条")

    # 4. 保存文件
    os.makedirs("data", exist_ok=True)

    with open(TRAIN_SET, "w", encoding="utf-8") as f:
        json.dump(train_set, f, ensure_ascii=False, indent=2)

    with open(TEST_SET, "w", encoding="utf-8") as f:
        json.dump(test_set, f, ensure_ascii=False, indent=2)

    print(f"\n✅ 拆分完成！")
    print(f"训练集：{TRAIN_SET}")
    print(f"测试集：{TEST_SET}")

    print("\n" + "=" * 50)
    print("⚠️  重要提醒（必读）")
    print("-" * 50)
    print(f"测试集已锁死，共 {len(test_set)} 条。")
    print("在最终验收日之前，绝对不要打开、查看、或使用 test_set_locked.json！")
    print("如果你提前查看了测试集并据此调整 Prompt，你的最终评估成绩将无效。")
    print("=" * 50)