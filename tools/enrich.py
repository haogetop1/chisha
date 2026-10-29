#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
菜谱升级工具：把 tools/enrich/*.json 里的「加强版补丁」按 id 就地打进
tools/data/*.json，然后再跑 merge_recipes.py 生成 assets/recipes.json。

补丁文件是数组，每项至少带 id，其余字段整体覆盖：
{
  "id": "chuan_001",
  "ingredients": [{"name":"嫩豆腐","amount":"400g","note":"...","prep":"..."}],
  "steps": {"select":"...","wash":"...","cut":"...","blanch":"...","cook":"...","season":"..."},
  "tips": "...", "commonFails": "...",
  "heat": [{"stage":"...","fire":"...","time":"...","signal":"..."}],
  "fails": [{"symptom":"...","fix":"..."}]
}
用补丁而不是新文件，是为了不破坏原数据文件的顺序与分卷，也不会撞 id。
"""
import json, os, glob, sys, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "tools", "data")
ENRICH = os.path.join(ROOT, "tools", "enrich")

PATCHABLE = ["ingredients", "steps", "tips", "commonFails", "heat", "fails",
             "timeMin", "difficulty", "servings"]

def main():
    files = sorted(glob.glob(os.path.join(ENRICH, "*.json")))
    if not files:
        print("没有补丁文件:", ENRICH); sys.exit(1)

    # 读全部补丁，查重
    patches, seen = [], {}
    for f in files:
        items = json.load(open(f, encoding="utf-8"))
        for p in items:
            pid = p["id"]
            if pid in seen:
                print(f"补丁重复 {pid}（{os.path.basename(f)} 与 {seen[pid]}）"); sys.exit(2)
            seen[pid] = os.path.basename(f)
            patches.append(p)
    print(f"补丁 {len(patches)} 条，来自 {len(files)} 个文件")

    # 读原数据
    where, data = {}, {}
    for f in sorted(glob.glob(os.path.join(DATA, "*.json"))):
        items = json.load(open(f, encoding="utf-8"))
        data[f] = items
        for i, r in enumerate(items):
            where[r["id"]] = (f, i)

    missing, changed = [], collections.defaultdict(list)
    for p in patches:
        loc = where.get(p["id"])
        if loc is None:
            missing.append(p["id"]); continue
        f, i = loc
        r = data[f][i]
        for k in PATCHABLE:
            if k in p:
                r[k] = p[k]
        changed[f].append(r)

    if missing:
        print(f"找不到对应菜谱 {len(missing)} 条: {missing[:20]}")

    for f, rows in changed.items():
        with open(f, "w", encoding="utf-8") as fh:
            json.dump(data[f], fh, ensure_ascii=False, indent=1)
        print(f"  更新 {os.path.basename(f)}: {len(rows)} 道 "
              f"({', '.join(r['name'] for r in rows[:6])}{'…' if len(rows) > 6 else ''})")

    total = sum(len(v) for v in changed.values())
    print(f"已就地升级 {total} 道。下一步：python3 tools/merge_recipes.py")

if __name__ == "__main__":
    main()
