package com.iscas.crashtracker.client.dataAnalysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.exception.RelatedCondType;
import com.iscas.crashtracker.client.exception.RelatedVarType;
import com.iscas.crashtracker.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author hanada
 * @Date 2022/6/24 10:22
 * @Version 1.0
 */
@Slf4j
public class ExceptionInfoCount {
    String[] versions = {"2.3", "4.4", "5.0", "6.0", "7.0", "8.0", "9.0", "10.0", "11.0", "12.0"};

    public void analyze() {
        getExceptionOfCrashInfo();
        log.info("getExceptionOfCrashInfo Finish...");

    }

    private void getExceptionOfCrashInfo() {
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"exceptionInfoCount.txt", "", false);
        log.info("write to "+ MyConfig.getInstance().getResultFolder() +"exceptionInfoCount.txt");

        StringBuilder sb = new StringBuilder();
        sb.append("version\t" + "exception\t" + "exceptionType\t" + "method\t");
        sb.append("msgWith_sS\t" + "msgWithout_sS\t" + "msgNotNull\t");

        sb.append(RelatedVarType.Unknown.toString() + "\t");
        sb.append(RelatedVarType.Empty.toString()  + "\t");
        sb.append(RelatedVarType.Parameter.toString() + "\t");
        sb.append(RelatedVarType.Field.toString()  + "\t");
        sb.append(RelatedVarType.ParaAndField.toString()  + "\t");

        sb.append(RelatedCondType.Basic.toString() + "\t");
        sb.append(RelatedCondType.NotReturn.toString()  + "\t");
        sb.append( RelatedCondType.Empty.toString()  + "\t");
        sb.append("ConditionLengthSum"  + "\t");

        sb.append("hasBackwardParamCaller"  + "\t");
        sb.append("backwardParamCallerNum"  + "\t");

        sb.append("hasKeyAPISameClassNum"  + "\t");
        sb.append("keyAPISameClassNum"  + "\t");

        sb.append("hasKeyAPIDiffClassNum"  + "\t");
        sb.append("keyAPIDiffClassNum"  + "\t");

        sb.append("hasKeyAPI"  + "\t");
        sb.append("keyAPINum"  + "\t");


        sb.append("exception"  + "\n");

        for (String version : versions) {
            String str = getExceptionWithGivenVersion(version);
            sb.append(str);
        }
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"exceptionInfoCount.txt", sb.toString(), true);

    }

    /**
     * getExceptionOfCrashInfo from exception.json
     * @return
     */
    private String getExceptionWithGivenVersion(String version) {
        MyConfig.getInstance().setExceptionFilePath("Files"+File.separator+"android" + version +File.separator+ "exceptionInfo" +File.separator);
        String fn = MyConfig.getInstance().getExceptionFilePath() + "summary" + File.separator + "exception.json";
        String jsonString = FileUtils.readJsonFile(fn);
        JSONObject wrapperObject = (JSONObject) JSONObject.parse(jsonString);
        if(wrapperObject==null) return "";

        int exceptionNum = 0;
        Set<String> exceptionType = new HashSet<String>();
        Set<String> uniqueMethod = new HashSet<String>();
        Map<String, Integer> relatedVarTypeMap = new HashMap<String, Integer>();
        Map<String, Integer> relatedCondTypeMap = new HashMap<String, Integer>();
        int extractedMsgWithsS = 0;
        int extractedMsgWithoutsS = 0;
        long conditionLengthSum = 0;
        int hasBackwardParamCaller = 0;
        long backwardParamCallerNum = 0;
        int hasKeyAPISameClassNum = 0;
        long keyAPISameClassNum = 0;
        int hasKeyAPIDiffClassNum = 0;
        long keyAPIDiffClassNum = 0;
        int hasKeyAPI = 0;

        String condLen = "";

        JSONArray methods = wrapperObject.getJSONArray("exceptions");//构建JSONArray数组
        exceptionNum = methods.size();
        for (int i = 0 ; i < methods.size();i++){
            JSONObject jsonObject = (JSONObject)methods.get(i);
            uniqueMethod.add(jsonObject.getString("method"));
            exceptionType.add(jsonObject.getString("type"));
            String message = jsonObject.getString("message");
            if(!message.equals("[\\s\\S]*")){
                if(message.contains("[\\s\\S]*"))
                    extractedMsgWithsS+=1;
                else
                    extractedMsgWithoutsS+=1;
            }

            String relatedVarType = jsonObject.getString("relatedVarType");
            if(!relatedVarTypeMap.containsKey(relatedVarType)){
                relatedVarTypeMap.put(relatedVarType, 1);
            }
            relatedVarTypeMap.put(relatedVarType, relatedVarTypeMap.get(relatedVarType)+1);

            String relatedCondType = jsonObject.getString("relatedCondType");
            if(!relatedCondTypeMap.containsKey(relatedCondType)){
                relatedCondTypeMap.put(relatedCondType, 1);
            }
            relatedCondTypeMap.put(relatedCondType, relatedCondTypeMap.get(relatedCondType)+1);

            String conditions = jsonObject.getString("conditions");
            if(conditions!=null){
                conditionLengthSum += conditions.split(",").length;
                condLen += conditions.split(",").length+ "\n";
            }
            if(jsonObject.getInteger("backwardParamCallerNum")!=null && jsonObject.getInteger("backwardParamCallerNum")>1) {
                hasBackwardParamCaller++; //the method itself will be added
                backwardParamCallerNum += jsonObject.getInteger("backwardParamCallerNum")-1;
            }
            if(jsonObject.getInteger("keyAPISameClassNum")!=null && jsonObject.getInteger("keyAPISameClassNum")>0) {
                hasKeyAPISameClassNum++;
                keyAPISameClassNum += jsonObject.getInteger("keyAPISameClassNum");
            }
            if(jsonObject.getInteger("keyAPIDiffClassNum")!=null && jsonObject.getInteger("keyAPIDiffClassNum")>0) {
                hasKeyAPIDiffClassNum++;
                keyAPIDiffClassNum += jsonObject.getInteger("keyAPIDiffClassNum");
            }
            if((jsonObject.getInteger("keyAPISameClassNum")!=null && jsonObject.getInteger("keyAPISameClassNum")>0) ||
                    (jsonObject.getInteger("keyAPIDiffClassNum")!=null && jsonObject.getInteger("keyAPIDiffClassNum")>0)) {
                hasKeyAPI++;
            }
        }
        FileUtils.writeText2File("condLen.txt", condLen, true);


        StringBuilder sb = new StringBuilder(version + "\t" + exceptionNum +"\t" + exceptionType.size()+"\t" + uniqueMethod.size()+"\t");

        sb.append(extractedMsgWithsS+ "\t");
        sb.append(extractedMsgWithoutsS+ "\t");
        sb.append(extractedMsgWithsS+extractedMsgWithoutsS+ "\t");

        sb.append(relatedVarTypeMap.get(RelatedVarType.Unknown.toString())+ "\t");
        sb.append(relatedVarTypeMap.get(RelatedVarType.Empty.toString()) + "\t");
        sb.append(relatedVarTypeMap.get(RelatedVarType.Parameter.toString()) + "\t");
        sb.append(relatedVarTypeMap.get(RelatedVarType.Field.toString()) + "\t");
        sb.append(relatedVarTypeMap.get(RelatedVarType.ParaAndField.toString()) + "\t");

        sb.append(relatedCondTypeMap.get(RelatedCondType.Basic.toString()) + "\t");
        sb.append(relatedCondTypeMap.get(RelatedCondType.NotReturn.toString()) + "\t");
        sb.append(relatedCondTypeMap.get(RelatedCondType.Empty.toString()) + "\t");

        sb.append(conditionLengthSum + "\t");
        sb.append(hasBackwardParamCaller + "\t");
        sb.append(backwardParamCallerNum + "\t");
        sb.append(hasKeyAPISameClassNum + "\t");
        sb.append(keyAPISameClassNum + "\t");
        sb.append(hasKeyAPIDiffClassNum + "\t");
        sb.append(keyAPIDiffClassNum + "\t");

        sb.append(hasKeyAPI + "\t");
        sb.append(keyAPISameClassNum + keyAPIDiffClassNum + "\t");


        sb.append(exceptionNum+ "\t");
        return  sb+"\n";
    }
}
