import json
import os
import re
from collections import Counter
from config import *

RESULT_DIR = 'preprocess_result'

def merge(classes):
    for cls in classes:
        for method in cls['methods']:
            thorw_id_count = Counter()
            throw_id_dict = {}
            for exception in method['exceptions'].copy():
                throw_id = exception['throwUnitOrder']
                if throw_id == -1:
                    throw_id = f'{exception["callChain"]}-{exception["invokeUnit"]}'
                if thorw_id_count[throw_id] == 0:
                    if 'preConditions' in exception:
                        exception['preConditions'] = [
                            exception['preConditions'],
                        ]
                    else:
                        exception['preConditions'] = []
                    
                    if 'keyPreCondition' in exception:
                        exception['keyPreCondition'] = [
                            exception['keyPreCondition']
                        ]
                    else:
                        exception['keyPreCondition'] = []

                    throw_id_dict[throw_id] = exception
                else:
                    if 'preConditions' in exception:
                        def check_attribute(exception1, exception2, attribute):
                            if exception1.get(attribute) != exception2.get(attribute):
                                print(f"[Error]: merge two exceptions that has different {attribute}")
                                print(f"method: {method['methodName']}")
                                print(f"exception1 {attribute}: {exception1.get(attribute)}")
                                print(f"exception2 {attribute}: {exception2.get(attribute)}")
                                print(f"exception1 invokeUnit: {exception1['invokeUnit']}")
                                print(f"exception2 invokeUnit: {exception2['invokeUnit']}")
                                print(f"exception1 callChain: {exception1['callChain']}")
                                print(f"exception2 callChain: {exception2['callChain']}")
                                exit()
                        target_exception = throw_id_dict[throw_id]
                        check_attribute(target_exception, exception, 'message')
                        check_attribute(target_exception, exception, 'exceptionName')
                        throw_id_dict[throw_id]['preConditions'].append(exception['preConditions'])
                        if 'keyPreCondition' in exception and exception['keyPreCondition'] not in throw_id_dict[throw_id]['keyPreCondition']:
                            throw_id_dict[throw_id]['keyPreCondition'].append(exception['keyPreCondition'])
                thorw_id_count[throw_id] += 1
            
            method['exceptions'] = list(reversed(throw_id_dict.values()))


def unfold(classes):
    unfold_result = []
    for cls in classes:
        class_name = cls['className']
        for method in cls['methods']:
            if re.search(r'\$.*__', class_name):
                continue
            unfold_result.append({
                "className": class_name,
                "methodSignature": method['methodName'],
                "modifier": method['modifier'],
                "exceptions": method['exceptions']
            })
    return unfold_result

def preprocess(version: str, tag: str, result_dir: str):
    with open(FILE(version, tag), 'r') as f:
        file = json.load(f)

    classes = file['classes']
    merge(classes)
    methods = unfold(classes)

    for method in methods:
        for exception in method['exceptions']:
            exception.pop('method')
            if 'conditions' in exception:
                exception.pop('conditions')
            if 'preConditions' not in exception:
                exception['preConditions'] = []
            if 'keyPreCondition' not in exception:
                exception['keyPreCondition'] = []
            if 'invokeUnit' not in exception:
                exception['invokeUnit'] = None
            if 'callChain' not in exception:
                exception['callChain'] = None
            if exception['message'] == '':
                exception['message'] = '[\\s\\S]*'
            rename_dict = {}

            for k, v in list(exception.items()):
                exception[rename_dict.get(k, k)] = exception.pop(k)

    os.makedirs(os.path.dirname(result_dir + f'/{tag}/'), exist_ok=True)
    with open(f'{result_dir}/{tag}/v{version}-{tag}.json', 'w') as json_file:
        json.dump(methods, json_file, indent=4)

for v in VERSIONS:
    preprocess(v, TAG, RESULT_DIR)
    print(f'--------Version {v} Done--------')