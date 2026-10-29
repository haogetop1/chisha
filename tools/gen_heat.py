#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
给还没有火候时间轴的菜谱自动生成一条。

思路：先按关键词判断这道菜属于哪种「烹饪手法」（炒/蒸/炖/煮/炸/拌/烤/烙），
再套用该手法的火候模板，把时长按这道菜的 timeMin 折算到每一段，
并把主料名填进阶段名里，让每条轴看起来是这道菜自己的。

已经手工精修过的（自带 heat）一律跳过，不会被覆盖。
"""
import glob, json, os, re, sys, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "tools", "data")

# ---------- 手法识别 ----------
# 菜名里的关键词权重最高（「辣椒炒蛋」就该是炒），关键词太泛的（煎/爆/酥）
# 不放进识别集，否则会把大批炒菜误判成炸。
NAME_KEYS = [
    ("烤", ["烤", "烘", "焗", "焙"]),
    ("炸", ["炸", "油淋"]),
    ("水煎", ["生煎", "水煎", "锅贴"]),
    ("煎", ["煎"]),
    ("蒸", ["蒸"]),
    ("烙", ["烙", "饼", "馍", "粑", "锅贴"]),
    ("拌", ["拌", "拍黄瓜", "沙拉"]),
    ("炖", ["炖", "焖", "煨", "卤", "煲", "红烧"]),
    ("煮", ["汤", "羹", "粥", "汆", "涮", "白切"]),
    ("炒", ["炒", "煸", "溜", "烩"]),
]
# 菜名判断不出来时，退回到做法正文里找（按命中词数计分，同分时按这里的顺序）
# 顺序上把「炖/烧/焖」放在「煎」前面——糖醋排骨这类菜正文里既提到煎也提到收汁，
# 但它真正的后半程是烧焖，判成煎就丢了最关键的一段。
STEP_KEYS = [
    ("烤", ["烤箱", "预热", "上下火"]),
    ("蒸", ["上笼", "上锅蒸", "蒸锅", "大火蒸"]),
    ("炖", ["炖", "焖", "煨", "卤", "煲", "收汁", "小火慢", "红烧"]),
    ("煮", ["煮开", "烧开", "加水", "熬", "汤汁"]),
    ("炸", ["炸至", "油温", "复炸", "下油锅"]),
    ("煎", ["煎至", "两面金黄", "小火煎"]),
    ("烙", ["擀成", "擀开", "平底锅"]),
    ("拌", ["拌匀", "过凉", "冰水"]),
    ("炒", ["翻炒", "煸炒", "爆香", "下锅炒"]),
]
DEFAULT_METHOD = "炒"

# 不能当「主料」用的泛用项
NOT_MAIN = ("面粉", "水", "淀粉", "油", "盐", "糖", "清水", "温水", "糯米粉", "粘米粉", "酵母")

# ---------- 各手法的火候模板 ----------
# (阶段名, 火候, 时长占位, 看什么信号)
#   时长占位 = 小数   → 占「烹饪时长预算」的比例
#   时长占位 = 字符串 → 固定时长，不随菜谱缩放
TEMPLATES = {
    "炒": (0.35, [
        ("热锅润油", "大火", 0.10, "锅烧到微微冒烟再下油，油面浮起细纹就够了"),
        ("爆香小料", "中火", 0.18, "姜蒜或干辣椒下锅，闻到香味、颜色微黄就好，别炸糊"),
        ("下{main}煸炒", "大火", 0.40, "表面变色、边缘微焦，水汽开始收干"),
        ("合炒调味", "中火", 0.22, "沿锅边淋入调料，翻匀让每块都裹上味"),
        ("收汁出锅", "大火", 0.10, "汤汁收紧挂住食材就关火，别炒到发干"),
    ]),
    "蒸": (0.70, [
        ("烧水上汽", "大火", "3 分钟", "水要一次加够，蒸锅上汽之后再放食材"),
        ("上笼蒸", "大火足汽", 0.70, "全程别开盖，蒸汽跑了就得补时间"),
        ("验熟", "关火", "—", "筷子能轻松扎透最厚的地方就是熟了"),
        ("焖", "关火", "2 分钟", "关火焖一下再揭盖，防回缩塌陷"),
        ("调味激香", "大火", "30 秒", "淋豉油撒葱花，热油一浇把香味激出来"),
    ]),
    "炖": (0.75, [
        ("焯水去腥", "大火", 0.12, "冷水下锅，水开后撇净浮沫再捞出"),
        ("爆香 / 炒糖色", "中火", 0.12, "香料出味、糖色炒到枣红就下主料"),
        ("加水烧开", "大火", "3 分钟", "水一次加够没过食材，中途别添凉水"),
        ("小火慢炖", "小火", 0.55, "汤面似开非开，筷子能扎透主料"),
        ("大火收汁", "大火", 0.21, "汤汁变稠能挂住食材就关火"),
    ]),
    "煮": (0.70, [
        ("冷水下料 / 焯水", "大火", 0.15, "冷水下锅，水开后撇净浮沫"),
        ("大火烧开", "大火", "3 分钟", "水一次加够，中途别添凉水"),
        ("撇沫转小火", "小火", 0.50, "汤面保持微沸，汤色慢慢变浓"),
        ("下主料", "中火", 0.25, "易熟的后放，别一开始就全下锅"),
        ("调味出锅", "关火前", 0.10, "盐最后放，尝过再决定加不加"),
    ]),
    "炸": (0.40, [
        ("腌渍入味", "—", 0.35, "提前腌好，下锅前把表面水分按干"),
        ("烧油", "中火", "3 分钟", "筷子插进去周围冒密集小泡，约六成热"),
        ("初炸定型", "中火", 0.35, "一面定型再翻面，别频繁翻动"),
        ("升温复炸", "大火", "30 秒", "油温升到八成热，外壳金黄酥脆就捞"),
        ("出锅控油", "—", "1 分钟", "捞出放架子上控油，叠放会回软"),
    ]),
    "水煎": (0.30, [
        ("摆生坯", "—", 0.15, "收口朝上摆进锅里，彼此留出间距，别挤"),
        ("热锅下油", "中火", "2 分钟", "油只要薄薄一层，晃匀润满锅底"),
        ("煎底", "中小火", 0.35, "底面煎到金黄再动它，中间别去翻"),
        ("加水焖", "中火", 0.35, "加水到生坯三分之一高，盖盖焖到水干"),
        ("收干出锅", "大火", 0.15, "水干后听到滋滋声再等半分钟，底部更脆"),
    ]),
    "煎": (0.35, [
        ("擦干表面", "—", 0.25, "表面水分按干，带水下锅会溅油也煎不上色"),
        ("热锅下油", "中火", "2 分钟", "油面浮起细纹，约五成热"),
        ("下{main}煎", "中小火", 0.45, "别急着翻动，等底面定型再翻"),
        ("翻面煎", "中小火", 0.20, "一面金黄再翻，尽量只翻一次"),
        ("出锅", "—", "—", "两面金黄、筷子能扎透就是熟了"),
    ]),
    "拌": (0.40, [
        ("焯水断生", "大火", 0.30, "水开下料，断生立刻捞出，别煮过头"),
        ("过凉", "冰水", "2 分钟", "过一遍冰水，口感更脆"),
        ("控干水分", "—", "3 分钟", "沥干再拌，带水会稀释味道"),
        ("调汁", "—", 0.25, "调料先在小碗里调匀，再倒进去"),
        ("拌匀静置", "—", 0.45, "拌匀后静置入味再上桌"),
    ]),
    "烤": (0.70, [
        ("预热烤箱", "—", "10 分钟", "提前预热到位，别把食材放进去等升温"),
        ("腌制入味", "—", 0.30, "腌够时间才入味，表面按干"),
        ("入炉烤", "上下火", 0.50, "放中层，中途翻面一次"),
        ("上色", "上火", 0.15, "表面金黄、边缘微焦"),
        ("出炉静置", "—", "5 分钟", "静置几分钟再切，汁水不流失"),
    ]),
    "烙": (0.50, [
        ("和面醒面", "—", 0.40, "揉匀后盖湿布，醒到面团柔软有延展性"),
        ("擀制成型", "—", 0.20, "厚薄均匀才熟得一致"),
        ("热锅下坯", "中小火", 0.22, "锅烧热后转中小火，火大容易糊底"),
        ("翻面烙", "中小火", 0.15, "底面金黄再翻，尽量只翻一次"),
        ("出锅", "—", "—", "两面金黄、鼓起分层就是熟了"),
    ]),
}


# ---------- 烹饪时长预算的上下限（分钟）----------
# timeMin 里含大量备料时间（发面、泡发、腌制），不设上限会算出「蒸 59 分钟」这种荒谬值。
BUDGET_RANGE = {
    "炒": (3, 12), "蒸": (3, 30), "炖": (10, 90), "煮": (5, 60),
    "炸": (3, 15), "煎": (3, 12), "拌": (3, 20), "烤": (10, 60), "烙": (3, 20),
    "水煎": (5, 20),
}


def fmt_time(minutes):
    """把分钟数转成人看着舒服的写法"""
    m = max(minutes, 0.0)
    if m < 1:
        sec = max(int(round(m * 60 / 10.0) * 10), 10)
        return f"{sec} 秒"
    if m < 10:
        return f"{round(m * 2) / 2:g} 分钟"
    return f"{int(round(m))} 分钟"


def detect_method(recipe):
    """菜名优先，判不出来再看做法正文（按命中词数计分），最后兜底为炒"""
    name = recipe.get("name", "")
    for m, keys in NAME_KEYS:
        if any(k in name for k in keys):
            return m
    text = " ".join((recipe.get("steps", {}) or {}).get(k, "")
                    for k in ("cook", "blanch", "season"))
    best, best_score = None, 0
    for m, keys in STEP_KEYS:
        score = sum(1 for k in keys if k in text)
        if score > best_score:
            best, best_score = m, score
    return best or DEFAULT_METHOD


def main_ingredient(recipe):
    """取第一个「真食材」，跳过面粉/水/淀粉这些泛用项——否则会冒出「下面粉煎」"""
    ings = recipe.get("ingredients") or []
    cands = []
    for ing in ings:
        name = re.sub(r"[（(][^）)]*[）)]", "", ing.get("name", "")).strip()
        name = re.sub(r"[、,，/].*$", "", name)
        if not name or any(x in name for x in NOT_MAIN):
            continue
        cands.append(name)
    if not cands and ings:
        cands = [re.sub(r"[（(][^）)]*[）)]", "", ings[0].get("name", "")).strip()]
    return (cands[0][:6] if cands else "") or "主料"


def build(recipe):
    method = detect_method(recipe)
    ratio, tpl = TEMPLATES[method]
    lo, hi = BUDGET_RANGE[method]
    budget = min(max(recipe.get("timeMin", 20) * ratio, lo), hi)
    main = main_ingredient(recipe)
    rows = []
    for stage, fire, dur, signal in tpl:
        t = dur if isinstance(dur, str) else fmt_time(budget * dur)
        rows.append({
            "stage": stage.replace("{main}", main),
            "fire": fire,
            "time": t,
            "signal": signal,
        })
    return method, rows


def handcrafted_ids():
    """手工精修过的菜谱（tools/enrich 里的补丁），这些永远不被生成器覆盖"""
    ids = set()
    for f in glob.glob(os.path.join(ROOT, "tools", "enrich", "*.json")):
        try:
            for p in json.load(open(f, encoding="utf-8")):
                if p.get("id"):
                    ids.add(p["id"])
        except Exception:
            pass
    return ids


def main():
    force = "--force" in sys.argv
    keep = handcrafted_ids()
    files = sorted(glob.glob(os.path.join(DATA, "*.json")))
    total = filled = kept = 0
    by_method = collections.Counter()

    for f in files:
        items = json.load(open(f, encoding="utf-8"))
        changed = False
        for r in items:
            total += 1
            if r["id"] in keep:
                kept += 1
                continue
            if r.get("heat") and not force:
                continue
            method, rows = build(r)
            r["heat"] = rows
            by_method[method] += 1
            filled += 1
            changed = True
        if changed:
            with open(f, "w", encoding="utf-8") as fh:
                json.dump(items, fh, ensure_ascii=False, indent=1)

    print(f"菜谱共 {total} 道：手工精修保留 {kept} 道 · 本次生成 {filled} 道"
          f"{'（--force 覆盖重算）' if force else ''}")
    print("手法分布：", " · ".join(f"{k} {v}" for k, v in by_method.most_common()))
    print("下一步：python3 tools/merge_recipes.py")


if __name__ == "__main__":
    main()
