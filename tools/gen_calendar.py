#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 app/src/main/assets/calendar.json：
把 2025~2045 年每天的「节气」和「节日」预先算好，App 运行时只做日期查表，
不做农历换算，也不需要网络。

依赖：pip install lunar_python
重新生成：python3 tools/gen_calendar.py
"""
import json, os, datetime
from lunar_python import Solar

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "calendar.json")

START = datetime.date(2025, 1, 1)
END = datetime.date(2045, 12, 31)

# 节日只取与 24 节气不重合的，避免一天算两个成就
FESTIVALS = {
    (1, 1): "元旦",
    (1, 15): "元宵节",
    (5, 5): "端午节",
    (7, 7): "七夕节",
    (8, 15): "中秋节",
    (9, 9): "重阳节",
    (12, 8): "腊八节",
    (12, 23): "小年",
}

SOLAR_FESTIVALS = {
    (1, 1): "元旦",
    (5, 1): "劳动节",
    (10, 1): "国庆节",
}


def main():
    terms, festivals = {}, {}
    d = START
    prev_lunar = None
    while d <= END:
        solar = Solar.fromYmd(d.year, d.month, d.day)
        lunar = solar.getLunar()
        key = d.strftime("%Y-%m-%d")

        # 节气
        jq = lunar.getJieQi()
        if jq:
            terms[key] = jq

        # 公历节日（元旦会被下面农历的元旦覆盖，值相同）
        if (d.month, d.day) in SOLAR_FESTIVALS:
            festivals[key] = SOLAR_FESTIVALS[(d.month, d.day)]

        # 农历节日
        m, day = lunar.getMonth(), lunar.getDay()
        if m > 0 and (m, day) in FESTIVALS:
            festivals[key] = FESTIVALS[(m, day)]

        # 春节 = 正月初一
        if m == 1 and day == 1:
            festivals[key] = "春节"

        # 除夕 = 第二天是正月初一
        nxt = d + datetime.timedelta(days=1)
        nl = Solar.fromYmd(nxt.year, nxt.month, nxt.day).getLunar()
        if nl.getMonth() == 1 and nl.getDay() == 1:
            festivals[key] = "除夕"

        d = nxt

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump({"terms": terms, "festivals": festivals}, f,
                  ensure_ascii=False, separators=(",", ":"))

    size = os.path.getsize(OUT)
    print(f"节气 {len(terms)} 条 / 节日 {len(festivals)} 条  共 {size/1024:.0f} KB")
    print(f"覆盖 {START} ~ {END}")

    print("\n抽查（正确性自检）：")
    checks = [
        (2026, 2, 4, "立春"), (2026, 2, 17, None), (2026, 6, 19, None),
        (2026, 9, 25, None), (2026, 12, 21, "冬至"), (2026, 12, 22, "冬至"),
    ]
    for y, mo, dd in [(y, m_, d_) for y, m_, d_, _ in checks]:
        k = f"{y}-{mo:02d}-{dd:02d}"
        print(f"  {k}  节气={terms.get(k, '—'):<4} 节日={festivals.get(k, '—')}")

    print("\n近三年主要节日：")
    for y in (2026, 2027, 2028):
        row = {v: k for k, v in festivals.items() if k.startswith(str(y))}
        picks = ["春节", "元宵节", "端午节", "七夕节", "中秋节", "重阳节", "除夕"]
        print(f"  {y}: " + "  ".join(f"{p}={row.get(p,'?')}" for p in picks))


if __name__ == "__main__":
    main()
