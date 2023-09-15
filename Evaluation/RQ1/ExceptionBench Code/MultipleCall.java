package testcase.ExceptionCondition;

import java.util.Objects;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultipleCall {

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
     * @throws RuntimeException: no exception
     */
    public void throw_exception_caller_with_arg_caught(int x){
        try {
            callee_with_arg(x);
        }catch (Exception e){
        }
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

    /**
     * @throws NullPointerException:  parameter0 is null
     */
    public void throw_message_callee_require(String s, String msg) {
        Objects.requireNonNull(s, msg);
    }

    /**
     * @throws NullPointerException: parameter0 is null
     * @throws IllegalAccessException: parameter0 is not null && parameter1 is not null && parameter1。len <=3 && parameter0 startswith deepCall
     * @throws IllegalArgumentException: parameter0 is not null && parameter1 is null
     * @throws RuntimeException: parameter0 is not null && parameter1 is not null && parameter1。len >3
     */
    public void deepCall(String s, String msg) throws IllegalAccessException {
        Objects.requireNonNull(s);
        calleeNullInDeepCall(msg);
        calleeSizeInDeepCall(msg);
        if(s.startsWith("deepCall"))
            throw new IllegalAccessException("throw_exception_in_deepCall");
    }

    /**
     * @throws IllegalArgumentException:  parameter0 is null
     */
    public void calleeNullInDeepCall(String msg) {
        if(msg==null)
            throw new IllegalArgumentException("throw_exception_in_deepCall1");
    }

    /**
     * @throws RuntimeException:  parameter0。len >3
     */
    public void calleeSizeInDeepCall(String msg) {
        if(msg.length()>3)
            throw new RuntimeException("throw_exception_in_deepCall2");
    }

}
