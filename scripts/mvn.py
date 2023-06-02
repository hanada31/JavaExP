import os
import sys
import shutil

if __name__ == '__main__' :
    os.system("mvn -f pom.xml clean package  -DskipTests")
    if os.path.exists("target/ExceptionExtractor-jar-with-dependencies.jar"):
        print("Successfully build! generate jar-with-dependencies in folder target/")
        shutil.copy("target/ExceptionExtractor-jar-with-dependencies.jar", "ExceptionExtractor.jar")
        print("copy jar to the root directory.")
    else:
        print("Fail to build! Please run \"mvn -f pom.xml package\" to see the detail info.")