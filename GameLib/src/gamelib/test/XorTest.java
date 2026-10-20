package gamelib.test;

import gamelib.model.Game;
import gamelib.model.Platform;
import gamelib.xor.Xor;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * XOR 混淆模块（§5 第 5 项）。
 *
 * <p>只测结构性性质，不测具体密钥值 —— 换密钥不该让测试变红。
 */
public final class XorTest {

    private XorTest() {
    }

    public static void run(TestRunner t) {
        testSelfInverse(t);
        testStructure(t);
        testAgainstSerialization(t);
    }

    private static void testSelfInverse(TestRunner t) {
        t.section("[60] Xor：自逆性");

        int[] lengths = {0, 1, 3, 4, 5, 100, 1000};
        for (int len : lengths) {
            byte[] data = new byte[len];
            for (int i = 0; i < len; i++) {
                data[i] = (byte) (i * 37 + 11);
            }
            byte[] once = Xor.xor(data);
            byte[] twice = Xor.xor(once);
            t.check(Arrays.equals(data, twice), "★ 长度 " + len + "：xor(xor(x)) == x");
        }

        // 明文里有可打印字符时，加密后应当看不出原文
        byte[] text = "purchasePrice title hidden".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] enc = Xor.xor(text);
        t.check(!Arrays.equals(text, enc), "★ 加密后与原文不同");
        t.check(new String(enc, java.nio.charset.StandardCharsets.ISO_8859_1)
                        .indexOf("price") < 0,
                "★ 加密后字节里搜不到原文片段");
    }

    private static void testStructure(TestRunner t) {
        t.section("[61] Xor：结构性质");

        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        t.check(Xor.xor(data).length == data.length, "★ 长度不变（不扩张、不压缩）");
        t.check(Xor.xor(new byte[0]).length == 0, "空数组 -> 空数组");

        byte[] copy = data.clone();
        Xor.xor(data);
        t.check(Arrays.equals(data, copy), "不修改输入数组（纯函数）");

        byte[] zeros = new byte[64];
        t.check(!Arrays.equals(zeros, Xor.xor(zeros)), "★ 全零输入也会被改变（不是恒等映射）");

        // 相同输入必须得到相同输出（密钥固定，无随机性）
        t.check(Arrays.equals(Xor.xor(data), Xor.xor(data)), "确定性：同样输入同样输出");
    }

    private static void testAgainstSerialization(TestRunner t) {
        t.section("[62] Xor：与序列化流配合");

        List<Game> games = new ArrayList<>();
        Game g = new Game("Elden Ring", Platform.STEAM, "ARPG", 298.0,
                LocalDate.of(2022, 3, 1), 10, 120.0, "神作");
        games.add(g);

        byte[] plain;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(buf)) {
                out.writeObject(games);
            }
            plain = buf.toByteArray();
        } catch (java.io.IOException e) {
            t.check(false, "序列化失败：" + e);
            return;
        }

        // 未加密的 Java 序列化流固定以 AC ED 00 05 开头
        t.check((plain[0] & 0xFF) == 0xAC && (plain[1] & 0xFF) == 0xED,
                "前提确认：明文流以 AC ED 开头");
        t.check(new String(plain, java.nio.charset.StandardCharsets.ISO_8859_1)
                        .contains("purchasePrice"),
                "前提确认：明文流里能搜到字段名 purchasePrice");

        byte[] encrypted = Xor.xor(plain);
        t.check((encrypted[0] & 0xFF) != 0xAC,
                "★ 加密后魔数 AC 消失（strings 认不出这是 Java 序列化文件）");
        t.check(!new String(encrypted, java.nio.charset.StandardCharsets.ISO_8859_1)
                        .contains("purchasePrice"),
                "★ 加密后搜不到字段名 purchasePrice");

        byte[] restored = Xor.xor(encrypted);
        t.check(Arrays.equals(plain, restored), "★ 解密后字节完全还原");
        t.check((restored[0] & 0xFF) == 0xAC, "解密后魔数 AC 回来");
    }
}
