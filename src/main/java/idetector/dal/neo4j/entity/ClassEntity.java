package idetector.dal.neo4j.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;



@Node("Class")
public class ClassEntity {
    @Id
    private String id;
}
