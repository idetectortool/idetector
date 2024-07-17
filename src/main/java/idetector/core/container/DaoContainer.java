package idetector.core.container;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Slf4j
@Data
@Component
public class DaoContainer {

    private Table<String, String, HashSet<String>> mappers;

    DaoContainer() {
        mappers = HashBasedTable.create();
    }

}

