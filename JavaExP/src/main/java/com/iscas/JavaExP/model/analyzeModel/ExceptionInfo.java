package com.iscas.JavaExP.model.analyzeModel;

import com.iscas.JavaExP.utils.FileUtils;
import soot.*;

import java.util.List;

/**
 * @Author hanada
 * @Date 2022/3/15 10:47
 * @Version 1.0
 */
public class ExceptionInfo implements  Cloneable {
    private AppModel.ExceptionType exceptionType;
    private String exceptionName;
    private String exceptionMsg="";

    private String modifier;
    private SootMethod sootMethod;
    private String sootMethodName;
    private Unit unit;
    private Trap trap;

    ConditionTrackerInfo conditionTrackerInfo;

    public ExceptionInfo() {
    }
    public ExceptionInfo(SootMethod sootMethod, Unit unit, String exceptionName) {
        this.sootMethod = sootMethod;
        initModifier();
        this.unit = unit;
        this.exceptionName = exceptionName;
        this.conditionTrackerInfo = new ConditionTrackerInfo(sootMethod,unit);
    }
    public ExceptionInfo(SootMethod sootMethod, Trap trap, String exceptionName) {
        this.sootMethod = sootMethod;
        initModifier();
        this.trap = trap;
        this.exceptionName = exceptionName;
        this.conditionTrackerInfo = new ConditionTrackerInfo(sootMethod,unit);

    }

    public ConditionTrackerInfo getConditionTrackerInfo() {
        return conditionTrackerInfo;
    }

    public void setConditionTrackerInfo(ConditionTrackerInfo conditionTrackerInfo) {
        this.conditionTrackerInfo = conditionTrackerInfo;
    }

    private void initModifier() {
        if(sootMethod.isPublic())
            setModifier("public");
        else if(sootMethod.isPrivate())
            setModifier("private");
        else if(sootMethod.isProtected())
            setModifier("protected");
        else
            setModifier("default");
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }



    public String getExceptionName() {
        return exceptionName;
    }



    public String getExceptionMsg() {
        return exceptionMsg;
    }

    public void setExceptionMsg(String exceptionMsg) {
        this.exceptionMsg = exceptionMsg;
    }

    public Unit getUnit() {
        return unit;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public void setExceptionName(String exceptionName) {
        this.exceptionName = exceptionName;
    }

    public void setSootMethod(SootMethod sootMethod) {
        this.sootMethod = sootMethod;
    }

    public String getSootMethodName() {
        return sootMethodName;
    }

    public void setSootMethodName(String sootMethodName) {
        //			"method":"<android.database.sqlite.SQLiteClosable: void acquireReference()>",
        String[] ss = sootMethodName.split(" ");
        String prefix = ss[0].replace("<","").replace(":",".");
        String suffix = ss[2].split("\\(")[0];
        this.sootMethodName = prefix+suffix;
    }

    public AppModel.ExceptionType getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(AppModel.ExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }

    public boolean findExceptionType(SootClass sootClass) {
        boolean isException = false;
        List<String> StandardChecked = FileUtils.getListFromFile("D:\\ProjectData\\IdeaProjects\\ExceptionExtractor\\ExceptionExtractor\\src\\main\\resources\\checked_exceptions.txt");
        List<String> StandardUnChecked_Runtime = FileUtils.getListFromFile("D:\\ProjectData\\IdeaProjects\\ExceptionExtractor\\ExceptionExtractor\\src\\main\\resources\\unchecked_exceptions.txt");
        //get exception type
        setExceptionType(AppModel.ExceptionType.ThirdParty);
        if(StandardChecked.contains(sootClass.getName())){ //should be empty
            setExceptionType(AppModel.ExceptionType.StandardChecked);
            isException = true;
        }else if(StandardUnChecked_Runtime.contains(sootClass.getName())){ //should be empty
            setExceptionType(AppModel.ExceptionType.StandardUnChecked_Runtime);
            isException = true;
        }else {
            for (SootClass superCls : Scene.v().getActiveHierarchy().getSuperclassesOf(sootClass)) {
                if (superCls.getName().contains("java.lang.RuntimeException")) {
                    setExceptionType(AppModel.ExceptionType.CustomUnChecked_Runtime);
                    isException = true;
                    break;
                }
                if (superCls.getName().contains("java.lang.Exception")) {
                    setExceptionType(AppModel.ExceptionType.CustomChecked);
                    isException = true;
                }
            }
        }
        return isException;
    }

//    @Override
//    protected Object clone() throws CloneNotSupportedException {
//        ExceptionInfo exceptionInfo = new ExceptionInfo();
//        exceptionInfo.setExceptionMsg(exceptionMsg);
//        exceptionInfo.setExceptionType(exceptionType);
//        exceptionInfo.setExceptionName(exceptionName);
//        exceptionInfo.setSootMethod(sootMethod);
//        exceptionInfo.setSootMethodName(sootMethodName);
//        exceptionInfo.setModifier(modifier);
//        exceptionInfo.setConditionTrackerInfo((ConditionTrackerInfo) conditionTrackerInfo.clone());
//        return exceptionInfo;
//    }
}

