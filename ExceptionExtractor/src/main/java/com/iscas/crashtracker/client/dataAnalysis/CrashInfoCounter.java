package com.iscas.crashtracker.client.dataAnalysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.crash.CrashInfo;
import com.iscas.crashtracker.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author hanada
 * @Date 2022/6/24 10:22
 * @Version 1.0
 */
@Slf4j
public class CrashInfoCounter {
    List<CrashInfo> crashInfoList = new ArrayList<>();

    public void analyze() {
        readAllCrashInfo();
        log.info("readCrashInfo Finish...");
        writeCrashInfo();
        log.info("writeCrashInfo Finish...");

    }

    private void writeCrashInfo() {
        String folder = MyConfig.getInstance().getResultFolder() +"statistic" + File.separator;
        FileUtils.writeText2File(folder+"crashLength.txt", "", false);
        FileUtils.writeText2File(folder +"crashLength.txt", "app\ttraceSize\tcrashMethodListSize\n", true);
        FileUtils.writeText2File(folder +"C-tag.txt", "", false);

        for(CrashInfo crashInfo: crashInfoList) {
            String str= crashInfo.getId()+"\t"+ crashInfo.getTrace().size()+"\t" +crashInfo.getCrashMethodList().size();
            FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +"crashLength.txt", str+"\n", true);
            if(crashInfo.getNoneCodeLabel().get(0)!=null && crashInfo.getNoneCodeLabel().get(0)!=null) {
                str = crashInfo.getRealCate() + "\t" + crashInfo.getId() + "\t" + crashInfo.getNoneCodeLabel().get(0);
                FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() + "C-tag.txt", str + "\n", true);
            }else{
                str = crashInfo.getRealCate() + "\t" + crashInfo.getId() + "\t";
                FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() + "C-tag.txt", str + "\n", true);
            }
        }
    }

    /**
     * readCrashInfo from CrashInfoFile
     */
    private void readAllCrashInfo() {
        String fn = MyConfig.getInstance().getCrashInfoFilePath();
        log.info("readCrashInfo::"+fn);
        String jsonString = FileUtils.readJsonFile(fn);
        JSONArray jsonArray = JSONArray.parseArray(jsonString);
        for (int i = 0 ; i < jsonArray.size();i++){
            JSONObject jsonObject = (JSONObject)jsonArray.get(i);
            CrashInfo crashInfo = new CrashInfo();
            crashInfoList.add(crashInfo);
            crashInfo.setIdentifier(jsonObject.getString("identifier"));
            crashInfo.setReal(jsonObject.getString("real"));
            crashInfo.setException(jsonObject.getString("exception"));
            crashInfo.setTrace(jsonObject.getString("trace"));
            crashInfo.setBuggyApi(jsonObject.getString("buggyApi"));
            crashInfo.setMsg(jsonObject.getString("msg").trim());
            crashInfo.setRealCate(jsonObject.getString("realCate"));
            crashInfo.addNoneCodeLabel(jsonObject.getString("tag"));
            crashInfo.setCategory(jsonObject.getString("category"));
            if(jsonObject.getString("fileName")!=null)
                crashInfo.setId(jsonObject.getString("fileName"));
            else
                crashInfo.setId(crashInfo.getIdentifier()+"-"+ jsonObject.getString("id"));
            crashInfo.setReason(jsonObject.getString("reason"));
            crashInfo.setMethodName(crashInfo.getTrace().get(0));
        }
    }
}
