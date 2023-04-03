import os
import sys
import shutil


def executeCmd(cmd):
    print(cmd)
    os.system(cmd)

if __name__ == '__main__' :
    sdk = "platforms"   
    #sdk = "../../other/android-sdk-linux/platforms"
    #sdk = "E:/AndroidSDK/android-sdk-windows-new/platforms"
    jarFile = "CrashTracker.jar"
   
    #os.system("mvn -f pom.xml package  -DskipTests")
    if os.path.exists("target"+ os.sep +"CrashTracker.jar"):
        print("Successfully build! generate jar-with-dependencies in folder target/")
        shutil.copy("target"+ os.sep +"CrashTracker.jar", jarFile)
        print("copy jar to the root directory.")
    else:
        print("Fail to build! Please run \"mvn -f pom.xml package\" to see the detail info.")
    
    #do not use the android.jar file in the Android SDK, as they have empty implementation. Instead, extract android.jar files from your android phone or an emulator with the target version.
    #Also, you can download from  https://github.com/hanada31/AndroidFrameworkImpl and unzip files
    #path = "framework/classes/"; "D:\\SoftwareData\\dataset\\android-framework\\classes\\";
    path = sys.argv[1]
    version = sys.argv[2]
    output = sys.argv[3]
    name = "android"+version;
    
    command = "java -jar "+jarFile+"  -path "+ path +" -name "+name+" -androidJar "+ sdk +" -client ExceptionInfoClient " +" -outputDir " +output +" -frameworkVersion " + version 
    print (command)
    executeCmd(command)
                    
