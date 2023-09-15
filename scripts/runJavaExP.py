import os
import sys
import shutil


def executeCmd(cmd):
    print(cmd)
    os.system(cmd)

if __name__ == '__main__' :
    jarFile = "JavaExP.jar"
   
    # os.system("mvn -f pom.xml package  -DskipTests")
    # if os.path.exists("target"+ os.sep +"JavaExP.jar"):
        # print("Successfully build! generate jar-with-dependencies in folder target/")
    # else:
        # print("Fail to build! Please run \"mvn -f pom.xml package\" to see the detail info.")
    
    path = sys.argv[1]
    name = sys.argv[2] 
    output = sys.argv[3]
    
    outputFolder = "exception_summary/"
    if not os.path.exists("logs-"+outputFolder):
        shutil.os.mkdir("logs-"+outputFolder)
    extra = ""
    if not os.path.exists("logs-"+outputFolder):
        shutil.os.mkdir("logs-"+outputFolder)
    extra = ""
    for index,arg in enumerate(sys.argv):
        if arg =="interProcedure":
            extra += " -interProcedure " 
        if arg =="lightWeight":
            extra += " -lightWeight " 
        if arg =="conflictCheck":
            extra += " -conflictCheck " 
    print(extra)
    
    
    vmArgs = ""
    if name == "all":
        print ("all files to be analyzed")
        for name in os.listdir(path):
            if os.path.exists(outputFolder + output+"//"+name):
                print(outputFolder + output+"//"+name)
                continue
            if name.endswith(".jar"):
                command = "java -jar " + vmArgs +jarFile+"  -path "+ path +" -name "+name+ " -client ExceptionInfoClient " +" -outputDir " + outputFolder + output  +extra + " >> "+"logs-"+outputFolder+name+"-log.txt"
                print (command)
                executeCmd(command)
    else:
        command = "java -jar " + vmArgs +jarFile+"  -path "+ path +" -name "+name+ " -client ExceptionInfoClient " +" -outputDir " + outputFolder + output  +extra +" >> "+"logs-"+outputFolder+name+"-log.txt"
        print (command)
        executeCmd(command)
