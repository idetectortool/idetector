package idetector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import idetector.config.GlobalConfiguration;
import idetector.core.Analyser;
import idetector.exception.JDKVersionErrorException;
import idetector.util.FileUtils;
import idetector.util.JavaVersion;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootApplication
@EntityScan({"idetector.dal.caching.bean","idetector.dal.neo4j.entity"})
@EnableNeo4jRepositories("idetector.dal.neo4j.repository")
public class App {

    private String target = null;

    private boolean isJDKProcess = false;
    private boolean withAllJDK = false;
    private boolean isSaveOnly = false;
    private boolean excludeJDK = false;
    private boolean isJDKOnly = false;
    private boolean checkFatJar = false;
    private boolean isOutPut = false;

    private List<String> includeLib = new ArrayList<>();

    private boolean isExtractLib = false;

    @Autowired
    private Analyser analyser;

    @Resource
    private ApplicationArguments arguments;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args).close();
    }

    private void applyOptions() {
        if (arguments.containsOption("isJDKProcess")) {
            isJDKProcess = true;
        }
        if (arguments.containsOption("withAllJDK") || arguments.containsOption("isJDKOnly")) {
            withAllJDK = true;
        }
        if (arguments.containsOption("vv")) {
            GlobalConfiguration.DEBUG = true;
        }
        if (arguments.containsOption("isSaveOnly")) {
            isSaveOnly = true;
        }
        if (arguments.containsOption("excludeJDK")) {
            excludeJDK = true;
        }
        if (arguments.containsOption("isJDKOnly")) {
            isJDKOnly = true;
        }
        if (arguments.containsOption("checkFatJar")){
            checkFatJar = true;
        }
        if (arguments.containsOption("isOutPut")){
            isOutPut = true;
        }
        if (arguments.containsOption("includeLib") && arguments.getOptionValues("includeLib").size() > 0) {
            includeLib = Arrays.asList(arguments.getOptionValues("includeLib").get(0).split(","));
        }if (arguments.containsOption("isExtractLib")) {
            isExtractLib = true;
        }
        if(arguments.getNonOptionArgs().size() == 1){
            target = arguments.getNonOptionArgs().get(0);

            if(!FileUtils.fileExists(target)){
                target = String.join(File.separator, System.getProperty("user.dir"), target);
                if(!FileUtils.fileExists(target)){
                    throw new IllegalArgumentException("target not exists!");
                }
            }
        }
        // check options
        if(isJDKOnly || isSaveOnly || isOutPut){
            // only process JDK dependencies
            // only save caches
        }else if(target != null){
            // process target JAR/WAR/CLASS
        }else{
            throw new IllegalArgumentException("Options Illegal!");
        }
    }

    @Bean
    CommandLineRunner run(){
        return args -> {
            try{
                if(!JavaVersion.isJDK8()){
                    throw new JDKVersionErrorException("Error JDK version. Please using JDK8.");
                }
                applyOptions();
                analyser.run(target, isJDKProcess, withAllJDK, isSaveOnly, excludeJDK, isJDKOnly, checkFatJar, isOutPut, includeLib, isExtractLib);
            }catch (IllegalArgumentException e){
                log.error(e.getMessage() +
                        "\nPlease use java -jar idetector target_directory [--checkFatJar|--isSaveOnly|--isOutPut] !" +
                        "\ntarget_directory 为相对路径" +
                        "\n--excludeJDK出现时，不添加当前运行jre环境" +
                        "\n--isJDKProcess出现时，将处理当前运行jre环境的分析" +
                        "\nExample: java -Xmx10g -jar idetector-sql.jar ./jars --checkFatJar");
            }catch (JDKVersionErrorException e){
                log.error(e.getMessage());
            }

        };
    }
}
