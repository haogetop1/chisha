#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 tools/data/*.json 合并成 app/src/main/assets/recipes.json，并做结构校验。
每道菜必须是：
{ id, name, cuisine, rarity, mealSlots, difficulty, timeMin, servings,
  ingredients[{name,amount,note,prep}], steps{select,wash,cut,blanch,cook,season},
  tips, commonFails }
v2 加强字段（可选，但推荐）：
  heat[{stage,fire,time,signal}]  火候时间轴
  fails[{symptom,fix}]            翻车对照表
  ingredients[].prep              这味食材买回来怎么处理
"""
import json, os, sys, glob, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "tools", "data")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "recipes.json")

STEPS = ["select", "wash", "cut", "blanch", "cook", "season"]
REQUIRED = ["id", "name", "cuisine", "rarity", "mealSlots", "difficulty",
            "timeMin", "servings", "ingredients", "steps", "tips", "commonFails"]

TARGET = {"川菜": 45, "粤菜": 45, "湘菜": 30, "鲁菜": 25, "苏菜": 25, "浙菜": 25,
          "闽菜": 20, "徽菜": 20, "家常菜": 50, "早餐面点": 50, "汤羹小吃": 30}

def main():
    files = sorted(glob.glob(os.path.join(DATA, "*.json")))
    if not files:
        print("没有数据文件"); sys.exit(1)
    all_recipes, ids = [], set()
    errors, warns = [], []
    for f in files:
        with open(f, encoding="utf-8") as fh:
            try:
                items = json.load(fh)
            except Exception as e:
                errors.append(f"{os.path.basename(f)}: JSON 解析失败 {e}"); continue
        for r in items:
            rid = r.get("id", "?")
            for k in REQUIRED:
                if k not in r:
                    errors.append(f"{rid}: 缺字段 {k}")
            if rid in ids:
                errors.append(f"{rid}: id 重复")
            ids.add(rid)
            steps = r.get("steps") or {}
            for s in STEPS:
                if not (steps.get(s) or "").strip():
                    errors.append(f"{rid}: steps.{s} 为空")
            if r.get("rarity") not in ("common", "signature", "rare"):
                errors.append(f"{rid}: rarity 非法 {r.get('rarity')}")
            for ms in r.get("mealSlots") or []:
                if ms not in ("breakfast", "lunch", "dinner"):
                    errors.append(f"{rid}: mealSlots 非法 {ms}")
            if not (r.get("ingredients") or []):
                errors.append(f"{rid}: ingredients 为空")

            # ---- v2 加强字段校验 ----
            heat = r.get("heat") or []
            if heat:
                if len(heat) < 3:
                    warns.append(f"{rid}: heat 只有 {len(heat)} 行，火候轴太短")
                for i, h in enumerate(heat):
                    for k in ("stage", "fire", "time", "signal"):
                        if not (h.get(k) or "").strip():
                            errors.append(f"{rid}: heat[{i}].{k} 为空")
            fails = r.get("fails") or []
            if fails:
                if len(fails) < 3:
                    warns.append(f"{rid}: fails 只有 {len(fails)} 条")
                for i, fc in enumerate(fails):
                    for k in ("symptom", "fix"):
                        if not (fc.get(k) or "").strip():
                            errors.append(f"{rid}: fails[{i}].{k} 为空")
            prep_n = sum(1 for ing in (r.get("ingredients") or [])
                         if (ing.get("prep") or "").strip())
            r["_rich"] = bool(heat or fails or prep_n)
            all_recipes.append(r)

    if errors:
        print(f"校验失败 {len(errors)} 处：")
        for e in errors[:40]:
            print("  -", e)
        sys.exit(2)

    order = {c: i for i, c in enumerate(TARGET)}
    all_recipes.sort(key=lambda r: (order.get(r["cuisine"], 99), r["id"]))

    by_cuisine = collections.Counter(r["cuisine"] for r in all_recipes)
    by_rarity = collections.Counter(r["rarity"] for r in all_recipes)
    chars = sum(len(json.dumps(r, ensure_ascii=False)) for r in all_recipes)

    print(f"总道数 {len(all_recipes)} / 目标 {sum(TARGET.values())}")
    for c, n in TARGET.items():
        got = by_cuisine.get(c, 0)
        flag = "OK " if got == n else ("缺" if got < n else "多")
        print(f"  {flag} {c}: {got}/{n}")
    print(f"稀有度: common={by_rarity.get('common',0)} signature={by_rarity.get('signature',0)} rare={by_rarity.get('rare',0)}")

    rich = sum(1 for r in all_recipes if r["_rich"])
    with_heat = sum(1 for r in all_recipes if r.get("heat"))
    with_fails = sum(1 for r in all_recipes if r.get("fails"))
    with_prep = sum(1 for r in all_recipes
                    if any((i.get("prep") or "").strip() for i in (r.get("ingredients") or [])))
    rich_chars = sum(len(json.dumps(r, ensure_ascii=False)) for r in all_recipes if r["_rich"])
    plain_chars = chars - rich_chars
    plain_n = len(all_recipes) - rich
    avg_rich = rich_chars / rich / 1000 if rich else 0
    avg_plain = plain_chars / plain_n / 1000 if plain_n else 0
    print(f"详细度: 加强版 {rich}/{len(all_recipes)} "
          f"(火候轴 {with_heat} · 翻车表 {with_fails} · 食材处理 {with_prep})")
    if rich and plain_n:
        print(f"  平均字数: 加强版 {avg_rich:.2f}k / 旧版 {avg_plain:.2f}k "
              f"(约 {avg_rich/avg_plain:.1f} 倍)")
    print(f"内容体量 {chars/1000:.1f}k 字符")

    if warns:
        print(f"提示 {len(warns)} 处：")
        for w in warns[:20]:
            print("  -", w)

    for r in all_recipes:
        r.pop("_rich", None)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(all_recipes, fh, ensure_ascii=False, separators=(",", ":"))
    print("已写出", OUT, f"{os.path.getsize(OUT)/1024:.0f} KB")

    # 顺手把通用挑菜库 / 切菜库拷进 assets，并做基本校验
    for name, label in (("picking.json", "挑菜库"), ("cutting.json", "切菜库")):
        src = os.path.join(ROOT, "tools", name)
        dst = os.path.join(ROOT, "app", "src", "main", "assets", name)
        if not os.path.exists(src):
            print(f"跳过 {label}（没有 {name}）"); continue
        tips = json.load(open(src, encoding="utf-8"))
        seen, terr = set(), []
        for i, e in enumerate(tips):
            k, t = (e.get("k") or "").strip(), (e.get("t") or "").strip()
            if not k or not t:
                terr.append(f"第 {i} 条 k/t 为空"); continue
            if k in seen:
                terr.append(f"关键词重复: {k}")
            seen.add(k)
        if terr:
            print(f"{label}校验失败 {len(terr)} 处：")
            for e in terr[:20]:
                print("  -", e)
            sys.exit(2)
        with open(dst, "w", encoding="utf-8") as fh:
            json.dump(tips, fh, ensure_ascii=False, separators=(",", ":"))
        print(f"{label} {len(tips)} 条 -> {dst}")

if __name__ == "__main__":
    main()
