package idetector.core.scanner;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import idetector.config.GlobalConfiguration;
import idetector.core.container.ChainContainer;
import idetector.core.container.DataContainer;
import idetector.dal.caching.bean.edge.Call;
import idetector.dal.caching.bean.ref.MethodReference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Data
@Slf4j
@Component
public class ResultOutScanner {

    @Autowired
    private DataContainer dataContainer;

    @Autowired
    private ChainContainer chainContainer;


    public void save(FileWriter fw) throws IOException {
        log.info("Save remained data to graphdb. START!");

        log.info("Save remained data to graphdb. DONE!");


        chainContainer.printResults();
        chainContainer.saveResults(fw);
        chainContainer.printSan();
        chainContainer.SaveSan();
    }

}
