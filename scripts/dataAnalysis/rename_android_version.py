import os
DIRECTORY_PREFIX = 'original_data/'
DIRECTORY = ['android-results-inter', 'android-results-intra']

def rename_files_in_directory(directory_path):
    for filename in os.listdir(directory_path):
        if filename.startswith("framework-"):
            parts = filename.split("_")
            version = parts[-1].split(".")[0]

            new_filename = f"framework-{version}"

            old_filepath = os.path.join(directory_path, filename)
            new_filepath = os.path.join(directory_path, new_filename)

            os.rename(old_filepath, new_filepath)
            print(f"Renamed: {old_filepath} -> {new_filepath}")

for direc in DIRECTORY:
    rename_files_in_directory(DIRECTORY_PREFIX + direc)
