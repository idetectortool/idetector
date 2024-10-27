package idetector.dal.neo4j.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;


@Node("Method")
public class MethodEntity {

    @Id
    private String id;
}
