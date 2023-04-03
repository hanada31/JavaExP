package com.iscas.crashtracker.client.exception;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.utils.FileUtils;
import com.iscas.crashtracker.utils.PrintUtils;
import lombok.extern.slf4j.Slf4j;
import soot.SootClass;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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
    public static void writeJsonForCurrentClass(SootClass sootClass, List<ExceptionInfo> exceptionInfoList) {
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
     * getSummaryJsonArray, json array info, write to exception.json file
     * @param exceptionInfoList
     * @param exceptionListElement
     */
    public static void getSummaryJsonArray(List<ExceptionInfo> exceptionInfoList, JSONArray exceptionListElement) {
        if(exceptionInfoList.size()>0) {
            for(ExceptionInfo info :exceptionInfoList){
                JSONObject jsonObject = new JSONObject(true);
                exceptionListElement.add(jsonObject);
                addBasic1(jsonObject, info);
                addBasic2(jsonObject, info);
                addConditions(jsonObject, info);
                addRelatedValues(jsonObject, info);
                addRelatedMethodNum(jsonObject, info);
                addBackwardParamCallerNum(jsonObject, info);
            }
        }
    }


    public static void writeJsonForFramework(JSONArray exceptionListElement) {
        String path = MyConfig.getInstance().getExceptionFilePath()+ "summary"+ File.separator;
        FileUtils.createFolder(path);
        JSONObject rootElement = new JSONObject(new LinkedHashMap());
        File file = new File(path+ "exception.json");
        try {
            file.createNewFile();
            rootElement.put("exceptions", exceptionListElement);
            PrintWriter printWriter = new PrintWriter(file);
            String jsonString = toJSONString(rootElement, SerializerFeature.PrettyFormat,
                    SerializerFeature.SortField);
            printWriter.write(jsonString);
            printWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void addBasic1(JSONObject jsonObject, ExceptionInfo info) {
        jsonObject.put("method", info.getSootMethod().getSignature());
        jsonObject.put("message", info.getExceptionMsg());
    }

    public static void addBasic2(JSONObject jsonObject, ExceptionInfo info) {
//        jsonObject.put("relatedVarType", info.getRelatedVarType());
        jsonObject.put("relatedVarType", info.getRelatedVarType());
//        jsonObject.put("relatedCondType", info.getRelatedCondType());
        jsonObject.put("relatedCondType", info.getRelatedCondType());
        jsonObject.put("modifier", info.getModifier());
        jsonObject.put("type", info.getExceptionType());
        jsonObject.put("osVersionRelated", info.isOsVersionRelated());
        jsonObject.put("resourceRelated", info.isResourceRelated());
        jsonObject.put("assessRelated", info.isAssessRelated());
        jsonObject.put("hardwareRelated", info.isHardwareRelated());
        jsonObject.put("manifestRelated", info.isManifestRelated());
    }

    public static void addConditions(JSONObject jsonObject, ExceptionInfo info) {
//        JSONArray conds  = new JSONArray(new ArrayList<>());
//        for(Value condValue: info.getConditions()){
//            JSONObject jsonObjectTemp = new JSONObject(true);
//            jsonObjectTemp.put("cond", condValue.getClass().getName()+"-->"+condValue.toString());
//            conds.add(jsonObjectTemp);
//        }
//        jsonObject.put("conds",conds);

        if(info.getConditions().size()>0)
            jsonObject.put("conditions", PrintUtils.printList(info.getConditions()));
    }

    public static void addRelatedValues(JSONObject jsonObject, ExceptionInfo info) {
        if(info.getRelatedParamValues().size()>0)
            jsonObject.put("paramValues", PrintUtils.printList(info.getRelatedParamValues()));
        if(info.getRelatedFieldValues().size()>0)
            jsonObject.put("fieldValues", PrintUtils.printList(info.getRelatedFieldValues()));
        if(info.getCaughtedValues().size()>0)
            jsonObject.put("caughtValues", PrintUtils.printList(info.getCaughtedValues()));
        if(info.getRelatedParamValues().size() + info.getRelatedFieldValues().size() + info.getCaughtedValues().size()>0)
            jsonObject.put("relatedValues", PrintUtils.printList(info.getRelatedParamValues())+"; "
                    +PrintUtils.printList(info.getRelatedFieldValues()) +"; "+ PrintUtils.printList(info.getCaughtedValues()));
    }

    private static void addBackwardParamCallerNum(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        if(exceptionInfo==null) return;
        jsonObject.put("backwardParamCallerNum", exceptionInfo.getCallerOfSingnlar2SourceVar().size());
    }

    public static void addRelatedMethodNum(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        if(exceptionInfo==null) return;
        jsonObject.put("keyAPISameClassNum", exceptionInfo.keyAPISameClassNum);
        jsonObject.put("keyAPIDiffClassNum", exceptionInfo.keyAPIDiffClassNum);
    }

    public static void addRelatedMethods(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        if(exceptionInfo==null) return;
        jsonObject.put("keyAPISameClassNum", exceptionInfo.keyAPISameClassNum);
        jsonObject.put("keyAPIDiffClassNum", exceptionInfo.keyAPIDiffClassNum);

        JSONArray relatedMethodsSameArray = new JSONArray();
        if (exceptionInfo.getRelatedMethodsInSameClass(true).size() > 0) {
            for (RelatedMethod mtd : exceptionInfo.getRelatedMethodsInSameClass(false)) {
                String mtdString = toJSONString(mtd, SerializerFeature.PrettyFormat,
                        SerializerFeature.SortField);
                JSONObject mtdObject = JSONObject.parseObject(mtdString);  // 转换为json对象
                relatedMethodsSameArray.add(mtdObject);
            }
        }
        jsonObject.put("keyAPISameClass" , relatedMethodsSameArray);

        JSONArray relatedMethodsDiffArray = new JSONArray();
        if (exceptionInfo.getRelatedMethodsInDiffClass(true).size() > 0) {
            for (RelatedMethod mtd : exceptionInfo.getRelatedMethodsInDiffClass(false)) {
                String mtdString = toJSONString(mtd, SerializerFeature.PrettyFormat,
                        SerializerFeature.SortField);
                JSONObject mtdObject = JSONObject.parseObject(mtdString);  // 转换为json对象
                relatedMethodsDiffArray.add(mtdObject);
            }
        }
        jsonObject.put("keyAPIDiffClass" , relatedMethodsDiffArray);
    }

    private static void addCallerOfParam(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        JSONObject callerOfSingnlar2SourceVar = new JSONObject(true);
        if (exceptionInfo.getCallerOfSingnlar2SourceVar().size() > 0) {
            for (String mtd : exceptionInfo.getCallerOfSingnlar2SourceVar().keySet()) {
                String vals = PrintUtils.printList(exceptionInfo.getCallerOfSingnlar2SourceVar().get(mtd));
                callerOfSingnlar2SourceVar.put(mtd, vals);
            }
        }
        jsonObject.put("callerOfSingnlar2SourceVar" , callerOfSingnlar2SourceVar);
    }

}
