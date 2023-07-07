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
     * @throws RuntimeException: parameter1 is null && parameter0 startsWith "Error" && parameter2 smaller than 3
     */
    public void caller(String errStr, String input,int num) {
        if(num<3)
            callee(input, errStr, num);
    }

    /**
     * @throws RuntimeException: parameter0 is null and parameter1 startsWith "Error"
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

    /**
     * @throws RuntimeException: parameter1 is null && parameter0 startsWith "Error" && parameter2 smaller than 3
     */
    public void caller2(String errStr, String input,int num) {
        if(num<3)
            callee2(input, errStr, num);
    }

    /**
     * @throws RuntimeException: parameter0 is null and parameter1 startsWith "Error"
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
                throw new RuntimeException("bug2!");
        }
        System.out.println(errStr);
    }

    /**
     * @throws IllegalArgumentException: parameter0 larger than -100
     */
    public void detectInvalidKey(int key) {
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
