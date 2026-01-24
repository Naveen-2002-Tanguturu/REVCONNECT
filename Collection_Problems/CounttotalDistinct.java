import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CounttotalDistinct {



    public static void main(String[] args){
        List<String> sentences = Arrays.asList("Nanna is my Hero","I Miss You Nanna");

        List<String> words = sentences.stream()
                .flatMap(s->Arrays.stream(s.split(" ")))
                .collect(Collectors.toList());

        int total = words.size();
        int distinct = (int) words.stream().distinct().count();

        Word stats = new Word(total, distinct);
        System.out.println(stats); // Total=6, Distinct=5
    }
}
class Word { int total, distinct; Word(int t,int d){total=t;distinct=d;} public String toString(){return "Total="+total+", Distinct="+distinct;} }