import java.util.*;

public class Duplicates {
    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1,2,3,2,1,4,5);
        Set<Integer> set = new LinkedHashSet<>(nums); // remove duplicates, keep order
        List<Integer> result = new ArrayList<>(set);
        System.out.println(result); // [1, 2, 3, 4, 5]
    }

}
