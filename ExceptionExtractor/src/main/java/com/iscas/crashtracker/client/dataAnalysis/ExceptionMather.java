package com.iscas.crashtracker.client.dataAnalysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.crash.CrashInfo;
import com.iscas.crashtracker.client.exception.ExceptionInfo;
import com.iscas.crashtracker.client.exception.RelatedCondType;
import com.iscas.crashtracker.client.exception.RelatedVarType;
import com.iscas.crashtracker.utils.FileUtils;
import com.iscas.crashtracker.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author hanada
 * @Date 2022/6/24 10:22
 * @Version 1.0
 */
@Slf4j
public class ExceptionMather {
    List<CrashInfo> crashInfoList = new ArrayList<>();
    String[] versions = {"2.3", "4.4", "5.0", "6.0", "7.0", "8.0", "9.0", "10.0", "11.0", "12.0"};
    Map<String, JSONObject> version2JsonStr = new HashMap<>();

    public void analyze() {
        readAllCrashInfo();
        log.info("readCrashInfo Finish...");

        exceptionOracleAnalysis();
        getExceptionOfCrashInfoWithGivenVersion();
        log.info("getExceptionOfCrashInfo Finish...");

    }

    private void exceptionOracleAnalysis() {
//        boolean flag = false;
        String folder = MyConfig.getInstance().getResultFolder() +"statistic" + File.separator;
        FileUtils.writeText2File(folder +"ETSCorrectness.txt", "", false);
        log.info("write to "+ folder +"ETSCorrectness.txt");
        for(CrashInfo crashInfo: crashInfoList) {
            String targetMethodName;
            String targetVer;
            String str= crashInfo.getId()+"\t"+crashInfo.getSignaler()+"\t";
            String[] versionTypes = new String[versions.length];
            String[] versionTypeCandis = new String[versions.length];
            String[] targetMethodNames = new String[versions.length];
            int i = 0;
            for (String version : versions) {
                Pair<String, String> pair = getExceptionWithGivenVersion(crashInfo, version, true);
                versionTypes[i] = pair.getO1();
                targetMethodNames[i] = pair.getO2();
                if (versionTypes[i].equals("notFound")) {
                    Pair<String, String> pair2 = getExceptionWithGivenVersion(crashInfo, version, false);
                    versionTypeCandis[i] = pair2.getO1();
                    targetMethodNames[i] = pair2.getO2();
                }
                i++;
            }
            int targetVerId = getTargetVersion(versionTypes);
            if (targetVerId == -1) {
                targetVerId = getTargetVersion(versionTypeCandis);
            }
            if (targetVerId == -1)
                targetVerId = 6;
            targetVer = versions[targetVerId];
            targetMethodName = targetMethodNames[targetVerId];
            log.info("target is "+ targetVer);

            RelatedVarType type = getVarTypeFromExceptionSummary(crashInfo, targetMethodName);
            String targetVerStr = type==null? RelatedVarType.Unknown.toString(): type.toString();
            str += targetVerStr +"\t"+crashInfo.getRelatedVarTypeOracle()+"\t"+ (targetVerStr.equals(crashInfo.getRelatedVarTypeOracle().toString()))+"\n";
            log.info(str);
            FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"ETSCorrectness.txt", str, true);
        }
    }



    private int getTargetVersion(String[] versionType) {
        int paraAndField =0 , fieldOnly =0 ,parameterOnly =0 , overrideMissing = 0;
//        log.info(PrintUtils.printArray(versionType));
        for(String relatedVarType: versionType) {
            if(relatedVarType ==null) continue;
            if (relatedVarType.equals(RelatedVarType.ParaAndField.toString())) paraAndField++;
            if (relatedVarType.equals(RelatedVarType.Field.toString())) fieldOnly++;
            if (relatedVarType.equals(RelatedVarType.Parameter.toString())) parameterOnly++;
            if (relatedVarType.equals(RelatedVarType.Empty.toString())) overrideMissing++;
        }
        String choice = RelatedVarType.Unknown.toString();
        if(paraAndField + parameterOnly + fieldOnly + overrideMissing ==0)
            choice = RelatedVarType.Unknown.toString();
        else if(paraAndField >= parameterOnly && paraAndField >= fieldOnly && paraAndField >= overrideMissing)
            choice =  RelatedVarType.ParaAndField.toString();
        else if(parameterOnly >= fieldOnly && parameterOnly >= paraAndField && parameterOnly >= overrideMissing)
            choice = RelatedVarType.Parameter.toString();
        else if(fieldOnly >= parameterOnly && fieldOnly >= paraAndField && fieldOnly >= overrideMissing)
            choice =  RelatedVarType.Field.toString();
        else if(overrideMissing >= parameterOnly && overrideMissing >= paraAndField && overrideMissing >= fieldOnly)
            choice = RelatedVarType.Empty.toString();

        for(int i = versionType.length-1; i>=0; i--) {
            if(versionType[i]!=null && versionType[i].equals(choice)){
                return i;
            }
        }
        return -1;
    }

    private RelatedVarType getVarTypeFromExceptionSummary(CrashInfo crashInfo, String targetMethodName) {
        if(!crashInfo.getMethodName().equals(targetMethodName)) {
            crashInfo.setMethodName(targetMethodName);
        }
        String fn = MyConfig.getInstance().getExceptionFilePath()+crashInfo.getClassName()+".json";
        log.info("readExceptionSummary::"+fn);
        String jsonString = FileUtils.readJsonFile(fn);
        JSONObject wrapperObject = (JSONObject) JSONObject.parse(jsonString);
        if(wrapperObject==null) {
            log.info( crashInfo.getClassName()+" is not modeled.");
            return RelatedVarType.Unknown;
        }
        JSONArray methods = wrapperObject.getJSONArray("exceptions");//构建JSONArray数组
        for (Object method : methods) {
            JSONObject jsonObject = (JSONObject) method;
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            exceptionInfo.setSootMethodName(jsonObject.getString("method"));
            exceptionInfo.setExceptionMsg(jsonObject.getString("message"));
            if (exceptionInfo.getSootMethodName().equals(crashInfo.getMethodName())) {
                if (exceptionInfo.getExceptionMsg() == null) continue;
                Pattern p = Pattern.compile(exceptionInfo.getExceptionMsg());
                Matcher m = p.matcher(crashInfo.getMsg());
                String str = exceptionInfo.getExceptionMsg();
                str = str.replace("[\\s\\S]*", "");
                str = str.replace("\\Q", "");
                str = str.replace("\\E", "");
                if (str.length() >= 3) {
                    if (m.matches()) {
                        return RelatedVarType.valueOf(jsonObject.getString("relatedVarType"));
                    }
                }
            }
        }
        for (Object method : methods) {
            JSONObject jsonObject = (JSONObject) method;
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            exceptionInfo.setSootMethodName(jsonObject.getString("method"));
            exceptionInfo.setExceptionMsg(jsonObject.getString("message"));
            if (exceptionInfo.getSootMethodName().equals(crashInfo.getMethodName())) {
                if (exceptionInfo.getExceptionMsg() == null) continue;
                Pattern p = Pattern.compile(exceptionInfo.getExceptionMsg());
                Matcher m = p.matcher(crashInfo.getMsg());
                if (m.matches()) {
                    return RelatedVarType.valueOf(jsonObject.getString("relatedVarType"));
                }
            }
        }
        return RelatedVarType.Unknown;
    }

    /**
     * getExceptionOfCrashInfo from exception.json
     */
    private Pair<String,String> getExceptionWithGivenVersion(CrashInfo crashInfo, String version, boolean classFilter) {
        String androidFolder = MyConfig.getInstance().getExceptionFolderPath()+File.separator+"android"+version+File.separator;
        MyConfig.getInstance().setExceptionFilePath(androidFolder+"exceptionInfo"+File.separator);
        MyConfig.getInstance().setPermissionFilePath(androidFolder+"Permission"+File.separator+"permission.txt");
        MyConfig.getInstance().setAndroidCGFilePath(androidFolder+"CallGraphInfo"+File.separator+"cg.txt");

        String fn = MyConfig.getInstance().getExceptionFilePath()+"summary"+ File.separator+ "exception.json";
        String jsonString = FileUtils.readJsonFile(fn);
        JSONObject wrapperObject = (JSONObject) JSONObject.parse(jsonString);
        if(wrapperObject==null) return new Pair<>("noFile",crashInfo.getSignaler());
        JSONArray methods = wrapperObject.getJSONArray("exceptions");//构建JSONArray数组
        for (Object method : methods) {
            JSONObject jsonObject = (JSONObject) method;
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            exceptionInfo.setSootMethodName(jsonObject.getString("method"));
            if (!classFilter || crashInfo.getSignaler().equals(exceptionInfo.getSootMethodName())) {
                exceptionInfo.setExceptionMsg(jsonObject.getString("message"));
                if (exceptionInfo.getExceptionMsg() == null) continue;
                Pattern p = Pattern.compile(exceptionInfo.getExceptionMsg());
                Matcher m = p.matcher(crashInfo.getMsg());
                if (m.matches()) {
                    String str = exceptionInfo.getExceptionMsg();
                    str = str.replace("[\\s\\S]*", "");
                    str = str.replace("\\Q", "");
                    str = str.replace("\\E", "");
                    if (str.length() < 3)
                        continue;
                    if (jsonObject.getString("relatedVarType") != null) {
                        return new Pair<>(jsonObject.getString("relatedVarType"), exceptionInfo.getSootMethodName());
                    }else{
                        return new Pair<>("Unknown", exceptionInfo.getSootMethodName());
                    }
                }
            }
        }
        return new Pair<>("notFound",crashInfo.getSignaler());
    }

    private void getExceptionOfCrashInfoWithGivenVersion() {
        FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"exceptionMatch.txt", "", false);
        log.info("write to "+ MyConfig.getInstance().getResultFolder() +"exceptionMatch.txt");
        for(CrashInfo crashInfo: crashInfoList) {
            StringBuilder str= new StringBuilder(crashInfo.getId() + "\t" + crashInfo.getSignaler() + "\t");
            for (String version : versions) {
                String relatedVarType = getExceptionWithGivenVersion(crashInfo, version);
                str.append(relatedVarType).append("\t");
            }
            FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"exceptionMatch.txt", str+"\n", true);
        }
    }

    /**
     * getExceptionOfCrashInfo from exception.json
     */
    private String getExceptionWithGivenVersion(CrashInfo crashInfo, String version) {
        String jsonString;
        JSONObject wrapperObject;
        if(version2JsonStr.containsKey(version)){
            wrapperObject = version2JsonStr.get(version);
        }else {
            String androidFolder = MyConfig.getInstance().getExceptionFolderPath()+File.separator+"android"+version+File.separator;
            MyConfig.getInstance().setExceptionFilePath(androidFolder+"exceptionInfo"+File.separator);
            MyConfig.getInstance().setPermissionFilePath(androidFolder+"Permission"+File.separator+"permission.txt");
            MyConfig.getInstance().setAndroidCGFilePath(androidFolder+"CallGraphInfo"+File.separator+"cg.txt");

            String fn = MyConfig.getInstance().getExceptionFilePath() + "summary" + File.separator + "exception.json";
            jsonString = FileUtils.readJsonFile(fn);
            wrapperObject = (JSONObject) JSONObject.parse(jsonString);
            version2JsonStr.put(version,wrapperObject);
        }

        if(wrapperObject==null) return "noFile";
        JSONArray methods = wrapperObject.getJSONArray("exceptions");//构建JSONArray数组
        for (Object method : methods) {
            JSONObject jsonObject = (JSONObject) method;
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            exceptionInfo.setSootMethodName(jsonObject.getString("method"));
            if (crashInfo.getSignaler().equals(exceptionInfo.getSootMethodName())) {
                exceptionInfo.setExceptionMsg(jsonObject.getString("message"));
                if (exceptionInfo.getExceptionMsg() == null) continue;
                Pattern p = Pattern.compile(exceptionInfo.getExceptionMsg());
                Matcher m = p.matcher(crashInfo.getMsg());
                if (exceptionInfo.getExceptionMsg().equals(crashInfo.getMsg()) || m.matches()) {
                    crashInfo.setExceptionInfo(exceptionInfo);
                    if (jsonObject.getString("relatedVarType") != null) {
                        return jsonObject.getString("relatedVarType");
                    }
                }
            }
        }
        return "unknown";
    }



    /**
     * readCrashInfo from CrashInfoFile
     */
    private void readAllCrashInfo() {
        String fn = MyConfig.getInstance().getCrashInfoFilePath();
        log.info("readCrashInfo::"+fn);
        String jsonString = FileUtils.readJsonFile(fn);
        JSONArray jsonArray = JSONArray.parseArray(jsonString);
        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            CrashInfo crashInfo = new CrashInfo();
            crashInfoList.add(crashInfo);
            crashInfo.setIdentifier(jsonObject.getString("identifier"));
            crashInfo.setReal(jsonObject.getString("real"));
            crashInfo.setException(jsonObject.getString("exception"));
            crashInfo.setTrace(jsonObject.getString("trace"));
            crashInfo.setBuggyApi(jsonObject.getString("buggyApi"));
            crashInfo.setMsg(jsonObject.getString("msg").trim());
            crashInfo.setRealCate(jsonObject.getString("realCate"));
            crashInfo.setCategory(jsonObject.getString("category"));
            if (jsonObject.getString("fileName") != null)
                crashInfo.setId(jsonObject.getString("fileName"));
            else
                crashInfo.setId(crashInfo.getIdentifier() + "-" + jsonObject.getString("id"));
            crashInfo.setReason(jsonObject.getString("reason"));
            crashInfo.setMethodName(crashInfo.getTrace().get(0));

            if (jsonObject.getString("relatedVarType") != null)
                crashInfo.setRelatedVarTypeOracle(RelatedVarType.valueOf(jsonObject.getString("relatedVarType")));
            if (jsonObject.getString("relatedCondType") != null)
                crashInfo.setRelatedCondTypeOracle(RelatedCondType.valueOf(jsonObject.getString("relatedCondType")));

            JSONObject callerOfSingnlar2SourceVar = jsonObject.getJSONObject("callerOfSingnlar2SourceVar");
            if (callerOfSingnlar2SourceVar != null) {
                for (String key : callerOfSingnlar2SourceVar.keySet()) {
                    String[] ids = ((String) callerOfSingnlar2SourceVar.get(key)).split(",");
                    for (String id : ids)
                        crashInfo.addCallerOfSingnlar2SourceVarOracle(SootUtils.getMethodSimpleNameFromSignature(key), Integer.parseInt(id));
                }
            }
        }
    }
}
