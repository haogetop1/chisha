# 修改记录

按时间倒序记录每个版本改动了哪些文件、为什么改。文件都在 `chisha/` 项目根目录下，相对于该目录给出路径。

---

## v1.4 · 食材准备 + 「在超市怎么挑」

**背景**：用户反馈「要买的东西」这个说法不准，而且菜谱里缺一块「在超市怎么挑食材」。

关键判断：如果把挑菜内容只写进已精修的那 9 道菜，等于 365 道里只有 9 道有，等于没补。所以做成**通用挑菜知识库**，让全部菜谱立刻用得上。

| 文件 | 改动 |
| --- | --- |
| `app/src/main/java/com/chisha/app/domain/Models.kt` | `Ingredient` 新增 `pick` 字段（在超市怎么挑） |
| `app/src/main/java/com/chisha/app/data/PickingTips.kt` | **新增**。加载 `assets/picking.json`，按关键词匹配挑菜建议 |
| `app/src/main/java/com/chisha/app/data/SeedLoader.kt` | 解析 `pick`；启动时加载挑菜库 |
| `app/src/main/java/com/chisha/app/ui/screens/RecipeScreen.kt` | 「要买的东西」→「食材准备」；新增「在超市怎么挑」板块（绿标签 + 挑法） |
| `tools/picking.json` | **新增**，152 条「食材关键词 → 怎么挑」，覆盖肉禽/水产/干货/蔬菜/米面/调料 |
| `tools/merge_recipes.py` | 校验挑菜库（k/t 非空、关键词不重复）并同步到 `assets/picking.json` |
| `README.md` | 补 `pick` 字段说明 + 挑菜库小节 |

**匹配规则**（`PickingTips.kt`）：菜谱自带的 `pick` 优先 → 否则按关键词匹配，**长关键词优先**（「五花肉」不会被「猪肉」抢走）→ 单字关键词（鱼/鸡/油/糖）命中面广容易误伤，所以名字里带「汁、酱、粉、精、露」这类调味品后缀时不予匹配。

踩过的坑：没加保护时「鱼香汁」被匹配成了「鱼」的挑法，已修。

**覆盖率**：1405 / 1643 个食材条目 = 86%；365 道菜里 193 道每味食材都有挑法。

---

## v1.3 · 353 张实拍菜品图

**背景**：用户要求菜品图从网上找。

实测：Wikimedia Commons 本机完全不可达（HTTP 000）；Unsplash/Pexels 可达但只有通用西餐素材，中文菜名零命中；小红书图片 CDN 直接 403。最终走下厨房（图床 `i2.chuimg.com` 直连可用）。

| 文件 | 改动 |
| --- | --- |
| `tools/fetch_photos.py` | **新增**。搜 → 解析候选 → 下载 → `cwebp` 压成 512×512 WebP |
| `app/src/main/java/com/chisha/app/data/DishPhotos.kt` | **新增**。`assets.list()` 建索引；IO 线程解码 + 24MB LRU 缓存 |
| `app/src/main/java/com/chisha/app/ui/components/Components.kt` | `RecipeArtwork` 有图就用图（Crop），没图回落渐变插画 |
| `app/src/main/java/com/chisha/app/ui/screens/CollectionScreen.kt` | 网格 4 列 → 3 列，每格 1:1 方图 + 完整菜名 + 状态 |
| `app/src/main/java/com/chisha/app/data/SeedLoader.kt` | 启动时 `DishPhotos.load(context)` |
| `app/src/main/assets/dish_photos/*.webp` | 353 张，11.6 MB（**不入库**，见 `.gitignore`） |

**两道保险**：
1. **严格匹配**——只有候选标题里含这道菜名的才采用。第一轮跑出 25 张错图（「干烧岩鲤」抓成奶酪片、「母油船鸭」抓成小鱿鱼），**错图比没图更糟**，所以改成对不上就不要；删掉后只把人工复核确认可用的 13 张（如「辣椒炒蛋」↔「辣椒炒鸡蛋」）加回来。
2. **兜底插画**——12 道冷门菜搜不到对得上的图，沿用菜系渐变插画，不开天窗。

体积影响：APK 19 MB → 31 MB。

---

## v1.2 · 菜谱详细化 + 修「返回后又自动抽奖」的 bug

两件事同一版。

### 2a. Bug：从菜谱页返回后会自动重进抽取页

**现象**：菜谱页按返回，老虎机又滚了一遍。

**根因**：`recipe/{id}` 只是 pop 回 `draw`，而 `draw` 这个 destination 一进 composable 就触发滚动动画。

**修法**（两处配合）：
- `ui/MainActivity.kt`：确认菜谱时 `popUpTo("home") { inclusive = false }`，把 `cuisine` / `draw` 一起弹出栈，返回直接回首页。
- `ui/AppViewModel.kt`：`DrawSession` 增加 `seq`（每真抽一次 +1）和 `animated`（动画是否已播过），并新增 `markSessionAnimated()`。
- `ui/screens/DrawScreen.kt`：`remember(session.seq)` 重置滚动状态，`animated == true` 时直接停在结果位、不再滚。

验证：抽奖 → 菜谱 → 返回，回到首页「晚上好，今天吃啥」，不重抽。

### 2b. 菜谱详细化（结构改造）

| 文件 | 改动 |
| --- | --- |
| `domain/Models.kt` | `Ingredient.prep`（买回来怎么处理）；新增 `HeatStep`、`FailCase`；`Recipe.heat` / `Recipe.fails` / `Recipe.isRich` |
| `data/SeedLoader.kt` | 解析 `prep` / `heat` / `fails`；**菜谱改为 CRC32 内容指纹自动重灌** |
| `data/Daos.kt` | `RecipeDao` 新增 `upsertAll`（按 id 覆盖，评分与做菜记录不受影响） |
| `ui/components/RecipeExtras.kt` | **新增**。`HeatTimeline`（左侧圆点按火候配色）、`FailTable`（红「症状」+ 绿「补救」）、`ExtraSection` |
| `ui/screens/RecipeScreen.kt` | 食材下加橙色「处理：」行；新增火候时间轴、翻车对照表 |
| `ui/screens/TutorialScreen.kt` | 「火候节奏」置顶——做菜时抬头第一眼要看的就是火多大 |
| `tools/enrich.py` | **新增**。按 id 打补丁，只覆盖指定字段，不破坏分卷顺序、不撞 id |
| `tools/enrich/batch01~04.json` | 9 道加强版菜谱，全部经网络查证 |
| `tools/merge_recipes.py` | 校验 v2 字段；打印「详细度」统计 |

**为什么要 CRC32 指纹**：原来 `seedRecipes` 是 `if (count > 0) return`，老用户升级后永远看不到新菜谱。改成比对 `assets/recipes.json` 的 CRC32，内容变了就按 id 覆盖一遍，不用手动维护版本号。已实测：直接 `adb install -r` 覆盖老包，内容正常换新。

**详细度**：加强版平均 2.89k 字符 vs 旧版 0.71k，约 **4.1 倍**。已完成 9 道：麻婆豆腐、白切鸡、剁椒鱼头、红烧肉、西湖醋鱼、小笼包、佛跳墙、松鼠鳜鱼、葱烧海参。

---

## v1.1 · 二十四节气 + 中国节日成就

| 文件 | 改动 |
| --- | --- |
| `domain/CalendarTable.kt` | **新增**。节气/节日查表；`load()` / `termOf()` / `festivalOf()` / `todayInfo()` |
| `domain/StatsBuilder.kt` | 新增 `term.*` / `festival.*` / `term.all` / `festival.all` 计数器 |
| `domain/AchievementEngine.kt` | 分组新增「二十四节气」「中国节日」 |
| `assets/achievements.json` | 100 → **138** 个（新增 `t01`–`t25`、`fe01`–`fe13`） |
| `assets/calendar.json` | **新增**。2025–2045 年，504 个节气 + 249 个节日 |
| `tools/gen_calendar.py` | **新增**。用 `lunar_python` 在构建期算好，运行时只查表 |
| `data/SeedLoader.kt` | 成就初始化改为**幂等补齐**（只插入缺失定义，不动已有进度）；启动时 `CalendarTable.load()` |
| `ui/screens/HomeScreen.kt` | 今日节气/节日标签；成就数从硬编码 `/100` 改为取实际值 |
| `ui/screens/ProfileScreen.kt` | 同上 |

**两个坑**：
1. **不能硬编码日期**。节气每年差一天，且立春可能早于春节（2026 年立春 2/4，春节 2/17）。所以构建期用 `lunar_python` 算好存表，运行时只查。
2. **成就 ID 撞车**。新加的节日成就用了 `f01`–`f08`，和已有的等级组 `f01`–`f08` 撞了——Room 按主键 upsert 会把旧的覆盖掉。改成 `fe01`–`fe13`。

顺带修的：`seedAchievements` 原来是 `if (count > 0) return`，老用户升级永远看不到新成就；改成幂等补齐后，实测 8/100 → 8/138，不用清数据。

---

## v1.0 · 首个可安装版本

从零搭起整套工具链与 App：

- **工具链**（全部装在 `$HOME` 下）：JDK 21（清华镜像）、Gradle 9.8.2（腾讯镜像）、Android SDK 37 + build-tools 36、模拟器与 AVD（官方 zip 手动装 + 手写 `config.ini`）。
- **App**：4 个 Tab（首页 / 图鉴 / 记录 / 我的），选菜系 → 老虎机抽菜 → 六段教程 → 拍照 → 结算经验升级 → 解锁成就。
- **抽取算法**：`DrawEngine.kt` 权重 + 评分惩罚（`PENALTY_BASE=0.6`，`PENALTY_FLOOR=0.15`）。实测分布 74.49% / 22.45% / 3.06%。
- **数据层**：Room（`fallbackToDestructiveMigration`），365 道菜 + 138 个成就。
- **拍照**：`PhotoStore.kt`，FileProvider + 长边 1280 + JPEG q85。

---

## 已知取舍（写进 README 第十节）

1. 353 / 365 道有实拍图，12 道冷门菜走渐变插画兜底。
2. 菜谱详细化分批推进，目前 9 道加强版，其余仍是旧版六段式（功能正常，只是没那么细）。
3. 节气表只到 2045 年，到期需重跑 `gen_calendar.py`。
4. 节气成就只认「当天做过菜」，不校验做的是不是应季菜。
5. APK 31 MB，绝大部分是菜品图。
