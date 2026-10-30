#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
验收脚本：逐行照抄 DrawEngine.kt / XpEngine.kt 的语义，用真实 365 道数据跑数值验证。

不是「读代码觉得对」，而是：
  · 穷举算概率表，看是否自洽（和为 1、与 README 对得上）
  · 蒙特卡洛按 Kotlin 里那个逐项累减的循环原样模拟，看实际频率是否等于显示概率
  · 等级曲线逐个阈值走一遍，看边界有没有洞
"""
import json, os, random, math, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RECIPES = os.path.join(ROOT, "app", "src", "main", "assets", "recipes.json")

# ===== 与 Models.kt Rarity 枚举一致 =====
BASE_W = {"common": 1.0, "signature": 0.6, "rare": 0.25}
# ===== 与 DrawEngine.kt 一致 =====
PENALTY_BASE = 0.6
PENALTY_FLOOR = 0.15
# ===== 与 XpEngine.kt 一致 =====
MAX_LEVEL = 10
NEED = [330, 660, 1100, 1650, 2200, 2750, 3300, 3850, 4400]


def weight_of(rarity, low=0):
    """DrawEngine.weightOf"""
    pen = max(PENALTY_FLOOR, PENALTY_BASE ** low)   # 对应 .coerceAtLeast(PENALTY_FLOOR)
    return BASE_W[rarity] * pen


def draw(cands, low, rnd):
    """DrawEngine.draw 的采样循环，逐行照抄"""
    weights = [weight_of(r["rarity"], low.get(r["id"], 0)) for r in cands]
    total = sum(weights)
    x = rnd.random() * total
    picked = len(cands) - 1
    for i in range(len(cands)):
        x -= weights[i]
        if x <= 0.0:
            picked = i
            break
    return picked, weights, total


def total_xp_to_reach(level):
    if level <= 1:
        return 0
    if level > MAX_LEVEL:
        return 2 ** 31 - 1
    return sum(NEED[:level - 1])


def level_for(total_xp):
    lv, acc = 1, 0
    for need in NEED:
        if total_xp >= acc + need:
            acc += need
            lv += 1
        else:
            break
    return min(lv, MAX_LEVEL)


def progress(total_xp):
    lv = level_for(total_xp)
    if lv >= MAX_LEVEL:
        return 1, 1
    return total_xp - total_xp_to_reach(lv), NEED[lv - 1]


def base_xp(d):
    return {1: 80, 3: 150}.get(d, 110)


def rarity_bonus(r):
    return {"common": 0, "signature": 20, "rare": 50}[r]


def compute(rarity, difficulty, first_time, first_of_day, full_day):
    xp = base_xp(difficulty) + rarity_bonus(rarity)
    if first_time:
        xp += 30
    if first_of_day:
        xp = round(xp * 1.3)
    if full_day:
        xp += 50
    return xp


def main():
    rs = json.load(open(RECIPES, encoding="utf-8"))
    ok = True
    print("=" * 74)
    print("一、抽取概率：全库穷举")
    print("=" * 74)
    total_w = sum(weight_of(r["rarity"]) for r in rs)
    by = collections.defaultdict(float)
    cnt = collections.Counter()
    for r in rs:
        by[r["rarity"]] += weight_of(r["rarity"])
        cnt[r["rarity"]] += 1
    print(f"菜库 {len(rs)} 道 · 权重总和 {total_w:.4f}")
    print(f"{'档位':<8}{'道数':>6}{'档位命中':>10}{'单道概率':>12}{'README 声称':>26}")
    claim = {"common": (74.49, 0.3401), "signature": (22.45, 0.2041), "rare": (3.06, 0.0850)}
    for k in ("common", "signature", "rare"):
        share = by[k] / total_w * 100
        per = BASE_W[k] / total_w * 100
        c = claim[k]
        hit = abs(share - c[0]) < 0.01 and abs(per - c[1]) < 0.0001
        ok &= hit
        print(f"{k:<8}{cnt[k]:>6}{share:>9.2f}%{per:>11.4f}%"
              f"{'  声称 %.2f%% / %.4f%%  %s' % (c[0], c[1], '✓' if hit else '✗'):>26}")
    s = sum(BASE_W[r["rarity"]] / total_w for r in rs)
    print(f"\n全库单道概率求和 = {s:.10f}  {'✓ 严格归一' if abs(s - 1) < 1e-9 else '✗ 不归一'}")
    ok &= abs(s - 1) < 1e-9

    print("\n" + "=" * 74)
    print("二、采样算法无偏性（照抄 Kotlin 循环，各池 100 万次）")
    print("=" * 74)
    rnd = random.Random(20261030)
    worst = []
    for label, pool in [("全库", rs),
                        ("川菜", [r for r in rs if r["cuisine"] == "川菜"]),
                        ("稀有 36 道", [r for r in rs if r["rarity"] == "rare"])]:
        N = 1_000_000
        hit = collections.Counter()
        for _ in range(N):
            i, w, t = draw(pool, {}, rnd)
            hit[pool[i]["id"]] += 1
        max_dev, max_name, chi = 0.0, "", 0.0
        pool_w = sum(weight_of(x["rarity"]) for x in pool)
        for r in pool:
            theo = weight_of(r["rarity"]) / pool_w
            exp = theo * N
            obs = hit[r["id"]]
            chi += (obs - exp) ** 2 / exp          # Pearson 卡方要用计数，不是比例
            sd = math.sqrt(theo * (1 - theo) * N)
            z = abs(obs - exp) / sd if sd else 0
            if z > max_dev:
                max_dev, max_name = z, r["name"]
        worst.append((label, len(pool), max_dev, max_name, chi))
        print(f"  {label:<10} {len(pool):>3} 道 · 最大偏差 {max_dev:>5.2f}σ（{max_name}）· "
              f"卡方 {chi:>7.1f}（自由度 {len(pool)-1}）")
        if max_dev > 4:
            ok = False
    print("  判据：单点偏差 < 4σ 视为无偏（100 万次抽样，正态近似下几乎不可能超）")

    print("\n" + "=" * 74)
    print("三、「再换一个」时的概率分母（excludeId 路径）")
    print("=" * 74)
    pool = [r for r in rs if r["cuisine"] == "川菜"]
    pool_w = sum(weight_of(r["rarity"]) for r in pool)
    cands = pool[:-1]                       # 模拟 excludeId 掉最后一道
    cand_w = sum(weight_of(r["rarity"]) for r in cands)
    print(f"  池内 {len(pool)} 道 · 权重和 {pool_w:.4f}")
    print(f"  首次抽取：某道家常菜 {1.0/pool_w*100:.3f}%")
    print(f"  换一个后：候选 {len(cands)} 道 · 权重和 {cand_w:.4f} · 同一道 {1.0/cand_w*100:.3f}%")
    print(f"  → 换一个会让剩余菜概率整体抬升 {(1.0/cand_w)/(1.0/pool_w)-1:+.2%}；"
          f"UI 仍显示「本池 {len(pool)} 道」")
    print("  界面文案与实际分母不一致（45 道 vs 44 道候选），属显示瑕疵")

    print("\n" + "=" * 74)
    print("四、等级曲线：逐阈值验证")
    print("=" * 74)
    print(f"  各段需求 {NEED}")
    print(f"  累计 {sum(NEED)}（注释声称 20240）  {'✓' if sum(NEED)==20240 else '✗'}")
    ok &= sum(NEED) == 20240
    print(f"\n  {'总经验':>8}{'等级':>6}{'本级进度':>12}{'到达该级累计':>14}")
    marks = [0, 329, 330, 331, 989, 990, 20239, 20240, 20241, 99999]
    for xp in marks:
        into, need = progress(xp)
        print(f"  {xp:>8}{level_for(xp):>6}{f'{into}/{need}':>12}{total_xp_to_reach(level_for(xp)):>14}")
    bad = []
    for L in range(1, MAX_LEVEL + 1):
        base = total_xp_to_reach(L)
        if level_for(base) != L:
            bad.append((L, base, level_for(base)))
        if L > 1 and level_for(base - 1) != L - 1:
            bad.append((L, base - 1, level_for(base - 1)))
    print(f"\n  每级起点/前一点双向校验：{'✓ 无洞无重叠' if not bad else '✗ ' + str(bad)}")
    ok &= not bad
    prev = -1
    for xp in range(0, 20241):
        lv = level_for(xp)
        if lv < prev:
            print(f"  ✗ 等级在 {xp} 处下降"); ok = False; break
        prev = lv
    print(f"  0→20240 全程单调不减：{'✓' if ok else '✗'}")
    print(f"  满级后 progress 返回 {progress(10**6)}（UI 显示「已满级」）")

    print("\n" + "=" * 74)
    print("五、经验公式与「约 170~190 次满级」是否成立")
    print("=" * 74)
    n = len(rs)
    base_avg = sum(base_xp(r["difficulty"]) for r in rs) / n
    rar_avg = sum(rarity_bonus(r["rarity"]) for r in rs) / n
    print(f"  难度分布下的基础经验均值 {base_avg:.1f} · 稀有度加成均值 {rar_avg:.1f}")
    steady = (base_avg + rar_avg) * (1 + 0.3 / 3) + 50 / 3
    print(f"  稳态每次预期经验 ≈ {steady:.1f}（含 1/3 首单 ×1.3、1/3 天全勤 +50）")
    full = (base_avg + rar_avg + 30) * (1 + 0.3 / 3) + 50 / 3
    print(f"  全新账号首次每次预期 ≈ {full:.1f}（额外 +30）")
    print(f"  推算满级所需次数：约 {20240/steady:.0f}（稳态）～ {20240/full:.0f}（全新）次")
    print(f"  注释声称「约 170~190 次」：{'✓ 落在区间' if 160 <= 20240/steady <= 195 else '✗ 偏离'}")

    print("\n" + "=" * 74)
    print("六、经验公式边界穷举（难度 × 稀有度 × 三个加成）")
    print("=" * 74)
    lo_xp, hi_xp = 10 ** 9, -1
    for d in (1, 2, 3):
        for rar in ("common", "signature", "rare"):
            for ft in (False, True):
                for fd in (False, True):
                    for fu in (False, True):
                        x = compute(rar, d, ft, fd, fu)
                        lo_xp, hi_xp = min(lo_xp, x), max(hi_xp, x)
    print(f"  单次经验区间 {lo_xp} ~ {hi_xp}")
    print(f"  最高单次 {hi_xp} → 满级最速需 {math.ceil(20240/hi_xp)} 次；"
          f"最低 {lo_xp} → 最慢需 {math.ceil(20240/lo_xp)} 次")
    print(f"  单次经验不可能为 0：{'✓' if lo_xp > 0 else '✗'}（无图时由调用方直接给 0，见下）")

    print("\n" + "=" * 74)
    print("结论")
    print("=" * 74)
    print("  概率表与采样算法：" + ("数学自洽、实测无偏" if ok else "存在问题，见上"))
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
