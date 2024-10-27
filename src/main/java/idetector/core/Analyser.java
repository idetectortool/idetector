package idetector.core;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.internal.value.ListValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import soot.CompilationDeathException;
import soot.Main;
import soot.Scene;
import soot.options.Options;
import idetector.config.GlobalConfiguration;
import idetector.config.SootConfiguration;
import idetector.core.container.DataContainer;
import idetector.core.container.RulesContainer;
import idetector.core.scanner.CallGraphScanner;
import idetector.core.scanner.ClassInfoScanner;
import idetector.core.scanner.DaoInfoScanner;
import idetector.core.scanner.ResultOutScanner;
import idetector.dal.neo4j.repository.MethodRefRepository;
import idetector.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static soot.SootClass.HIERARCHY;
import static idetector.util.FileUtils.findPackages;


@Slf4j
@Component
public class Analyser {

    @Autowired
    private DataContainer dataContainer;
    @Autowired
    private ClassInfoScanner classInfoScanner;
    @Autowired
    private DaoInfoScanner daoInfoScanner;
    @Autowired
    private CallGraphScanner callGraphScanner;
    @Autowired
    private RulesContainer rulesContainer;
    @Autowired
    private ResultOutScanner resultOutScanner;

    //test
    @Autowired
    private MethodRefRepository methodRefRepository;
    //test

    public void run(String target, boolean isJDKProcess,
                    boolean withAllJDK, boolean isSaveOnly,
                    boolean excludeJDK, boolean isJDKOnly, boolean checkFatJar, boolean isOutPut, List<String> includeLib, boolean isExtractLib) throws IOException {
        if(isSaveOnly){
            save();
            System.exit(0);
        }else if (isOutPut){
            outResult();
        }else{
            Map<String, String> jdkPaths = getJdkjdkPaths(withAllJDK);
            log.info("Get {} JDK jdkPaths", jdkPaths.size());
            Map<String, String> classPaths = excludeJDK?
                    new HashMap<>():new HashMap<>(jdkPaths);
            Map<String, String> targets = new HashMap<>();
            Map<String, String> daoPaths = new HashMap<>();
            Map<String,String> libPackMap = new HashMap<>();
            Map<String,String> jdkPackMap = new HashMap<>();
            if(isJDKOnly){
                targets.putAll(jdkPaths);
            }else{
                Map<String, String> jarPaths = new HashMap<>();

                FileUtils.getTargetDirectoryJarFiles(target, checkFatJar, jarPaths, daoPaths, libPackMap, isExtractLib);
                Map<String, String> newJarPaths = new HashMap<>();
                for (Map.Entry<String, String> entry: jarPaths.entrySet()) {

                    String entryName = entry.getValue().toLowerCase();
                    String fileName = new File(entry.getValue()).getName().toLowerCase();
                    if (entryName.contains("classes")
                            || entryName.contains("jdbc")
                            || entryName.contains("spring-test")
                            || entryName.contains("spring-core")
                            || entryName.contains("spring-web")
                            || entryName.contains("spring-context")
                            || entryName.contains("hibernate-jpa")
                            || entryName.contains("hibernate-core")
                            || entryName.contains("mybatis-plus-core")
                            || entryName.contains("persist4j")
                            || (entryName.endsWith(".jar") && !entryName.contains(File.separator+"lib"))
                            || (entryName.contains(File.separator+"lib") && includeLib.stream().anyMatch(fileName.toLowerCase()::contains)))
                        newJarPaths.put(entry.getKey(),entry.getValue());
                }
                classPaths.putAll(newJarPaths);
                targets.putAll(newJarPaths);
                if(isJDKProcess || withAllJDK){
                    targets.putAll(jdkPaths);
                    for(String path: jdkPaths.values()) {
                        findPackages(path, jdkPackMap);
                    }
                }
            }
            dataContainer.setLibPackMap(libPackMap);
            dataContainer.setJdkPackMap(jdkPackMap);
            dataContainer.setIncludeLib(includeLib);

            runSootAnalysis(targets, daoPaths, new ArrayList<>(classPaths.values()) );
        }
    }

    public void runSootAnalysis(Map<String, String> targets,Map<String, String> daoPaths, List<String> classPaths) throws IOException {
        try{
            SootConfiguration.initSootOption();
            FileWriter fw = new FileWriter(String.join(File.separator, System.getProperty("user.dir"), "results", String.format("%d.txt",System.currentTimeMillis())));

            long start = System.nanoTime();
            addBasicClasses();
            // set class paths
            Scene.v().setSootClassPath(String.join(File.pathSeparator, new HashSet<>(classPaths)));
            // get target filepath
            List<String> realTargets = getTargets(targets);
            if(realTargets.isEmpty()){
                log.info("Nothing to analysis!");
                return;
            }
            Main.v().autoSetOptions();


            daoInfoScanner.run(daoPaths);

            classInfoScanner.run(realTargets);

            callGraphScanner.run();

            resultOutScanner.save(fw);

            rulesContainer.saveStatus();

            log.info("Cost {} seconds", TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));

            fw.write(String.format("Cost %d seconds\n", TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)));
            fw.close();

        }catch (CompilationDeathException e){
            if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED) {
                throw e;
            }
        }
    }

    public List<String> getTargets(Map<String, String> targets){
        Set<String> stuff = new HashSet<>();
        List<String> newIgnore = new ArrayList<>();
        targets.forEach((filename, filepath) -> {
            if(!rulesContainer.isIgnore(filename)){
                stuff.add(filepath);
//                newIgnore.add(filename);
            }
        });
        rulesContainer.getIgnored().addAll(newIgnore);
        log.info("Total analyse {} targets.", stuff.size());
        Options.v().set_process_dir(new ArrayList<>(stuff));
        return new ArrayList<>(stuff);
    }

    public void addBasicClasses(){
        List<String> basicClasses = rulesContainer.getBasicClasses();
        for(String cls:basicClasses){
            Scene.v().addBasicClass(cls ,HIERARCHY);
        }
    }

    public void save(){
        log.info("Start to save cache.");
        long start = System.nanoTime();

        dataContainer.save2CSV();
        dataContainer.save2Neo4j();
        clean();
        log.info("Cost {} seconds"
                , TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));
    }

    /**
     * @return jre
     */
    public Map<String, String> getJdkjdkPaths(boolean all){
        String javaHome = System.getProperty("java.home");

        String[] jre;
        if(all){// 19个
            jre = new String[]{"../lib/dt.jar","../lib/sa-jdi.jar","../lib/tools.jar","../lib/jconsole.jar","lib/resources.jar","lib/rt.jar","lib/jsse.jar","lib/jce.jar","lib/charsets.jar","lib/ext/cldrdata.jar","lib/ext/dnsns.jar","lib/ext/jaccess.jar","lib/ext/localedata.jar","lib/ext/nashorn.jar","lib/ext/sunec.jar","lib/ext/sunjce_provider.jar","lib/ext/sunpkcs11.jar","lib/ext/zipfs.jar","lib/management-agent.jar"};
        }else{
            jre = new String[]{"lib/rt.jar","lib/jce.jar","lib/ext/nashorn.jar"};
        }
        Map<String, String> exists = new HashMap<>();
        for(String cp:jre){
            String path = String.join(File.separator, javaHome, cp);
            File file = new File(path);
            if(file.exists()){
                exists.put(FileUtils.getFileMD5(file), path);
            }
        }
        log.info("Load " +exists.size()+" jre jars.");
        return exists;
    }

    public void clean(){
        try {
            File cacheDir = new File(GlobalConfiguration.CACHE_PATH);
            File[] files = cacheDir.listFiles();
            if(files != null){
                for(File file: files){
                    if(file.getName().endsWith(".csv")){
                        Files.deleteIfExists(file.toPath());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void outResult(){
        log.info("Start to Output Result.");
        long start = System.nanoTime();

        // 路径输出
        File file = new File("./result.txt");
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write("");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<ListValue> sinkList = methodRefRepository.findALlSink();

        for (ListValue sinkNode : sinkList){
            List<ListValue> callStartNodeLists = methodRefRepository.findCallBySignature(sinkNode.get(0).asString());
            Deque<String> pathStack = new ArrayDeque<>();
            Deque<ListValue> fullInfoPathStack = new ArrayDeque<>();
            Deque<List<ListValue>> callStack = new ArrayDeque<>();
            Deque<String> discardNode = new ArrayDeque<>();

            pathStack.addLast(sinkNode.get(0).asString());
            fullInfoPathStack.addLast(sinkNode);
            callStack.addLast(callStartNodeLists);

            while (!pathStack.isEmpty()){

                if (pathStack.size() > 10){
                    pathStack.pollLast();
                    fullInfoPathStack.pollLast();
                    callStack.pollLast();

                    continue;
                }

                List<ListValue> callNodes = callStack.pollLast();
                assert callNodes != null;
                if (!callNodes.isEmpty()){
                    String firstNodeSignature = callNodes.get(0).get(0).asString();
                    ListValue tmpNode = callNodes.get(0);
                    if (!pathStack.contains(firstNodeSignature) && !discardNode.contains(firstNodeSignature)){
                        pathStack.addLast(firstNodeSignature);
                        fullInfoPathStack.addLast(tmpNode);
                        List<ListValue> callNodesTmp = methodRefRepository.findCallBySignature(firstNodeSignature);
                        callNodes.remove(0);
                        callStack.addLast(callNodes);
                        callStack.addLast(callNodesTmp);
                    }else {
                        callNodes.remove(0);
                        callStack.addLast(callNodes);
                        continue;
                    }
                }else {
                    String node = pathStack.pollLast();
                    discardNode.addLast(node);
                    fullInfoPathStack.pollLast();
                    continue;
                }
                if (!pathStack.isEmpty() && methodRefRepository.isSourceBySignature(pathStack.getLast()).get(0).asBoolean()){
                    try {
                        writer = new FileWriter(file,true);
                        writer.write(fullInfoPathStack + "\n");
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    pathStack.pollLast();
                    callStack.pollLast();
                    fullInfoPathStack.pollLast();
                }
            }
        }
        log.info("Cost {} seconds"
                , TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));
    }
}