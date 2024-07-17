package idetector.core.scanner;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import idetector.core.collector.DaoInfoCollector;

import java.util.Map;

@Data
@Slf4j
@Component
public class DaoInfoScanner {

    @Autowired
    private DaoInfoCollector daoInfoCollector;

    public void run(Map<String, String> paths){
        daoInfoCollector.collect(paths);
    }

}
