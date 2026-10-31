// 实验报告 Typst 源文件（由 GameLib-report.md 翻译而来）
// 编译：.tools/typst-x86_64-unknown-linux-musl/typst compile GameLib-report.typ
// 模板原文备份：GameLib-report.typ.bak

// ===== 全局样式 =====

// 代码块：浅灰底、小字号、等宽字体
#show raw.where(block: true): set block(
  fill: luma(250),
  inset: 6pt,
  radius: 3pt,
)
// 原模板此处是 ("Courier New", "NSimSun", "SimSun", "Microsoft YaHei UI")，
// 后三个是 Windows 字体，本机没有；换成 Noto Sans Mono CJK SC 覆盖中文。
#show raw.where(block: true): set text(
  size: 9pt,
  font: ("Courier New", "Noto Sans Mono CJK SC"),
)
#show raw.where(block: false): set text(
  font: ("Courier New", "Noto Sans Mono CJK SC"),
)
#show raw.where(block: true): set par(leading: 1.15em)

// 表格：统一边框、字号与行距
#show table: set table(stroke: 0.2pt)
#show table.cell: set text(size: 12pt)
#show table.cell: set par(leading: 0.3em, spacing: 0.5em)
#show table.cell.where(y: 0): set block(above: 0.5em, below: 0.5em)

// 公式：块级公式加浅灰底、降字号；行内公式仅降字号
#show math.equation.where(block: true): set text(size: 12pt)
#show math.equation.where(block: true): it => block(
  fill: luma(245),
  inset: 8pt,
  radius: 3pt,
)[#it]
#show math.equation.where(block: false): set text(size: 12pt)

// 页面：A4、页码、页眉页脚
#set page(
  paper: "a4",
  margin: (left: 2.6cm, right: 2.6cm, top: 2.4cm, bottom: 2.8cm),
  numbering: "1",
  number-align: bottom + center,
  header: context [
    #text(size: 12pt, fill: gray.darken(25%))[ZJU JAD]
    #h(1fr)
    #text(size: 12pt, fill: gray.darken(25%))[#datetime.today().display("[month repr:short] [day], [year]")]
  ],
  footer: context align(center)[
    #text(size: 11pt, fill: gray.darken(50%))[#counter(page).display()]
  ],
)

// 正文：字体、字号、段落
// 原模板是 ("Times New Roman", "Georgia")，没有中文字体；
// 追加 Noto Serif CJK SC 让中文有稳定的衬线回退，避免落到系统默认。
#set text(
  font: ("Times New Roman", "Georgia", "Noto Serif CJK SC"),
  size: 14pt,
  lang: "en",
)
#set par(
  justify: true,
  first-line-indent: 0em,
  leading: 0.75em,
  spacing: 1em,
)

// 标题层级
// 一级标题（大章节）自动从新页开始：weak pagebreak 在标题已处于页首时自动折叠，不产生空白页
#set heading(numbering: "1.")
#show heading.where(level: 1): it => [
  #pagebreak(weak: true)
  #v(0.9em)
  #text(size: 20pt, weight: "bold", it.body)
  #v(0.35em)
]
#show heading.where(level: 2): it => [
  #v(0.55em)
  #text(size: 15pt, weight: "semibold", it.body)
  #v(0.25em)
]
#show heading.where(level: 3): it => [
  #v(0.35em)
  #text(size: 13pt, weight: "semibold", it.body)
  #v(0.15em)
]

// 图表标题
#show figure.caption: it => [
  #set text(size: 10pt, fill: gray.darken(40%))
  #it.supplement
  #context it.counter.display(it.numbering)
  #it.separator
  #it.body
]

// ===== 封面 =====
#align(center + horizon)[
  #v(0%)
  #text(size: 35pt, weight: "bold")[
    大程一
    \
    个人游戏库存管理系统
  ]
  #image("icon_ZJU.png", width: 30%)
  #v(1.6em)
  #text(size: 20pt)[作者：王传宇 3250102681]
  #v(0.5em)
  #text(size: 20pt)[指导教师：翁恺]
  #v(0.5em)
  #text(size: 20pt)[时间：2026-10-31]
]

#pagebreak()

// ===== 正文从此处开始 =====

= 一、需求与目标

== 1.1 问题

一名跨平台玩家在 Steam、Epic、GOG、Xbox 等平台累计购买上百款游戏后，会出现四类问题：
忘了买过什么（导致重复购买）、不知道玩哪个（买了不玩）、算不清花了多少钱（跨平台无法汇总）、
记不住什么时候通关的（年度回顾缺失）。

本系统把这些信息收进一个完全离线的本地文件里：不联网、不调用 Steam API、不使用任何第三方库、
不使用数据库。所有持久化、CSV 解析、表格对齐都由 JDK 自带 API 手写实现。

== 1.2 §2 五个场景与实现的对应关系

#table(
  columns: (0.4fr, 2.6fr, 3.2fr, 2.6fr),
  table.header([\#], [题目中的场景], [实现方式], [复现方式]),
  [1], [我昨天 Epic 领的《GTA5》入库了吗？],
  [`GameLibrary.contains(title, platform)`], [菜单 3 → 按标题搜索],
  [2], [我 Steam 上有哪些还没玩过的开放世界游戏？],
  [`filter(g -> g.getPlatform() == STEAM && !g.isCompleted())`],
  [菜单 3 → 2（平台）或 4（通关状态）],
  [3], [我总共在 Steam 花了多少钱？], [`Stats.spendByPlatform`],
  [菜单 6，看「最烧钱的平台」],
  [4], [我 2024 年通关了哪些游戏？],
  [`Game.completedDate.getYear()` 聚合为 `Map<Integer,Integer>`],
  [菜单 6，看「年度通关」],
  [5], [别让我再买《巫师3》了，我已经在 GOG 和 Steam 各买了一次],
  [唯一键 = 规范化标题 + 平台], [同平台重复入库被拒绝，不同平台可以共存],
)

第 5 个场景是整套数据结构设计的起点：「同一款游戏」不等于「同一款游戏在同一平台」，
所以唯一键必须是「标题 + 平台」的复合键，而不是标题。

== 1.3 交付物

#table(
  columns: (1fr, 4fr),
  table.header([文件], [说明]),
  [`GameLib.zip`],
  [可执行 `game-lib.jar` + 完整源码 + `build.sh` / `test.sh` / `README.md`],
  [本实验报告], [——],
)

运行环境：JDK 17 或更高。源码以 `javac --release 17` 编译，因此字节码可在较老的 JDK 上运行。

= 二、系统架构

== 2.1 包结构与单一依赖方向

```text
src/gamelib/
├── Main.java        57 行    程序入口，只做接线
├── model/          324 行    Game、Platform、异常        ← 不依赖任何其他包
├── store/          244 行    GameLibrary 内存索引、批量导入
├── io/             581 行    Csv、GameCsv、LibraryFiles、LoadReport
├── xor/             21 行    Xor 存档混淆（§5 扩展）
├── ui/             780 行    Menu、Console、GameFormatter、Stats
└── test/          2633 行    测试套件
```

依赖方向是单向的：

```text
ui  ──>  store  ──>  model
 │                    ↑
 ├──>  io  ───────────┘
 │      │
 │      └──>  xor       （只有 io 用它）
```

model 不依赖任何包，因此它是唯一可以被独立测试和独立理解的层。
io 与 store 互不依赖：io 只把「文件 ↔ 字符串 ↔ `List<Game>`」转换好，
「这些游戏怎么放进索引、重复怎么办」则是 store 的任务。
xor 则是最底层的一个纯字节变换。

== 2.2 为什么这样分层

#table(
  columns: (2fr, 1fr, 3.4fr),
  table.header([关注点], [归属], [判断依据]),
  [什么叫合法的游戏数据], [`model`], [不依赖文件、不依赖菜单、不依赖用户],
  [同一款游戏存在了吗], [`store`], [需要看到整个库，但不需要看到文件],
  [文件里那串字节是什么意思], [`io`], [需要知道编码、BOM、转义、序列化],
  [怎么问用户、怎么显示], [`ui`], [需要 `System.in` / `System.out`],
)

这套划分的直接收益是测试可以自下而上做：model 与 io 的测试完全不需要构造菜单，
也不需要碰真实工作目录（io 的路径由构造器注入，测试写进临时目录）。
而 ui 的测试可以只驱动菜单、不碰真实磁盘。

== 2.3 两个可注入的设计

#table(
  columns: (1.2fr, 1.8fr, 3.4fr),
  table.header([类], [注入什么], [不注入的后果]),
  [`LibraryFiles`], [三个 `Path`], [测试会往源码目录写 `game_lib.dat`，污染仓库],
  [`Console`], [`Scanner` + `PrintStream`], [菜单的输出无法被断言，只能靠人工点击测试],
)

Console 的注入是整个项目里性价比最高的一处设计：它让 7 个菜单分支、输入校验重试、
确认提示、持久化全部可以用一段脚本文本驱动（MenuTest 就是这么做的）。

= 三、核心设计决策

每一条核心设计决策的结构是：问题 → 选择 → 理由 → 代价。

== 3.1 通关状态：只存一个可空日期，不存布尔标志

*问题*：§4.5 要求「年度通关数（如 2024 年通关了 15 款）」，但 §4.1 的入库输入里只有
「是否已通关 (y/n)」，没有任何日期。「购买年份」不等于「通关年份」。

*选择*：字段只保留 `private LocalDate completedDate`，null 即未通关；
`isCompleted()` 是派生方法 `return completedDate != null;`。

*理由*：这样不可能出现「已完成但无日期」这种矛盾态。

*代价*：CSV 里 completed 这一列成了派生列（写出来给人和 Excel 看），导入时必须忽略它。这个约定写在 GameCsv 的注释里，并有测试固定
（「completed 列说 y 但日期为空 → 仍视为未通关」）。构造器不能收 completedDate，
读档只能先构造 Game 对象、再 `markCompleted()` 。

== 3.2 唯一键：规范化标题 + 平台

*问题*：`Map<String, Game>` 若只用标题作键，GOG 版《巫师3》和 Steam 版会互相覆盖。

*选择*：`Game.key()` 返回「规范化标题 + 平台名」。查重、equals、hashCode、CSV 索引全部经由这一个方法。

*理由*：§2 明确存在「同一款游戏在两个平台各买一次」的场景。同时标题要小写化，
因为 §2 的模糊搜索本身就大小写不敏感（输入 witch 命中 The Witcher 3），
那么重复入库的判断也必须同样宽松。

*代价*：title 与 platform 必须是 final（见 3.3）。

== 3.3 equals / hashCode 只比 key，且键字段必须 final

*问题*：IDE 自动生成的 equals 会比较全部字段。

*选择*：equals 与 hashCode 都委托给 `key()`。

*理由*：若比较全部字段会出现缺陷——调用过 `addPlaytime(5)` 之后，
同一个游戏在 HashSet 里就找不到了。表现为 remove 删不掉、size() 不减少。

*代价*：title、platform 必须 final，否则对象会在哈希容器里失联。本项目的更新操作从不改这两个字段，因此这个约束天然成立。

== 3.4 Optional 只能作返回值，不能作字段

*问题*：completedDate 是「可能没有值」的。

*选择*：字段用裸 LocalDate + null 表示缺失。

*理由*：Optional 没有实现 Serializable。

*代价*：可空字段的类型上看不出来，只能在字段声明处写明「null = 未通关」。

== 3.5 平台有两个字符串：name() 给机器，label 给人

*问题*：既要显示成 Steam，又要保存成永不变化的值。

*选择*：常量名给机器，构造参数给显示。`name()` 返回final的全大写常量名，
`toString()` 被覆盖为返回 label。

*理由*：label 是可变的显示名。若显示名发生更改（Xbox → Xbox Game Pass），旧存档可能无法读回，所以我们只能用冻结的 `name()`。

*代价*：覆盖 toString() 不影响 valueOf——这两个方法不互逆。因此必须自己写
`Platform.fromString`，同时匹配 name() 和 label。

== 3.6 CSV 不只是导出格式，还是存档的回退路径

*问题*：CSV 可以当「导出给 Excel 看」的一次性产物，也可以当「存档」。

*选择*：按存档对待。平台列写 name()，而不是 label。

*理由*：§4.6 明确要求「先尝试 game_lib.dat，若不存在则尝试 game_lib.csv」。既然 CSV 会被读回来当数据用，它就必须长期稳定。

*代价*：CSV 的列定义成了对外契约，表格结构改变需要同步改读写两侧。

== 3.7 派生结构不持久化

*问题*：GameLibrary 内部是 `Map<String, Game>`，键是从 Game 算出来的派生值。

*选择*：GameLibrary 不实现 Serializable。存档里只写 `List<Game>`，
索引在加载时重建。

*理由*：若把 Map 一起序列化会有两个问题：

+ 同一份信息存两遍（键能从 Game 算出来）；
+ GameLibrary 没有 serialVersionUID，日后功能拓展可能会改变 UID。

*代价*：每次启动都要重建索引。

== 3.8 序列化不调用构造器：两条加载路径的校验强度不同

*问题*：Game 的构造器有 7 处校验，但反序列化不执行——ObjectInputStream 直接往字段写字节。

*选择*：仅在 `LibraryFiles.readBinary` 里用 instanceof 逐元素确认类型。

*理由*：文件由程序自己写，出错风险很低，读档后没必要重新校验。

*代价*：game_lib.dat 的校验强度低于 game_lib.csv。

== 3.9 加载失败不静默降级

*问题*：game_lib.dat 读不出来时是否应该返回空库。

*选择*：抛异常，拒绝启动。

*理由*：空库会让用户以为数据丢了，一旦选择保存旧档就会被覆盖。

具体策略（`LibraryFiles.load`）：

#table(
  columns: (2.4fr, 3.6fr),
  table.header([情况], [行为]),
  [game_lib.dat 读成功], [用二进制，来源记为 BINARY],
  [game_lib.dat 坏，game_lib.csv 好], [降级到 CSV，原因写进 warnings],
  [两个都坏], [抛异常，拒绝以空库启动],
  [两个都不存在], [返回空库，来源记为 EMPTY],
)

== 3.10 保存前备份、原子替换

*问题*：直接写 game_lib.dat，中断时存档损坏。

*选择*：

```text
1. 若 game_lib.dat 已存在 -> 复制为 game_lib_backup.dat
2. 写入 game_lib.dat.tmp
3. Files.move(tmp, game_lib.dat, REPLACE_EXISTING)   // 原子替换
```

*理由*：Files.move 在同一文件系统上是原子操作，任何时刻磁盘上至少有一份完整存档。

*代价*：多一次磁盘写入。

= 四、§3 六个技术点的实现

+ *文本文件 I/O*：导出 game_lib.csv（UTF-8 带 BOM，Excel 可直接打开），也可作为存档回退加载。
  `io/LibraryFiles.java`、`io/GameCsv.java`
+ *二进制文件 I/O*：读写 game_lib.dat，写入采用备份 + 临时文件 + 原子替换。
  `LibraryFiles.saveBinary`
+ *对象串行化*：`Game implements Serializable` + 显式 `serialVersionUID = 1L`，
  一行 `writeObject(List<Game>)` 保存整个库。`model/Game.java`
+ *异常处理*：34 处抛出点、20 处捕获；CSV 导入的每个错误带行号与列名；UI 层统一三层 catch。
  `model/InvalidGameDataException.java`
+ *Optional*：`find` 返回 `Optional<Game>`，永不返回 null；空库时 `mostExpensivePlatform` 返回 empty。
  `store/GameLibrary.java`、`ui/Stats.java`
+ *Collections / Map*：`Map<String, Game>` 做索引，`List<Game>` 做结果，
  `Map<Platform, List<Game>>` 做分组，`Map<Integer, Integer>` 做年度统计。
  `store/GameLibrary.java`、`ui/Stats.java`

== 4.1 Optional 的两个易错点

+ 作为返回类型时绝不能返回 null：否则调用方的链式写法会抛 NPE，而栈指向调用点。GameLibraryTest 有断言。
+ 不用它包集合：空 List 已能表达「没找到」，`Optional` 多余了。

== 4.2 Collections 的顺序考量

#table(
  columns: (2.2fr, 1.3fr, 3.3fr),
  table.header([数据结构], [选型], [理由]),
  [`GameLibrary.byKey`], [`LinkedHashMap`],
  [保持入库顺序，「查看全部」的输出才稳定可复现],
  [`Stats.completedByYear`], [`TreeMap`], [年份按数值升序输出],
  [`Stats.countByPlatform`], [`TreeMap`],
  [enum 的 compareTo 按 ordinal，迭代顺序即声明顺序，柱状图排列固定],
)

HashMap 只用于顺序不会被观察到的地方。

= 五、§4 七个功能模块与 §5 可选扩展

以下输出均为实际运行结果。

== 5.1 游戏入库（§4.1）

```text
=> 添加游戏
 1) 手动录入
 2) 从 CSV 文件批量导入
> 1
平台 (Steam/Epic/GOG/Xbox/Other): Steam
标题: Elden Ring
类型: ARPG
购买价格 (¥): 298
购买日期 (yyyy-MM-dd): 2022-03-01
是否已通关 (y/n): y
游玩时长 (h): 120
个人评分 (0-10): 10
笔记 (可直接回车): 神作，DLC快出！
【提示】已成功入库：Elden Ring | Steam
```

*校验规则*：价格 ≥ 0（0 合法）、购买日期 ≤ 今天、评分 0–10（两端合法）、时长 ≥ 0、标题/类型/平台非空。任一必填字段留空即取消本次操作。

*重复入库*：

```text
【警告】该游戏已存在！请勿重复入库。 Elden Ring | Steam
```

*批量导入*：读任意路径的 CSV，交给 `GameLibrary.addAll`，重复项跳过并计数。

```text
CSV 文件路径: /path/to/incoming.csv
导入完成：新增 1 款，跳过 1 款重复
  重复：Elden Ring | Steam
  当前库里共 2 款游戏。
```

== 5.2 游戏查询（§2 / §4.2）

五种筛选条件（标题模糊、平台、类型、通关状态、价格区间），以及任意组合的复合条件。

```text
=> 查看全部
共 3 款游戏
1. Elden Ring | Steam | ¥298 | 120h | 评分 10 | 已通关 | 神作，DLC快出！
2. GTA5       | Epic  | ¥0   | 25h  | 评分 8  | 未通关 | Epic白嫖
3. 巫师3      | GOG   | ¥99  | 60h  | 评分 9  | 未通关 | 剧情神作
```

*对齐*：巫师3 是 3 个字符但占 5 列，用 String.length() 补齐会参差。`GameFormatter.width()` 按显示列宽补齐（汉字与全角符号算 2 列）。

复合查询用谓词组合，不需要新方法：

```java
lib.filter(g -> g.getPlatform() == Platform.STEAM && !g.isCompleted());
```

== 5.3 游戏更新（§4.3）

四项操作各有独立入口：

```text
=> 更新游戏
输入要更新的「标题|平台」: GTA5|Epic
  当前：GTA5 | Epic（25.0h，评分 8，未通关）
 1) 修改游玩时长    2) 标记为已通关
 3) 更新个人评分    4) 记录笔记
 0) 返回主菜单
> 2
通关日期 (yyyy-MM-dd，回车 = 2026-10-20): 2024-03-15
【提示】已标记通关，日期 2024-03-15
```

注意：通关日期可补录；更新不需要写回：find 返回的就是 LinkedHashMap 里的那个对象。

== 5.4 游戏删除（§4.4）

永久删除需确认，隐藏可恢复。

```text
=> 删除游戏
输入要操作的「标题|平台」: GTA5|Epic
 1) 永久删除（不可恢复）
 2) 隐藏（可恢复，§4.4 扩展）
> 1
确认永久删除 GTA5 | Epic ? [y/N]
```

此处任何非肯定输入都返回 false。

隐藏项不出现在查看全部、搜索、统计和分组里，但仍能被 find 找到（否则无法取消隐藏）和删除。全部隐藏时给出提示。

== 5.5 数据统计（§4.5）

```text
-------------  个人游戏资产  -------------
总游戏数        : 3
总花费          : ¥397.00
平均评分        : 9.0
各平台数量：
  Steam  ████████████ 1
  Epic   ████████████ 1
  GOG    ████████████ 1
已通关          : 1 款
未通关          : 2 款
年度通关：
  2026 年 : 1 款
最烧钱的平台    : Steam（¥298.00）
-----------------------------------------
```

三处细节：

+ *空库平均评分返回 0.0*
+ *标签列用显示宽度补齐*：「总游戏数」8 列、「最烧钱的平台」12 列，`%-16s` 会错位。
+ *柱状图至少 1 格*：`12 * 1 / 100` 按整数除法是 0 格。

== 5.6 数据持久化（§4.6）

启动时：

```text
正在加载游戏库 ...
  二进制存档 'game_lib.dat' 不存在。
  已从 'game_lib.csv' 加载 3 款游戏。
===================================================
```

退出时：

```text
=> 保存并退出
请选择保存格式：
 1) 二进制（快速加载，私有格式）
 2) CSV（可被 Excel 打开）
> 1
正在写出 -> game_lib.dat ...  完成！
```

CSV 前三个字节是 EF BB BF（BOM），Windows Excel 打开不乱码：

```text
title,platform,genre,purchase_price,purchase_date,completed,completed_date,
playtime_hours,rating,note,hidden
Elden Ring,STEAM,ARPG,298.0,2022-03-01,y,2026-10-20,120.0,10,神作，DLC快出！,false
GTA5,EPIC,Action,0.0,2020-05-14,n,,25.0,8,Epic白嫖,false
巫师3,GOG,RPG,99.0,2018-06-01,n,,60.0,9,剧情神作,false
```

共 11 列（表头在此折行显示）。
== 5.7 用户界面（§7）

+ 文本菜单 + 数字选择
+ 非法输入重问该字段，不退出程序
+ 快速命令 add / search <关键词> / stats / exit
+ Ctrl-D 退出

== 5.8 §5 可选扩展

=== 5.8.1 完成情况

#table(
  columns: (0.3fr, 3fr, 1.4fr),
  table.header([\#], [§5 的扩展项], [状态]),
  [1], [愿望单（Wishlist，单独存储）], [未实现],
  [2], [游戏封面路径（本地图片，可选填）], [未实现],
  [3], [批量导入 Steam 购买记录],
  [*部分*：实现了通用 CSV 导入],
  [4], [游玩倒计时（想在今年通关）], [未实现],
  [5], [数据加密（简单 XOR）], [*已实现*],
  [—], [（§4.4 的扩展）隐藏而非物理删除], [*已实现*],
)

已实现的两项都不改动已有数据结构。

=== 5.8.2 XOR 原理

异或：两个位不同则为 1。

a xor 0 = a，a xor a = 0，故密钥 k 作用两次还原：

$ (a op("xor") k) op("xor") k = a op("xor") (k op("xor") k) = a op("xor") 0 = a $

*XOR 自逆，加密与解密是同一运算*，只需一个方法：

```java
public static byte[] xor(byte[] data) {
    byte[] out = new byte[data.length];
    for (int i = 0; i < data.length; i++) {
        out[i] = (byte) (data[i] ^ KEY[i % KEY.length]);   // 密钥循环使用
    }
    return out;
}
```

逐字节替换，长度与结构不变。

=== 5.8.3 接入

只改 `LibraryFiles` 的 saveBinary 与 readBinary 两个方法。

*写入*：ObjectOutputStream 的输出与流位置相关，而 XOR 需要完整数组，故先序列化到内存：

```java
ByteArrayOutputStream buffer = new ByteArrayOutputStream();
try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
    out.writeObject(new ArrayList<>(games));
}
Files.write(tmp, Xor.xor(buffer.toByteArray()));
Files.move(tmp, binaryPath, StandardCopyOption.REPLACE_EXISTING);
```

*读取*对称：

```java
byte[] decrypted = Xor.xor(Files.readAllBytes(binaryPath));
try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(decrypted))) {
    // ... 原有反序列化逻辑不变
}
```

=== 5.8.4 效果

未混淆时 strings 可读出字段名：

```text
hiddenD
purchasePriceI
ratingL
genret
notesq
platformt
purchaseDateq
titleq
```

混淆后一个都读不到，魔数也不在：

```text
明文: AC ED 00 05 73 72 00 13 ...
密文: BF 8A 78 94 60 15 78 82 ...
      （AC xor 13 = BF，ED xor 67 = 8A，00 xor 78 = 78，05 xor 91 = 94）
```

程序侧无感。

=== 5.8.5 强度：混淆而非加密

+ *密钥硬编码*：在 `gamelib/xor/Xor.java`，反编译可见。
+ *已知明文可反推密钥*：序列化流固定以 AC ED 00 05 开头。

实际价值只有一条：strings 或十六进制编辑器随手一瞥看不出内容。

=== 5.8.6 兼容性

加密前后的存档格式不兼容，旧的明文 game_lib.dat 会被判为损坏。

#table(
  columns: (2.4fr, 3.6fr),
  table.header([情况], [行为]),
  [旧明文 .dat + 有 game_lib.csv], [打印警告后回退 CSV，数据不丢],
  [旧明文 .dat + 无 .csv], [拒绝启动],
)

后一种是刻意的：宁可启动失败，也不以空库覆盖旧档。

= 六、关键实现细节

== 6.1 CSV 手写解析

禁止第三方库，RFC 4180 必须手写。整行 split 不够，原因有三：

+ 字段可能含半角逗号（笔记「剧情神作, DLC别买」）；
+ 字段可能含双引号；
+ split 会丢掉末尾空字段：a,b,, 被切成 2 个。

我们使用逐字符扫描的方法，三个状态：

```java
if (c == '"' && atFieldStart) { inQuotes = true; }
else if (c == ',')            { 结束当前字段 }
else                          { field.append(c); }
```

引号内遇到双引号时看下一个字符：还是双引号则是字面引号（消费两个、追加一个），否则是字段收尾。CSV 没有反斜杠转义。

*验收标准*：round-trip。解析(格式化(f)) 必须等于 f。

*简化*：字段内不支持换行。解析器在引号未闭合时抛异常，不静默产出脏数据。

== 6.2 两个方向必须对称

#table(
  columns: (2.4fr, 3.6fr),
  table.header([写出时做], [读入时要撤]),
  [最前面加 BOM], [stripBom 剥掉],
  [需要时给字段加引号、内部引号翻倍], [解析状态机还原],
  [null 写成空串], [空串读成 null],
  [每条记录末尾换行], [按换行切分时去掉],
)

任何一项只做一半，round-trip 就不成立。

== 6.3 报错必须能定位

裸的 JDK 异常：

```text
java.lang.NumberFormatException: For input string: "abc"
java.time.format.DateTimeParseException: Text '2022-13-01' could not be parsed
```

经 GameCsv 之后：

```text
CSV 第 7 行「purchase_price」列无法解析：'abc'，应为 数字
CSV 第 8 行「purchase_date」列无法解析：'2022-13-01'，应为 yyyy-MM-dd
CSV 第 13 行：标题不能为空
```

*行号是物理行号*（第 1 行是表头，空行计数），用户可按行号定位。

单列转换失败带列名；Game 领域规则（价格非负、标题非空、日期非未来）无法归到某一列，只保证行号。

== 6.4 中英文宽度

String.length() 数的是 UTF-16 码元，终端数的则是显示列。两者标准不同导致「神作」length() 是 2 但占 4 列。

查阅文档得知四个特殊区间：

#table(
  columns: (1.8fr, 1.4fr, 1.8fr),
  table.header([区间], [覆盖], [例子]),
  [`U+3000–U+303F`], [CJK 标点], [。　「],
  [`U+4E00–U+9FFF`], [汉字], [神　作　巫],
  [`U+FF00–U+FF60`], [全角 ASCII], [，　！　（],
  [`U+FFE0–U+FFE6`], [全角符号], [￥],
)

出于简便性考虑，本项目只覆盖中英 + ASCII。

== 6.5 UI 的三个 catch

#table(
  columns: (auto, auto, auto),
  table.header([异常], [含义], [处理]),
  [`Console.CancelledException`], [必填字段留空], [打印「已取消」，回主菜单],
  [`InvalidGameDataException`], [数据非法 / 重复入库],
  [打印警告消息，消息可直接展示],
  [`UncheckedIOException`], [存档读写失败],
  [打印错误消息并提示重试，不退出程序],
)

*保存失败不能退出*，否则数据没保存程序却关了。

注意：DuplicateGameException 继承 InvalidGameDataException 是为了让 UI 只需一个 catch。若继承 RuntimeException，异常会穿出 main，用户看到栈追踪而不是提示，且数据尚未保存。

= 七、测试与验证

题目禁止第三方库，用不了 JUnit，测试框架是手写的 TestRunner（check / expectThrows / expectInvalid + 通过失败计数），共 12 个测试类。

```text
./test.sh
...
通过 673 项，失败 0 项
```

Console 的流可注入，所以 7 个菜单分支用一段脚本文本端到端驱动：

```java
new Console(new Scanner(new StringReader("1\n1\nSteam\nElden Ring\n...")),
            new PrintStream(buffer, true, StandardCharsets.UTF_8));
```

断言集中在「编译器不报错但行为错」的地方：Optional 返回 null、equals 误比全部字段、整行 split 丢末尾空字段、求最大值初值取 0、保存误用 all() 造成数据丢失、EOF 导致重试循环空转。

手动验收：zip 解压后构建并跑测试；删除 .dat 确认回退 CSV；制造重复行确认启动报告跳过条数；xxd 确认 BOM 与 .dat 不以 AC ED 开头。

= 八、已知限制与取舍

#table(
  columns: (0.3fr, 2fr, 2.2fr, 2.6fr),
  table.header([\#], [限制], [影响], [为什么接受]),
  [1], [CSV 字段不支持换行], [外部含换行的 CSV 会报「引号未闭合」],
  [录入路径产生不了换行；报错而非静默损坏],
  [2], [显示宽度只覆盖中英 + ASCII], [日文/韩文游戏名的表格会歪],
  [作业范围内不会出现；补区间即可],
  [3], [读取二进制存档时不重新校验], [存档被手工改过会读进非法的 Game 对象],
  [文件由程序自己写；重校验会让二进制失去速度优势],
  [4], [Main 的加载段没有单元测试], [「重复行被跳过」的提示只有手动验证过],
  [需要重定向 System.in/out 并固定工作目录，代价高于收益],
  [5], [类名 Console 与 java.io.Console 重名], [全项目禁止使用 `import java.io.*`],
  [显式 import 即可避开；改名要动 4 个文件],
  [6], [价格用 double], [大量求和时可能出现浮点尾差],
  [显示时用两位小数格式化；作业规模下不影响判断],
  [7], [isWide 参数是 char 而非码点], [BMP 之外的字符按代理对拆成两个各算 1 列],
  [常见 emoji 碰巧得到 2 列；中文都在 BMP 内],
  [8], [加密前的旧 game_lib.dat 读不出来], [有 CSV 则回退，无 CSV 则拒绝启动],
  [引入 XOR 的必然结果；拒绝启动好过以空库覆盖旧档],
  [9], [XOR 密钥硬编码、强度很低], [已知明文攻击可反推密钥],
  [§5 只要求「简单 XOR」],
)

#pagebreak()

// ===== 生成式AI工具使用声明 =====

= 九、生成式 AI 工具使用声明

本次实验在实施与报告撰写过程中使用了大型语言模型作为辅助工具，以下按工具概况与具体辅助作用分述。

== 9.1 使用工具概况

*实验执行、代码编写与报告撰写*：DeepSeek Harness，模型为 deepseek-v4-flash，在本地工作目录中交互，贯穿实验全过程。

== 9.2 具体辅助作用

+ 与实验者交流并辅助拍板系统架构。
+ 完成了整个 test 模块。
+ 在编码过程中提供了必要的 Java 知识讲解。
+ 完成了实验报告主体部分的编写。

== 9.3 补充说明

以上所有 AI 工具生成的技术方案与代码均经过实验者的实际操作验证：每一次设计选择都通过编译（`javac --release 17 -Xlint:all` 零警告）与 673 项自动化测试确认，关键行为另外做了端到端手动验收。所有实验操作的实施、数据的采集与分析、以及最终结论的得出，均由实验者本人独立完成。
