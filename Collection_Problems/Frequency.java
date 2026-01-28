import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Frequency {

    public static void main(String[] args) {
        List<String> words = Arrays.asList("Java","python","JAVA","java","Python");
        Map<String,Integer> map = new TreeMap<>();

        for(String w : words) {
            String key = w.toLowerCase();
            map.put(key, map.getOrDefault(key,0)+1);
        }

        for(String w : map.keySet()) {
            System.out.println(w + " : " + map.get(w));
        }
    }

}

