package gamelib.store;

import java.util.List;

public record ImportReport(
    int added,
    int duplicates,
    int skipped,
    List<String> duplicateTitles)
{

    public int total() {
        return added + duplicates + skipped;
    }

    public ImportReport {
        duplicateTitles = List.copyOf(duplicateTitles);
    }
}
