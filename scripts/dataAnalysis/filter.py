import json

# FILENAME='report-<apache-results-intra>-<2.5-2.13.0>.json'
FILENAME='report-<android-results-intra>-<2.3-12.0>.json'
REPORT=f'report_result/{FILENAME}'
RESULT=f'report_filter_result/{FILENAME}'

output = []
exception_count = 0
with open(REPORT, 'r') as f:
    methods = json.load(f)

for method in methods:
    exception_length = len(method['exceptions'])
    exception_count += exception_length
    if exception_length != 0:
        output.append(method)

with open(RESULT, 'w') as json_file:
    json.dump(output, json_file, indent=4)

print(exception_count)
