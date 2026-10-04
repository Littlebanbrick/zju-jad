package gamelib.store;

import java.util.List;

/**
 * 批量导入的结果。
 *
 * @param added           成功入库的条数
 * @param duplicates      因重复被跳过的条数
 * @param skipped         因输入为 null 被跳过的条数
 * @param duplicateTitles 重复项明细，形如 "The Witcher 3 | Steam"
 */
public record ImportReport(
    int added, 
    int duplicates, 
    int skipped,
    List<String> duplicateTitles) 
{

    /**
     * 输入总条数。
     *
     * <p>不变量：{@code total() == added + duplicates + skipped}
     */
    public int total() {
        return added + duplicates + skipped;
    }

    /** 不可变副本：record 的不可变性只到引用层，调用方能改掉传进来的 list。 */
    public ImportReport {
        duplicateTitles = List.copyOf(duplicateTitles);
    }
}