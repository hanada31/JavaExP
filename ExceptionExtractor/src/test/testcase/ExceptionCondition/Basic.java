package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class Basic {
    /**
     * @wit @wit "simplifiedPathConjunction": "true",
     *
     * @our RefinedCondition: no condition
     */
    public void throw_without_condition(int x){
        System.out.println(x);
        throw new RuntimeException("throw_without_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "null == x",
     *
     * @our RefinedCondition: parameter0 is null
     */
    public void throw_with_null_condition(String x){
        if(x==null)
        throw new RuntimeException("throw_with_null_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "x.equals(\"value\")",
     *
     * @our RefinedCondition: parameter0 equals "value"
     */
    public void throw_with_value_condition(String x){
        if(x.equals("value"))
            throw new RuntimeException("throw_with_value_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "x.equals(\"value\")",
     *
     * @our RefinedCondition: parameter0 equals "value"
     *
     */
    public void throw_with_modified_var_condition(String x){
        String y;
        y = x;
        if(y.equals("value"))
            throw new RuntimeException("throw_with_modified_var_condition");
    }

    /**
     * @wit "backwardsPathConjunction": "(x.equals(\"value123\".substring(0, 5)))",
     *
     * @our RefinedCondition: parameter0 equals "value"
     *
     */
    public void throw_with_modified_value_condition(String x){
        String y = "value123";
        if(x.equals(y.substring(0,5)))
            throw new RuntimeException("throw_with_modified_value_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "x.equals(y) && y.length() <= 2",
     * @wit "simplifiedPathConjunction": "x.equals(y) && y.length() > 2",
     *
     * @our RefinedCondition: parameter0 equals parameter1
     * @our RefinedCondition: parameter0 equals virtualinvoke @parameter1: java.lang.String.<java.lang.String: java.lang.String substring(int,int)>(0, 5)
     */
    public void throw_with_modified_value_condition2(String x, String y){
        if(y.length()>2)
            y = y.substring(0,5);
        if(x.equals(y))
            throw new RuntimeException("throw_with_modified_value_condition2");
    }

    /**
     * @wit "simplifiedPathConjunction": "null == y && null != x",
     *
     * @our RefinedCondition: parameter1 is null
     * @our RefinedCondition: parameter0 is not null
     */
    public void throw_with_combined_condition(String x, String y){
        if(x!=null && y == null)
            throw new RuntimeException("throw_with_combined_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "null == y || null != x",
     *
     * @our RefinedCondition: parameter0 is null
     * @our RefinedCondition: parameter1 is null
     */
    public void throw_with_combined_condition2(String x, String y){
        if(x!=null || y != null)
            throw new RuntimeException("throw_with_combined_condition2");
    }

    /**
     * @wit "simplifiedPathConjunction": "null == y || null != x",
     *
     * @our RefinedCondition: parameter0 is null
     * @our RefinedCondition: parameter1 is null
     */
    public void throw_with_combined_condition3(String x, String y, String z){
        if(x==null || y == null || z == null)
            throw new RuntimeException("throw_with_combined_condition2");
    }

    /**
     * @our RefinedCondition: parameter1 is null
     * @our RefinedCondition: parameter0 is not null
     */
    public void throw_with_combined_condition4(String x, String y){
        if(x!=null)
            if(y == null)
                throw new RuntimeException("throw_with_combined_condition3");
    }


    /**
     * @our RefinedCondition: parameter0 is not null
     * @our RefinedCondition: parameter1 is null
     */
    public void throw_with_combined_condition5(String x, String y){
        if(x==null) {
            if (y == null)
                System.out.println();
            else
                throw new RuntimeException("throw_with_combined_condition4");
        }
    }

}
