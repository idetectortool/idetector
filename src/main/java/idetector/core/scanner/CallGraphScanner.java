package idetector.core.scanner;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.internal.value.ListValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import idetector.core.collector.CallGraphCollector;
import idetector.core.container.ChainContainer;
import idetector.core.container.DataContainer;
import idetector.core.data.WorklistItem;
import idetector.dal.caching.bean.edge.Call;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.dal.caching.service.MethodRefService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Data
@Slf4j
@Component
public class CallGraphScanner {

    @Autowired
    private MethodRefService methodRefService;
    @Autowired
    private DataContainer dataContainer;
    @Autowired
    private ChainContainer chainContainer;
    @Autowired
    private CallGraphCollector collector;

    @Autowired
    private Executor callCollector;


    private static int total;
    private static int split;
    private static int current;

    public void run() {
        collect();
    }

    public void collect() {
        log.info("Build call graph. START!");
        total = 0;
        while (!dataContainer.worklistIsEmpty()){
            total = total + 1;
            WorklistItem worklistItem = dataContainer.getOneFormWorkList();
            collector.collect(worklistItem, dataContainer, chainContainer);
        }
        log.info("Status: 100%, total: {}", (total));
        log.info("Build call graph. DONE!");
    }
}