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
                # 提取所需的字段
                api_amount = data.get("api_amount", 0)
                api_exist_in_first_version_amount = data.get("api_exist_in_first_version_amount", 0)
                api_exist_in_last_version_amount = data.get("api_exist_in_last_version_amount", 0)
                added_api_per_version = data.get("added_api_per_version", 0)
                removed_api_per_version = data.get("removed_api_per_version",
                                                                             0)

                # 输出数据
                output_line = f"{folder_name}\t{api_amount}\t{api_exist_in_first_version_amount}\t{api_exist_in_last_version_amount}\t{added_api_per_version}\t{removed_api_per_version}"
                print(output_line)
