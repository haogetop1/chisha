#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用与 App 内 DrawEngine 完全相同的权重规则，算出真实的抽取分布。
改 W / 菜库分档数量后重跑，就能看到档位命中率和单道菜概率怎么变。
"""
import json, os, random, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RECIPES = os.path.join(ROOT, "app", "src", "main", "assets", "recipes.json")

# ---- 与 DrawEngine.kt 保持一致 ----
W = {"common": 1.0, "signature": 0.6, "rare": 0.25}
PENALTY_BASE = 0.6
PENALTY_FLOOR = 0.15
# -----------------------------------

def weight(r, low=0):
    pen = max(PENALTY_FLOOR, PENALTY_BASE ** low)
    return W[r["rarity"]] * pen

def main():
    rs = json.load(open(RECIPES, encoding="utf-8"))
    total_w = sum(weight(r) for r in rs)
    by = collections.defaultdict(float)
    cnt = collections.Counter()
    for r in rs:
        by[r["rarity"]] += weight(r)
        cnt[r["rarity"]] += 1

    print(f"菜库总数 {len(rs)}   权重总和 {total_w:.2f}\n")
    print(f"{'档位':<10}{'道数':>6}{'权重占比':>10}{'档位命中':>10}{'单道概率':>10}")
    for k in ("common", "signature", "rare"):
        share = by[k] / total_w
        per = (W[k] / total_w) * 100
        print(f"{k:<10}{cnt[k]:>6}{by[k]/total_w*100:>9.1f}%{share*100:>9.2f}%{per:>9.4f}%")

    # 蒙特卡洛验证
    N = 200000
    weights = [weight(r) for r in rs]
    picks = random.choices(rs, weights=weights, k=N)
    sim = collections.Counter(p["rarity"] for p in picks)
    print("\n20 万次模拟：", {k: f"{sim[k]/N*100:.2f}%" for k in ("common", "signature", "rare")})

    rarest = min(rs, key=lambda r: weight(r))
    print(f"\n最稀有的一道：{rarest['name']}（{rarest['rarity']}）单道概率 {W[rarest['rarity']]/total_w*100:.4f}%")
    print(f"抽中任意稀有菜约需 {1/(by['rare']/total_w):.0f} 次")

    # 各菜系池内分布（选菜系后抽取）
    print("\n按菜系看（选该菜系后，池内归一化）：")
    for c in sorted({r["cuisine"] for r in rs}):
        pool = [r for r in rs if r["cuisine"] == c]
        tw = sum(weight(r) for r in pool)
        rr = sum(weight(r) for r in pool if r["rarity"] == "rare")
        print(f"  {c:<8} {len(pool):>3} 道   稀有权重占比 {rr/tw*100:>5.2f}%")

if __name__ == "__main__":
    main()
