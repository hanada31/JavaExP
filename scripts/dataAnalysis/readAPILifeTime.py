import os
import json

# 指定statistic_result文件夹的路径
base_folder = "statistic_result"

# 遍历statistic_result文件夹中的子文件夹
for folder_name in os.listdir(base_folder):
    folder_path = os.path.join(base_folder, folder_name)

    # 检查子文件夹名称是否包含 "inter" 字符串
    if "inter" in folder_name:
        # 检查子文件夹中是否存在statistic.json文件
        json_file_path = os.path.join(folder_path, "statistic.json")
        if os.path.exists(json_file_path):
            with open(json_file_path, "r") as json_file:
                data = json.load(json_file)

                # 获取api_level的数据
                api_level_data = data.get("only_include_exception_api", {})

                # 提取所需的字段
                average_lifetime = api_level_data.get("average_lifetime", 0)
                pre_condition_sensitive_api_lifetime = api_level_data.get("pre_condition_sensitive_api_lifetime", 0)
                changed_sensitive_api_lifetime = api_level_data.get("changed_sensitive_api_lifetime", 0)


                # 输出数据
                output_line = f"{folder_name}\t{average_lifetime}\t{pre_condition_sensitive_api_lifetime}\t{changed_sensitive_api_lifetime}"
                print(output_line)
