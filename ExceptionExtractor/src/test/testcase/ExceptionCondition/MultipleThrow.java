package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultipleThrow {

    /**
     *java.lang.RuntimeException
     * @wit "simplifiedPathConjunction": "null == x",  @wit-correct
     *
     * java.lang.NullPointerException
     * @wit ""simplifiedPathConjunction": "x.startsWith(\"123\") && null != x",  @wit-correct @wit-badInput
     *
     * java.lang.RuntimeException
     * @our RefinedCondition: parameter0 is null  @our-correct
     *
     * java.lang.NullPointerException
     * @our RefinedCondition: parameter0 is not null, parameter0 startsWith "123"  @our-correct
     */
    public void throw_two(String x){
        if(x==null)
            throw new RuntimeException("throw_two");

        if(x.startsWith("123"))
            throw new NullPointerException("throw_two");
    }

    /**
     *java.lang.RuntimeException
     * @wit "simplifiedPathConjunction": "null == x",  @wit-correct
     *
     * java.lang.NullPointerException
     * @wit ""simplifiedPathConjunction":"x.startsWith(\"123\") && null == y && null != x",  @wit-wrongCond
     *
     * java.lang.RuntimeException
     * @our RefinedCondition: parameter0 is null  @our-correct
     *
     * java.lang.NullPointerException
     * @our RefinedCondition: parameter0 is not null, parameter0 startsWith "123"  @our-correct
     */
    public void throw_two_nouse_if(String x , String y){
        if(x==null)
            throw new RuntimeException("throw_two_nouse_if");

        if(y==null)
            System.out.println(y);

        if(x.startsWith("123"))
            throw new NullPointerException("throw_two_nouse_if");
    }


}
