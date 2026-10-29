#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
逐道菜去下厨房搜成品图，下载并压成 WebP 放进 assets/dish_photos/{id}.webp。

用法：
  python3 tools/fetch_photos.py --limit 8          # 先试 8 道
  python3 tools/fetch_photos.py                    # 全部
  python3 tools/fetch_photos.py --ids chuan_001,yue_001
  python3 tools/fetch_photos.py --force            # 已存在的也重抓

产物：
  app/src/main/assets/dish_photos/{id}.webp
  tools/data/photos.json   # id -> {title, url, page, picked_at}
"""
import argparse, json, os, re, subprocess, sys, time, urllib.parse, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RECIPES = os.path.join(ROOT, "app", "src", "main", "assets", "recipes.json")
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "dish_photos")
MANIFEST = os.path.join(ROOT, "tools", "photo_manifest.json")
TMP = "/tmp/chisha_photo_dl.jpg"

UA = ("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
      "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
# 由 --loose 打开：允许「标题不含菜名」也抓（仅用于人工复核过的近似菜名）
LOOSE = False
SEARCH = "https://www.xiachufang.com/search/?keyword={}"
# 图床自带裁剪参数，先在源站缩到 560，省带宽
IMG_PARAM = "?imageView2/1/w/560/h/560/interlace/1/q/88"

LI_RE = re.compile(r"<li>(.*?)</li>", re.S)
IMG_RE = re.compile(r'data-src="(https://i[0-9]\.chuimg\.com/[^"?]+)')
NAME_RE = re.compile(r'<p class="name">.*?<a href="(/recipe/\d+/)"[^>]*>\s*([^<]+?)\s*</a>', re.S)
STATS_RE = re.compile(r'(\d[\d,\.]*)\s*做过')


def get(url, referer=None, timeout=20):
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept": "text/html,application/xhtml+xml,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.9",
    })
    if referer:
        req.add_header("Referer", referer)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read()


def search_candidates(name):
    """返回 [(title, img_url, page, heat)]，已按「标题含菜名」过滤并排序"""
    url = SEARCH.format(urllib.parse.quote(name))
    for attempt in range(3):
        try:
            html = get(url).decode("utf-8", "ignore")
            break
        except Exception as e:
            if attempt == 2:
                print(f"    搜索失败: {e}")
                return []
            time.sleep(2 + attempt * 2)
    out = []
    for li in LI_RE.findall(html):
        m_img = IMG_RE.search(li)
        m_name = NAME_RE.search(li)
        if not (m_img and m_name):
            continue
        title = re.sub(r"\s+", "", m_name.group(2))
        heat = 0
        m_s = STATS_RE.search(li)
        if m_s:
            try:
                heat = int(m_s.group(1).replace(",", ""))
            except ValueError:
                heat = 0
        out.append((title, m_img.group(1), "https://www.xiachufang.com" + m_name.group(1), heat))
    # 标题里带这道菜名的才要；按做过的人数从高到低排
    hit = [c for c in out if name in c[0]]
    hit.sort(key=lambda c: -c[3])
    # 严格模式：对不上菜名就宁可不要图（错图比没图更糟），走 App 里的兜底插画
    return hit or (out[:1] if LOOSE else [])


def to_webp(dst_webp):
    subprocess.run(["cwebp", "-resize", "512", "512", "-q", "78", "-quiet",
                    TMP, "-o", dst_webp], check=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--ids", default="")
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--loose", action="store_true", help="允许标题不含菜名也抓")
    ap.add_argument("--delay", type=float, default=1.2)
    args = ap.parse_args()
    global LOOSE
    LOOSE = args.loose

    recipes = json.load(open(RECIPES, encoding="utf-8"))
    if args.ids:
        wanted = set(args.ids.split(","))
        recipes = [r for r in recipes if r["id"] in wanted]

    os.makedirs(OUT_DIR, exist_ok=True)
    manifest = {}
    if os.path.exists(MANIFEST):
        manifest = json.load(open(MANIFEST, encoding="utf-8"))

    todo = [r for r in recipes
            if args.force or not os.path.exists(os.path.join(OUT_DIR, r["id"] + ".webp"))]
    if args.limit:
        todo = todo[:args.limit]
    print(f"待抓 {len(todo)} 道 / 共 {len(recipes)} 道（已存在 {len(recipes) - len(todo)} 道跳过）")

    ok, fail = 0, []
    for i, r in enumerate(todo, 1):
        rid, name = r["id"], r["name"]
        cands = search_candidates(name)
        if not cands:
            print(f"[{i}/{len(todo)}] {rid} {name}: 没搜到")
            fail.append(rid)
            time.sleep(args.delay)
            continue
        title, img_url, page, heat = cands[0]
        dst = os.path.join(OUT_DIR, rid + ".webp")
        try:
            data = get(img_url + IMG_PARAM, referer=page, timeout=30)
            with open(TMP, "wb") as fh:
                fh.write(data)
            to_webp(dst)
            size = os.path.getsize(dst)
            manifest[rid] = {"title": title, "url": img_url, "page": page, "heat": heat}
            ok += 1
            flag = "" if name in title else "  [标题不含菜名，需人工复核]"
            print(f"[{i}/{len(todo)}] {rid} {name}: {size//1024}KB  《{title}》{flag}")
        except Exception as e:
            print(f"[{i}/{len(todo)}] {rid} {name}: 下载失败 {e}")
            fail.append(rid)
        time.sleep(args.delay)

    with open(MANIFEST, "w", encoding="utf-8") as fh:
        json.dump(manifest, fh, ensure_ascii=False, indent=1)
    total = sum(os.path.getsize(os.path.join(OUT_DIR, f))
                for f in os.listdir(OUT_DIR) if f.endswith(".webp"))
    print(f"\n成功 {ok}，失败 {len(fail)}。图片目录共 {len(os.listdir(OUT_DIR))} 张，"
          f"合计 {total/1024/1024:.1f} MB")
    if fail:
        print("失败列表:", fail[:40])


if __name__ == "__main__":
    main()
