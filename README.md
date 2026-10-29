# 吃啥 · Android 单机做菜 App

每天问你想吃哪个菜系 → 老虎机抽一道菜 → 从买菜到调味全过程教你 → 拍成品图 → 结算经验、升级、解锁成就。

**纯离线**：不申请 `INTERNET` 权限，所有数据都在本机，无账号、无云同步、无任何网络请求。

---

## 一、安装

```bash
# 方式一：直接装到已连接的安卓手机（需打开 USB 调试）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 方式二：把 APK 传到手机，点击安装（需允许「安装未知来源应用」）
```

- 最低系统：Android 8.0（API 26）
- 目标系统：Android 17（API 37）
- 包名：`com.chisha.app`

---

## 二、数据存在哪

全部在应用私有目录，卸载即清除：

| 内容 | 位置 |
|---|---|
| 数据库（记录/评分/成就/等级） | `/data/data/com.chisha.app/databases/chisha.db` |
| 成品图 | `/data/data/com.chisha.app/files/photos/<uuid>.jpg` |
| 菜谱（只读内置） | `assets/recipes.json` |
| 成就（只读内置） | `assets/achievements.json` |
| 节气与节日表（只读内置） | `assets/calendar.json`（2025~2045 年） |

成品图统一压缩到长边 1280、JPEG 质量 85，单张约 150~300KB。

导出数据（调试用）：

```bash
adb exec-out run-as com.chisha.app cat databases/chisha.db > chisha.db
adb pull /data/data/com.chisha.app/files/photos ./photos
```

---

## 三、改菜谱

菜谱是纯 JSON，改完重新构建即可。**不要直接改 `assets/recipes.json`**（它是构建产物），改 `tools/data/*.json` 里的分文件：

```
tools/data/
├── chuan_1.json  chuan_2.json   川菜 45
├── yue_1.json    yue_2.json     粤菜 45
├── xiang.json                   湘菜 30
├── lu.json                      鲁菜 25
├── su.json                      苏菜 25
├── zhe.json                     浙菜 25
├── min.json                     闽菜 20
├── hui.json                     徽菜 20
├── home_1.json   home_2.json    家常菜 50
├── bf_1.json     bf_2.json      早餐面点 50
└── soup.json                    汤羹小吃 30
```

单道菜的结构：

```json
{
  "id": "chuan_001",
  "name": "麻婆豆腐",
  "cuisine": "川菜",
  "rarity": "common",              // common 家常 / signature 大菜 / rare 稀有
  "mealSlots": ["lunch", "dinner"], // breakfast / lunch / dinner
  "difficulty": 1,                  // 1 简单 / 2 中等 / 3 复杂
  "timeMin": 25,
  "servings": 2,
  "ingredients": [{"name": "嫩豆腐", "amount": "400g", "note": "选石膏点的"}],
  "steps": {
    "select": "选菜技巧…",
    "wash":   "洗菜技巧…",
    "cut":    "切菜技巧…",
    "blanch": "焯水与预处理…（不需要就写「本菜无需焯水」）",
    "cook":   "下锅炒制…",
    "season": "调味收尾…"
  },
  "tips": "一句提醒",
  "commonFails": "容易翻车的地方",

  // ↓ v2 加强字段，可选但推荐。有的话菜谱页会多两块 UI
  "heat": [                          // 火候时间轴
    {"stage": "煸牛肉末", "fire": "中火", "time": "3 分钟", "signal": "水汽收干、颜色金黄"}
  ],
  "fails": [                         // 翻车对照表
    {"symptom": "豆腐碎成豆花", "fix": "只用锅铲背推，绝不翻炒"}
  ]
}
```

`ingredients[]` 还可以带这两个字段：

```json
{"name": "卤水豆腐", "amount": "400g", "note": "别用内酯豆腐",
 "prep": "切成 2cm 见方，刀面沾水、手要轻",
 "pick": "表面光滑细嫩、有豆香没有酸味，盒装的水要清澈不发浑"}
```

- `prep` = 买回来**怎么处理**（切法、泡发、腌制…），渲染成食材下面那行橙色的「处理：」
- `pick` = 在超市**怎么挑**，渲染成独立的一节「在超市怎么挑」

改完合并 + 校验：

```bash
python3 tools/merge_recipes.py
```

校验会检查：字段齐全、六段非空、id 不重复、rarity 合法、mealSlots 合法、每菜系道数对得上。**不通过会直接报错并列出问题，不会写出坏数据。** 末尾会打印「详细度」统计，对比加强版和旧版的平均字数。

### 批量升级：用补丁而不是直接改分卷

直接打开 `tools/data/*.json` 容易破坏格式和顺序。推荐把加强版写成补丁，按 id 就地覆盖：

```bash
# tools/enrich/batch01.json 里写 [{"id":"chuan_001", "ingredients":[…], "heat":[…], "fails":[…]}]
python3 tools/enrich.py        # 按 id 打进对应分卷文件
python3 tools/merge_recipes.py # 再合并
```

补丁只写要改的字段，其余原样保留，也不会撞 id。

### 升级后老用户能拿到新菜谱吗

能。`SeedLoader` 用 `assets/recipes.json` 的 CRC32 指纹判断是否重灌：内容变了就按 id 覆盖一遍，**评分和做菜记录不动**。不用手动维护版本号。

### 通用挑菜库（「在超市怎么挑」那一节的数据来源）

`tools/picking.json` 是一张「食材关键词 → 怎么挑」的表，目前 152 条，覆盖 365 道菜里 **86% 的食材条目**（193 道菜的每味食材都有挑法）。

匹配规则在 `data/PickingTips.kt`：
1. 菜谱自己写了 `pick` 就用菜谱的；
2. 否则按关键词匹配，**长关键词优先**（「五花肉」不会被「猪肉」抢走）；
3. 单字关键词（鱼/鸡/油/糖…）怕误伤，所以名字里带「汁、酱、粉、精、露」这类调味品后缀时不予匹配——不然「鱼香汁」会被当成鱼。

改完跑一次 `python3 tools/merge_recipes.py`，它会校验（k/t 非空、关键词不重复）并同步到 `assets/picking.json`。

### 菜品图：抓、换、补

```bash
python3 tools/fetch_photos.py                 # 抓缺的（已存在的跳过）
python3 tools/fetch_photos.py --limit 8       # 先试 8 道
python3 tools/fetch_photos.py --ids chuan_001,yue_001 --force   # 指定重抓
python3 tools/fetch_photos.py --loose --ids xiang_002            # 允许标题不完全匹配
```

流程是：按菜名下厨房搜索页 → 解析 `img[data-src]` 和菜谱标题 → **只有标题里含这道菜名的候选才会被采用**（严格模式，错图比没图更糟）→ 走图床自带的 560 裁剪 → `cwebp -resize 512 -q 78` 压成 WebP → 落到 `assets/dish_photos/{id}.webp`。

- 产物清单在 `tools/photo_manifest.json`（记了标题、原图地址、来源页、做过人数）。
- 跑完会打印 `[标题不含菜名，需人工复核]`，这些要自己看一眼。
- 删掉某个 `{id}.webp` 再跑一次就会重抓那一道。
- App 端在 `data/DishPhotos.kt`：启动时 `assets.list()` 建一次索引，解码走 IO 线程 + 24MB LRU 缓存；没图的菜自动回落到渐变插画，不会开天窗。

---

## 四、调抽取概率

概率参数集中在两个地方，改一处要同步另一处：

**App 内**：`app/src/main/java/com/chisha/app/domain/DrawEngine.kt`

```kotlin
const val PENALTY_BASE = 0.6    // 每次 ≤2 星评分，该菜权重乘 0.6
const val PENALTY_FLOOR = 0.15  // 权重下限
```

单道权重定义在 `Models.kt` 的 `Rarity` 枚举：

```kotlin
COMMON("common", "家常", 1.0, ...)
SIGNATURE("signature", "大菜", 0.6, ...)
RARE("rare", "稀有", 0.25, ...)
```

**标定脚本**：`tools/calibrate_weights.py` 里同样的 `W` 字典。改完跑一遍看实际分布：

```bash
python3 tools/calibrate_weights.py
```

当前实际分布（365 道全库）：

| 档位 | 道数 | 档位命中率 | 单道概率 |
|---|---|---|---|
| 家常 | 219 | 74.49% | 0.3401% |
| 大菜 | 110 | 22.45% | 0.2041% |
| 稀有 | 36 | **3.06%** | **0.0850%** |

> 注意：`60/30/10` 是**菜库构成比例**，不是抽取概率。因为 219 道家常菜分走 74% 的概率、每道 0.34%，而 36 道稀有菜只分走 3%、每道 0.085%，只有这样「稀有」才真的稀有。若强行让档位命中率 = 60/30/10，单道稀有菜的概率反而会超过家常菜，稀有度就自相矛盾了。

---

## 五、调等级曲线

`app/src/main/java/com/chisha/app/domain/XpEngine.kt`

```kotlin
// 升到下一级所需经验，索引 0 表示 1→2 级
val needByLevel = intArrayOf(330, 660, 1100, 1650, 2200, 2750, 3300, 3850, 4400)
// 累计 20240，按平均每次 110~120 经验，约 170~190 次完成满级

val titles = listOf("厨房新手", "打下手", ...)  // 10 个等级称号
```

单次经验公式在同文件 `compute()`：

```
基础    难度 1/2/3 → 80 / 110 / 150
稀有加成 家常 +0 / 大菜 +20 / 稀有 +50
首次做   +30
当天首单 ×1.3
三餐全勤 +50
```

**没有成品图就不给经验**，只记一条「已完成（无图）」，逻辑在 `AppViewModel.commitCook()`。

---

## 六、改配色

`app/src/main/java/com/chisha/app/ui/theme/Theme.kt` 顶部一组常量：

```kotlin
val WarmBg = Color(0xFFFFF8F0)      // 米白底
val Ochre = Color(0xFFC8551F)       // 赭石橙主色
val RarityCommon = Color(0xFF98A2AE)    // 家常灰
val RaritySignature = Color(0xFF8E6BBF) // 大菜紫
val RarityRare = Color(0xFFD9A21B)      // 稀有金
```

菜系主色在 `domain/Models.kt` 的 `Cuisine` 枚举里，每个菜系一个 `Color`。

**应用固定使用浅色主题**，不跟随系统深色——这本身是设计决定：深色底会让菜品卡发脏。

---

## 七、成就系统（138 个）

100 个常规成就 + 24 个节气成就 + 12 个节日成就 + 2 个集齐类金成就。

全部写在 `app/src/main/assets/achievements.json`，加第 139 个只需加一行 JSON，不改代码：

```json
{"id":"a11","name":"四百道","desc":"累计完成 400 道菜","group":"cook","tier":"gold","key":"totalCooks","target":400}
```

`key` 必须是 `domain/StatsBuilder.kt` 里计算出来的计数器名，常用的有：

| key | 含义 |
|---|---|
| `totalCooks` / `photos` / `comments` / `ratings` | 各类累计计数 |
| `collection` | 图鉴解锁数 |
| `level` | 当前等级 |
| `streak.best` | 最长连续天数 |
| `cuisine.川菜` | 该菜系完成数 |
| `rarity.rare` / `rarity.signature` | 稀有/大菜完成数 |
| `slot.breakfast` | 早餐名额使用次数 |
| `flag.*` | 布尔型成就（命中即为 1） |

布尔型 `flag.*` 的完整清单见 `StatsBuilder.build()`，包括 `flag.day3slots`（三餐齐）、`flag.before6`（6 点前）、`flag.regret`（1 星后改 5 星）等。

### 节气与节日成就（38 个）

| 分组 | 数量 | key 示例 | 判定 |
|---|---|---|---|
| 二十四节气 | 24 铜 + 1 金 | `term.立冬` / `term.all` | 该节气当天做一道菜 / 集齐 24 个节气 |
| 中国节日 | 3 铜 + 9 银 + 1 金 | `festival.春节` / `festival.all` | 该节日当天做一道菜 / 集齐 12 个节日 |

**关键点：日期不能写死。** 节气每年差一天，农历节日还要换农历，而且「立春早于春节」真实存在（2026 年立春 2 月 4 日，春节 2 月 17 日）。所以流程是：

```
tools/gen_calendar.py  ──►  assets/calendar.json  ──►  CalendarTable.kt 查表
   构建期算一次                 504 个节气 + 249 个节日       运行时零换算
```

`gen_calendar.py` 依赖 `lunar_python`（`pip install lunar_python`），覆盖 **2025-01-01 ~ 2045-12-31**。超出范围时 `CalendarTable.isCovered()` 返回 false，只是不再产生节气成就，不会崩溃或误判。

重新生成：

```bash
pip install lunar_python
python3 tools/gen_calendar.py     # 会打印 2026 年各节日日期做自检
```

首页顶部会自动显示当天的节气或节日（节日优先），例如「今日节气 立冬」「今日 元旦 · 做一道应景的」。

**新增成就时注意**：`id` 必须全局唯一。曾经因为节日成就用 `f01-f08` 撞上了等级成就的 `f01-f08`，导致 Room 里按主键 upsert 互相覆盖 —— 现在节日组统一用 `fe01..fe13` 前缀。

---

## 八、重新构建

本机环境（已装好，重装参考）：

```bash
# JDK 21
export JAVA_HOME="$HOME/tools/jdk-21.0.13+9/Contents/Home"
# Android SDK
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

cd chisha
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

版本组合（2026-10 验证可用）：

| 组件 | 版本 |
|---|---|
| AGP | 9.5.0 |
| Gradle | 9.8.2 |
| Kotlin | 2.4.21 |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00 |
| Room | 2.8.5 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| JDK | 21 |

> AGP 9 起 Kotlin 编译已内置，**不要**再 apply `org.jetbrains.kotlin.android`，否则直接报错；但 `org.jetbrains.kotlin.plugin.compose` 仍要显式 apply。

---

## 九、在模拟器上跑

```bash
avdmanager 不可用（系统镜像为手动安装），直接用已建好的 AVD：

export ANDROID_AVD_HOME="$HOME/.android/avd"
$ANDROID_HOME/emulator/emulator -avd api37 -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect &

adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.chisha.app/.MainActivity

# 截图必须用 exec-out，macOS 26 上模拟器的 gRPC 截图会崩
adb exec-out screencap -p > shot.png
```

---

## 十、已知取舍

1. **353 / 365 道有实拍成品图**。图片来自下厨房的公开菜谱页，逐道按菜名搜索抓取，压成 512×512 WebP 后打进 assets（共 11.6 MB），完全离线可用。剩下 12 道冷门菜（干烧岩鲤、三层套鸡、母油船鸭、清蒸石鸡等）搜不到对得上的图，沿用原来的「菜系主色渐变 + 菜名首字」插画兜底。抓取脚本见下节，重抓或补图改一条命令就行。
2. **图鉴菜名现在显示完整菜名**，网格从 4 列改成 3 列，每格是 1:1 方图 + 菜名 + 状态。
3. **生食类菜谱**（顺德鱼生）有寄生虫风险，菜谱里已明确标注需用鱼生级食材且孕妇小孩不建议吃。
4. **发酵类菜谱**（湘西酸肉、臭鳜鱼）需要提前数天准备，菜谱里写了时间但没做倒计时提醒。
5. **节气表只到 2045 年**。到期后需要重新跑 `tools/gen_calendar.py` 生成更远的年份，否则节气/节日成就不再触发（其余功能不受影响）。
6. **节气成就只认「当天做过菜」**，不校验做的是不是应季菜。想加「立春吃春饼」这类耦合判定，需要给菜谱打应季标签，是另一个量级的工作。
7. **菜谱详细化是分批推进的**。目前 365 道里已完成 9 道「加强版」（带食材处理 / 火候轴 / 翻车表），单道字数约是旧版的 4 倍。其余仍是旧版六段式，功能完全正常，只是没那么细。
8. **APK 体积**从 19 MB 涨到 31 MB，绝大部分是菜品图。想瘦身就把 `dish_photos` 里的图换成更小尺寸（改 `fetch_photos.py` 里的 `cwebp -resize` 参数重跑）。
