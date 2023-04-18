package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultipleCall {

    /**
     * @wit "simplifiedPathConjunction": "x.equals(\"67890\") && x.length() != 2",
     *
     * @our RefinedCondition: parameter0 equals "12345"
     * @our RefinedCondition: parameter0 equals "67890"
     * todo: inter-call-record, expand or not
     */
    public void throw_with_ret_value_condition(String x){
        String y = call_another_method(x);
        if(x.equals(y))
            throw new RuntimeException("throw_with_callee_condition2");
    }

    /**
     * @wit not generated?
     * @our todo
     */
    public void throw_exception_in_callee_directly(){
        call_exception_thrower();
    }

    /**
     * @wit "simplifiedPathConjunction": "x < 1000",
     * @our todo
     */
    public void throw_exception_in_callee_not_directly(int x){
        call_exception_thrower(x);
    }

    public String call_another_method(String x) {
        if(x.length() ==2)
            return "12345";
        else
            return "67890";
    }

    private void call_exception_thrower(int m) {
        if(m<1000)
            throw new RuntimeException("throw_exception_in_callee_directly");
    }

    private void call_exception_thrower() {
        throw new RuntimeException("throw_exception_in_callee_directly");
    }

}
