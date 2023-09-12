import re
import json
from config import *
import pprint
import pathlib
from collections import Counter

INPUT_DIR = f'statistic_result/{TAG}/changed-data'
DATA_TYPE = ['added_api', 'removed_api', 'added_exceptions', 'removed_exceptions', 'type_changed_exceptions', 'pre_conditions_changed_exceptions', 'message_changed_exceptions']
INPUT = lambda type: f'{INPUT_DIR}/{type}.json'
OUTPUT_DIR = f'statistic_result/{TAG}/changed-data-callChain-filter'
OUTPUT = lambda type: f'{OUTPUT_DIR}/{type}.json'

def callChain_is_include(callChain_list: list, method_history_set):
    for callChain in callChain_list:
        callChain_methods = callChain.split('->')
        for method in callChain_methods:
            methodSignature = re.findall("<[^<>]*>", method)
            if len(methodSignature) == 0:
                continue
            methodSignatureStripArguments = re.sub(r"\([^\(\)]*\)", "()", methodSignature[0])
            if methodSignatureStripArguments in method_history_set:
                return True
            else:
                method_history_set.add(methodSignatureStripArguments)
    return False


if __name__ == "__main__":
    exceptions_counter = Counter()
    for type in DATA_TYPE:
        method_history_set = set()
        with open(INPUT(type), 'r') as f:
            methods = json.load(f)

        for method in methods:
            for exception in method['exceptions'].copy():
                callChain_all = []
                callChain = exception.get('callChain', None)
                if callChain is not None:
                    callChain_all.append(callChain)
                for _callChain in exception['callChain_history']:
                    callChain = list(_callChain.values())[0]
                    callChain_all.append(callChain)
                if callChain_is_include(callChain_all, method_history_set):
                    method['exceptions'].remove(exception)

        for method in methods.copy():
            if len(method['exceptions']) == 0:
                methods.remove(method)
            exceptions_counter[type] += len(method['exceptions'])

        pathlib.Path(OUTPUT_DIR).mkdir(exist_ok=True) 
        with open(OUTPUT(type), 'w') as f:
            json.dump(methods, f, indent=4)
        
        print(f'********** {type} callChain method history set **********')
        pprint.pprint(method_history_set)
    
    # print statistic
    with open(OUTPUT('statistic-after-filter'), 'w') as f:
        json.dump(exceptions_counter, f, indent=4)