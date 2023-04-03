package test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author hanada
 * @Date 2022/6/24 11:20
 * @Version 1.0
 */
@Slf4j
public class TestQE {
    @Test
    public void testQE(){
        String input = "[\\s\\S]*settings: [\\s\\S]*adf[\\s\\S]*[\\s\\S]*[\\s\\S]*[\\s\\S]*aa[\\s\\S]*";
        String[] ss =input.split("\\Q[\\s\\S]*\\E");
        String exceptionMsg = "";
        for(int i= 0; i<ss.length-1;i++){
            exceptionMsg+="\\Q"+ss[i]+"\\E"+"[\\s\\S]*";
        }
        exceptionMsg+="\\Q"+ss[ss.length-1]+"\\E";
        String temp = "";
        while(!exceptionMsg.equals(temp)) {
            temp= exceptionMsg;
            exceptionMsg = exceptionMsg.replace("\\Q\\E", "");
            exceptionMsg = exceptionMsg.replace("\\E\\Q", "");
            exceptionMsg = exceptionMsg.replace("[\\s\\S]*[\\s\\S]*", "[\\s\\S]*");
        }
        log.info(exceptionMsg);
        Map map = new HashMap();
        map.put("11",23);
        System.out.println(map.get("22"));
    }
}
