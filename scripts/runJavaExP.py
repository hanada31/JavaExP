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
    extra = ""
    if len(sys.argv)>4 and sys.argv[4] =="interProcedure":
        extra += " -interProcedure " 
    vmArgs = ""
    if name == "all":
        print ("all files to be analyzed")
        for name in os.listdir(path):
            if name.endswith(".jar"):
                command = "java -jar " + vmArgs +jarFile+"  -path "+ path +" -name "+name+ " -client ExceptionInfoClient " +" -outputDir " +output  +extra +" >> logs/"+name+"-log.txt"
                print (command)
                executeCmd(command)
    else:
        command = "java -jar " + vmArgs +jarFile+"  -path "+ path +" -name "+name+ " -client ExceptionInfoClient " +" -outputDir " +output  +extra +" >> logs/"+name+"-log.txt"
        print (command)
        executeCmd(command)
