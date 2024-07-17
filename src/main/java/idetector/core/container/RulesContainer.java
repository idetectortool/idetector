package idetector.core.container;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import idetector.config.GlobalConfiguration;
import idetector.core.data.idetectorRule;
import idetector.util.FileUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Data
@Component
public class RulesContainer {

    private Map<String, idetectorRule> rules = new HashMap<>();
    private List<String> ignored;
    private List<String> excludedClasses;
    private List<String> basicClasses;

    public RulesContainer() throws FileNotFoundException {
        load();
        loadIgnore();
        loadBasicClasses();
    }

    public idetectorRule.Rule getRule(String classname, String method){
        if(rules.containsKey(classname)){
            idetectorRule rule = rules.get(classname);
            if(rule.contains(method)){
                return rule.getRule(method);
            }
        }
        return null;
    }

    public idetectorRule getRule(String classname){
        return rules.getOrDefault(classname, null);
    }

    public boolean isIgnore(String jar){
        return ignored.contains(jar);
    }

    public boolean isType(String classname, String method, String type){
        if(rules.containsKey(classname)){
            idetectorRule rule = rules.get(classname);
            if(rule.contains(method)){
                idetectorRule.Rule tr = rule.getRule(method);
                return type.equals(tr.getType());
            }
        }
        return false;
    }

    @SuppressWarnings({"unchecked"})
    private void load() throws FileNotFoundException {
        idetectorRule[] tempRules = (idetectorRule[]) FileUtils.getJsonContent(GlobalConfiguration.KNOWLEDGE_PATH, idetectorRule[].class);
        if(tempRules == null){
            throw new FileNotFoundException("Sink File Not Found");
        }
        for(idetectorRule rule:tempRules){
            rule.init();
            rules.put(rule.getName(), rule);
        }
        if (FileUtils.fileExists(GlobalConfiguration.SANITIZATION_PATH)){
            idetectorRule[] sanRules = (idetectorRule[]) FileUtils.getJsonContent(GlobalConfiguration.SANITIZATION_PATH, idetectorRule[].class);
//            assert sanRules != null;
            for(idetectorRule rule:sanRules){
                rules.put(rule.getName(), rule);
            }
        }


        log.info("load "+ rules.size() +" rules success!");
    }
    @SuppressWarnings({"unchecked"})
    private void loadIgnore(){
        ignored = (List<String>) FileUtils.getJsonContent(GlobalConfiguration.IGNORE_PATH, List.class);
        if(ignored == null){
            ignored = new ArrayList<>();
        }
    }
    @SuppressWarnings({"unchecked"})
    private void loadBasicClasses(){
        basicClasses = (List<String>) FileUtils.getJsonContent(GlobalConfiguration.BASIC_CLASSES_PATH, List.class);
        if(basicClasses == null){
            basicClasses = new ArrayList<>();
        }
    }

    @SuppressWarnings({"unchecked"})
    private void loadExcludedClasses(){
        excludedClasses = (List<String>) FileUtils.getJsonContent(GlobalConfiguration.EXCLUDED_CLASS_PATH, List.class);
        if(excludedClasses == null){
            excludedClasses = new ArrayList<>();
        }
    }

    public void saveStatus(){
        FileUtils.putJsonContent(GlobalConfiguration.IGNORE_PATH, ignored);
    }
}
