#!/bin/bash

versions=("10.0" "11.0" "12.0" "13.0" "14.0" "15.0" "16.0" "17.0" "18.0" "19.0" "20.0" "21.0" "22.0" "23.0")

# 遍历所有传入的版本号
for version in "${versions[@]}"; do
    # 构造下载URL
    url="https://repo1.maven.org/maven2/com/google/guava/guava/${version}/guava-${version}.jar"

    # 下载文件
    wget $url

    # 检查wget命令是否成功执行
    if [ $? -eq 0 ]; then
        echo "Downloaded jgrapht-core-${version}.jar successfully."
    else
        echo "Failed to download jgrapht-core-${version}.jar."
    fi
done

