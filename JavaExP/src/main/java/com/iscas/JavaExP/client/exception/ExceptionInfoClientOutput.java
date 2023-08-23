package com.iscas.JavaExP.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.iscas.JavaExP.base.Global;
import com.iscas.JavaExP.base.MyConfig;
import com.iscas.JavaExP.model.analyzeModel.*;
import com.iscas.JavaExP.utils.FileUtils;
import com.iscas.JavaExP.utils.PrintUtils;
import com.iscas.JavaExP.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;

import java.io.File;
import java.io.FileNotFoundException;
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

    public ExceptionInfoClientOutput() throws FileNotFoundException {}

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
        jsonObject.put("modifier", info.getModifier());
        if(info.getIntraThrowUnit()==null)
            jsonObject.put("throwUnit", info.getUnit().toString());
        else
            jsonObject.put("throwUnit", info.getUnit().toString()+";"+info.getIntraThrowUnit().toString());
        jsonObject.put("throwUnitOrder", info.getThrowUnitOrder());
        jsonObject.put("ExceptionName", info.getExceptionName());
        jsonObject.put("ExceptionType", info.getExceptionType());
        jsonObject.put("message", info.getExceptionMsg());
    }

    public static void addBasic2(JSONObject jsonObject, ExceptionInfo info) {

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

    private static void addPreConditions(JSONObject jsonObject, ExceptionInfo exceptionInfo) {
        ArrayList<String> preCondList= new ArrayList<>();
        if(exceptionInfo==null) return;
        // 将数组添加到JSON对象中
        if (exceptionInfo.isRethrow()) {
            Trap trap = exceptionInfo.getTrap();
            preCondList.add("This is a rethrow exception after an exception with type " + trap.getException().getName() + " is caught, " +
                    "when executing the statements from " + trap.getBeginUnit() + " to " + trap.getEndUnit());
        }
        for (ConditionWithValueSet conditionWithValueSet : exceptionInfo.getConditionTrackerInfo().getRefinedConditions().values()) {
            if (conditionWithValueSet.toString().length() > 0){
                for(RefinedCondition refinedCondition: conditionWithValueSet.getRefinedConditions())
                    if(!preCondList.contains(refinedCondition.toString())) {
                        if (refinedCondition.getLeftStr().length() == 0)
                            refinedCondition.setLeftStr("sth.");
                        if (refinedCondition.getRightStr().length() == 0)
                            refinedCondition.setRightStr("sth.");
                        preCondList.add(refinedCondition.toString());
                    }
            }
        }
        if (!exceptionInfo.isRethrow() && preCondList.size() == 0 && exceptionInfo.getConditionTrackerInfo().getRelatedVarType() == RelatedVarType.Empty) {
            preCondList.add("Direct Throw Without Any Condition");//RefinedCondition:
        }
        if (preCondList.size() > 0) {
            jsonObject.put("preConditions", preCondList);
        }
    }
    /**
     * printExceptionInfoList in json format
     */
    public static void printExceptionInfoList(){
        Set<String> history = new HashSet<>();
        Map<String, List<ExceptionInfo>> map = Global.v().getAppModel().getMethod2ExceptionList();
        JSONObject rootElement = new JSONObject(new LinkedHashMap());
        JSONArray classListElement = new JSONArray(new ArrayList<>());
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
            JSONObject classElement = new JSONObject(new LinkedHashMap());
            JSONArray methodListElement = new JSONArray(new ArrayList<>());
            for (SootMethod sootMethod : sootClass.getMethods()) {
                JSONObject methodElement = new JSONObject(new LinkedHashMap());
                JSONArray exceptionListElement = new JSONArray(new ArrayList<>());
                if (!map.containsKey(sootMethod.getSignature())) continue;
                for (ExceptionInfo exceptionInfo : map.get(sootMethod.getSignature())) {
                    JSONObject jsonObject = new JSONObject(true);
                    addBasic1(jsonObject, exceptionInfo);
                    addBasic2(jsonObject, exceptionInfo);
                    addConditions(jsonObject, exceptionInfo);
                    addPreConditions(jsonObject, exceptionInfo);
                    if (!history.contains(jsonObject.toString())) {
                        history.add(jsonObject.toString());
                        exceptionListElement.add(jsonObject);
                    }
                }
                if(exceptionListElement.size()>0) {
                    methodElement.put("methodName", sootMethod.getSignature());
                    if(sootMethod.isPublic())
                        methodElement.put("modifier", "public");
                    else if(sootMethod.isPrivate())
                        methodElement.put("modifier","private");
                    else if(sootMethod.isProtected())
                        methodElement.put("modifier","protected");
                    else
                        methodElement.put("modifier","default");
                    methodElement.put("exceptions", exceptionListElement);
                    methodListElement.add(methodElement);
                }
            }
            if(methodListElement.size()>0) {
                classElement.put("className", sootClass.getName());
                classElement.put("methods", methodListElement);
                classListElement.add(classElement);
            }
        }
        if(classListElement.size()>0) {
            rootElement.put("classes", classListElement);
        }
        try {
            PrintWriter printWriter = new PrintWriter(MyConfig.getInstance().getExceptionFilePath() + "exceptionConditions.txt");
            String jsonString = toJSONString(rootElement, SerializerFeature.PrettyFormat, SerializerFeature.SortField);
            printWriter.write(jsonString);
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * printExceptionInfoList in json format
     */
    public static void printExceptionInfoListOfAll(){
        Set<String> history = new HashSet<>();
        Map<String, List<ExceptionInfo>> map = Global.v().getAppModel().getMethod2ExceptionList();
        JSONObject rootElement = new JSONObject(new LinkedHashMap());
        JSONArray classListElement = new JSONArray(new ArrayList<>());
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
            JSONObject classElement = new JSONObject(new LinkedHashMap());
            JSONArray methodListElement = new JSONArray(new ArrayList<>());
            for (SootMethod sootMethod : sootClass.getMethods()) {
                JSONObject methodElement = new JSONObject(new LinkedHashMap());
                JSONArray exceptionListElement = new JSONArray(new ArrayList<>());
                if (map.containsKey(sootMethod.getSignature())) {
                    for (ExceptionInfo exceptionInfo : map.get(sootMethod.getSignature())) {
                        JSONObject jsonObject = new JSONObject(true);
                        addBasic1(jsonObject, exceptionInfo);
                        addBasic2(jsonObject, exceptionInfo);
                        addConditions(jsonObject, exceptionInfo);
                        addPreConditions(jsonObject, exceptionInfo);
                        if (!history.contains(jsonObject.toString())) {
                            history.add(jsonObject.toString());
                            exceptionListElement.add(jsonObject);
                        }
                    }
                }
                methodElement.put("methodName", sootMethod.getSignature());
                methodElement.put("exceptions", exceptionListElement);
                methodListElement.add(methodElement);
            }
            classElement.put("className", sootClass.getName());
            classElement.put("methods", methodListElement);
            classListElement.add(classElement);

        }
        rootElement.put("classes", classListElement);

        try {
            PrintWriter printWriter = new PrintWriter(MyConfig.getInstance().getExceptionFilePath() + "exceptionConditionsOfAll.txt");
            String jsonString = toJSONString(rootElement, SerializerFeature.PrettyFormat, SerializerFeature.SortField);
            printWriter.write(jsonString);
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
