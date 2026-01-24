import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SafeRemoval {


    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(Arrays.asList(1,2,3,4));
        Iterator<Integer> it = list.iterator();
        while(it.hasNext()) {
            if(it.next()%2==0) it.remove(); // remove even numbers safely
        }
        System.out.println(list); // [1, 3]
    }
}
