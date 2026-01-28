import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupDeptandSal {


    public static void main(String[] args) {
        List<Employee1> emp = Arrays.asList(
                new Employee1("IT",45000),
                new Employee1("HR",75000),
                new Employee1("IT",85000)
        );

        Map<String,Double> avg = emp.stream()
                .collect(Collectors.groupingBy(e->e.dept, Collectors.averagingDouble(e->e.salary)));

        System.out.println(avg);
    }
}
class Employee1 { String dept; double salary; Employee1(String d,double s){dept=d;salary=s;} }
