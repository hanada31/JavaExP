package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class Basic {
    /**
     * @wit "simplifiedPathConjunction": "true", @wit-correct
     *
     * @our RefinedCondition: no condition  @our-correct
     */
    public void throw_without_condition(int x){
        System.out.println(x);
        throw new RuntimeException("throw_without_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "null == x", @wit-correct
     *
     * @our RefinedCondition: parameter0 is null  @our-correct
     */
    public void throw_with_null_condition(String x){
        if(x==null)
        throw new RuntimeException("throw_with_null_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "x.equals(\"value\")", @wit-correct
     *
     * @our RefinedCondition: parameter0 equals "value"  @our-correct
     */
    public void throw_with_value_condition(String x){
        if(x.equals("value"))
            throw new RuntimeException("throw_with_value_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "!x.equals(\"value\")", @wit-correct
     * "z3Inputs": "[z0 = False]\n",    @wit-wrongInput
     *
     * @our RefinedCondition: parameter0 not equals "value"  @our-correct
     */
    public void throw_with_value_condition_not_equal(String x){
        boolean z= x.equals("value");
        if(!z)
            throw new RuntimeException("throw_with_value_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "x.equals(\"value\")", @wit-correct @wit-badInput
     *
     * @our RefinedCondition: parameter0 equals "value"  @our-correct
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
     * @wit-correct @wit-badCond @wit-wrongInput
     *
     * @our RefinedCondition: parameter0 equals "value"  @our-correct
     *
     */
    public void throw_with_modified_value_condition(String x){
        String y = "value123";
        if(x.equals(y.substring(0,5)))
            throw new RuntimeException("throw_with_modified_value_condition");
    }

    /**
     * @wit "backwardsPathConjunction": "(x.equals(\"value123\".substring(0, 5)))",
     * @wit-correct @wit-badCond @wit-wrongInput
     *
     * @our RefinedCondition: parameter0 equals "value"  @our-correct
     *
     */
    public void throw_with_modified_value_condition2(String x){
        String y = "value123";
        String z = x;
        if(z.equals(y.substring(0,5)))
            throw new RuntimeException("throw_with_modified_value_condition2");
    }

    /**wongInpu
     * @wit "simplifiedPathConjunction": "x.equals(y) && y.length() <= 2",
     * @wit "simplifiedPathConjunction": "x.equals(y) && y.length() > 2",
     * @wit-wrongCond
     *
     * @our RefinedCondition: parameter0 equals "value", which is true
     * @our-wrongCond
     */
    public void throw_with_modified_value_condition3(String x, String y){
        if(y.length()>2)
            y = y.substring(0,5);
        if(x.equals(y))
            throw new RuntimeException("throw_with_modified_value_condition3");
    }

    /**
     * @wit "simplifiedPathConjunction": "null == y && null != x",  @wit-correct
     *
     * @our RefinedCondition: parameter1 is null  @our-correct
     * @our RefinedCondition: parameter0 is not null
     */
    public void throw_with_combined_condition(String x, String y){
        if(x!=null && y == null)
            throw new RuntimeException("throw_with_combined_condition");
    }

    /**
     * @wit "simplifiedPathConjunction": "null == y || null != x", @wit-correct
     *
     * @our RefinedCondition: parameter0 is null  @our-correct
     * @our RefinedCondition: parameter1 is not null
     *
     * @our RefinedCondition: parameter0 is not null
     */
    public void throw_with_combined_condition2(String x, String y){
        if(x!=null || y != null)
            throw new RuntimeException("throw_with_combined_condition2");
    }

    /**
     * @wit "simplifiedPathConjunction": "x==null || y == null || z == null", @wit-correct
     *
     * @our RefinedCondition: parameter0 is not null  @our-correct
     * @our RefinedCondition: parameter1 is not null
     * @our RefinedCondition: parameter2 is null
     *
     * @our RefinedCondition: parameter0 is not null
     * @our RefinedCondition: parameter1 is null
     *
     * @our RefinedCondition: parameter0 is null
     */
    public void throw_with_combined_condition3(String x, String y, String z){
        if(x==null || y == null || z == null)
            throw new RuntimeException("throw_with_combined_condition2");
    }

    /**
     * @wit "simplifiedPathConjunction": "null == y && null != x", @wit-correct
     *
     * @our RefinedCondition: parameter1 is null  @our-correct
     * @our RefinedCondition: parameter0 is not null
     */
    public void throw_with_combined_condition4(String x, String y){
        if(x!=null)
            if(y == null)
                throw new RuntimeException("throw_with_combined_condition3");
    }


    /**
     * @wit "simplifiedPathConjunction": "null == x && null != y", @wit-correct
     *
     * @our RefinedCondition: parameter0 is null  @our-correct
     * @our RefinedCondition: parameter1 is not null
     */
    public void throw_with_combined_condition5(String x, String y){
        if(x==null) {
            if (y == null)
                System.out.println();
            else
                throw new RuntimeException("throw_with_combined_condition4");
        }
    }

    /**
     * @wit "simplifiedPathConjunction": "null == x && null == y && z > 0", @wit-correct
     * @wit "simplifiedPathConjunction": "null == x && null == y && z <= 0"
     *
     * @our RefinedCondition: parameter0 is null  @our-wrongCond
     * @our RefinedCondition: parameter1 is null
     * @our RefinedCondition: parameter2 larger than 0, which is true
     *
     * @our RefinedCondition: parameter0 is null  @our-wrongCond
     * @our RefinedCondition: parameter1 is null
     * @our RefinedCondition: parameter2 smaller 0, which is true
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
