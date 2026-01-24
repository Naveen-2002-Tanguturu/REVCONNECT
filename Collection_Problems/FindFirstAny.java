import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FindFirstAny {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(2,6,3,7,9,1,8,5);

        Optional<Integer> firstEven = nums.stream().filter(n->n%2==0).findFirst();
        Optional<Integer> anyEven = nums.parallelStream().filter(n->n%2==0).findAny();

        System.out.println("First even: "+firstEven.get());
        System.out.println("Any even: "+anyEven.get());
    }
}
