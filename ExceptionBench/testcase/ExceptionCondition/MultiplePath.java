package testcase.ExceptionCondition;

import java.util.List;

/**
 * @Author hanada
 * @Date 2023/4/13 12:46
 * @Version 1.0
 */
public class MultiplePath {
    /**
     * @throws RuntimeException: parameter0 startsWith "123"
     */
    public void throw_with_multiple_paths(String x, int n, List<String> list){
        for(String s :list) {
            System.out.println();
        }

        if(x.startsWith("123"))
            throw new NullPointerException("throw_with_multiple_paths1");
    }

    /**
     * @throws RuntimeException: parameter0 startsWith "123"
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
     * @throws RuntimeException: parameter0 startsWith "123"
     */
    public void throw_with_multiple_paths2(String x, int n, List<String> list){
        if (n>2 && list!=null && !list.isEmpty()){
            callIfMethod(list.get(0));
        }
        if(x.startsWith("123"))
            throw new NullPointerException("throw_with_multiple_paths2");
    }

    /**
     * @throws RuntimeException: parameter0 startsWith "123"
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
            throw new NullPointerException("throw_with_multiple_paths3");
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
