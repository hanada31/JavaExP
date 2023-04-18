package testcase.ExceptionCondition;

import java.util.List;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultiplePath {
    /**
     * @wit  not generate
     *
     * @our RefinedCondition{leftVar='parameter0', operator='startsWith', rightValue='"123"'}
     */
    public void throw_with_multiple_paths(String x, int n, List<String> list){
        for(String s :list) {
            System.out.println();
        }

        if(x.startsWith("123"))
            throw new NullPointerException("throw_with_multiple_paths1");
    }

    /**
     * @wit "simplifiedPathConjunction": "x.startsWith(\"123\") && n <= 2",
     * @wit "simplifiedPathConjunction": "list.get(0)_equals && x.startsWith(\"123\") && n > 2",
     * @wit "simplifiedPathConjunction": "x.startsWith(\"123\") && !list.get(0).equals(\"bc\") && n > 2",
     *
     * @our RefinedCondition{leftVar='parameter0', operator='startsWith', rightValue='"123"'}
     *
     */
    public void throw_with_multiple_paths1(String x, int n, List<String> list){
        if (n>2){
            if(list.get(0).equals("ab")){
                System.out.println("ab");
            }else if(list.get(0).equals("bc")){
                System.out.println("bc");
            }
        }
        if(x.startsWith("123"))
            throw new NullPointerException("throw_with_multiple_paths1");
    }

    /**

     * @wit "simplifiedPathConjunction": "list.get(0).equals(\"a\") && x.startsWith(\"123\") && !list.isEmpty() && n > 2 && list != null",
     * @wit "simplifiedPathConjunction": "x.startsWith(\"123\") && !list.get(0).equals(\"b\") && !list.isEmpty() && n > 2 && list != null",
     * @wit "simplifiedPathConjunction": "x.startsWith(\"123\") && (list.isEmpty() || list == null || n <= 2)",
     *
     * @our RefinedCondition{leftVar='parameter0', operator='startsWith', rightValue='"123"'}
     */
    public void throw_with_multiple_paths2(String x, int n, List<String> list){
        if (n>2 && list!=null && !list.isEmpty()){
            callIfMethod(list.get(0));
        }
        if(x.startsWith("123"))
            throw new NullPointerException("throw_with_multiple_paths2");
    }


    /**
     * @wit  not generate
     *
     * @our RefinedCondition{leftVar='parameter0', operator='startsWith', rightValue='"123"'}
     */
    public void throw_with_multiple_paths3(String x, int n, List<String> list){
        if (n>2){
            for(String s :list) {
                callIfMethod(s);
            }
        }else{
            callIfMethod(x);
        }
        if(x.startsWith("123"))
            throw new NullPointerException("throw_with_multiple_paths1");
    }


    private void callIfMethod(String s) {
        if(s.equals("a")){
            System.out.println("a");
            if(s.equals("ab")){
                System.out.println("ab");
            }else if(s.equals("bc")){
                System.out.println("bc");
            }
        }else if(s.equals("b")){
            System.out.println("b");
            if(s.equals("ab")){
                System.out.println("ab");
            }else if(s.equals("bc")){
                System.out.println("bc");
            }
        }else if(s.equals("c")){
            System.out.println("b");
            if(s.equals("ab")){
                System.out.println("ab");
            }else if(s.equals("bc")){
                System.out.println("bc");
            }
        }
    }

}
