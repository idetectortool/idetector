import openai
from openai import OpenAI
import json
import re
import findCode
import subprocess


openai.api_key = 's'

client = OpenAI(api_key=openai.api_key, base_url='https://openai')


prior_knowledge = '''
    Now, you are a professional code auditor, and your goal is to determine whether the function I provide is a sanitizer. Specifically, you need to assess whether this sanitizer is related to SQL injection or file operation sanitization. I will provide you with some examples of sanitizers, and your task is to learn from them to understand the sanitization operations.
    SQL injection filtering is generally performed through whitelisting/blacklisting or pattern-based checks for the presence of sensitive keywords such as "select," "and," "or," "&," "|," or quotation marks. Below are examples of SQL injection sanitization functions that I will provide to you:
    /**
         * @description sql injection filter
    */
    protected static boolean sqlValidate(String str){
            // Harmonization to lower case
            String s = str.toLowerCase();
            // Filter out the sql keywords, special characters need to add \\ in front of the escape
            String badStr =
                    "select|update|and|or|delete|insert|truncate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute|table|"+
                            "char|declare|sitename|xp_cmdshell|like|from|grant|use|group_concat|column_name|" +
                            "information_schema.columns|table_schema|union|where|order|by|" +
                            "'\\*|\\;|\\-|\\--|\\+|\\,|\\//|\\/|\\%|\\#";
            // Matching using regular expressions
            boolean matches = s.matches(badStr);
            return matches;
    }
    
    Or use a regular to limit the input
    
    public static boolean check(String value) {
        return value.matches([a-zA-Z0-9_\\ \\,\\.]+);
    }
    
   File upload or file read sanitization operations may either be directly embedded within business logic functions or abstracted into a general-purpose function to validate filenames or common characters. Typically, the validation involves checking the file extension type and whether the filename contains dangerous characters like ../ or ..   Or it will check the suffix, such as determining whether the filename is png, function isPNG()
   The following is an example of a sanitization operation included in business logic. (if(!fileType.contains(ext)){ return responseErrorData(response,1,"File format error, upload failed.");}):
   @RequestMapping(value="/uploadvideo",method={RequestMethod.POST})
   	public String gok4(HttpServletRequest request,HttpServletResponse response,@RequestParam(value="uploadfile" ,required=true) MultipartFile uploadfile,
   			@RequestParam(value="param",required=false) String param,
   			@RequestParam(value="fileType",required=true) String fileType){
   		try{
   			String[] type = fileType.split(",");
   			//Setting the image type
   			setFileTypeList(type);
   			//Get the extension of the uploaded file type, first get the . position of the uploaded file, then intercept the file from the . and then intercept the next position from . to the end of the file, and finally get the extension.
   			String ext = FileUploadUtils.getSuffix(uploadfile.getOriginalFilename());
   			if(!fileType.contains(ext)){
   				return responseErrorData(response,1,"File format error, upload failed.");
   			}
   			//Get file path
   			String filePath = getPath(request,ext,param);
   			File file = new File(getProjectRootDirPath(request)+filePath);
   			//Save file
   			uploadfile.transferTo(file);
   			//Return data
   			return responseData(filePath,0,"上传成功",response);
   		}
   	}

    The following is a more general file upload sanitization function.:
     /**
         * Check Suffix
         * @param size long file size
         * @param suffixName String suffixName
     */
     private void isValidPic(long size, String suffixName, String type) {
            JSONObject config = getConfig();
            if (!config.containsKey(type)) {
                throw new CrmebException("This type is not supported");
            }
            String supportNameSuffix = config.getJSONObject(type).getString("suffix");
            List<String> suffixNameList = CrmebUtil.stringToArrayStr(supportNameSuffix);
            if (!suffixNameList.contains(suffixName)) {
                throw new CrmebException("The file format must be" + supportSize);
            }
     }
     
     note that: Some sanitization functions will mix sql and xss filtering together, and this counts as sanitization functions
'''

cot_think = '''
    Then, you can proceed step by step to determine whether the function I submit to you is a sanitization function.

    Step 1: If the function contains comments at the beginning, review the comments to get a general understanding of the function’s purpose.

    Step 2: Check the function name and classname. If the name is something obvious like sqlfilter or checkFileName or project.tools.filter.commonFilter, it is highly likely to be a sanitizer.

    Step 3: Review the function logic. This is the most crucial step. You need to apply prior knowledge to evaluate the content I provide.  

    Following this thought process, you can perform a step-by-step evaluation to determine whether the function is a sanitization function. I will provide examples next.
'''

content_system = '''{
	"rule_for_chatgpt":"You are a professional code auditor, I will provide you with a Java class and method. You need to think step by step to help me determine whether the functionality of this method involves security filtering for sql injection or security filtering for filename or suffixes of file download or security filtering for filename or suffixes of file upload. Please strictly follow the audit_rule below for analysis.",
    "sql_filter_audit_rule1":"The sql injection filtering generally checks for the presence of sensitive keywords such as 'select','and','or','&','|' or quotes through black and white lists or regularity.",
    "sql_filter_audit_rule2":"For SQL injection filtering functions, you need to determine whether the function is a general SQL injection filtering function for this project (It does not include business logic operations, only keyword checking and filtering. Other business functions can directly call this function for filtering operations.), rather than returning true simply because it contains some validation operations.",
    "sql_filter_audit_rule3":"If this function includes business logic operations, it is most likely not an SQL injection filtering function.",
    "sql_filter_audit_rule4":"I need to confirm whether this method is related to SQL injection filtering, not whether it poses an SQL injection risk.",
    "path_vulnerability_audit_rule1":"You must carefully confirm that functions related to file operation security filtering must check the filename suffix using blacklist or whitelist or check the presence of path traversal special strings like '../'",
    "path_vulnerability_audit_rule2":"For file operation filtering, it is most likely related to file upload, download, import, or export. There is no need to focus on business validations within other business functions.",
    "path_vulnerability_audit_rule3":"Do not return true simply because the function involves file upload or download operations. I need to confirm that the function includes filtering operations.",
    "audit_rule1":"Please exclude certain validations that are clearly unrelated to SQL injection and file operations, such as verifying user phone numbers, usernames, and similar operations.",
    "audit_rule2":"Please review only the filtering functions related to SQL injection and file operations. Other types of filtering functions are not within the scope of consideration.",
    "audit_rule3":"Functions specifically used for security filtering usually have highly targeted method names or class names, such as 'sqlfilter', 'project.tools.filter.commonFilter', etc.",
    "audit_rule4":"You can utilize the function's comment information to assist in identifying sanitizers. For instance, a sanitizer that checks file suffixes might have a comment like /** Check File Suffix **/.",
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

checker_system = '''{
    "rule_for_checker":"Now, you are a professional code auditor. I will provide you with the previous ChatGPT role setting rules, my questions, and ChatGPT's answers. You need to think step by step to help me verify the correctness of ChatGPT's judgment on sanitizers, following these audit rules.",
    "audit_rule1":"A statement like if (dirName != null && !"".equals(dirName.trim())) that simply checks if a string is empty cannot be considered a filtering operation.",
    "audit_rule2":"User permission checks cannot be considered filtering operations, such as verifying if a user is an administrator or has a logged-in status",
    "audit_rule3":"A function that contains an obvious path traversal filter for patterns like ../ or .. in its content can be considered a filtering function.",
    "audit_rule4":"A function that contains obvious filters for SQL-sensitive keywords such as and, or, update, ', and " in its content can be considered an SQL injection filtering function. note that: Some sanitizers may mix sql and xss filtering together, and this counts as sanitizers",
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


command = ['D:\\java\\8u202\\bin\\java.exe', '-jar', 'idetector-1.0.0.RELEASE.jar', './jars', '--checkFatJar', '--vv',
           '--isJDKProcess']

# subprocess.run(command)


project_dir = "./solr"
ruledict = []
filesan = open('rules/{}san.txt'.format(project_dir), 'w')

for class_name, method_name in getmethodinfo():
    trueTime = 0
    falseTime = 0
    for chatRange in range(5):
        parts = class_name.rsplit('.', 1)
        filesan.write(class_name + '.' +method_name)
        print(class_name + '.' +method_name)
        content_user = findCode.search_java_file_in_directory(project_dir, parts[0], parts[1]) + "\n check the {}".format(
            method_name) + " in Class {}".format(class_name)
        # content_user = findCode.search_methods_in_directory(project_dir,parts[0], parts[1],method_name)+ "\n check the {}".format(
        #     method_name) + " in Class {}".format(class_name)

        # print(content_user)
        conversation_history = []
        conversation_history.append({"role": "system", "content": content_system})
        conversation_history.append({"role": "user", "content": prior_knowledge+cot_think})
        assistant_response = "Sure, feel free to provide the function you'd like me to evaluate, and I'll follow the outlined steps to determine if it's a sanitization function related to SQL injection or file operations."
        conversation_history.append({"role": "assistant", "content": assistant_response})

        conversation_history.append({"role": "user", "content": content_user})
        response = client.chat.completions.create(
            model="gpt-4o",
            messages=conversation_history,
            temperature=0.7,
            # max_tokens=150,
            top_p=1,
            frequency_penalty=0.0,
            presence_penalty=0.0
        )

        respoonse_data = response.choices[0].message.content
        conversation_history.append({"role": "assistant", "content": respoonse_data})
        new_respoonse_data = respoonse_data.replace('```json', '').replace('```', '').replace('\n','')
        try:
            data = json.loads(new_respoonse_data)
        except:
            try:
                restr = re.findall(r'```json\s*({.*?})\s*', respoonse_data, re.DOTALL)[0]
                data = json.loads(restr)
                print(restr)
            except:
                falseTime = falseTime + 1
                print("Json Error!")
                print(respoonse_data)
                filesan.write('Json Error!\n')
                filesan.write(respoonse_data + '\n')
                continue
        print(new_respoonse_data)
        filesan.write(new_respoonse_data + '\n')
        if (data["isfilter"]):

            print("========checker=======")
            filesan.write("========checker=======" + '\n')
            content_user = content_user + str(data)
            response_check = client.chat.completions.create(
                model="gpt-4o",
                messages=[{"role": "system", "content": checker_system}, {"role": "user", "content": str(
                    conversation_history)}],
                temperature=0.7,
                # max_tokens=150,
                top_p=1,
                frequency_penalty=0.0,
                presence_penalty=0.0
            )
            respoonse_data_checker = response_check.choices[0].message.content
            new_respoonse_data_checker = respoonse_data_checker.replace('```json', '').replace('```', '').replace('\n','')

            try:
                data = json.loads(new_respoonse_data_checker)
                if (data["istrue"]):
                    trueTime = trueTime + 1
                else:
                    falseTime = falseTime + 1
            except:
                try:
                    restr = re.findall(r'```json\s*({.*?})\s*', respoonse_data_checker, re.DOTALL)[0]
                    data = json.loads(restr)
                    print(restr)
                    if (data["istrue"]):
                        trueTime = trueTime + 1
                    else:
                        falseTime = falseTime + 1
                except:
                    falseTime = falseTime + 1
                    print("Json Error!")
                    print(new_respoonse_data_checker)
                    filesan.write('Json Error!\n')
                    filesan.write(new_respoonse_data_checker + '\n')
                    continue
            print(new_respoonse_data_checker)
            filesan.write(new_respoonse_data_checker + '\n')

        else:
            falseTime = falseTime + 1
    add_rule_to_dict(class_name, method_name, trueTime > falseTime)



filesan.close()

print(ruledict)
with open('rules/sanitization.json', 'w', encoding='utf-8') as file:
    json.dump(ruledict, file, ensure_ascii=False, indent=4)

subprocess.run(command)
