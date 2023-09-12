from config import *

FILE = f'report_result/report-<{TAG}>-<{VERSIONS[0]}-{VERSIONS[-1]}>.json'
OUTPUT = f'statistic_result/{TAG}'

def statistic_methods(methods):
    added_exception_amount = 0
    removed_exception_amount = 0
    exception_amount = 0
    message_changed_exception_amount = 0
    type_changed_exception_amount = 0
    pre_conditions_changed_exception_amount = 0

    for method in methods:
        added_version = VERSIONS.index(method['added_version'][1:])

        if 'removed_version' in method:
            removed_version = VERSIONS.index(method['removed_version'][1:])
        else:
            removed_version = VERSIONS.index(VERSIONS[-1]) + 1
        
        exceptions = method['exceptions']

        for exception in exceptions:
            exception_amount += 1
            len_type_chagned = len(exception['exceptionName_history'])
            len_message_changed = len(exception['message_history'])
            len_pre_conditions_changed = len(exception['preConditions_history'])
            type_changed_exception_amount += len_type_chagned
            message_changed_exception_amount += len_message_changed
            pre_conditions_changed_exception_amount += len_pre_conditions_changed

            exception_added_version = VERSIONS.index(exception['added_version'])
            if exception_added_version != added_version:
                added_exception_amount += 1
            
            if 'removed_version' in exception:
                exception_removed_version = VERSIONS.index(exception['removed_version'])
                removed_exception_amount += 1
            else:
                exception_removed_version = removed_version
    return {
        "exception_amount": exception_amount,
        "added_exception_amount": added_exception_amount,
        "removed_exception_amount": removed_exception_amount,
        "message_changed_exception_amount": message_changed_exception_amount,
        "type_changed_exception_amount": type_changed_exception_amount,
        "pre_conditions_changed_exception_amount": pre_conditions_changed_exception_amount
    }