package testcase.ExceptionCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author hanada
 * @Date 2023/6/1 13:42
 * @Version 1.0
 */
public class Motivation {

    /**
     * The motivating example
     * @param first
     * @param second
     * @param key
     * @throws RuntimeException if param first is null and param second startsWith "Error"
     * @throws IllegalArgumentException if param first is null and param key < 0
     */
    private final String ERRORSTR = "Error String";
    public void motivatingExample(String first, String second, int key){
        String errStr = "NoError";
        if(first==null) {
            if(key<0)
                detectInvalidKey(key);
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

    private void detectInvalidKey(int key) {
        if(key<0)
            throw new IllegalArgumentException("invalid key");
    }

    public void motivatingExample2(String first, String second, int key){
        String errStr = "NoError";
        if(first==null) {
            if(key<0)
                detectInvalidKey(key);
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

    public void motivatingExample3(String first, String second, int key){
        String errStr = "NoError";
        if(first==null) {
            if(key<0)
                detectInvalidKey(key);
            else {
                System.out.println(getValuesWithKey().get(0));
            }
            errStr = second;
            if (errStr.startsWith(ERRORSTR.substring(0,5)))
                throw new RuntimeException("bug!");
        }
        System.out.println(errStr);
    }

    private List<String> getValuesWithKey() {
        List<String> values = new ArrayList<>();
        values.add("1");
        values.add("22");
        values.add("333");
        return values;
    }

    public void example1(String a, String b, int c){
        if ((a==null || b==null) && c>0)
            throw new RuntimeException("bug1!");
        System.out.println(ERRORSTR);
    }

}
