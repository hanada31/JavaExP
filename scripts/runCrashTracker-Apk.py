import os
import os
import sys
import shutil
from concurrent.futures import ThreadPoolExecutor
import threading
import time

reRun = True
filterList = list()

def isAPKisAnalyzed(resPath, name):
    if(not os.path.exists(resPath+os.sep+"BuggyCandidatesRanking.txt")): 
        return False
    file_object1 = open(resPath+os.sep+"BuggyCandidatesRanking.txt",'r')
    try:
        while True:
            line = file_object1.readline()
            if line:
                if name in line:
                    return True
            else:
                break
    finally:
        file_object1.close()
    print (name +" is not analyzed: " )
    return False


def analyzeApk(apkPath, resPath, sdk, frameworkVersion, strategy):
    logDir = resPath+os.sep+"logs"
    outputDir = resPath+os.sep+"output"
    print ("@@@"+outputDir)
    if(not os.path.exists(logDir)): 
        os.makedirs(logDir) 
    if(not os.path.exists(outputDir)): 
        os.makedirs(outputDir) 
        
    if(os.path.exists(apkPath)): 
        apks = os.listdir(apkPath)
        
        extraArgs = "" 
        if frameworkVersion != "no": 
            extraArgs = extraArgs +  " -frameworkVersion " + frameworkVersion +" "
        extraArgs = extraArgs +" -strategy " +strategy +" "
        
        pool = ThreadPoolExecutor(max_workers=4)

        for apk in apks:
            if len(filterList)>0  and apk  in filterList:
                continue
            if apk[-4:] ==".apk":
                resFile = outputDir + os.sep + apk[:-4] + os.sep +apk[:-4] + ".json"
                if(reRun or not os.path.exists(resFile) or not isAPKisAnalyzed(resPath,apk[:-4])): 
                    command = "java -jar "+jarFile+"  -path "+ apkPath +" -name "+apk+" -androidJar "+ sdk +" "+ extraArgs +" -crashInput Files"+ os.sep +"crashInfo.json  -exceptionInput Files  -client ApkCrashAnalysisClient" +" -outputDir "+outputDir #+ " >> "+logDir+ os.sep +jar[:-4]+".txt"
                    future1 = pool.submit(executeCmd, command)
        pool.shutdown()

def executeCmd(cmd):
    print(cmd)
    os.system(cmd)

def readFilterFile(filterFile):
    f = open(filterFile, 'r')
    lines = f.readlines()
    f.close()
    for line in lines:
        filterList.append(line.strip()+".apk")
    
if __name__ == '__main__' :
    sdk = "platforms"   
    #sdk = "../../other/android-sdk-linux/platforms"
    #sdk = "E:/AndroidSDK/android-sdk-windows-new/platforms"
    
    jarFile = "CrashTracker.jar"
    
    apkPath = sys.argv[1]
    resPath = sys.argv[2]
    frameworkVersion = sys.argv[3]
    strategy = sys.argv[4] 
    #os.system("git submodule update --init soot-dev")
    #os.system("mvn -f pom.xml clean package -DskipTests")
    if os.path.exists("target"+ os.sep +"CrashTracker.jar"):
        print("Successfully build! generate jar-with-dependencies in folder target/")
        shutil.copy("target"+ os.sep +"CrashTracker.jar", jarFile)
        print("copy jar to the root directory.")
    else:
        print("Fail to build! Please run \"mvn -f pom.xml package\" to see the detail info.")
    
    if len(sys.argv)>5:
        filterFile = sys.argv[5]    
        readFilterFile(filterFile)
    analyzeApk(apkPath, resPath, sdk, frameworkVersion, strategy)
    
