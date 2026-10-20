# Personal Game Library Manager

个人游戏库存管理系统 —— Java Application Design 课程大程一。

完全离线运行：不使用任何第三方库，不联网，不依赖 Steam API，数据只保存在本地文件。
所有 CSV 解析、二进制存档、对齐表格都由 JDK 自带 API 手写实现。

---

## 1. 运行要求

- **JDK 17 或更高**（源码用 `javac --release 17` 编译，字节码是 Java 17 版本）
- 无需 Maven / Gradle，无需网络

---

## 2. 直接运行

```bash
java -jar game-lib.jar
```

程序在**当前工作目录**读写数据文件（`game_lib.dat` / `game_lib.csv`）。
建议单独建一个目录运行，避免把数据文件写进源码目录：

```bash
mkdir -p ~/my-games && cd ~/my-games
java -jar /path/to/game-lib.jar
```

按数字 `1`–`7` 选菜单，或直接输入快速命令：

| 快速命令 | 作用 |
|---|---|
| `add` | 添加游戏 |
| `search witch` | 按标题模糊搜索 |
| `stats` | 统计报表 |
| `exit` | 保存并退出 |

`Ctrl-D` 也可退出。

---

## 3. 从源码构建与测试

```bash
./build.sh      # 编译 + 打包出 game-lib.jar
./test.sh       # 编译 + 运行全部测试
```

两条脚本都用 `javac --release 17 -encoding UTF-8 -Xlint:all` 编译，零警告。

测试入口是 `gamelib.test.SelfTest`（**不是** `Main`）：

```bash
java -cp out gamelib.test.SelfTest
```

加 `-Xlint:all` 是为了让 `serialVersionUID`、未检查转换这类问题在编译期就暴露。

---

## 4. 数据文件

| 文件 | 格式 | 说明 |
|---|---|---|
| `game_lib.dat` | Java 序列化（二进制） | 启动时**优先**加载，最快 |
| `game_lib_backup.dat` | 同上 | 每次保存二进制前自动备份旧的 `game_lib.dat` |
| `game_lib.csv` | UTF-8 文本（带 BOM） | 二进制缺失或损坏时的回退路径，也可用 Excel 直接打开 |

**启动顺序**（§4.6）：`game_lib.dat` → `game_lib.csv` → 都没有则提示"新建游戏库"。

**如果文件存在却读不出来，程序会拒绝启动而不是以空库运行** —— 否则用户以为数据丢了，
一保存就把旧存档覆盖成空库。此时可以手工修复或删除对应文件再启动。

**CSV 带 UTF-8 BOM**，所以 Windows 版 Excel 双击打开时中文不会乱码。

---

## 5. 保存策略

```text
1. 若 game_lib.dat 已存在 -> 复制为 game_lib_backup.dat
2. 写入 game_lib.dat.tmp
3. 原子替换 game_lib.dat
```

**不直接写 `game_lib.dat`**：写到一半被中断的话，存档和备份会一起损坏。
`Files.move` 在同一文件系统上是原子操作。

保存时使用 `GameLibrary.allIncludingHidden()` 而非 `all()` ——
用 `all()` 会让**隐藏的游戏从磁盘上永久消失**。

---

## 6. 目录结构

```
src/gamelib/
├── Main.java                    程序入口，只做接线（加载 + 构造 Menu）
├── model/
│   ├── Game.java                游戏实体 + 校验 + equals/hashCode
│   ├── Platform.java            平台枚举（name() 给机器，label 给人）
│   └── InvalidGameDataException.java
├── store/
│   ├── GameLibrary.java         内存索引 Map<String,Game>、查询、分组、批量导入
│   ├── ImportReport.java        批量导入回执（record）
│   └── DuplicateGameException.java
├── io/
│   ├── Csv.java                 RFC 4180 转义与解析（纯字符串，不认 Game）
│   ├── GameCsv.java             Game <-> 11 列 CSV 的映射
│   ├── LibraryFiles.java        唯一碰磁盘的类：读写、备份、原子替换、加载优先级
│   └── LoadReport.java          加载结果（record）
├── ui/
│   ├── Menu.java                主循环 + 7 个菜单分支
│   ├── Console.java             输入原语 + 校验重试（流可注入）
│   ├── GameFormatter.java       对齐表格（含中英文宽度修正）
│   └── Stats.java               统计报表（record）
└── test/                        测试套件
```

依赖方向是单向的：`ui → store → model`，`io` 依赖 `model`。
`model` 不依赖任何其他包。

---

## 7. 已知限制

| 限制 | 影响 |
|---|---|
| CSV 字段不支持换行 | 交互录入路径产生不了换行；外部含换行的 CSV 会报「引号未闭合」而非静默损坏 |
| 宽度对齐只覆盖中英 + ASCII | 日文/韩文游戏名的表格会歪 |
| 价格用 `double` | 显示时用 `%.2f` 格式化，统计求和可能有浮点尾差 |
| 读取二进制存档时不重新校验 | 等价于信任 `game_lib.dat`；CSV 路径会完整走一遍构造器校验 |

---

## 8. 测试

`./test.sh` 运行 `gamelib.test.SelfTest`，覆盖 model / store / io / ui 四层，
包含 CSV round-trip、序列化往返、隐藏项持久化、菜单端到端（脚本化输入驱动）等。

测试是手写的断言工具（`TestRunner`），因为题目禁止第三方库（JUnit 用不了）。
