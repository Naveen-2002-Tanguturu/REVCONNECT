import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MostFrequent {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1,2,4,4,4,5,6,6,6,6,1);
        Map<Integer,Integer> count = new HashMap<>();
        for(int n : nums) count.put(n, count.getOrDefault(n,0)+1);

        int maxFreq=0, answer=Integer.MAX_VALUE;
        for(int n : count.keySet()) {
            int freq = count.get(n);
            if(freq>maxFreq || (freq==maxFreq && n<answer)) {
                maxFreq=freq; answer=n;
            }
        }

        System.out.println(answer);
    }
}

