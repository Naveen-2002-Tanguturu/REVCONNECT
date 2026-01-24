import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FlatternSort {
    public static void main(String[] args){
        List<List<String>> data = Arrays.asList(
                Arrays.asList("Hello","Hi"),
                Arrays.asList("Hi","vamsi")
        );

        List<String> result = data.stream()
                .flatMap(List::stream)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        System.out.println(result);
    }
}


