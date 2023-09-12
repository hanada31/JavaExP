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
                api_level_data = data.get("api_level", {})

                # 提取所需的字段
                exception_amount = api_level_data.get("exception_amount", 0)
                added_exception_amount = api_level_data.get("added_exception_amount", 0)
                removed_exception_amount = api_level_data.get("removed_exception_amount", 0)
                exception_message_changed_amount = api_level_data.get("exception_message_changed_amount", 0)
                exception_type_changed_amount = api_level_data.get("exception_type_changed_amount", 0)
                exception_pre_conditions_changed_amount = api_level_data.get("exception_pre_conditions_changed_amount",
                                                                             0)

                # 输出数据
                output_line = f"{folder_name}\t{exception_amount}\t{added_exception_amount}\t{removed_exception_amount}\t{exception_type_changed_amount}\t{exception_message_changed_amount}\t{exception_pre_conditions_changed_amount}"
                print(output_line)
