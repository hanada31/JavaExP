#!/bin/bash

# 检查是否有至少一个参数
versions=("1.0" "1.1.1" "1.2" "1.3" "2.0")

# 遍历所有传入的版本号
for version in "${versions[@]}"; do
    # 构造下载URL
    url="https://repo1.maven.org/maven2/org/graphstream/gs-core/${version}/gs-core-${version}.jar"

    # 下载文件
    wget $url

    # 检查wget命令是否成功执行
    if [ $? -eq 0 ]; then
        echo "Downloaded jgrapht-core-${version}.jar successfully."
    else
        echo "Failed to download jgrapht-core-${version}.jar."
    fi
done

