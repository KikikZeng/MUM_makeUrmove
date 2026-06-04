# MUM-makeUrmove – 叙事对战·话语权博弈

vivoAIGC大模型比赛

## Structure
- docs: 文档（UML、报告等）
- src: 项目代码
- assets: UI资源


## 🧠 项目简介

MUM（makeUrmove）是一款以大语言模型为核心的AI原生叙事对战游戏。用户输入任意历史文献、文学选段或新闻文本，AI自动将文段拆解为多个阵营/角色，并将文中的关键事件、人物言论、因果逻辑抽取为“手牌”。用户扮演其中一方，通过类似锄大地的出牌机制（单张、对子、顺子等）与其他AI阵营进行叙事话语权争夺。

本项目基于小组原有的锄大地Card Game项目改造，保留核心出牌框架，新增：
- 文本解析与角色/卡牌生成（LLM映射）
- 多阵营AI Agent对战
- AI裁判实时裁决胜负

## 🎮 核心玩法

1. **上传文本**（支持纯文本或文件，≤5000字）
2. **AI解析** – 识别阵营（默认3-4个）、生成卡牌（事件/论据/人物行为）
3. **选择阵营** – 用户扮演一方，其余由AI控制
4. **出牌对战** – 每轮出牌需符合叙事逻辑（单张=孤立论据，顺子=因果链）
5. **AI裁判** – 根据相关性、逻辑强度、压制效果判定胜负
6. **胜利条件** – 率先出完手牌，即构建出未被压制的完整叙事

## 🛠️ 技术栈

- 前端：待定（可复用原锄大地HTML/CSS/JS框架）
- 后端：Node.js / Python（推荐）
- AI：vivo 蓝心大模型 API / 其他LLM（文本解析、AI决策、裁判）
- 版本控制：Git + GitHub

## 📦 项目状态

- [x] 锄大地基础框架迁移完成
- [ ] 接入LLM实现文本→卡牌映射
- [ ] 实现多AI Agent出牌决策
- [ ] 开发AI裁判模块
- [ ] 前端交互优化

## 🚀 快速开始

```bash
# 克隆项目
git clone https://github.com/KikikZeng/MUM_makeUrmove.git
cd MUM_makeUrmove

# 安装依赖（待补充）
npm install  # 或 pip install -r requirements.txt

# 运行开发服务器
npm run dev  # 或 python app.py
