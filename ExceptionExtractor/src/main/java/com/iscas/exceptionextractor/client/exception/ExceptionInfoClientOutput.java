package com.iscas.exceptionextractor.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.iscas.exceptionextractor.base.Global;
import com.iscas.exceptionextractor.base.MyConfig;
import com.iscas.exceptionextractor.model.analyzeModel.*;
import com.iscas.exceptionextractor.utils.FileUtils;
import com.iscas.exceptionextractor.utils.PrintUtils;
import com.iscas.exceptionextractor.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.SootClass;
import soot.SootMethod;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * @Author hanada
 * @Date 2022/3/11 15:06
 * @Version 1.0
 */
@Slf4j
public class ExceptionInfoClientOutput {

    public ExceptionInfoClientOutput() {}

    /**
     * write to Json File after each class is Analyzed
     * @param sootClass
     */
    public static void writeThrownExceptionInJsonForCurrentClass(SootClass sootClass, List<ExceptionInfo> exceptionInfoList) {
        String path = MyConfig.getInstance().getExceptionFilePath();
        if(exceptionInfoList.size()>0) {
            String jsonPath = path + sootClass.getName() + ".json";
            log.info("writeToJson "+jsonPath);
            File file = new File(jsonPath);
            JSONObject rootElement = new JSONObject(new LinkedHashMap());
            try {
                file.createNewFile();
                JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
                rootElement.put("exceptions", exceptionListElement);
                for(ExceptionInfo info :exceptionInfoList){
                    JSONObject jsonObject = new JSONObject(true);
                    exceptionListElement.add(jsonObject);
                    addBasic1(jsonObject, info);
                    addBasic2(jsonObject, info);
                    addConditions(jsonObject, info);
                    addRelatedValues(jsonObject, info);
                    addRelatedMethods(jsonObject, info);
                    addCallerOfParam(jsonObject, info);
                }
                PrintWriter printWriter = new PrintWriter(file);
                String jsonString = toJSONString(rootElement, SerializerFeature.PrettyFormat,
                        SerializerFeature.SortField);
                printWriter.write(jsonString);
                printWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // collect

    /**
     * getSummaryJsonArrayOfDeclaredException, json array info, write to exception.json file
     * @param exceptionInfoList
     * @param exceptionListElement
     */
    public static void getSummaryJsonArrayOfDeclaredException(List<ExceptionInfo> exceptionInfoList, JSONArray exceptionListElement) {
        Map<String, JSONObject> map = new HashMap<>();
        if(exceptionInfoList.size()>0) {
            for (ExceptionInfo info : exceptionInfoList) {
                if (map.containsKey(info.getExceptionName())) {
                    JSONObject jsonObject = map.get(info.getExceptionName());
                    jsonObject.getJSONArray("method").add(info.getSootMethod().getSignature());
                } else {
                    JSONObject jsonObject = new JSONObject(true);
                    exceptionListElement.add(jsonObject);
                    addNameAndType(jsonObject, info);
                    map.put(info.getExceptionName(),jsonObject);
                }
            }
        }
    }

    /**
     * getSummaryJsonArrayOfCaughtException, json array info, write to exception.json file
     * @param exceptionInfoList
     * @param exceptionListElement
     */
    public static void getSummaryJsonArrayOfCaughtException(List<ExceptionInfo> exceptionInfoList, JSONArray exceptionListElement) {
        Map<String, JSONObject> map = new HashMap<>();
        if(exceptionInfoList.size()>0) {
            for(ExceptionInfo info :exceptionInfoList){
                if(map.containsKey(info.getExceptionName())){
                    JSONObject jsonObject = map.get(info.getExceptionName());
                    jsonObject.getJSONArray("method").add(info.getSootMethod().getSignature());
                    jsonObject.put("methodNumber",jsonObject.getIntValue("methodNumber")+1);
                }else{
                    JSONObject jsonObject = new JSONObject(true);
                    exceptionListElement.add(jsonObject);
                    addNameAndType(jsonObject, info);
                    jsonObject.put("methodNumber",1);
                    map.put(info.getExceptionName(),jsonObject);
                }
            }
        }
    }
    /**
     * getSummaryJsonArray, json array info, write to exception.json file
     * @param exceptionInfoList
     * @param exceptionListElement
     */
    public static void getSummaryJsonArrayOfThrownException(List<ExceptionInfo> exceptionInfoList, JSONArray exceptionListElement) {
        Map<String, JSONObject> map = new HashMap<>();
        if(exceptionInfoList.size()>0) {
            for(ExceptionInfo info :exceptionInfoList){
                if(map.containsKey(info.getExceptionName())){
                    JSONObject jsonObject = map.get(info.getExceptionName());
                    jsonObject.getJSONArray("method").add(info.getSootMethod().getSignature());
                    jsonObject.put("methodNumber",jsonObject.getIntValue("methodNumber")+1);
                }else{
                    JSONObject jsonObject = new JSONObject(true);
                    exceptionListElement.add(jsonObject);
                    addNameAndType(jsonObject, info);
                    jsonObject.put("methodNumber",1);
                    map.put(info.getExceptionName(),jsonObject);
                }
            }
        }
    }

    /**
     * getSummaryJsonArray, json array info, write to exception.json file
     * @param exceptionInfoList
     * @param exceptionListElement
     */
    public static void getSummaryJsonArrayOfThrownException2(List<ExceptionInfo> exceptionInfoList, JSONArray exceptionListElement) {
        Map<String, JSONObject> map = new HashMap<>();
        if(exceptionInfoList.size()>0) {
            for(ExceptionInfo info :exceptionInfoList){
                JSONObject jsonObject = new JSONObject(true);
                exceptionListElement.add(jsonObject);
                addBasic1(jsonObject, info);
//                addBasic2(jsonObject, info);
                addConditions(jsonObject, info);
                addRelatedValues(jsonObject, info);
                addRelatedMethodNum(jsonObject, info);
                addBackwardParamCallerNum(jsonObject, info);
            }
        }
    }
    public static void writeExceptionSummaryInJson(JSONArray exceptionListElement, String filename) {
        String path = MyConfig.getInstance().getExceptionFilePath()+ "summary"+ File.separator;
        FileUtils.createFolder(path);
        JSONObject rootElement = new JSONObject(new LinkedHashMap());
        File file = new File(path+ filename+".json");
        try {
            file.createNewFile();
            rootElement.put(filename, exceptionListElement);
            PrintWriter printWriter = new PrintWriter(file);
            String jsonString = toJSONString(rootElement, SerializerFeature.PrettyFormat,
                    SerializerFeature.SortField);
            printWriter.write(jsonString);
            printWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addNameAndType(JSONObject jsonObject, ExceptionInfo info) {
        jsonObject.put("ExceptionName", info.getExceptionName());
        jsonObject.put("ExceptionType", info.getExceptionType());
        if(info.getSootMethod()!=null) {
            JSONArray array = new JSONArray();
            array.add(info.getSootMethod().getSignature());
            jsonObject.put("method", array);
        }
    }

    public static void addBasic1(JSONObject jsonObject, ExceptionInfo info) {
        jsonObject.put("method", info.getSootMethod().getSignature());
        jsonObject.put("ExceptionName", info.getExceptionName());
        jsonObject.put("ExceptionType", info.getExceptionType());
        jsonObject.put("message", info.getExceptionMsg());
    }

    public static void addBasic2(JSONObject jsonObject, ExceptionInfo info) {
        jsonObject.put("modifier", info.getModifier());
//        jsonObject.put("osVersionRelated", info.isOsVersionRelated());
//        jsonObject.put("resourceRelated", info.isResourceRelated());
//        jsonObject.put("assessRelated", info.isAssessRelated());
//        jsonObject.put("hardwareRelated", info.isHardwareRelated());
//        jsonObject.put("manifestRelated", info.isManifestRelated());
    }

    public static void addConditions(JSONObject jsonObject, ExceptionInfo info) {
//        jsonObject.put("relatedCondType", info.getRelatedCondType());
        ConditionTrackerInfo conditionTrackerInfo = info.getConditionTrackerInfo();
        if(conditionTrackerInfo.getConditions().size()>0)
            jsonObject.put("conditions", PrintUtils.printList(conditionTrackerInfo.getConditions()));
    }

    public static void addRelatedValues(JSONObject jsonObject, ExceptionInfo info) {
        ConditionTrackerInfo conditionTrackerInfo = info.getConditionTrackerInfo();
        jsonObject.put("relatedVarType", conditionTrackerInfo.getRelatedVarType());
        if(conditionTrackerInfo.getRelatedParamValues().size()>0)
            jsonObject.put("paramValues", PrintUtils.printList(conditionTrackerInfo.getRelatedParamValues()));
        if(conditionTrackerInfo.getRelatedFieldValues().size()>0)
            jsonObject.put("fieldValues", PrintUtils.printList(conditionTrackerInfo.getRelatedFieldValues()));
        if(conditionTrackerInfo.getCaughtValues().size()>0)
            jsonObject.put("caughtValues", PrintUtils.printList(conditionTrackerInfo.getCaughtValues()));
        if(conditionTrackerInfo.getRelatedParamValues().size() + conditionTrackerInfo.getRelatedFieldValues().size() + conditionTrackerInfo.getCaughtValues().size()>0)
            jsonObject.put("relatedValues", PrintUtils.printList(conditionTrackerInfo.getRelatedParamValues())+"; "
                    +PrintUtils.printList(conditionTrackerInfo.getRelatedFieldValues()) +"; "+ PrintUtils.printList(conditionTrackerInfo.getCaughtValues()));
    }

    private static void addBackwardParamCallerNum(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        if(exceptionInfo==null) return;
        ConditionTrackerInfo conditionTrackerInfo = exceptionInfo.getConditionTrackerInfo();
        jsonObject.put("backwardParamCallerNum", conditionTrackerInfo.getCallerOfSingnlar2SourceVar().size());
    }

    public static void addRelatedMethodNum(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        if(exceptionInfo==null) return;
        ConditionTrackerInfo conditionTrackerInfo = exceptionInfo.getConditionTrackerInfo();
        jsonObject.put("keyAPISameClassNum", conditionTrackerInfo.keyAPISameClassNum);
        jsonObject.put("keyAPIDiffClassNum", conditionTrackerInfo.keyAPIDiffClassNum);
    }

    public static void addRelatedMethods(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        if(exceptionInfo==null) return;
        ConditionTrackerInfo conditionTrackerInfo = exceptionInfo.getConditionTrackerInfo();
        jsonObject.put("keyAPISameClassNum", conditionTrackerInfo.keyAPISameClassNum);
        jsonObject.put("keyAPIDiffClassNum", conditionTrackerInfo.keyAPIDiffClassNum);

        JSONArray relatedMethodsSameArray = new JSONArray();
        if (conditionTrackerInfo.getRelatedMethodsInSameClass(true).size() > 0) {
            for (RelatedMethod mtd : conditionTrackerInfo.getRelatedMethodsInSameClass(false)) {
                String mtdString = toJSONString(mtd, SerializerFeature.PrettyFormat,
                        SerializerFeature.SortField);
                JSONObject mtdObject = JSONObject.parseObject(mtdString);  // 转换为json对象
                relatedMethodsSameArray.add(mtdObject);
            }
        }
        jsonObject.put("keyAPISameClass" , relatedMethodsSameArray);

        JSONArray relatedMethodsDiffArray = new JSONArray();
        if (conditionTrackerInfo.getRelatedMethodsInDiffClass(true).size() > 0) {
            for (RelatedMethod mtd : conditionTrackerInfo.getRelatedMethodsInDiffClass(false)) {
                String mtdString = toJSONString(mtd, SerializerFeature.PrettyFormat,
                        SerializerFeature.SortField);
                JSONObject mtdObject = JSONObject.parseObject(mtdString);  // 转换为json对象
                relatedMethodsDiffArray.add(mtdObject);
            }
        }
        jsonObject.put("keyAPIDiffClass" , relatedMethodsDiffArray);
    }

    private static void addCallerOfParam(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        ConditionTrackerInfo conditionTrackerInfo = exceptionInfo.getConditionTrackerInfo();
        JSONObject callerOfSingnlar2SourceVar = new JSONObject(true);
        if (conditionTrackerInfo.getCallerOfSingnlar2SourceVar().size() > 0) {
            for (String mtd : conditionTrackerInfo.getCallerOfSingnlar2SourceVar().keySet()) {
                String vals = PrintUtils.printList(conditionTrackerInfo.getCallerOfSingnlar2SourceVar().get(mtd));
                callerOfSingnlar2SourceVar.put(mtd, vals);
            }
        }
        jsonObject.put("callerOfSignaler2SourceVar" , callerOfSingnlar2SourceVar);
    }

    /**
     * printExceptionInfoList
     */
    public static void printExceptionInfoList() {
        StringBuilder sb = new StringBuilder();
        Map<String, List<ExceptionInfo>> map = Global.v().getAppModel().getMethod2ExceptionList();
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if(!map.containsKey(sootMethod.getSignature())) continue;
                for (ExceptionInfo exceptionInfo: map.get(sootMethod.getSignature())) {
                    //print condition information
                    sb.append(sootMethod.getSignature() + "\n");
                    sb.append(exceptionInfo.getExceptionName() + "\n");
                    StringBuilder subStr = new StringBuilder();
                    for (ConditionWithValueSet conditionWithValueSet : exceptionInfo.getConditionTrackerInfo().getRefinedConditions().values()) {
                        if (conditionWithValueSet.toString().length() > 0)
                            subStr.append(conditionWithValueSet + "\n");
                    }
                    if (subStr.length() ==0 && exceptionInfo.getConditionTrackerInfo().getRelatedVarType() == RelatedVarType.Empty) {
                        subStr.append("RefinedCondition: no condition\n");
                    }
                    if (subStr.length() >0){
                        sb.append(subStr+ "\n");
                    }
                }
            }
        }
        FileUtils.writeText2File(MyConfig.getInstance().getExceptionFilePath() + "exceptionConditions.txt", sb.toString(), false);


    }
}
