package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultipleThrow {

    /**
     *java.lang.RuntimeException
     * @wit "simplifiedPathConjunction": "null == x",
     *
     * java.lang.NullPointerException
     * @wit ""simplifiedPathConjunction": "x.startsWith(\"123\") && null != x",
     *
     * java.lang.RuntimeException
     * @our RefinedCondition: parameter0 is null
     *
     * java.lang.NullPointerException
     * @our RefinedCondition: parameter0 startsWith "123"
     */
    public void throw_two(String x){
        if(x==null)
        throw new RuntimeException("throw_with_null_condition");

        if(x.startsWith("123"))
            throw new NullPointerException("throw_with_multiple_paths2");
    }

}
