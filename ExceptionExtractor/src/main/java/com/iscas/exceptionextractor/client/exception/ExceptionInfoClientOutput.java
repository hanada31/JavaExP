package com.iscas.exceptionextractor.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.iscas.exceptionextractor.base.MyConfig;
import com.iscas.exceptionextractor.utils.FileUtils;
import com.iscas.exceptionextractor.utils.PrintUtils;
import lombok.extern.slf4j.Slf4j;
import soot.SootClass;

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
//                addBasic1(jsonObject, info);
//                addBasic2(jsonObject, info);
//                addConditions(jsonObject, info);
//                addRelatedValues(jsonObject, info);
//                addRelatedMethodNum(jsonObject, info);
//                addBackwardParamCallerNum(jsonObject, info);
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
                addBasic2(jsonObject, info);
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
        jsonObject.put("ExceptionName", info.getExceptionName());
        jsonObject.put("ExceptionType", info.getExceptionType());
        jsonObject.put("method", info.getSootMethod().getSignature());
        jsonObject.put("message", info.getExceptionMsg());
    }

    public static void addBasic2(JSONObject jsonObject, ExceptionInfo info) {
        jsonObject.put("relatedCondType", info.getRelatedCondType());
        jsonObject.put("modifier", info.getModifier());
        jsonObject.put("osVersionRelated", info.isOsVersionRelated());
        jsonObject.put("resourceRelated", info.isResourceRelated());
        jsonObject.put("assessRelated", info.isAssessRelated());
        jsonObject.put("hardwareRelated", info.isHardwareRelated());
        jsonObject.put("manifestRelated", info.isManifestRelated());
    }

    public static void addConditions(JSONObject jsonObject, ExceptionInfo info) {
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
