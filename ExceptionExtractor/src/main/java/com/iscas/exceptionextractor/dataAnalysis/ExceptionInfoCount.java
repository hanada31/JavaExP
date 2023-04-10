package com.iscas.exceptionextractor.dataAnalysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.exceptionextractor.base.MyConfig;
import com.iscas.exceptionextractor.client.exception.ExceptionType;
import com.iscas.exceptionextractor.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @Author hanada
 * @Date 2022/6/24 10:22
 * @Version 1.0
 */
@Slf4j
public class ExceptionInfoCount {
//    String[] versions = {"2.3", "4.4", "5.0", "6.0", "7.0", "8.0", "9.0", "10.0", "11.0", "12.0"};
    String[] versions = {"2.3"};
    JSONObject declaredExceptions;
    JSONObject thrownExceptions;
    JSONObject caughtExceptions;

    public void analyze() {
        getExceptionNumberCountSummary();
        getExceptionTypeCountSummary();
        getEachExceptionTypeCount();
        log.info("getExceptionOfCrashInfo Finish...");

    }


    private void readFiles(String version) {
        MyConfig.getInstance().setExceptionFilePath(MyConfig.getInstance().getResultFolder()+"android" + version +File.separator+ "exceptionInfo" +File.separator);
        String declaredExceptionFile = MyConfig.getInstance().getExceptionFilePath() + "summary" + File.separator + "declaredException.json";
        String declaredExceptionFileJson = FileUtils.readJsonFile(declaredExceptionFile);
        declaredExceptions = (JSONObject) JSONObject.parse(declaredExceptionFileJson);

        String thrownExceptionFile = MyConfig.getInstance().getExceptionFilePath() + "summary" + File.separator + "thrownException.json";
        String thrownExceptionFileJson = FileUtils.readJsonFile(thrownExceptionFile);
        thrownExceptions = (JSONObject) JSONObject.parse(thrownExceptionFileJson);

        String caughtExceptionFile = MyConfig.getInstance().getExceptionFilePath() + "summary" + File.separator + "caughtException.json";
        String caughtExceptionFileJson = FileUtils.readJsonFile(caughtExceptionFile);
        caughtExceptions = (JSONObject) JSONObject.parse(caughtExceptionFileJson);
    }

    private void getExceptionNumberCountSummary() {
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"exceptionNumberCount.txt", "", false);
        log.info("write to "+ MyConfig.getInstance().getExceptionFilePath() +"exceptionNumberCount.txt");

        StringBuilder sb = new StringBuilder();
        sb.append("tag\t");
        sb.append("version\t" + "declaredNumber\t" + "thrownNumber\t" + "caughtNumber\t");
        sb.append("declaredNumber\t" + "thrownTypeNumber\t" + "caughtTypeNumber\n");

        for (String version : versions) {
            String str = getExceptionCount(version);
            sb.append("overall count\t");
            sb.append(str);
        }
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"exceptionNumberCount.txt", sb.toString(), true);
    }

    private void getExceptionTypeCountSummary() {

        StringBuilder sb = new StringBuilder();
        sb.append("tag\t");
        sb.append("version\t" + "declaredCustomChecked\t" + "declaredCustomUnChecked_Runtime\t");
        sb.append("thrownStandardChecked\t" + "thrownStandardUnChecked_Runtime\t" + "thrownCustomChecked\t" + "thrownCustomUnChecked_Runtime\t"+ "thrownThirdParty\t");
        sb.append("caughtStandardChecked\t" + "caughtStandardUnChecked_Runtime\t" + "caughtCustomChecked\t" + "caughtCustomUnChecked_Runtime\t"+ "caughtThirdParty\n");

        for (String version : versions) {
            boolean repeat = false;
            String str = getExceptionTypeCountSummary(version,repeat);
            sb.append("type count\t");
            sb.append(str);
        }

        sb.append("tag\t");
        sb.append("version\t" + "declaredCustomChecked\t" + "declaredCustomUnChecked_Runtime\t");
        sb.append("thrownStandardChecked\t" + "thrownStandardUnChecked_Runtime\t" + "thrownCustomChecked\t" + "thrownCustomUnChecked_Runtime\t"+ "thrownThirdParty\t");
        sb.append("caughtStandardChecked\t" + "caughtStandardUnChecked_Runtime\t" + "caughtCustomChecked\t" + "caughtCustomUnChecked_Runtime\t"+ "caughtThirdParty\n");

        for (String version : versions) {
            boolean repeat = true;
            String str = getExceptionTypeCountSummary(version,repeat);
            sb.append("unit count\t");
            sb.append(str);
        }
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"exceptionNumberCount.txt", sb.toString(), true);
    }

    /**
     * getExceptionOfCrashInfo from exception.json
     * @return
     */
    private String getExceptionCount(String version) {
        readFiles(version);
        int declaredNumber = 0;
        int thrownNumber = 0;
        int thrownTypeNumber = 0;
        int caughtNumber = 0;
        int caughtTypeNumber = 0;

        if(declaredExceptions!=null){
            JSONArray declared = declaredExceptions.getJSONArray("declaredException");//构建JSONArray数组
            declaredNumber = declared.size();
        }
        if(thrownExceptions!=null) {
            JSONArray thrown = thrownExceptions.getJSONArray("thrownException");//构建JSONArray数组
            thrownTypeNumber = thrown.size();
            for(Object object:thrown){
                JSONObject jsonObject = (JSONObject) object;
                thrownNumber += jsonObject.getInteger("methodNumber");
            }
        }
        if(caughtExceptions!=null) {
            JSONArray caught = caughtExceptions.getJSONArray("caughtException");//构建JSONArray数组
            caughtTypeNumber = caught.size();
            for(Object object:caught){
                JSONObject jsonObject = (JSONObject) object;
                caughtNumber += jsonObject.getInteger("methodNumber");
            }
        }

        StringBuilder sb = new StringBuilder(version + "\t" + declaredNumber +"\t" + thrownNumber+"\t" + caughtNumber+"\t" +
                declaredNumber +"\t"  + thrownTypeNumber+"\t" + caughtTypeNumber+"\n");

        return  sb+"\n";
    }

    private String getExceptionTypeCountSummary(String version, boolean repeat) {
        readFiles(version);
        int declaredNumber = 0;
        int declaredCustomChecked = 0;
        int declaredCustomUnChecked_Runtime = 0;
        int thrownNumber = 0;
        int thrownStandardChecked = 0;
        int thrownStandardUnChecked_Runtime = 0;
        int thrownCustomChecked = 0;
        int thrownCustomUnChecked_Runtime = 0;
        int thrownThirdParty = 0;
        int caughtNumber = 0;
        int caughtStandardChecked = 0;
        int caughtStandardUnChecked_Runtime = 0;
        int caughtCustomChecked = 0;
        int caughtCustomUnChecked_Runtime = 0;
        int caughtThirdParty = 0;

        if(declaredExceptions!=null){
            JSONArray declared = declaredExceptions.getJSONArray("declaredException");//构建JSONArray数组
            for(Object object:declared){
                JSONObject jsonObject = (JSONObject) object;
                int number = 1;
                if(jsonObject.getString("ExceptionType").equals(ExceptionType.CustomChecked.toString()))
                    declaredCustomChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.CustomUnChecked_Runtime.toString()))
                    declaredCustomUnChecked_Runtime += number;
            }
            declaredNumber = declaredCustomChecked + declaredCustomUnChecked_Runtime;
        }
        if(thrownExceptions!=null) {
            JSONArray thrown = thrownExceptions.getJSONArray("thrownException");//构建JSONArray数组
            for(Object object:thrown){
                JSONObject jsonObject = (JSONObject) object;
                int number = repeat? jsonObject.getInteger("methodNumber"):1;
                if(jsonObject.getString("ExceptionType").equals(ExceptionType.StandardChecked.toString()))
                    thrownStandardChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.StandardUnChecked_Runtime.toString()))
                    thrownStandardUnChecked_Runtime += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.CustomChecked.toString()))
                    thrownCustomChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.CustomUnChecked_Runtime.toString()))
                    thrownCustomUnChecked_Runtime += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.ThirdParty.toString()))
                    thrownCustomUnChecked_Runtime += number;
            }
            thrownNumber = thrownStandardChecked + thrownStandardUnChecked_Runtime + thrownCustomChecked + thrownCustomUnChecked_Runtime + thrownThirdParty;
        }
        if(caughtExceptions!=null) {
            JSONArray caught = caughtExceptions.getJSONArray("caughtException");//构建JSONArray数组
            for(Object object:caught){
                JSONObject jsonObject = (JSONObject) object;
                int number = repeat? jsonObject.getInteger("methodNumber"):1;
                if(jsonObject.getString("ExceptionType").equals(ExceptionType.StandardChecked.toString()))
                    caughtStandardChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.StandardUnChecked_Runtime.toString()))
                    caughtStandardUnChecked_Runtime += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.CustomChecked.toString()))
                    caughtCustomChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.CustomUnChecked_Runtime.toString()))
                    caughtCustomUnChecked_Runtime += number;
                else if(jsonObject.getString("ExceptionType").equals(ExceptionType.ThirdParty.toString()))
                    caughtThirdParty += number;
            }
            caughtNumber = caughtStandardChecked + caughtStandardUnChecked_Runtime + caughtCustomChecked + caughtCustomUnChecked_Runtime + caughtThirdParty;
        }

        StringBuilder sb = new StringBuilder(version +"\t" + declaredCustomChecked+"\t" + declaredCustomUnChecked_Runtime+ "\t");
        sb.append(thrownStandardChecked+"\t" + thrownStandardUnChecked_Runtime+"\t" + thrownCustomChecked+"\t" + thrownCustomUnChecked_Runtime+"\t"+ thrownThirdParty+"\t");
        sb.append(caughtStandardChecked+"\t" + caughtStandardUnChecked_Runtime+"\t" + caughtCustomChecked+"\t" + caughtCustomUnChecked_Runtime+"\t"+ caughtThirdParty+"\n");
        return  sb+"\n";
    }

    private void getEachExceptionTypeCount() {
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"eachExceptionTypeCount.txt", "", false);
        log.info("write to "+ MyConfig.getInstance().getExceptionFilePath() +"eachExceptionTypeCount.txt");

        StringBuilder sb = new StringBuilder();
        sb.append("tag + version\t");
        sb.append("exceptionName\t" + "count\n");

        for (String version : versions) {
            readFiles(version);
            if(thrownExceptions!=null) {
                JSONArray thrown = thrownExceptions.getJSONArray("thrownException");//构建JSONArray数组
                for(Object object:thrown){
                    JSONObject jsonObject = (JSONObject) object;
                    sb.append("throw count\t" + jsonObject.getString("ExceptionName") +"\t" + jsonObject.getInteger("methodNumber")+"\n");
                }
            }
        }

        sb.append("\ntag + version\t");
        sb.append("exceptionName\t" + "count\n");

        for (String version : versions) {
            readFiles(version);
            if(caughtExceptions!=null) {
                JSONArray caught = caughtExceptions.getJSONArray("caughtException");//构建JSONArray数组
                for(Object object:caught){
                    JSONObject jsonObject = (JSONObject) object;
                    sb.append("catch count\t" + jsonObject.getString("ExceptionName") +"\t" + jsonObject.getInteger("methodNumber")+"\n");
                }
            }
        }
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"eachExceptionTypeCount.txt", sb.toString(), true);
    }



}
