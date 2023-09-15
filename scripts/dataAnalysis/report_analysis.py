import json
import os
import copy
from collections import Counter
from config import *
from report_analysis_tool import statistic_methods

FILE = f'report_result/report-<{TAG}>-<{VERSIONS[0]}-{VERSIONS[-1]}>.json'
# FILE = f'report_result/report-<{TAG}>-<{VERSIONS[0]}-{VERSIONS[-1]}>-callChain_filter.json'
OUTPUT = f'statistic_result/{TAG}'
# api lifetime counter
api_lifetime_count = Counter()
pre_condition_sensitive_api_lifetime_count = Counter()
changed_sensitive_api_lifetime_count = Counter()
# only include api that has exception at least 1
only_include_exception_api_lifetime = {
    "api_lifetime": Counter(),
    "changed_sensitive_api_lifetime": Counter(),
    "pre_condition_sensitive_api_lifetime": Counter()
}
# sum of added api, not include api exist in first version
added_api_amount = 0
# sum of removed api, not include api exist in last version
removed_api_amount = 0
# added in first version api
first_version_exist_api_amount = 0
# not removed api yet in last version
last_version_exist_api_amount = 0
# exist in first version and last version
always_exist_api_amount = 0
# always exist api exception changed counter
always_exist_api_changed_counter = Counter()
always_exist_api_type_changed_counter = Counter()
always_exist_api_message_changed_counter = Counter()
always_exist_api_preCondition_changed_counter = Counter()
always_exist_api_added_exception_counter = Counter()
always_exist_api_removed_exception_counter = Counter()
# all api exception changed counter
api_changed_counter = Counter()
api_type_changed_counter = Counter()
api_message_changed_counter = Counter()
api_preCondition_changed_counter = Counter()
api_added_exception_counter = Counter()
api_removed_exception_counter = Counter()
# sum of api
api_count = 0
# sum of exception
exception_count = 0
# times that exception message was changed in full lifetime exception
message_changed_count = 0
type_changed_count = 0
pre_conditions_changed_count = 0
# sum of added/removed exception exception
added_exception_amount = 0
removed_exception_amount = 0

filter_api = []
added_api = []
removed_api = []
added_exceptions = {}
removed_exceptions = {}
message_changed_exceptions = {}
type_changed_exceptions = {}
pre_conditions_changed_exceptions = {}

max_exceptions = 0
num_of_exceptions_counter = Counter()

def update_exception(_method, _exception, target):
    method = _method.copy()
    exception = _exception.copy()
    signature = method['methodSignature']
    if signature in target:
        target[signature]['exceptions'].append(exception)
    else:
        method['exceptions'] = [exception]
        target[signature] = method

def init_report(path):
    with open(path, 'r') as f:
        methods = json.load(f)

    filter_methods = []
    for method in methods:
        # filter methods
        if '$' in method['className']:
            continue
        if 'Lambda' in method['className']:
            continue
        if 'lambda$' in method['methodSignature']:
            continue
        filter_methods.append(method)
    return filter_methods

def extract_invoked_method_set(methods):
    invoked_methods = set()
    for method in methods:
        for exception in method['exceptions']:
            if exception['throwUnitOrder'] == -1:
                invoked_methods.add(exception['invokedMethod'])
    return invoked_methods

# filter set by conditions
# filter: filter fucntion list used for filter
def filter_by_conditions(methods, filter):
    results = []
    for method in methods:
        filtered = filter(method)
        
        if filtered is True:
            results.append(method)
    return results

api_lifetime_method = {}
for i in range(len(VERSIONS) + 1):
    api_lifetime_method[i] = []

methods = init_report(FILE)
invoked_methods_signature = extract_invoked_method_set(methods)
filter_function = lambda method: method['modifier'] == 'public' or method['methodSignature'] in invoked_methods_signature
public_and_invoked_methods = filter_by_conditions(copy.deepcopy(methods), filter_function)
for method in public_and_invoked_methods:
    throw_order_great_than_equal_zero = lambda exception: exception['throwUnitOrder'] >= 0
    method['exceptions'] = filter_by_conditions(method['exceptions'], throw_order_great_than_equal_zero)

exception_level_statistic = statistic_methods(public_and_invoked_methods)

for method in methods:
    # filter methods
    if method['modifier'] != 'public':
        continue

    filter_api.append(method)
    added_version = VERSIONS.index(method['added_version'][1:])
    if added_version != 0:
        added_api_amount += 1
        added_api.append(method.copy())
    else:
        first_version_exist_api_amount += 1

    if 'removed_version' in method:
        removed_version = VERSIONS.index(method['removed_version'][1:])
        removed_api_amount += 1
        removed_api.append(method.copy())
    else:
        last_version_exist_api_amount += 1
        removed_version = VERSIONS.index(VERSIONS[-1]) + 1
    
    always_exist_api = False
    if added_version == 0 and removed_version == VERSIONS.index(VERSIONS[-1]) + 1:
        always_exist_api_amount += 1
        always_exist_api = True

    api_count += 1
    lifetime = removed_version - added_version
    api_lifetime_count[lifetime] += 1
    api_lifetime_method[lifetime].append(method)
    if len(method['exceptions']) > 0:
        only_include_exception_api_lifetime['api_lifetime'][lifetime] += 1

    exceptions = method['exceptions']
    num_of_exceptions_counter[len(exceptions)] += 1
    max_exceptions = max(max_exceptions, len(exceptions))
    pre_conditions_sensitive_lifetime_node = [added_version, removed_version]
    changed_sensitive_lifetime_node = [added_version, removed_version]
    len_api_type_changed_count = 0
    len_api_message_changed_count = 0
    len_api_preCondition_changed_count = 0
    len_api_added_exception_count = 0
    len_api_removed_exception_count = 0

    for exception in exceptions:
        exception_count += 1
        len_type_chagned = len(exception['exceptionName_history'])
        len_message_changed = len(exception['message_history'])
        len_pre_conditions_changed = len(exception['preConditions_history'])
        len_api_type_changed_count += len_type_chagned
        len_api_message_changed_count += len_message_changed
        len_api_preCondition_changed_count += len_pre_conditions_changed
        type_changed_count += len_type_chagned
        message_changed_count += len_message_changed
        pre_conditions_changed_count += len_pre_conditions_changed
        if len_type_chagned != 0:
            update_exception(method, exception, type_changed_exceptions)
        if len_message_changed != 0:
            update_exception(method, exception, message_changed_exceptions)
        if len_pre_conditions_changed != 0:
            update_exception(method, exception, pre_conditions_changed_exceptions)
        exception_added_version = VERSIONS.index(exception['added_version'])
        if exception_added_version != added_version:
            added_exception_amount += 1
            len_api_added_exception_count += 1
            update_exception(method, exception, added_exceptions)
            pre_conditions_sensitive_lifetime_node.append(exception_added_version)
            changed_sensitive_lifetime_node.append(exception_added_version)
        
        if 'removed_version' in exception:
            exception_removed_version = VERSIONS.index(exception['removed_version'])
            removed_exception_amount += 1
            len_api_removed_exception_count += 1
            update_exception(method, exception, removed_exceptions)
            pre_conditions_sensitive_lifetime_node.append(exception_removed_version)
            changed_sensitive_lifetime_node.append(exception_removed_version)
        else:
            exception_removed_version = removed_version
        exception_lifetime = exception_removed_version - exception_added_version

        for pre_condition_key in exception['preConditions_history']:
            changed_version_str = list(pre_condition_key.keys())[0][8:]
            changed_node = VERSIONS.index(changed_version_str)
            pre_conditions_sensitive_lifetime_node.append(changed_node)
            changed_sensitive_lifetime_node.append(changed_node)
        
        for message_key in exception['message_history']:
            changed_version_str = list(message_key.keys())[0][8:]
            changed_node = VERSIONS.index(changed_version_str)
            changed_sensitive_lifetime_node.append(changed_node)
    
        for type_key in exception['exceptionName_history']:
            changed_version_str = list(type_key.keys())[0][8:]
            changed_node = VERSIONS.index(changed_version_str)
            changed_sensitive_lifetime_node.append(changed_node)
    
    total_changed_count = len_api_type_changed_count + len_api_message_changed_count + len_api_preCondition_changed_count + len_api_added_exception_count + len_api_removed_exception_count
    if always_exist_api is True:
        always_exist_api_changed_counter[total_changed_count] += 1
        always_exist_api_type_changed_counter[len_api_type_changed_count] += 1
        always_exist_api_message_changed_counter[len_api_message_changed_count] += 1
        always_exist_api_preCondition_changed_counter[len_api_preCondition_changed_count] += 1
        always_exist_api_added_exception_counter[len_api_added_exception_count] += 1
        always_exist_api_removed_exception_counter[len_api_removed_exception_count] += 1
    api_changed_counter[total_changed_count] += 1
    api_type_changed_counter[len_api_type_changed_count] += 1
    api_message_changed_counter[len_api_message_changed_count] += 1
    api_preCondition_changed_counter[len_api_preCondition_changed_count] += 1
    api_added_exception_counter[len_api_added_exception_count] += 1
    api_removed_exception_counter[len_api_removed_exception_count] += 1
        
    # remove duplicate
    pre_conditions_sensitive_lifetime_node = list(set(pre_conditions_sensitive_lifetime_node))
    pre_conditions_sensitive_lifetime_node = sorted(pre_conditions_sensitive_lifetime_node)
    for i in range(len(pre_conditions_sensitive_lifetime_node) - 1):
        pre_condition_sensitive_api_lifetime = pre_conditions_sensitive_lifetime_node[i + 1] - pre_conditions_sensitive_lifetime_node[i]
        pre_condition_sensitive_api_lifetime_count[pre_condition_sensitive_api_lifetime] += 1
        if len(exceptions) > 0:
            only_include_exception_api_lifetime['pre_condition_sensitive_api_lifetime'][pre_condition_sensitive_api_lifetime] += 1

    changed_sensitive_lifetime_node = list(set(changed_sensitive_lifetime_node))
    changed_sensitive_lifetime_node = sorted(changed_sensitive_lifetime_node)
    for i in range(len(changed_sensitive_lifetime_node) - 1):
        changed_sensitive_api_lifetime = changed_sensitive_lifetime_node[i + 1] - changed_sensitive_lifetime_node[i]
        changed_sensitive_api_lifetime_count[changed_sensitive_api_lifetime] += 1
        if len(exceptions) > 0:
            only_include_exception_api_lifetime['changed_sensitive_api_lifetime'][changed_sensitive_api_lifetime] += 1
    
os.makedirs(os.path.dirname(f'{OUTPUT}/lifetime-data/'), exist_ok=True)
for key, value in api_lifetime_method.items():
    with open(f'{OUTPUT}/lifetime-data/lifetime-{key}.json', 'w') as json_file:
        json.dump(value, json_file, indent=4)
with open(f'{OUTPUT}/filter_api.json', 'w') as f:
    json.dump(filter_api, f, indent=4)
with open(f'{OUTPUT}/exception_level_methods.json', 'w') as f:
    json.dump(public_and_invoked_methods, f, indent=4)

os.makedirs(os.path.dirname(f'{OUTPUT}/changed-data/'), exist_ok=True)
with open(f'{OUTPUT}/changed-data/added_api.json', 'w') as json_file:
    json.dump(added_api, json_file, indent=4)
with open(f'{OUTPUT}/changed-data/removed_api.json', 'w') as json_file:
    json.dump(removed_api, json_file, indent=4)
with open(f'{OUTPUT}/changed-data/added_exceptions.json', 'w') as json_file:
    json.dump(list(added_exceptions.values()), json_file, indent=4)
with open(f'{OUTPUT}/changed-data/removed_exceptions.json', 'w') as json_file:
    json.dump(list(removed_exceptions.values()), json_file, indent=4)
with open(f'{OUTPUT}/changed-data/type_changed_exceptions.json', 'w') as json_file:
    json.dump(list(type_changed_exceptions.values()), json_file, indent=4)
with open(f'{OUTPUT}/changed-data/message_changed_exceptions.json', 'w') as json_file:
    json.dump(list(message_changed_exceptions.values()), json_file, indent=4)
with open(f'{OUTPUT}/changed-data/pre_conditions_changed_exceptions.json', 'w') as json_file:
    json.dump(list(pre_conditions_changed_exceptions.values()), json_file, indent=4)

print('--- api lifetime count ---')
print(api_lifetime_count)

changed_version = len(VERSIONS) - 1
api_lifetime_sum = sum(api_lifetime_count.values())

def statistic_counter(counter, name):
    weighted_sum = sum(key * value for key, value in counter.items())
    total_count = sum(counter.values())
    average_value = weighted_sum / total_count
    global statistic
    total_count = round(total_count, 2)
    average_value = round(average_value, 2)

    # plot
    import matplotlib.pyplot as plt

    x = list(counter.keys())
    y = list(counter.values())

    bars = plt.bar(x, y)

    for bar in bars:
        yval = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2, yval + 0.01, yval, ha='center', va='bottom')

    plt.xticks(x)

    plt.title(f'{name.replace("_", " ")} lifetime graph')
    plt.xlabel('Lifetime')
    plt.ylabel('Count')

    plt.savefig(f'{OUTPUT}/figures/{name}-lifetime.pdf')
    plt.show()
    plt.clf()
    return average_value

def counter_statistic(counter: Counter):
    zero = counter[0]
    not_zero = sum(counter.values()) - counter[0]
    del counter[0]
    sorted_counter = {k: counter[k] for k in sorted(counter.keys())}
    statistic = {'not-changed': zero, 'changed_total': not_zero, 'changed_statistic': sorted_counter}
    return statistic


os.makedirs(os.path.dirname(f'{OUTPUT}/figures/'), exist_ok=True)
statistic = {
    "start_version": VERSIONS[0],
    "end_version": VERSIONS[-1],
    "api_amount": api_count,
    "api_exist_in_first_version_amount": first_version_exist_api_amount,
    "api_exist_in_last_version_amount": last_version_exist_api_amount,
    "alwasy_api_amout": always_exist_api_amount,
    "added_api_amount": added_api_amount,
    "added_api_per_version": round(added_api_amount / changed_version, 2),
    "removed_api_amount": removed_api_amount,
    "removed_api_per_version": round(removed_api_amount / changed_version, 2),
    "api_level": {
        "exception_amount": exception_count,
        "added_exception_amount": added_exception_amount,
        "removed_exception_amount": removed_exception_amount,
        "exception_message_changed_amount": message_changed_count,
        "exception_type_changed_amount": type_changed_count,
        "exception_pre_conditions_changed_amount": pre_conditions_changed_count,
    },
    "exception_level": exception_level_statistic,
    "all_api_lifetime": {
        "average_lifetime": statistic_counter(api_lifetime_count, "all-api"),
        "pre_condition_sensitive_api_lifetime": statistic_counter(pre_condition_sensitive_api_lifetime_count, "pre_condition_sensitive_all-api"),
        "changed_sensitive_api_lifetime": statistic_counter(changed_sensitive_api_lifetime_count, "changed_sensitive_all-api")
    },
    "only_include_exception_api_lifetime": {
        "average_lifetime": statistic_counter(only_include_exception_api_lifetime['api_lifetime'], "exceptions-api"),
        "pre_condition_sensitive_api_lifetime": statistic_counter(only_include_exception_api_lifetime['pre_condition_sensitive_api_lifetime'], "pre_condition_sensitive_exceptions-api"),
        "changed_sensitive_api_lifetime": statistic_counter(only_include_exception_api_lifetime['changed_sensitive_api_lifetime'], "changed_sensitive_exceptions-api")
    },
    "all_api": {
        "total_changed_counter": counter_statistic(api_changed_counter),
        "type_changed_counter": counter_statistic(api_type_changed_counter),
        "message_changed_counter": counter_statistic(api_message_changed_counter),
        "preCondition_changed_counter": counter_statistic(api_preCondition_changed_counter),
        "added_exception_counter": counter_statistic(api_added_exception_counter),
        "removed_exception_counter": counter_statistic(api_removed_exception_counter)
    },
    "always_exist_api": {
        "total_changed_counter": counter_statistic(always_exist_api_changed_counter),
        "type_changed_counter": counter_statistic(always_exist_api_type_changed_counter),
        "message_changed_counter": counter_statistic(always_exist_api_message_changed_counter),
        "preCondition_changed_counter": counter_statistic(always_exist_api_preCondition_changed_counter),
        "added_exception_counter": counter_statistic(always_exist_api_added_exception_counter),
        "removed_exception_counter": counter_statistic(always_exist_api_removed_exception_counter)
    }
}

with open(f'{OUTPUT}/statistic.json', 'w') as f:
    json.dump(statistic, f, indent=4)

print('--- max exceptions number in one method ---')
print(max_exceptions)
print('--- number of exceptions in methods ---')
print(num_of_exceptions_counter)