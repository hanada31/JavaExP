from config import *
import json

INPUT_DIR = f'statistic_result/{TAG}/changed-data-callChain-filter'
DATA_TYPE = ['added_api', 'removed_api', 'added_exceptions', 'removed_exceptions', 'type_changed_exceptions', 'pre_conditions_changed_exceptions', 'message_changed_exceptions']
INPUT = lambda type: f'{INPUT_DIR}/{type}.json'
OUTPUT_DIR = f'statistic_result/{TAG}/changed-data-callChain-filter-gap'
OUTPUT = lambda type: f'{OUTPUT_DIR}/{type}.json'

if __name__ == "__main__":
    for type in DATA_TYPE:
        with open(INPUT(type), 'r') as f:
            methods = json.load(f)
    
    