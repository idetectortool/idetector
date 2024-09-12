import openai
from openai import OpenAI
import json
import re
import findCode
import subprocess


openai.api_key ='your-key'

client = OpenAI(api_key=openai.api_key,base_url='https://opeiai-address')


content_system = '''{
	"rule_for_chatgpt":"You are a professional code auditor, I will provide you with a Java class and method. You need to help me determine whether the functionality of this method involves security filtering for sql injection or security filtering for filename or suffixes of file download or security filtering for filename or suffixes of file upload. Please strictly follow the audit_rule below for analysis.",
	"sql_filter_audit_rule1":"The sql injection filtering generally checks for the presence of sensitive keywords such as 'select','and','or','&','|' or quotes through black and white lists or regularity. Give you an example 'public static String                  SQL_PATTERN = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|((extractvalue|updatexml|if|mid|database)([\\s]*?)\\()|"+ "(\\b(select|update|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute|case when|sleep|union|load_file)\\b)"; public static boolean isValidOrderBySql(String value) {Pattern sqlPattern = Pattern.compile(SQL_PATTERN, Pattern.CASE_INSENSITIVE);if (sqlPattern.matcher(value.toLowerCase()).find()) {return true;}return false;}'",
	"sql_filter_audit_rule2":"For SQL injection filtering functions, you need to determine whether the function is a general SQL injection filtering function for this project (It does not include business logic operations, only keyword checking and filtering. Other business functions can directly call this function for filtering operations.), rather than returning true simply because it contains some validation operations.",
	"sql_filter_audit_rule3":"Functions specifically used for security filtering usually have highly targeted names, such as 'sqlfilter', etc.",
	"sql_filter_audit_rule4":"If this function includes business logic operations, it is most likely not an SQL injection filtering function.",
	"sql_filter_audit_rule5":"I need to confirm whether this method is related to SQL injection filtering, not whether it poses an SQL injection risk.",
	"path_vulnerability_audit_rule1":"You must carefully confirm that functions related to file operation security filtering must check the filename suffix using blacklist or whitelist or check the presence of path traversal special strings like '../'. Give you an example '@GetMapping("common/download")public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request){String realFileName = System.currentTimeMillis()+fileName.substring(fileName.indexOf("_")+1);try{//防止跨目录访问漏洞if(fileName.contains("..;/")||fileName.contains("../")){throw new BusinessException("不允许跨目录访问");}String filePath = LuckyFrameConfig.getDownloadPath() + fileName;response.setCharacterEncoding("utf-8");response.setContentType("multipart/form-data");response.setHeader("Content-Disposition","attachment;fileName="+setFileDownloadHeader(request, realFileName));FileUtils.writeBytes(filePath, response.getOutputStream());if (delete){FileUtils.deleteFile(filePath);}}catch (Exception e){log.error("", e);}}'",
	"path_vulnerability_audit_rule2":"For file operation filtering, it is most likely related to file upload, download, import, or export. There is no need to focus on business validations within other business functions.",
	"path_vulnerability_audit_rule3":"Do not return true simply because the function involves file upload or download operations. I need to confirm that the function includes filtering operations!!!",
	"audit_rule1":"Please exclude certain validations that are clearly unrelated to SQL injection and file operations, such as verifying user phone numbers, usernames, and similar operations.",
	"audit_rule2":"Please review only the filtering functions related to SQL injection and file operations. Other types of filtering functions are not within the scope of consideration.",
	"output":"Return the results to me in JSON format. If the method is security filtered, just respond with '{"isfilter": true,"reason": "xxxxx"}',else return '{"isfilter": false}'#Other than that, don't tell me anything!"
}

output_format if as below
{
  "isfilter": true,
  "type": "sql or file",
  "reason":"give me the reason why the function is about security filtering?"
}

{
  "isfilter": false
}
'''


checker_system='''{
    "rule_for_checker":"Now, you are a professional vulnerability discovery expert. I will provide you with the previous ChatGPT role setting rules, my questions, and ChatGPT's answers. You need to help me verify the correctness of ChatGPT's judgment on filtering functions, following these audit rules.",
    "audit_rule1":"A statement like if (dirName != null && !"".equals(dirName.trim())) that simply checks if a string is empty cannot be considered a filtering operation.",
    "audit_rule2":"User permission checks cannot be considered filtering operations, such as verifying if a user is an administrator or has a logged-in status",
    "audit_rule3":"A function that contains an obvious path traversal filter for patterns like ../ or .. in its content can be considered a filtering function.",
    "audit_rule4":"A function that contains obvious filters for SQL-sensitive keywords such as and, or, update, ', and " in its content can be considered an SQL injection filtering function.",
    "audit_rule5":"Some filtering functions may only check certain blacklists or whitelists and then return false or true. This is because some filtering functions are called by other functions, and the next steps in the function logic depend on the content of these filtering functions. e.g. check file suffixes  public static boolean isCsv(MultipartFile file) {return file.getOriginalFilename().toLowerCase().endsWith(FileTypeEnum.CSV.getFormat());}",
    "audit_rule6":"Some file upload operations can also be considered filtering functions if the function body explicitly checks file extensions or detects operations like ../. For example:  // Check if the file extension is valid// isContains(extName);if (StringUtils.isNotEmpty(uploadCommonVo.getExtStr())) {// Split the file extensionsList<String> extensionList = CrmebUtil.stringToArrayStr(uploadCommonVo.getExtStr());if (extensionList.size() > 0) {// Check if the extension is allowedif (!extensionList.contains(extName)) {throw new CrmebException("The type of the uploaded file can only be: " + uploadCommonVo.getExtStr());}} else {throw new CrmebException("The type of the uploaded file can only be: " + uploadCommonVo.getExtStr());}} Annther exampele if("data:image/jpeg;".equalsIgnoreCase(dataPrix)){//data:image/jpeg;base64,base64编码的jpeg图片数据 else{outputObject.setreturnMessage("文件类型不正确，只允许上传jpg格式的图片");}"
    "output":"Return the results to me in JSON format.
    
    output_format if as below
    {
      "istrue": true
    }

    {
      "istrue": false,
      "reason":"give me the reason why ChatGPT's previous answer was incorrect."
    }

}
'''


def parse_methods(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        lines = file.readlines()

    method_pattern = re.compile(
        r'<(?P<class_name>[\w\.]+):\s*(?P<return_type>[\w\.\[\]]+)\s+(?P<method_name>\w+)\(.*\)>')
    methods = []

    for line in lines:
        match = method_pattern.search(line)
        if match:
            class_name = match.group('class_name')
            method_name = match.group('method_name')
            methods.append((class_name, method_name))

    return methods


def getmethodinfo():
    file_path = 'rules/tempSanitization.json'
    methods = parse_methods(file_path)
    return methods



def add_rule_to_dict(class_name, method_name, is_filter):
    if not is_filter:
        return
    for rule in ruledict:
        if rule["name"] == class_name:
            rule["rules"].append({
                "function": method_name,
                "type": "sanitization",
                "vul": "",
                "actions": {},
                "polluted": [],
                "signatures": []
            })
            return

    ruledict.append({
        "name": class_name,
        "rules": [{
            "function": method_name,
            "type": "sanitization",
            "vul": "",
            "actions": {},
            "polluted": [],
            "signatures": []
        }]
    })


command = ['D:\\java\\8u202\\bin\\java.exe', '-jar', 'idetector-1.0.0.RELEASE.jar', './jars', '--checkFatJar', '--vv', '--isJDKProcess']

subprocess.run(command)


project_dir="./davinciold"
ruledict=[]
filesan = open('rules/{}san.txt'.format(project_dir), 'w')
for class_name, method_name in getmethodinfo():
    parts = class_name.rsplit('.', 1)
    content_user = findCode.search_java_file_in_directory(project_dir,parts[0],parts[1]) + "\n check the {}".format(method_name) +" in Class {}".format(class_name)
    # print(content_user)
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "system", "content": content_system}, {"role": "user", "content": content_user}],
        temperature=0.7,
        # max_tokens=150,
        top_p=1,
        frequency_penalty=0.0,
        presence_penalty=0.0
    )


    respoonse_data = response.choices[0].message.content
    new_respoonse_data = respoonse_data.replace('```json', '').replace('```', '')
    try:
        data = json.loads(new_respoonse_data)
    except:
        print("Json Error!")
        print(respoonse_data)
        filesan.write('Json Error!\n')
        filesan.write(respoonse_data+'\n')
        continue
    print(new_respoonse_data)
    filesan.write(new_respoonse_data+'\n')
    if(data["isfilter"]):
        print("========checker=======")
        filesan.write("========checker=======" + '\n')
        content_user = content_user + str(data)
        response_check = client.chat.completions.create(
                model="gpt-4o",
                messages=[{"role": "system", "content": checker_system}, {"role": "user", "content": content_user}],
                temperature=0.7,
                # max_tokens=150,
                top_p=1,
                frequency_penalty=0.0,
                presence_penalty=0.0
        )
        respoonse_data_checker = response_check.choices[0].message.content
        new_respoonse_data_checker = respoonse_data_checker.replace('```json', '').replace('```', '')
        try:
                data = json.loads(new_respoonse_data_checker)
        except:
                print("Json Error!")
                print(respoonse_data_checker)
                filesan.write('Json Error!\n')
                filesan.write(respoonse_data_checker + '\n')
                continue
        print(new_respoonse_data_checker)
        filesan.write(new_respoonse_data_checker + '\n')
        add_rule_to_dict(class_name, method_name, data["istrue"])

filesan.close()


print(ruledict)
with open('rules/sanitization.json', 'w', encoding='utf-8') as file:
    json.dump(ruledict, file, ensure_ascii=False, indent=4)

subprocess.run(command)