package idetector.core.container;

import idetector.core.data.idetectorRule;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import idetector.core.data.Chain;
import idetector.core.data.ChainBlock;
import idetector.core.data.Context;
import idetector.dal.caching.bean.ref.MethodReference;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Data
@Component
public class ChainContainer {

    @Autowired
    private DataContainer dataContainer;
    @Autowired
    private RulesContainer rulesContainer;
    private Map<String, Set<Chain>> chainMap = new HashMap<>();
    private Map<String, Set<Chain>> TruechainMap = new HashMap<>();
    private Set<String> sanitizationSet = new HashSet<>();

    public void addSan(String MethodSig){
        sanitizationSet.add(MethodSig);
    }

    public void addChain(Context context, String curMethodSign, List<Integer> curPollutedLocation) {
        Chain chain = new Chain();
        List<String> simpleChain = new ArrayList<>();
        ChainBlock chainBlock = new ChainBlock(curMethodSign, curPollutedLocation);
        simpleChain.add(curMethodSign);
        chain.getChain().add(chainBlock);
        Boolean  flag = false;
        while (context != null) {
            if (context.isAlreadySan()){
                flag = true;
            }
            String methodSignature = context.getMethodSignature();
            MethodReference methodReference = dataContainer.getMethodRefBySignature(methodSignature);
            methodReference.getRelatedChains().add(chain.getId());
            List<Integer> pollutedLocation = context.getPollutedArgs();
            chainBlock = new ChainBlock(methodSignature, pollutedLocation);
            simpleChain.add(methodSignature);
            chain.getChain().add(chainBlock);
            context = context.getPreContext();
        }
        Set<Chain> chains = chainMap.getOrDefault(curMethodSign, new HashSet<>());
        Set<Chain> chains1 = TruechainMap.getOrDefault(curMethodSign, new HashSet<>());
//            chains.add(chain);
//            chainMap.put(curMethodSign, chains);
////            log.debug("Find sink method: {}", key);
////            log.debug("Add vul call chain: {}", simpleChain);

//
        if (!chains.contains(chain)) {
            chains.add(chain);
            chainMap.put(curMethodSign, chains);
//            log.debug("Find sink method: {}", key);
//            log.debug("Add vul call chain: {}", simpleChain);
        }

        if (!chains1.contains(chain) && !flag){
            chains1.add(chain);
            TruechainMap.put(curMethodSign,chains1);
        }
    }


    public void printResults() {
        int count1 = 0;
        Map<String, idetectorRule> rules = rulesContainer.getRules();
        Map<String,String> sanitizationSet = new HashMap<>();
        for (Map.Entry<String, idetectorRule> entry : rules.entrySet()) {
            String key = entry.getKey();
            idetectorRule value = entry.getValue();
            for (idetectorRule.Rule rule: value.getRules()){
                if (rule.getType().equals("sanitization")){
                    sanitizationSet.put(value.getName(),rule.getFunction());
                }
            }
            // 在这里处理 key 和 value
//            System.out.println("Key: " + key + ", Value: " + value);
        }
        Set<String> maybeguolv = new HashSet<>();
        Map<String, Set<String>> savedCallee2Callers = dataContainer.getSavedCallee2Callers();
        for (Map.Entry<String, Set<String>> entry : savedCallee2Callers.entrySet()) {
            String callee = entry.getKey(); // 获取 Map 的键（被调用者）
            Set<String> callers = entry.getValue(); // 获取 Map 的值（调用者集合）

//            System.out.println("Callee: " + callee);
            for (Map.Entry<String, String> san : sanitizationSet.entrySet()) {
                if (callee.contains(san.getKey())&&callee.contains(san.getValue())){

                    for (String caller : callers) {
                        maybeguolv.add(caller);
                    }
                }
            }
        }
        for(Map.Entry<String, Set<Chain>>entry: chainMap.entrySet()) {
            log.debug("========================================");
            log.debug("[{}] Found sink method:", ++count1);
            log.debug(" {}", entry.getKey());
            int count2 = 0;
            Boolean guolvFlag = false;
            for(Chain chain: entry.getValue()) {
                log.debug("({}) Found vul call chain:", ++count2);
                List<ChainBlock> chainBlocks = chain.getChain();
                Collections.reverse(chainBlocks);
                for(Integer i = 0; i < chainBlocks.size(); i++) {
                    log.debug("{}{}", StringUtils.repeat(" ", i+1), chainBlocks.get(i).getMethodSignature());
                    for (String maysan :maybeguolv){
                        if (maysan.equals(chainBlocks.get(i).getMethodSignature())){
                            guolvFlag=true;
                        }
                    }
                }
//                dataContainer.getSavedCallerByCallee()
                if (guolvFlag){
                    log.debug("This vul call chain contains filtering operations!!!");
                }
            }

        }
    }

    public void printSan() {
        for (String MethodSig:sanitizationSet){
            if (!MethodSig.startsWith("<sun.")){
                System.out.println(MethodSig);
            }
        }
    }

    public void SaveSan() throws IOException {
        try {
            FileWriter fw = new FileWriter(String.join(File.separator, System.getProperty("user.dir"), "rules", "tempSanitization.json"));
            for (String MethodSig:sanitizationSet){
                if (!MethodSig.startsWith("<sun.")){
                    fw.write(MethodSig+"\n");
                }
            }
            fw.close();
        } catch (Exception e){
            throw e;
        }

    }




    @SneakyThrows
    public void saveResults(FileWriter fw) {

        int count1 = 0;
        Map<String, idetectorRule> rules = rulesContainer.getRules();
        Map<String,String> sanitizationSet = new HashMap<>();
        for (Map.Entry<String, idetectorRule> entry : rules.entrySet()) {
            String key = entry.getKey();
            idetectorRule value = entry.getValue();
            for (idetectorRule.Rule rule: value.getRules()){
                if (rule.getType().equals("sanitization")){
                    sanitizationSet.put(value.getName(),rule.getFunction());
                }
            }
            // 在这里处理 key 和 value
//            System.out.println("Key: " + key + ", Value: " + value);
        }
        Set<String> maybeguolv = new HashSet<>();
        Map<String, Set<String>> savedCallee2Callers = dataContainer.getSavedCallee2Callers();
        for (Map.Entry<String, Set<String>> entry : savedCallee2Callers.entrySet()) {
            String callee = entry.getKey(); // 获取 Map 的键（被调用者）
            Set<String> callers = entry.getValue(); // 获取 Map 的值（调用者集合）

//            System.out.println("Callee: " + callee);
            for (Map.Entry<String, String> san : sanitizationSet.entrySet()) {
                if (callee.contains(san.getKey())&&callee.contains(san.getValue())){

                    for (String caller : callers) {
                        maybeguolv.add(caller);
                    }
                }
            }
        }
        for(Map.Entry<String, Set<Chain>>entry: TruechainMap.entrySet()) {
            Boolean guolvFlag = false;
            fw.write("========================================\n");
            fw.write(String.format("[%d] Found sink method:\n", ++count1));
            fw.write(String.format(" %s\n", entry.getKey()));
            int count2 = 0;
            for(Chain chain: entry.getValue()) {
                fw.write(String.format("(%d) Found vul call chain:\n", ++count2));
                List<ChainBlock> chainBlocks = chain.getChain();
                for(Integer i = 0; i < chainBlocks.size(); i++) {
                    fw.write(String.format("%s%s\n", StringUtils.repeat(" ", i+1), chainBlocks.get(i).getMethodSignature()));
                    for (String maysan :maybeguolv){
                        if (maysan.equals(chainBlocks.get(i).getMethodSignature())){
                            guolvFlag=true;
                        }
                    }
                }
                if (guolvFlag){
                    fw.write("This vul call chain contains filtering operations!!!\n");
//                    log.debug("This vul call chain contains filtering operations!!!");
                }
            }
        }
    }

    public Boolean containBlock(String methodSignature, List<Integer> pollutedLocation) {
        for (Set<Chain>chains: chainMap.values()) {
            for (Chain chain: chains) {
                for (ChainBlock chainBlock: chain.getChain()) {
                    if (chainBlock.getMethodSignature().equals(methodSignature)
                            && chainBlock.getPollutedLocation().equals(pollutedLocation)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
