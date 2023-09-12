import json

TAG1 = 'results-intra-noCatchAnalysis'
TAG2 = 'results-intra'
VERSION = '2.6'
FILE1 = f'preprocess_result/v{VERSION}-{TAG1}.json'
FILE2 = f'preprocess_result/v{VERSION}-{TAG2}.json'

def find_method(methods, signature):
    for method in methods:
        if method['methodSignature'] == signature:
            return method
    return None

def find_exception(exceptions, order, unit):
    for exception in exceptions:
        if exception['throwUnitOrder'] == order and exception['throwUnit'] == unit:
            return exception
    return None

with open(FILE1, 'r') as f:
    new_methods = json.load(f)

with open(FILE2, 'r') as f:
    old_methods = json.load(f)


def unorder_list_equal(list1: list, list2: list):
    if len(list1) != len(list2):
        return False
    for item in list1:
        if item in list2:
            list2.remove(item)
        else:
            return False
    return True

def compare_preCond(preCond1, preCond2):
    cond_list1 = preCond1.copy()
    cond_list2 = preCond2.copy()
    for cond1 in cond_list1:
        found_same = False
        for cond2 in cond_list2:
            if unorder_list_equal(cond1, cond2):
                found_same = True
        if found_same is False:
            return False
    return True


diff_list = []
for new_method in new_methods:
    old_method = find_method(old_methods, new_method['methodSignature'])
    if old_method is None:
        print("Error: multiple same signature")
        exit()
    
    changed_exception_list = []
    for new_exception in new_method['exceptions']:
        old_exception = find_exception(old_method['exceptions'], new_exception['throwUnitOrder'], new_exception['throwUnit'])
        if old_exception is None:
            print(f"no exception for {new_method['methodSignature']} and {new_exception['throwUnit']} {new_exception['throwUnitOrder']}")
            continue
        excep = new_exception.copy()
        changed = False
        if new_exception['message'] != old_exception['message']:
            changed = True
            excep['message'] = {
                f"{TAG1}": new_exception['message'],
                f"{TAG2}": old_exception['message']
            }
        new_preCond = new_exception['preConditions'] if 'preConditions' in new_exception else []
        old_preCond = old_exception['preConditions'] if 'preConditions' in old_exception else []
        if compare_preCond(new_preCond, old_preCond) is False:
            changed = True
            excep['preConditions'] = {
                f"{TAG1}": new_preCond,
                f"{TAG2}": old_preCond
            }
        if changed is True:
            changed_exception_list.append(excep)
    
    if len(changed_exception_list) > 0:
        added_method = new_method.copy()
        added_method['exceptions'] = changed_exception_list
        diff_list.append(added_method)


with open(f'compare_result/v{VERSION}-<{TAG1}>-vs-<{TAG2}>.json', 'w') as json_file:
    json.dump(diff_list, json_file, indent=4)

