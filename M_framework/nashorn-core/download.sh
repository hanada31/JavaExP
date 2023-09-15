#!/bin/bash

versions=("15.0" "15.1" "15.2" "15.3" "15.4")

# 遍历所有传入的版本号
for version in "${versions[@]}"; do
    # 构造下载URL
    url="https://repo1.maven.org/maven2/org/openjdk/nashorn/nashorn-core/${version}/nashorn-core-${version}.jar"

    # 下载文件
    wget $url

    # 检查wget命令是否成功执行
    if [ $? -eq 0 ]; then
        echo "Downloaded jgrapht-core-${version}.jar successfully."
    else
        echo "Failed to download jgrapht-core-${version}.jar."
    fi
done

