package testcase.ExceptionCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author hanada
 * @Date 2023/6/1 13:42
 * @Version 1.0
 */
public class Motivation {


private final String ERRORSTR = "Error String";
/**
 * caller
 * @throws RuntimeException if param input first is null and param errStr input startsWith "Error"
 */
public void caller(String errStr, String input,int num) {
    callee(input, errStr, num);
}
/**
 * callee
 * @throws RuntimeException if param first is null and param second startsWith "Error"
 */
public void callee(String first, String second, int key){
    String errStr = "NoError";
    if(first==null) {
        if(key < 0)
            System.out.println("invalid key");
        else {
            for (String value: getValuesWithKey()){
                System.out.println(value);
            }
        }
        errStr = second;
        if (errStr.startsWith(ERRORSTR.substring(0,5)))
            throw new RuntimeException("bug!");
    }
    System.out.println(errStr);
}

    public void caller2(String errStr, String input,int num) {
        callee2(input, errStr, num);
    }
/**
 * callee
 * @throws RuntimeException if param first is null and param second startsWith "Error"
 */
public void callee2(String first, String second, int key){
    String errStr = "NoError";
    if(first==null) {
        if(key < 0)
            System.out.println("invalid key");
        else {
            for (int i =0; i< getValuesWithKey().size(); i++){
                System.out.println(getValuesWithKey().get(i));
            }
        }
        errStr = second;
        if (errStr.startsWith(ERRORSTR.substring(0,5)))
            throw new RuntimeException("bug!");
    }
    System.out.println(errStr);
}

private void detectInvalidKey(int key) {
    if(key > -100)
        throw new IllegalArgumentException();
}

    private List<String> getValuesWithKey() {
        List<String> values = new ArrayList<>();
        values.add("1");
        values.add("22");
        values.add("333");
        return values;
    }

}
