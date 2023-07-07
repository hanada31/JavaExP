
import json

def extract_information(json_data):
    for class_info in json_data:
        qualified_signature = class_info['qualifiedSignature']
        exception = class_info['exception']
        extracted_data.append(qualified_signature + exception)
        #print(qualified_signature + exception)

# The program begins here.
extracted_data = []

for num in range(0, 108):
    filename = str(num + 1) + ".json"  # Replace with your file name
    
    with open(filename) as file:
        file_content = file.read()
        json_data = json.loads(file_content)

    extract_information(json_data)

print(len(extracted_data))
list_after_dedumplication = list(set(extracted_data))
print(len(list_after_dedumplication))