import json
from enum import Enum
import os

VERSION1 = 1.0
VERSION2 = 1.1
FILE1 = f'results/commons-io-{VERSION1}.jar/exceptionInfo/exceptionConditionsOfAll.json'
FILE2 = f'results/commons-io-{VERSION2}.jar/exceptionInfo/exceptionConditionsOfAll.json'


with open(FILE1, 'r') as f:
    file1 = json.load(f)
    old_classes = file1['classes']

with open(FILE2, 'r') as f:
    file2 = json.load(f)
    new_classes = file2['classes']


def find_base(targets, source, keys):
    target_list = []
    for target in targets:
        is_match = True
        for key in keys:
            if target[key] != source[key]:
                is_match = False
        if is_match:
            target_list.append(target)
    return target_list
        

def find_class(classes, source_class):
    return find_base(classes, source_class, ['className'])


def find_method(methods, source_method):
    return find_base(methods, source_method, ['methodName'])


def find_exception(exceptions, source_exception):
    return find_base(exceptions, source_exception, ["method", "ExceptionName", "ExceptionType", "message", "modifier"])


def unorder_list_equal(list1: list, list2: list):
    if len(list1) != len(list2):
        return False
    for item in list1:
        if item in list2:
            list2.remove(item)
        else:
            return False
    return True


def exception_equal(exception1, exception2):
    if ('preConditions' not in exception1) and ('preConditions' not in exception2):
        return True
    elif ('preConditions' not in exception1) or ('preConditions' not in exception2):
        return False
    return unorder_list_equal(exception1['preConditions'], exception2['preConditions'])


class DATA(Enum):
    CLASS_TYPE = 0,
    METHOD_TYPE = 1

def exception_num(target, type: DATA):
    if type == DATA.METHOD_TYPE:
        return len(target['exceptions'])
    elif type == DATA.CLASS_TYPE:
        count = 0
        for method in target['methods']:
            count += exception_num(method, DATA.METHOD_TYPE)
        return count

def method_num(target):
    return len(target['methods'])

def add_method_recursively(data, cls, method):
    class_list = find_class(data['classes'], cls)
    if len(class_list) == 0:
        data['classes'].append({"className": cls['className'], "methods": [method, ]})
    elif len(class_list) == 1:
        target = class_list[0]
        target['methods'].append(method)
    else:
        print("Error: Found multiple same class")
        exit()

def add_exception_recursively(data, cls, method, exception):
    class_list = find_class(data['classes'], cls)
    if len(class_list) == 0:
        data['classes'].append({"className": cls['className'], "methods": [{"methodName": method['methodName'], "exceptions": exception}]})
    elif len(class_list) == 1:
        target_class = class_list[0]
        method_list = find_method(target_class['methods'], method)
        if len(method_list) == 0:
            target_class['methods'].append({"methodName": method['methodName'], "exceptions": [exception, ]})
        elif len(method_list) == 1:
            target_method = method_list[0]
            target_method['exceptions'].append(exception)
        else:
            print("Error: Found multiple same method")
            exit()
    else:
        print("Error: Found multiple same class")
        exit()


added_api = []
removed_api = []
changed_data = []

# to remove same method existed in both old and new
old_method_removed_list = []
new_method_removed_list = []
changed_exception_count = 0
added_exception_count = 0
removed_exception_count = 0

for new_class in new_classes:
    matched_class_list = find_class(old_classes, new_class)
    if len(matched_class_list) == 0:
        continue
    elif len(matched_class_list) > 1:
        print(f"ERROR: Found multiple classes for **{new_class['className']}**")
        exit()
    old_class = matched_class_list[0]
    
    for new_method in new_class['methods']:
        matched_method_list = find_method(old_class['methods'], new_method)
        if len(matched_method_list) == 0:
            continue
        elif len(matched_method_list) > 1:
            print(f"ERROR: Found multiple methods for **{new_method['methodName']}** in **{new_class['className']}**")
            exit()
        old_method = matched_method_list[0]
        old_method_removed_list.append(old_method.copy())
        new_method_removed_list.append(new_method.copy())
        method_data = {"className": new_class['className'], "methodName": new_method['methodName'], "exceptions": []}
        
        old_exception_removed_list = []
        new_exception_removed_list = []
        for new_exception in new_method['exceptions']:
            old_exception_list = find_exception(old_method['exceptions'], new_exception)
            if len(old_exception_list) == 0:
                continue
            elif len(old_exception_list) > 1:
                for old_exception in old_exception_list:
                    if exception_equal(old_exception, new_exception):
                        old_exception_removed_list.append(old_exception.copy())
                        new_exception_removed_list.append(new_exception.copy())
                        break
                continue

            old_exception = old_exception_list[0]
            old_exception_removed_list.append(old_exception.copy())
            new_exception_removed_list.append(new_exception.copy())
            if exception_equal(old_exception, new_exception) is False:
                method_data['exceptions'].append({
                    "exceptionName": new_exception['ExceptionName'],
                    "exceptionType": new_exception['ExceptionType'],
                    "message": new_exception['message'],
                    "modifier": new_exception['modifier'],
                    "label": "changed exception",
                    "conditions": {
                        "old": old_exception.get('conditions', 'None'),
                        "new": new_exception.get('conditions', 'None')
                    },
                    "preConditions": {
                        "old": old_exception.get('preConditions', 'None'),
                        "new": new_exception.get('preConditions', 'None')
                    }
                })
                changed_exception_count += 1
        
        for exception in new_exception_removed_list:
            new_method['exceptions'].remove(exception)
        for exception in old_exception_removed_list:
            old_method['exceptions'].remove(exception)
        
        for added_exception in new_method['exceptions']:
            method_data['exceptions'].append({
                "exceptionName": added_exception['ExceptionName'],
                "exceptionType": added_exception['ExceptionType'],
                "message": added_exception['message'],
                "modifier": added_exception['modifier'],
                "label": "added exception",
                "conditions": {
                    "new": new_exception.get('conditions', 'None')
                },
                "preConditions": {
                    "new": new_exception.get('preConditions', 'None')
                }
            })
            added_exception_count += 1
        
        for removed_exception in old_method['exceptions']:
            method_data['exceptions'].append({
                "exceptionName": removed_exception['ExceptionName'],
                "exceptionType": removed_exception['ExceptionType'],
                "message": removed_exception['message'],
                "modifier": removed_exception['modifier'],
                "label": "removed exception",
                "conditions": {
                    "old": removed_exception.get('conditions', 'None')
                },
                "preConditions": {
                    "old": removed_exception.get('preConditions', 'None')
                }
            })
            removed_exception_count += 1
        
        changed_data.append(method_data)

for new_class in new_classes:
    for new_method in new_class['methods'].copy():
        if new_method in new_method_removed_list:
            new_class['methods'].remove(new_method)

for old_class in old_classes:
    for old_method in old_class['methods'].copy():
        if old_method in old_method_removed_list:
            old_class['methods'].remove(old_method)
        
added_api_count = 0
for new_class in new_classes:
    for new_method in new_class['methods']:
        added_api_count += 1
        added_api.append({"className": new_class['className'], "methodName": new_method['methodName']})

removed_api_count = 0
for old_class in old_classes:
    for old_method in old_class['methods']:
        removed_api_count += 1
        removed_api.append({"className": old_class['className'], "methodName": old_method['methodName']})

for api in changed_data.copy():
    exceptions = api['exceptions']
    if len(exceptions) == 0:
        changed_data.remove(api)
changed_api_count = len(changed_data)

statistic = {
    "old_version": VERSION1,
    "new_version": VERSION2,
    "added_api": added_api_count,
    "removed_api": removed_api_count,
    "changed_api": changed_api_count,
    "changed_exception": changed_exception_count,
    "added_exception": added_exception_count,
    "removed_exception": removed_exception_count
}

RESULT_DIR = 'analysis_result/'
os.makedirs(os.path.dirname(RESULT_DIR), exist_ok=True)

with open(f'{RESULT_DIR}changed_api.json', "w") as json_file:
    json.dump(changed_data, json_file, indent=4)
        
with open(f'{RESULT_DIR}added_api.json', "w") as json_file:
    json.dump(added_api, json_file, indent=4)

with open(f'{RESULT_DIR}removed_api.json', "w") as json_file:
    json.dump(removed_api, json_file, indent=4)

with open(f'{RESULT_DIR}statistic.json', "w") as json_file:
    json.dump(statistic, json_file, indent=4)
