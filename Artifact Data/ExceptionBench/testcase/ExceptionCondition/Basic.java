package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class Basic {
    /**
     * @throws RuntimeException: no condition
     */
    public void throw_without_condition(int x){
        System.out.println(x);
        throw new RuntimeException("throw_without_condition");
    }

    /**
     * @throws RuntimeException: parameter0 is null
     */
    public void throw_with_null_condition(String x){
        if(x==null)
        throw new RuntimeException("throw_with_null_condition");
    }

    /**
     * @throws RuntimeException: parameter0 equals "value"
     */
    public void throw_with_value_condition(String x){
        if(x.equals("value"))
            throw new RuntimeException("throw_with_value_condition");
    }

    /**
     * @throws RuntimeException: parameter0 not equals "value"
     */
    public void throw_with_value_condition_not_equal(String x){
        boolean z= x.equals("value");
        if(!z)
            throw new RuntimeException("throw_with_value_condition_not_equal");
    }

    /**
     * @throws RuntimeException: parameter0 equals "value"
     */
    public void throw_with_modified_var_condition(String x){
        String y;
        y = x;
        if(y.equals("value"))
            throw new RuntimeException("throw_with_modified_var_condition");
    }

    /**
     * @throws RuntimeException: parameter0 equals "value"
     */
    public void throw_with_modified_value_condition(String x){
        String y = "value123";
        if(x.equals(y.substring(0,5)))
            throw new RuntimeException("throw_with_modified_value_condition");
    }

    /**
     * @throws RuntimeException: parameter0 equals "value"
     */
    public void throw_with_modified_value_condition2(String x){
        String y = "value123";
        String z = x;
        if(z.equals(y.substring(0,5)))
            throw new RuntimeException("throw_with_modified_value_condition2");
    }


    /**
     * @throws RuntimeException: parameter1 length >2 && parameter1 substring(0,5) equals parameter0
     * @throws RuntimeException: parameter1 length <=2 && parameter1 equals parameter0
     */
    public void throw_with_modified_value_condition3(String x, String y){
        if(y.length()>2)
            y = y.substring(0,5);
        if(x.equals(y))
            throw new RuntimeException("throw_with_modified_value_condition3");
    }

    /**
     * @throws RuntimeException: parameter0 is not null && parameter1 is null
     */
    public void throw_with_combined_condition(String x, String y){
        if(x!=null && y == null)
            throw new RuntimeException("throw_with_combined_condition");
    }

    /**
     * @throws RuntimeException: parameter0 is not null || parameter1 is null
     */
    public void throw_with_combined_condition2(String x, String y){
        if(x!=null || y != null)
            throw new RuntimeException("throw_with_combined_condition2");
    }

    /**
     * @throws RuntimeException: parameter0 is null || parameter1 is null || parameter2 is null
     */
    public void throw_with_combined_condition3(String x, String y, String z){
        if(x==null || y == null || z == null)
            throw new RuntimeException("throw_with_combined_condition3");
    }

    /**
     * @throws RuntimeException: parameter0 is not null && parameter1 is null
     */
    public void throw_with_combined_condition4(String x, String y){
        if(x!=null)
            if(y == null)
                throw new RuntimeException("throw_with_combined_condition4");
    }

    /**
     * @throws RuntimeException: parameter0 is null && parameter1 is null
     */
    public void throw_with_combined_condition5(String x, String y){
        if(x==null) {
            if (y == null)
                System.out.println();
            else
                throw new RuntimeException("throw_with_combined_condition5");
        }
    }

    /**
     * @throws RuntimeException: parameter0 is null && parameter1 is null
     */
    public void throw_with_combined_condition6(String x, String y,int z){
        if(x==null) {
            if(z>0)
                System.out.println(z);
            if (y == null)
                throw new RuntimeException("throw_with_combined_condition6");
        }
    }

}
