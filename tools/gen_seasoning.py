#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
给每道菜生成「调味料什么时候放」清单。

做法：扫这道菜**实际用到的**食材，去调味料字典里匹配，命中就记下
「投放时间节点 + 为什么这时候放」，最后按时间先后排序。
所以清单是这道菜自己的——用了蚝油才会出现蚝油，没用就不出现。

时间节点的先后顺序（即下锅先后）：
  腌制 → 热锅爆香 → 主料入锅 → 合炒调味 → 加水炖煮 → 收汁 → 出锅前 → 关火后
"""
import glob, json, os, re, sys, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "tools", "data")
DICT = os.path.join(ROOT, "tools", "seasoning.json")

# 时间节点的先后顺序
NODE_ORDER = [
    "下锅前（腌制）",
    "调馅时",
    "热锅爆香（小火）",
    "热锅爆香",
    "分两次（爆香用粒、关火撒粉）",
    "分两次（一半爆香、一半出锅）",
    "分两次（葱白爆香、葱叶出锅）",
    "主料入锅时（沿锅边）",
    "主料入锅时（上色）",
    "合炒调味（沿锅边）",
    "合炒调味",
    "加水 / 炖煮时（中小火）",
    "加水 / 炖煮时",
    "收汁时（勾芡）",
    "出锅前（沿锅边）",
    "出锅前（收汁时）",
    "出锅前",
    "关火后（热油泼）",
    "关火后",
]
_ORDER = {n: i for i, n in enumerate(NODE_ORDER)}
FALLBACK_ORDER = 99

SALT = {"name": "盐", "when": "出锅前（收汁时）",
        "why": "早放会让肉出水、不易酥烂，蔬菜也会大量出水。最后放，咸味浮在表面，用量还能少一点。"}
# 甜品类的兜底换成糖
SWEET = {"name": "糖", "when": "合炒调味",
         "why": "甜品靠糖定味。早放容易糊底发苦，主料下锅后再调，边调边尝。"}
# 清淡主食（粥、馒头、豆浆…）本身不靠调料，给一条中性的说明
PLAIN = {"name": "盐或糖（按口味）", "when": "出锅前", "why": "清淡主食本身不靠调料提味，出锅前按自己口味略调，边调边尝就行。"}
SWEET_HINT = ("糖水", "甜品", "蜜", "糕", "酥", "糖不甩", "芋圆", "豆沙", "汤圆", "布丁", "奶冻")
PLAIN_HINT = ("粥", "米饭", "馒头", "豆浆", "油条", "米饭", "白饭", "粽", "团", "馒头", "花卷", "窝头")
# 带馅的面点：盐要在调馅那一步就放，不能等包好再调味
FILLING_HINT = ("饺", "馄饨", "盒子", "包子", "烧卖", "馅饼", "馅", "春卷", "汤圆")
FILLING = {"name": "盐", "when": "调馅时",
           "why": "调馅这一步就要把味定好——包好之后再补盐是补不进去的。顺一个方向搅到发黏起胶，馅才抱团不出水。"}


def clean(name):
    """去掉括号说明，再按顿号/逗号切成独立的食材名"""
    s = re.sub(r"[（(][^）)]*[）)]", "", name or "")
    parts = re.split(r"[、,，/和]", s)
    return [p.strip() for p in parts if p.strip()]


def build(recipe, table):
    hits = {}
    for ing in recipe.get("ingredients") or []:
        for part in clean(ing.get("name")):
            # 一个食材名里可能有多味调料（比如「生抽、白糖」），逐个找最长匹配
            for k, w, y in table:
                if k in part:
                    if k not in hits:
                        hits[k] = {"name": k, "when": w, "why": y}
                    break
    name = recipe.get("name", "")
    if not hits:
        if any(h in name for h in SWEET_HINT):
            return [dict(SWEET)]
        if any(h in name for h in PLAIN_HINT):
            return [dict(PLAIN)]
        if any(h in name for h in FILLING_HINT):
            return [dict(FILLING)]
        return [dict(SALT)]
    rows = list(hits.values())
    # 咸味菜几乎都要放盐，食材表里没单列的话补一条，保证清单够用
    if not any(h in name for h in SWEET_HINT + PLAIN_HINT):
        if not any(r["name"] == "盐" for r in rows):
            rows.append(dict(FILLING) if any(h in name for h in FILLING_HINT) else dict(SALT))
    rows.sort(key=lambda r: _ORDER.get(r["when"], FALLBACK_ORDER))
    return rows


def main():
    force = "--force" in sys.argv
    raw = json.load(open(DICT, encoding="utf-8"))
    # 长关键词优先，避免「糖」抢掉「冰糖」
    table = sorted(((e["k"], e["w"], e["y"]) for e in raw), key=lambda t: -len(t[0]))

    files = sorted(glob.glob(os.path.join(DATA, "*.json")))
    total = filled = 0
    nohit = collections.Counter()
    for f in files:
        items = json.load(open(f, encoding="utf-8"))
        changed = False
        for r in items:
            total += 1
            if r.get("seasoning") and not force:
                continue
            rows = build(r, table)
            r["seasoning"] = rows
            if len(rows) == 1 and rows[0]["name"] in ("盐", "糖"):
                nohit[r["name"]] += 1
            filled += 1
            changed = True
        if changed:
            with open(f, "w", encoding="utf-8") as fh:
                json.dump(items, fh, ensure_ascii=False, indent=1)

    print(f"菜谱共 {total} 道，本次生成调味料清单 {filled} 道{'（--force 覆盖重算）' if force else ''}")
    if nohit:
        print(f"没匹配到任何调料、走了兜底的 {len(nohit)} 道："
              + "、".join(list(nohit)[:12]) + ("…" if len(nohit) > 12 else ""))
    else:
        print("每道菜都匹配到了实际调料，没有走兜底")
    print("下一步：python3 tools/merge_recipes.py")


if __name__ == "__main__":
    main()
