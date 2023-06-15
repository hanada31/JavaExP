package com.iscas.JavaExP.dataAnalysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.JavaExP.base.MyConfig;
import com.iscas.JavaExP.model.analyzeModel.AppModel;
import com.iscas.JavaExP.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author hanada
 * @Date 2022/6/24 10:22
 * @Version 1.0
 */
@Slf4j
public class ExceptionInfoCount {
//    String[] jars = {"android2.3", "android4.4", "android5.0", "android6.0", "android7.0", "android8.0", "android9.0", "android10.0", "android11.0", "android12.0"};
    String[] jars = {"jdk1.8\\nashorn1.8", "jdk1.8\\rt1.8", "jdk1.8\\tools1.8"};
    JSONObject declaredExceptions;
    JSONObject thrownExceptions;
    JSONObject caughtExceptions;
    static String exceptionNumberCountFile= "exceptionNumberCount.txt";
    static String eachExceptionTypeCountFile= "eachExceptionTypeCount.txt";
    public void analyze() {
        getExceptionNumberCountSummary();
        getExceptionTypeCountSummary();
        getEachExceptionTypeCount();
        try {
            rewriteExceptionNumberCount();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("getExceptionOfCrashInfo Finish...");

    }


    private void readFiles(String name) {
        MyConfig.getInstance().setExceptionFilePath(MyConfig.getInstance().getResultFolder()+ name +File.separator+ "exceptionInfo" +File.separator);
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
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +exceptionNumberCountFile, "", false);
        log.info("write to "+ MyConfig.getInstance().getResultFolder() +exceptionNumberCountFile);

        StringBuilder sb = new StringBuilder();
        sb.append("tag\t"+ "name\t" +"declaredNumber\t" + "thrownNumber\t" + "caughtNumber\t");
        sb.append("declaredNumber\t" + "thrownTypeNumber\t" + "caughtTypeNumber\n");

        for (String jar : jars) {
            String str = getExceptionCount(jar);
            sb.append("overallNum\t" +jar +"\t");
            sb.append(str);
        }
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +exceptionNumberCountFile, sb.toString(), true);
    }

    private void getExceptionTypeCountSummary() {

        StringBuilder sb = new StringBuilder();
        sb.append("\ntag\t"+ "name\t"+ "declaredCustomChecked\t" + "declaredCustomUnChecked_Runtime\t");
        sb.append("thrownStandardChecked\t" + "thrownStandardUnChecked_Runtime\t" + "thrownCustomChecked\t" + "thrownCustomUnChecked_Runtime\t"+ "thrownThirdParty\t");
        sb.append("caughtStandardChecked\t" + "caughtStandardUnChecked_Runtime\t" + "caughtCustomChecked\t" + "caughtCustomUnChecked_Runtime\t"+ "caughtThirdParty\n");

        for (String jar : jars) {
            boolean repeat = false;
            String str = getExceptionTypeCountSummary(jar,repeat);
            sb.append("typeNum\t" +jar +"\t");
            sb.append(str);
        }

        sb.append("\ntag\t"+ "name\t"+"declaredCustomChecked\t" + "declaredCustomUnChecked_Runtime\t");
        sb.append("thrownStandardChecked\t" + "thrownStandardUnChecked_Runtime\t" + "thrownCustomChecked\t" + "thrownCustomUnChecked_Runtime\t"+ "thrownThirdParty\t");
        sb.append("caughtStandardChecked\t" + "caughtStandardUnChecked_Runtime\t" + "caughtCustomChecked\t" + "caughtCustomUnChecked_Runtime\t"+ "caughtThirdParty\n");

        for (String jar : jars) {
            boolean repeat = true;
            String str = getExceptionTypeCountSummary(jar,repeat);
            sb.append("unitNum\t" + "V"+jar
                    +"\t" );
            sb.append(str);
        }
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +exceptionNumberCountFile, sb.toString(), true);
    }

    /**
     * getExceptionOfCrashInfo from exception.json
     * @return
     */
    private String getExceptionCount(String name) {
        readFiles(name);
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

        StringBuilder sb = new StringBuilder( declaredNumber +"\t" + thrownNumber+"\t" + caughtNumber+"\t" +
                declaredNumber +"\t"  + thrownTypeNumber+"\t" + caughtTypeNumber);

        return  sb+"\n";
    }

    private String getExceptionTypeCountSummary(String name, boolean repeat) {
        readFiles(name);
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
                if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.CustomChecked.toString()))
                    declaredCustomChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.CustomUnChecked_Runtime.toString()))
                    declaredCustomUnChecked_Runtime += number;
            }
            declaredNumber = declaredCustomChecked + declaredCustomUnChecked_Runtime;
        }
        if(thrownExceptions!=null) {
            JSONArray thrown = thrownExceptions.getJSONArray("thrownException");//构建JSONArray数组
            for(Object object:thrown){
                JSONObject jsonObject = (JSONObject) object;
                int number = repeat? jsonObject.getInteger("methodNumber"):1;
                if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.StandardChecked.toString()))
                    thrownStandardChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.StandardUnChecked_Runtime.toString()))
                    thrownStandardUnChecked_Runtime += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.CustomChecked.toString()))
                    thrownCustomChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.CustomUnChecked_Runtime.toString()))
                    thrownCustomUnChecked_Runtime += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.ThirdParty.toString()))
                    thrownCustomUnChecked_Runtime += number;
            }
            thrownNumber = thrownStandardChecked + thrownStandardUnChecked_Runtime + thrownCustomChecked + thrownCustomUnChecked_Runtime + thrownThirdParty;
        }
        if(caughtExceptions!=null) {
            JSONArray caught = caughtExceptions.getJSONArray("caughtException");//构建JSONArray数组
            for(Object object:caught){
                JSONObject jsonObject = (JSONObject) object;
                int number = repeat? jsonObject.getInteger("methodNumber"):1;
                if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.StandardChecked.toString()))
                    caughtStandardChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.StandardUnChecked_Runtime.toString()))
                    caughtStandardUnChecked_Runtime += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.CustomChecked.toString()))
                    caughtCustomChecked += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.CustomUnChecked_Runtime.toString()))
                    caughtCustomUnChecked_Runtime += number;
                else if(jsonObject.getString("ExceptionType").equals(AppModel.ExceptionType.ThirdParty.toString()))
                    caughtThirdParty += number;
            }
            caughtNumber = caughtStandardChecked + caughtStandardUnChecked_Runtime + caughtCustomChecked + caughtCustomUnChecked_Runtime + caughtThirdParty;
        }

        StringBuilder sb = new StringBuilder( declaredCustomChecked+"\t" + declaredCustomUnChecked_Runtime+ "\t");
        sb.append(thrownStandardChecked+"\t" + thrownStandardUnChecked_Runtime+"\t" + thrownCustomChecked+"\t" + thrownCustomUnChecked_Runtime+"\t"+ thrownThirdParty+"\t");
        sb.append(caughtStandardChecked+"\t" + caughtStandardUnChecked_Runtime+"\t" + caughtCustomChecked+"\t" + caughtCustomUnChecked_Runtime+"\t"+ caughtThirdParty);
        return  sb+"\n";
    }

    private void getEachExceptionTypeCount() {
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +eachExceptionTypeCountFile, "", false);
        log.info("write to "+ MyConfig.getInstance().getExceptionFilePath() +eachExceptionTypeCountFile);

        StringBuilder sb = new StringBuilder();
        for (String jar : jars) {
            sb.append("\ntag\tname\texceptionName\tcount\n");
            readFiles(jar);
            if(thrownExceptions!=null) {
                JSONArray thrown = thrownExceptions.getJSONArray("thrownException");//构建JSONArray数组
                for(Object object:thrown){
                    JSONObject jsonObject = (JSONObject) object;
                    sb.append("throwNum\t"+jar+"\t" + jsonObject.getString("ExceptionName") +"\t" + jsonObject.getInteger("methodNumber")+"\n");
                }
            }
        }

        for (String jar : jars) {
            sb.append("\ntag\tname\texceptionName\tcount\n");
            readFiles(jar);
            if(caughtExceptions!=null) {
                JSONArray caught = caughtExceptions.getJSONArray("caughtException");//构建JSONArray数组
                for(Object object:caught){
                    JSONObject jsonObject = (JSONObject) object;
                    sb.append("catchNum\t"+jar+"\t" + jsonObject.getString("ExceptionName") +"\t" + jsonObject.getInteger("methodNumber")+"\n");
                }
            }
        }
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +eachExceptionTypeCountFile, sb.toString(), true);
    }


    public static void rewriteExceptionNumberCount() throws InterruptedException {
        String inputFilePath = MyConfig.getInstance().getResultFolder() +eachExceptionTypeCountFile;
        String outputFilePath = MyConfig.getInstance().getResultFolder() +"merged_"+eachExceptionTypeCountFile;
        File f= new File(inputFilePath);
        int id= 1;
        while (!f.exists()) {
            Thread.currentThread().sleep(1000 * id++);
            if(f.exists() || id>100) break;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
            if(reader==null) return;
            String line;
            String[] fields;
            String currentTag = "";
            String firstLine = "";
            Map<String, ArrayList< String>> tagMap = new LinkedHashMap<>();
            while ((line = reader.readLine()) != null) {
                if(line.contains("tag\tname\texceptionName\tcount")) {
                    firstLine += line.strip() + "\t\t";
                }else{
                    fields = line.split("\t");
                    if(fields.length<2) continue;
                    currentTag = fields[0]+fields[1];
                    if(!tagMap.containsKey(currentTag)){
                        tagMap.put(currentTag, new ArrayList<String>());
                    }
                    tagMap.get(currentTag).add(line.strip()+"\t\t");
                }
            }
            writer.write(firstLine + "\n");
            int max = 0;
            for(Map.Entry<String, ArrayList< String>> en:tagMap.entrySet()){
                if(en.getValue().size()>max) max = en.getValue().size();
            }
            for(int i =0; i<max; i++) {
                for (ArrayList<String> list : tagMap.values()) {
                    if(list.size()>i)
                        writer.write(list.get(i));
                    else
                        writer.write("\t\t\t\t\t");
                }
                writer.write("\n");
            }
            reader.close();
            writer.close();

            System.out.println("Merge completed successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
