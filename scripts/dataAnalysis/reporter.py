import json
from collections import OrderedDict
from matcher.MessageMatcher import MessageMatcher
from matcher.PreConditionsMatcher import PreConditionMatcher
from matcher.KeyPreConditionMatcher import KeyPreConditionMatcher
from matcher.TypeMatcher import TypeMatcher
from matcher.CompletePreConditionsMatcher import CompletePreConditionsMatcher
import time
start_time = time.time()
from config import *

FILE = lambda version: f'preprocess_result/{TAG}/v{version}-{TAG}.json'
RESULT_DIR = 'report_result'
report: dict = {}
unmatched_count = 0


def find_method(signature):
    global report
    return report.get(signature, None)


def update_report(methods, version):
    global report
    new_report = {}
    length = len(methods)
    i = 0
    while i < length:
        method = methods[i]
        previous_method = find_method(method['methodSignature'])
        if previous_method is None:
            method['added_version'] = f'v{version}'
            for exception in method['exceptions']:
                exception['added_version'] = version
                exception['exceptionName_history'] = []
                exception['message_history'] = []
                exception['preConditions_history'] = []
                exception['keyPreCondition_history'] = []
                exception['invokeUnit_history'] = []
                exception['callChain_history'] = []
            new_report[method['methodSignature']] = method
        else:
            if 'removed_version' in previous_method:
                print('[INFO]: Found a re-added method:')
                print(f'{method["methodSignature"]}')
                method['methodSignature'] += '*'
                continue
            del report[previous_method['methodSignature']]
            merged_method = diff_method(previous_method, method, version)
            if version == VERSIONS[-1]:
                merged_method['lifetime_label'] = f'live in last version v{version}'
            new_report[merged_method['methodSignature']] = merged_method
        i += 1
    
    for remaining_method in report.values():
        if 'removed_version' not in remaining_method:
            remaining_method['removed_version'] = f'v{version}'
        new_report[remaining_method['methodSignature']] = remaining_method
    
    report = new_report

def diff_method(_old_method, _new_method, version):
    old_method = _old_method.copy()
    new_method = _new_method.copy()
    merged_method = old_method.copy()
    merged_exceptions = []
    target_exceptions = new_method['exceptions'].copy()
    for exception in target_exceptions:
        candidate_exceptions = [exception for exception in old_method['exceptions'] if 'removed_version' not in exception]
        matched_exceptions = candidate_exceptions.copy()
        for matcher in [TypeMatcher, MessageMatcher, CompletePreConditionsMatcher]:
            matched_exceptions = matcher().match(matched_exceptions, exception)

        if len(matched_exceptions) >= 1:
            old_exception = matched_exceptions[0]
            old_method['exceptions'].remove(old_exception)
            new_method['exceptions'].remove(exception)
            merged_exception = diff_exception(old_exception, exception, version)
            merged_exceptions.append(merged_exception)

    for exception in new_method['exceptions']:
        candidate_exceptions = [exception for exception in old_method['exceptions'] if 'removed_version' not in exception]
        matched_exceptions = MessageMatcher().match(candidate_exceptions, exception)
        matched_exceptions = PreConditionMatcher().match(matched_exceptions, exception)
        
        if len(matched_exceptions) != 1:
            same_type_exceptions = TypeMatcher().match(candidate_exceptions, exception)
            if len(same_type_exceptions) > 1:
                matched_exceptions = same_type_exceptions
                for matcher in [MessageMatcher, CompletePreConditionsMatcher, KeyPreConditionMatcher]:
                    new_matched_exceptions = matcher().match(matched_exceptions, exception)
                    if len(new_matched_exceptions) == 1:
                        matched_exceptions = new_matched_exceptions
                        break
                    elif len(new_matched_exceptions) > 1:
                        matched_exceptions = new_matched_exceptions
            elif len(same_type_exceptions) == 1:
                for matcher in [MessageMatcher, CompletePreConditionsMatcher, KeyPreConditionMatcher]:
                    new_matched_exceptions = matcher().match(same_type_exceptions, exception)
                    if len(new_matched_exceptions) == 1:
                        matched_exceptions = new_matched_exceptions
                        break
        
        if len(matched_exceptions) == 1:
            old_exception = matched_exceptions[0]
            old_method['exceptions'].remove(old_exception)
            merged_exception = diff_exception(old_exception, exception, version)
            merged_exceptions.append(merged_exception)
        else:
            added_exception = exception.copy()
            global unmatched_count
            unmatched_count += 1
            added_exception['added_version'] = version
            added_exception['exceptionName_history'] = []
            added_exception['message_history'] = []
            added_exception['preConditions_history'] = []
            added_exception['keyPreCondition_history'] = []
            added_exception['invokeUnit_history'] = []
            added_exception['callChain_history'] = []
            merged_exceptions.append(added_exception)
    
    for remaining_exception in old_method['exceptions']:
        if 'removed_version' not in remaining_exception:
            remaining_exception['removed_version'] = version
        merged_exceptions.append(remaining_exception)
    
    merged_method['exceptions'] = merged_exceptions
    return merged_method


def diff_exception(old_exception, new_exception, version):
    merged_exception = old_exception.copy()
    if old_exception['exceptionName'] != new_exception['exceptionName']:
        merged_exception['exceptionName'] = new_exception['exceptionName']
        merged_exception['exceptionName_history'].append({
            f'version<{version}': old_exception['exceptionName']
        })
    if old_exception['message'] != new_exception['message']:
        merged_exception['message'] = new_exception['message']
        merged_exception['message_history'].append({
            f'version<{version}': old_exception['message']
        })
    if PreConditionMatcher.pre_conditions_equal(old_exception['preConditions'], new_exception['preConditions']) is False:
        merged_exception['preConditions'] = new_exception['preConditions']
        merged_exception['preConditions_history'].append({
            f'version<{version}': old_exception['preConditions']
        })
    if PreConditionMatcher.unorder_list_equal(old_exception['keyPreCondition'], new_exception['keyPreCondition']) is False:
        merged_exception['keyPreCondition'] = new_exception['keyPreCondition']
        merged_exception['keyPreCondition_history'].append({
            f'version<{version}': old_exception['keyPreCondition']
        })
    if old_exception['invokeUnit'] != new_exception['invokeUnit']:
        merged_exception['invokeUnit'] = new_exception['invokeUnit']
        merged_exception['invokeUnit_history'].append({
            f'version<{version}': old_exception['invokeUnit']
        })
    if old_exception['callChain'] != new_exception['callChain']:
        merged_exception['callChain'] = new_exception['callChain']
        merged_exception['callChain_history'].append({
            f'version<{version}': old_exception['callChain']
        })
    return merged_exception


def sort_report_keys(report):
    final_report = list(report.values())
    method_key_order = ['className', 'methodSignature', 'modifier', 'added_version', 'removed_version', 'lifetime_label', 'exceptions']
    exception_key_order = ['exceptionName', 'modifier', 'added_version', 'removed_version', 'exceptionName', 'exceptionName_history', 'message', 'message_history', 'preConditions', 'preConditions_history', 'keyPreCondition', 'keyPreCondition_history', 'callChain', 'callChain_history', 'invokeUnit', 'invokeUnit_history', 'invokeMethod', 'throwUnit', 'throwUnitInMethod', 'throwUnitOrder']
    # sort dict key order
    for method in final_report.copy():
        # sort exceptions
        exceptions = method['exceptions']
        ordered_exceptions = []
        for exception in exceptions:
            ordered_dict = OrderedDict()
            for key in exception_key_order:
                if key in exception:
                    ordered_dict[key] = exception[key]
            for key in exception:
                if key not in ordered_dict:
                    ordered_dict[key] = exception[key]
            ordered_exceptions.append(ordered_dict)
        method['exceptions'] = ordered_exceptions

        # sort method
        ordered_method = OrderedDict()
        for key in method_key_order:
            if key in method:
                ordered_method[key] = method[key]
        for key in method:
            if key not in ordered_method:
                ordered_method[key] = method[key]
        final_report.remove(method)
        final_report.append(ordered_method)
    return final_report


def sort_report_class(methods):
    from collections import Counter
    class_methods = {}
    class_exceptions_counter = Counter()
    for method in methods:
        className = method['className']
        class_exceptions_counter[className] += len(method['exceptions'])
        if className not in class_methods:
            class_methods[className] = [method]
        else:
            class_methods[className].append(method)
    
    print('********** Exceptions Amount Of Class Counter **********')
    import pprint
    pprint.pprint(class_exceptions_counter)

    sorted_report = []
    for (key, frequency) in class_exceptions_counter.most_common():
        for method in class_methods[key]:
            sorted_report.append(method)
    return sorted_report



for v in VERSIONS:
    with open(FILE(v), 'r') as f:
        methods = json.load(f)
        update_report(methods, v)
    print(f'********** Version {v} has done **********')

methods = sort_report_keys(report)
methods = sort_report_class(methods)
with open(f'{RESULT_DIR}/report-<{TAG}>-<{VERSIONS[0]}-{VERSIONS[-1]}>.json', 'w') as json_file:
    json.dump(methods, json_file, indent=4)

print(f'unmatched exception and added exception count: {unmatched_count}')
print("--- %s seconds ---" % (time.time() - start_time))