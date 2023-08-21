package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultipleCall {

    /**
     * @throws RuntimeException: parameter0 equals "67890"  && parameter0 length not equals 2
     */
    public void throw_with_ret_value_condition(String x){
        String y = call_another_method(x);
        if(x.equals(y))
            throw new RuntimeException("throw_with_ret_value_condition");
    }

    public String call_another_method(String x) {
        if(x.length() ==2)
            return "12345";
        else
            return "67890";
    }


    /**
     * @throws RuntimeException: no condition
     */
    public void throw_exception_caller(){
        callee_without_arg();
    }

    /**
     * @throws RuntimeException: parameter0 < 1000
     */
    public void throw_exception_caller_with_arg(int x){
        callee_with_arg(x);
    }

    /**
     * @throws RuntimeException: parameter0 < 1000
     */
    public void throw_exception_caller_with_arg_caught(int x){
        try {
            callee_with_arg(x);
        }catch (Exception e){
        }
    }

    public void throw_exception_caller_with_arg_notcaught(int x){
        try {
            int a = 3;
            compute(a);
            System.out.println();
        }catch (Exception e){
        }
        callee_with_arg(x);
    }

    private void compute(int a) {
        System.out.println(a);
    }

    /**
     * @throws RuntimeException: parameter0 < 1000
     */
    public void callee_with_arg(int m) {
        if(m<1000)
            throw new RuntimeException("throw_exception_in_callee");
    }

    /**
     * @throws RuntimeException: no condition
     */
    public void callee_without_arg() {
        throw new RuntimeException("throw_exception_in_callee_directly");
    }

}
