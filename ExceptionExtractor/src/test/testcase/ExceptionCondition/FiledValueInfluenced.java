package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class FiledValueInfluenced {
       /**
     * @wit "simplifiedPathConjunction": "outVar == 0",
     * @wit "z3Inputs": "[outVar0 = 0]\n",
     *
     * @our RefinedCondition: @this: testcase.ExceptionCondition.FiledValueInfluenced.<testcase.ExceptionCondition.FiledValueInfluenced: int outVar> is 0
     */
    int outVar = 1;
    public void throw_with_outVar_condition(String x){
        if(outVar==0)
            throw new RuntimeException("throw_with_outVar_condition");
    }

    public void decreaseOutVar(String x){
        outVar--;
    }

    public void zeroOutVar(String x){
        outVar =0;
    }

}
