package gamelib.store;

import java.util.List;

// record 用于声明不可变的数据载体
// 比如这里写了 int added，编译器会自动生成一个 final 字段、一个构造器、一个 getter 方法（名字和字段一样）
// 还会自动生成 equals()、hashCode()、toString() 方法
//
// 可以自行派生别的方法（比如这里的 total() 方法），但不能派生别的字段（比如 private int total; 是不允许的）

// ImportReport 记录了导入游戏的结果，包括成功导入的数量、重复的数量、跳过的数量，以及重复游戏的标题列表
// 便于 UI 显示导入结果
public record ImportReport(
    int added,
    int duplicates,
    int skipped,
    List<String> duplicateTitles)
{

    public int total() {
        return added + duplicates + skipped;
    }

    // 紧凑构造器：没有参数列表
    // 这里相当于
    // public ImportReport(
    //          int added, 
    //          int duplicates, 
    //          int skipped, 
    //          List<String> duplicateTitles
    // ) {
    //     this.added = added;
    //     this.duplicates = duplicates;
    //     this.skipped = skipped;
    //     this.duplicateTitles = List.copyOf(duplicateTitles);
    // }
    // duplicateTitles 是 List 类型，可能是可变的，因而生成一个不可变的副本存在 ImportReport 中，保证 ImportReport 的不可变性
    public ImportReport {
        duplicateTitles = List.copyOf(duplicateTitles);
    }
}
