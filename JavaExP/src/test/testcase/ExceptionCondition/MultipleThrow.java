package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultipleThrow {

    /**
     * @throws RuntimeException: parameter0 is null
     * @throws NullPointerException: parameter0 is not null, parameter0 startsWith "123"
     */
    public void throw_two_exception(String x){
        if(x==null)
            throw new RuntimeException("throw_two_exception");

        if(x.startsWith("123"))
            throw new NullPointerException("throw_two_exception");
    }

    /**
     * @throws RuntimeException: parameter0 is null
     * @throws NullPointerException: parameter0 is not null, parameter0 startsWith "123"
     */
    public void throw_two_exception2(String x , String y){
        if(x==null)
            throw new RuntimeException("throw_two_exception2");

        if(y==null)
            System.out.println(y);

        if(x.startsWith("123"))
            throw new NullPointerException("throw_two_exception2");
    }


}
