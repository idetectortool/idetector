package idetector.dal.neo4j.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import idetector.config.GlobalConfiguration;
import idetector.dal.neo4j.repository.MethodRefRepository;
import idetector.util.FileUtils;


@Slf4j
@Service
public class MethodService {

    @Autowired
    private MethodRefRepository methodRefRepository;

    public void importMethodRef(){
        if(FileUtils.fileExists(GlobalConfiguration.METHODS_CACHE_PATH)){
            methodRefRepository.loadMethodRefFromCSV(
                    FileUtils.getWinPath(GlobalConfiguration.METHODS_CACHE_PATH));
        }
    }

    public MethodRefRepository getRepository(){
        return methodRefRepository;
    }
}
