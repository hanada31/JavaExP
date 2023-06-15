# JavaExP

Java Exception Precondition extractor 




## Install Requirements

1. Python 3.8

2. Java 11

3. maven 3.6.3

4. Windows  (by default)

   

## Run JavaExP 

*Note: Only **English** characters are allowed in the path.*

```
# Initialize soot-dev submodule
git submodule update --init soot-dev

# Use -DskipTests to skip tests of soot (make build faster)
mvn -f pom.xml clean package -DskipTests

# Copy jar to root directory
cp target/JavaExP.jar JavaExP.jar 

# run JavaExP.jar 
python scripts/runJavaExP.py Benchmark [project under test in Benchmark folder] [output folder]

```



## Result 

get the result files in [output folder].

In the "exceptionInfo" folder, you can get a file named "exceptionConditions.txt", which gives the list of generated exception preconditions.

For example,

```
<testcase.ExceptionCondition.Motivation: void caller(java.lang.String,java.lang.String,int)>
Type:java.lang.RuntimeException
Message:\Qbug!\E
ExceptionPreConditions:
parameter1 is null
parameter0 startsWith "Error"
parameter2 smaller than 3
```

It gives the exception precondition for method caller in the of the following code snippets.

```
    private final String ERRORSTR = "Error String";

    /**
     * @throws RuntimeException: parameter1 is null && parameter0 startsWith "Error" && parameter2 smaller than 3
     */
    public void caller(String errStr, String input,int num) {
        if(num<3)
            callee(input, errStr, num);
    }

    /**
     * @throws RuntimeException: parameter0 is null and parameter1 startsWith "Error"
     */
    public void callee(String first, String second, int key){
        String errStr = "NoError";
        if(first==null) {
            if(key < 0)
                System.out.println("invalid key");
            else {
                for (String value: getValuesWithKey()){
                    System.out.println(value);
                }
            }
            errStr = second;
            if (errStr.startsWith(ERRORSTR.substring(0,5)))
                throw new RuntimeException("bug!");
        }
        System.out.println(errStr);
    }
```

