#coding=utf-8
import os
import re
import subprocess
import zipfile

def search_methods_in_directory(directory, package_name, class_name, method_name):
    package_path = package_name.replace('.', '/')
    expected_file_name_java = f"{class_name}.java"
    expected_file_name_class = f"{class_name}.class"

    method_definition = ''
    # imports = []

    for root, dirs, files in os.walk(directory):
        for file in files:
            if file == expected_file_name_java or file == expected_file_name_class:
                file_path = os.path.join(root, file)
                decompiled_java_file_path = os.path.join(root, package_path, expected_file_name_java)

                if file.endswith('.class') and not os.path.exists(decompiled_java_file_path):

                    decompile_class_file(file_path, root)

                if os.path.exists(decompiled_java_file_path):
                    method_definition = find_method_in_java_file(decompiled_java_file_path, method_name)
                    # imports = find_imports_in_java_file(decompiled_java_file_path)
                    break  # Break as we found the class file

    return method_definition


def search_java_file_in_directory(directory, package_name, class_name):
    package_path = package_name.replace('.', '/')
    expected_file_name_java = f"{class_name}.java"
    expected_file_name_class = f"{class_name}.class"

    method_definition = ''
    # imports = []

    for root, dirs, files in os.walk(directory):
        for file in files:
            if file == expected_file_name_java or file == expected_file_name_class:
                file_path = os.path.join(root, file)
                decompiled_java_file_path = os.path.join(root, package_path, expected_file_name_java)

                if file.endswith('.class') and not os.path.exists(decompiled_java_file_path):

                    decompile_class_file(file_path, root)

                if os.path.exists(decompiled_java_file_path):
                    with open(decompiled_java_file_path, 'r', encoding='utf-8') as file:
                        return file.read()
                    # imports = find_imports_in_java_file(decompiled_java_file_path)
                    # break  # Break as we found the class file

    return method_definition

def find_method_in_java_file(java_file_path, method_name):
    with open(java_file_path, 'r', encoding='utf-8') as file:
        contents = file.read()

        # method_start_regex = rf"\b{method_name}\s*\([^\)]*\)\s*{{"
        method_start_regex = rf"\b{method_name}\s*\(\s*[^\)]*\s*\)\s*throws\s*\w+\s*{{"
        start_matches = re.finditer(method_start_regex, contents, re.DOTALL)

        for start_match in start_matches:
            start_index = start_match.end()
            open_brackets = 1
            for i in range(start_index, len(contents)):
                if contents[i] == '{':
                    open_brackets += 1
                elif contents[i] == '}':
                    open_brackets -= 1
                if open_brackets == 0 or i == len(contents) - 1:
                    method_definition = contents[start_match.start():i + 1].strip()
                    print(f"Found in {java_file_path}:")
                    # print(method_definition)
                    return method_definition
    return ''

def extract_and_delete_jars(directory):

    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".jar"):
                jar_path = os.path.join(root, file)

                extract_dir = os.path.splitext(jar_path)[0]
                os.makedirs(extract_dir, exist_ok=True)

                with zipfile.ZipFile(jar_path, 'r') as zip_ref:
                    zip_ref.extractall(extract_dir)

                os.remove(jar_path)

def decompile_class_file(class_file_path, output_directory):
    
    java_file = os.path.join(output_directory, os.path.basename(class_file_path) + ".java")
    subprocess.run(["D:/java/JDK11/bin/java.exe", "-jar", "E:/python_project/pythonProject/tools/cfr-0.152.jar", class_file_path, "--outputdir", output_directory])
    return java_file

# print(search_methods_in_directory('./novelplus','com.java2nb.system.controller','DataPermController','list'))