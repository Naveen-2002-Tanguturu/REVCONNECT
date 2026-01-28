import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamApi {
    public static void main(String[] args) {
        List<Employe> emp = Arrays.asList(
                new Employe("vamsi",90000),
                new Employe("sai",95000),
                new Employe("kumar",99000)
        );

        List<String> names = emp.stream()
                .filter(e->e.salary>75000)
                .map(e->e.name)
                .sorted()
                .collect(Collectors.toList());

        System.out.println(names);
    }
}
class Employe { String name; double salary; Employe(String n,double s){name=n;salary=s;} }