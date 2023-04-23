package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultipleCall {

    /**
     * @wit "simplifiedPathConjunction": "x.equals(\"67890\") && x.length() != 2", @wit-correct
     *
     * @our RefinedCondition: parameter0 equals "12345"
     * @our RefinedCondition: parameter0 equals "67890"  @our-badCond
     * todo: inter-call-record, expand or not
     */
    public void throw_with_ret_value_condition(String x){
        String y = call_another_method(x);
        if(x.equals(y))
            throw new RuntimeException("throw_with_callee_condition2");
    }
    public String call_another_method(String x) {
        if(x.length() ==2)
            return "12345";
        else
            return "67890";
    }

    /**
     * @wit not generated?  @wit-wrongCond
     * @our RefinedCondition: no condition  @our-correct
     */
    public void throw_exception_caller(){
        callee_without_arg();
    }

    /**
     * @wit "simplifiedPathConjunction": "x < 1000",   @wit-correct
     *
     * @our RefinedCondition: @parameter0: int smaller than 1000, which is true  @our-correct
     */
    public void throw_exception_caller_with_arg(int x){
        callee_with_arg(x);
    }


    private void callee_with_arg(int m) {
        if(m<1000)
            throw new RuntimeException("throw_exception_in_callee_directly");
    }

    private void callee_without_arg() {
        throw new RuntimeException("throw_exception_in_callee_directly");
    }

}
