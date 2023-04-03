package com.iscas.crashtracker.client.crash;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.iscas.crashtracker.base.Global;
import com.iscas.crashtracker.client.exception.ExceptionInfo;
import com.iscas.crashtracker.client.exception.RelatedCondType;
import com.iscas.crashtracker.client.exception.RelatedVarType;
import com.iscas.crashtracker.utils.ConstantUtils;
import com.iscas.crashtracker.utils.PrintUtils;
import com.iscas.crashtracker.utils.StringUtils;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;

/**
 * @Author hanada
 * @Date 2022/3/22 20:16
 * @Version 1.0
 */
public class CrashInfo {

    String id;
    String methodName;
    String className;
    String real;
    String exception;
    List<String> trace = new ArrayList<>();
    String buggyApi;
    String msg;
    String realCate;
    String category;
    String identifier;
    String reason;
    String signaler;
    String crashAPI;
    String crashMethod;
    List<String> crashMethodList = new ArrayList<>();
    String crashCallBack;
    ExceptionInfo exceptionInfo;
    List<Edge> edges = new ArrayList<>();
    Map<Integer, ArrayList<Edge>> edgeMap = new TreeMap<Integer, ArrayList<Edge>>();
    List<String> classesInTrace= new ArrayList<>();
    Map<String, Integer> buggyCandidates = new TreeMap<>();
    Map<String, BuggyCandidate> buggyCandidateObjs = new HashMap<>();
    Map<String, ExtendCandiMethod> extendedCallDepth = new HashMap<String, ExtendCandiMethod>();
    boolean findCandidateInTrace = false;
    int minScore = ConstantUtils.INITSCORE;
    int maxScore = ConstantUtils.BOTTOMSCORE;
    List<String> noneCodeLabels = new ArrayList<String>();
    String faultInducingPart;
    List<Integer> faultInducingParas = null;
    RelatedVarType relatedVarTypeOracle;
    RelatedCondType relatedCondTypeOracle = RelatedCondType.Empty;
    private Map<String, List<Integer>> callerOfSingnlar2SourceVarOracle = new HashMap<>();
    public RelatedVarType getRelatedVarTypeOracle() {
        return relatedVarTypeOracle;
    }

    public String getFaultInducingParasInStr() {
        return PrintUtils.printList(faultInducingParas);
    }

    public void setRelatedVarTypeOracle(RelatedVarType relatedVarTypeOracle) {
        this.relatedVarTypeOracle = relatedVarTypeOracle;
    }

    public RelatedCondType getRelatedCondTypeOracle() {
        return relatedCondTypeOracle;
    }

    public void setRelatedCondTypeOracle(RelatedCondType relatedCondTypeOracle) {
        this.relatedCondTypeOracle = relatedCondTypeOracle;
    }

    class ExtendCandiMethod {
        int depth;
        JSONArray trace;
        ExtendCandiMethod(int depth, JSONArray trace){
            this.depth = depth;
            this.trace = trace;
        }
    }

    public Map<String, ExtendCandiMethod> getExtendedCallDepth() {
        return extendedCallDepth;
    }

//    public boolean addExtendedCallDepth(String key, int value, List<String> trace) {
//        if(key.startsWith("java.")|| key.startsWith("androidx.")  || key.startsWith("android.") || key.startsWith("com.android.")) return false;
//        if(!extendedCallDepth.containsKey(key) || extendedCallDepth.get(key).depth>value ) {
//            extendedCallDepth.put(key, new ExtendCandiMethod(value, trace));
//            return true;
//        }
//        return false;
//    }
    public boolean addExtendedCallDepth(String key, int value, JSONObject reason) {
        if(key.startsWith("java.")|| key.startsWith("androidx.")  || key.startsWith("android.") || key.startsWith("com.android.")) return false;
        if(!extendedCallDepth.containsKey(key) || extendedCallDepth.get(key).depth>value ) {
            extendedCallDepth.put(key, new ExtendCandiMethod(value, reason.getJSONArray("Trace")));
            return true;
        }
        return false;
    }


    public Map<String, Integer> getBuggyCandidates() {
        return buggyCandidates;
    }

    public Map<String, BuggyCandidate> getBuggyCandidateObjs() {
        return buggyCandidateObjs;
    }


    public void addBuggyCandidates(String candi, int score, JSONObject reason) {
        boolean findPrexInTrace = false;
        for(String traceMtd: getCrashMethodList()){
            int id = Math.max(traceMtd.split("\\.").length-2, 2);
            String prefixInTrace = StringUtils.getPkgPrefix(traceMtd, id);
            if(candi.contains(prefixInTrace)) {
                findPrexInTrace = true;
                break;
            }
        }
        if(!findPrexInTrace) return;
        String pkgPrefix = StringUtils.getPkgPrefix(Global.v().getAppModel().getPackageName(),2);
        if(!candi.contains(pkgPrefix)) {
            score = score - ConstantUtils.OUTOFPKGSCORE;
        }
        if(this.buggyCandidates.containsKey(candi) && this.buggyCandidates.get(candi) >= score)
            score = this.buggyCandidates.get(candi);
        if(score > ConstantUtils.BOTTOMSCORE) {
            if(this.buggyCandidates.containsKey(candi)){
//                System.out.println(reason.remove("Influenced Field"));
                this.buggyCandidateObjs.get(candi).addReasonTrace(reason);
            }else {
                BuggyCandidate candiObj = new BuggyCandidate(candi, score);
//                System.out.println(reason.remove("Influenced Field"));
                candiObj.addReasonTrace(reason);
                this.buggyCandidateObjs.put(candi, candiObj);
            }
            if(score> ConstantUtils.INITSCORE) maxScore = ConstantUtils.INITSCORE;
            this.buggyCandidates.put(candi, score);
            if(score< minScore) minScore = score;
            if(score> maxScore) maxScore = score;

        }
    }

    public List<String> getCrashMethodList() {
        return crashMethodList;
    }

    public String getSignaler() {
        return signaler;
    }

    public void setSignaler(String signaler) {
        this.signaler = signaler;
    }

    public String getCrashAPI() {
        return crashAPI;
    }

    public void setCrashAPI(String crashAPI) {
        this.crashAPI = crashAPI;
    }

    public String getCrashMethod() {
        return crashMethod;
    }

    public void setCrashMethod(String crashMethod) {
        this.crashMethod = crashMethod;
    }

    public String getCrashCallBack() {
        return crashCallBack;
    }

    public void setCrashCallBack(String crashCallBack) {
        this.crashCallBack = crashCallBack;
    }


    public String getReal() {
        return real;
    }

    public void setReal(String real) {
        this.real = real;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public List<String> getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        trace= trace.replace("\"","");
        trace= trace.replace("[","").replace("]","");
        trace= trace.replace("at ","");
        String[] ss = trace.split(",");
        boolean firstUserAPI= true;
        for(int i=0; i< ss.length;i++){
            String method = ss[i];
            if(method.contains(" ") || !method.contains(".")) continue;
            this.trace.add(method);
            if(method.lastIndexOf(".")>=0)
                this.classesInTrace.add(method.substring(0, method.lastIndexOf(".")));
            if(i==0)
                setSignaler(method);
            else if(getCrashAPI()==null && ss[i-1].startsWith("android") && !method.startsWith("android")){
                setCrashAPI(ss[i-1]);
                setCrashMethod(method);
            }else if(getCrashAPI()!=null && getCrashCallBack()==null
                    && (method.startsWith("androidx.") || method.startsWith("android.")|| method.startsWith("com.android.")|| method.startsWith("java")) ) {
                setCrashCallBack(method);
            }
            if(!method.startsWith("androidx.") &&!method.startsWith("android.") &&!method.startsWith("com.android.") && !method.startsWith("java")) {
//                if(firstUserAPI)
                    crashMethodList.add(method);
            }else{
                if(crashMethodList.size()>0)  firstUserAPI = false;
            }

        }
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBuggyApi() {
        return buggyApi;
    }

    public void setBuggyApi(String buggyApi) {
        this.buggyApi = buggyApi;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getRealCate() {
        return realCate;
    }

    public void setRealCate(String realCate) {
        this.realCate = realCate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ExceptionInfo getExceptionInfo() {
        return exceptionInfo;
    }

    public void setExceptionInfo(ExceptionInfo exceptionInfo) {
        if(this.exceptionInfo!=null){
            this.exceptionInfo.getConditions().addAll(exceptionInfo.getConditions());
            this.exceptionInfo.getConditionUnits().addAll(exceptionInfo.getConditionUnits());

            this.exceptionInfo.getRelatedParamValues().addAll(exceptionInfo.getRelatedParamValues());
            this.exceptionInfo.getRelatedFieldValues().addAll(exceptionInfo.getRelatedFieldValues());
            this.exceptionInfo.getCaughtedValues().addAll(exceptionInfo.getCaughtedValues());
            this.exceptionInfo.getRelatedValueIndex().addAll(exceptionInfo.getRelatedValueIndex());
            this.exceptionInfo.getRelatedFieldValuesInStr().addAll(exceptionInfo.getRelatedFieldValuesInStr());
            this.exceptionInfo.getRelatedParamValuesInStr().addAll(exceptionInfo.getRelatedParamValuesInStr());


            this.exceptionInfo.getRelatedMethods().addAll(exceptionInfo.getRelatedMethods());
            this.exceptionInfo.getRelatedMethodsInDiffClass(false).addAll(exceptionInfo.getRelatedMethodsInDiffClass(false));
            this.exceptionInfo.getRelatedMethodsInSameClass(false).addAll(exceptionInfo.getRelatedMethodsInSameClass(false));

            this.exceptionInfo.getTracedUnits().addAll(exceptionInfo.getTracedUnits());

            this.exceptionInfo.setResourceRelated(this.exceptionInfo.isResourceRelated() | exceptionInfo.isResourceRelated());
            this.exceptionInfo.setHardwareRelated(this.exceptionInfo.isHardwareRelated() | exceptionInfo.isHardwareRelated());
            this.exceptionInfo.setManifestRelated(this.exceptionInfo.isManifestRelated() | exceptionInfo.isManifestRelated());
            this.exceptionInfo.setAssessRelated(this.exceptionInfo.isAssessRelated() | exceptionInfo.isAssessRelated());
            this.exceptionInfo.setOsVersionRelated(this.exceptionInfo.isOsVersionRelated() | exceptionInfo.isOsVersionRelated());

        }else{
            this.exceptionInfo = exceptionInfo;
        }
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public String getMethodName() {
        return methodName;
    }
    public String getSubMethodName() {
        int index = methodName.lastIndexOf(".");
        if(index>=0 && index+1 < methodName.length()){
            return methodName.substring(index+1);
        }
        return null;
    }


    public String getClassName() {
        return className;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
        if(methodName.lastIndexOf(".")>=0)
            this.className = methodName.substring(0,methodName.lastIndexOf("."));
    }

    public List<String> getClassesInTrace() {
        return classesInTrace;
    }

    public Map<Integer, ArrayList<Edge>> getEdgeMap() {
        return edgeMap;
    }

    public void add2EdgeMap(Integer depth, Edge e) {
        if(!edgeMap.containsKey(depth)){
            edgeMap.put(depth,new ArrayList<Edge>());
        }
        if(!edgeMap.get(depth).contains(e))
            edgeMap.get(depth).add(e);
        edges.add(e);
    }

    public void addNoneCodeLabel(String l) {
        if(!noneCodeLabels.contains(l))
            noneCodeLabels.add(l);
    }

    public List<String> getNoneCodeLabel() {
        return noneCodeLabels;
    }


    public String getFaultInducingPart() {
        return faultInducingPart;
    }

    public void setFaultInducingPart(String faultInducingPart) {
        this.faultInducingPart = faultInducingPart;
    }

    public List<Integer> getFaultInducingParas() {
        return faultInducingParas;
    }

    public void setFaultInducingParas(List<Integer> faultInducingParas) {
        this.faultInducingParas = faultInducingParas;
    }

    public Map<String, List<Integer>> getCallerOfSingnlar2SourceVarOracle() {
        return callerOfSingnlar2SourceVarOracle;
    }

    public void setCallerOfSingnlar2SourceVarOracle(Map<String, List<Integer>> callerOfSingnlar2SourceVar) {
        this.callerOfSingnlar2SourceVarOracle = callerOfSingnlar2SourceVar;
    }

    public void addCallerOfSingnlar2SourceVarOracle(String method, int sourceId ) {
        if(callerOfSingnlar2SourceVarOracle.containsKey(method)){
            if(callerOfSingnlar2SourceVarOracle.get(method).contains(sourceId)){
                return;
            }
        }else{
            callerOfSingnlar2SourceVarOracle.put(method, new ArrayList<>());
        }
        callerOfSingnlar2SourceVarOracle.get(method).add(sourceId);

    }

    @Override
    public String toString() {
        return "CrashInfo{" +
                "methodName='" + methodName + '\'' +
                ", real='" + real + '\'' +
                ", exception='" + exception + '\'' +
                ", trace=" + trace +
                ", buggyApi='" + buggyApi + '\'' +
                ", msg='" + msg + '\'' +
                ", realCate='" + realCate + '\'' +
                ", category='" + category + '\'' +
                ", identifier='" + identifier + '\'' +
                ", reason='" + reason + '\'' +
                ", exceptionInfo=" + exceptionInfo +
                ", edges=" + edges +
                '}';
    }

}
