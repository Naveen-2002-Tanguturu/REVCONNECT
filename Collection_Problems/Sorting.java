import java.util.Arrays;
import java.util.List;

public class Sorting {
    public static void main(String[] args) {
        List<Employee> list = Arrays.asList(
                new Employee(1,"vamsi",55000),
                new Employee(2,"sai",45000),
                new Employee(3,"kumar",65000),
                new Employee(4,"krishna",55000)
        );

        list.sort((a,b) -> {
            int cmp = Double.compare(b.salary,a.salary);
            if(cmp==0) cmp = a.name.compareTo(b.name);
            if(cmp==0) cmp = a.id - b.id;
            return cmp;
        });

        System.out.println(list);
    }
}

class Employee {
    int id; String name; double salary;
    Employee(int i, String n, double s){ id=i; name=n; salary=s; }
    public String toString(){ return id+"-"+name+"-"+salary; }
}
