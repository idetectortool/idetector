package idetector.dal.caching.bean.edge;

import lombok.Getter;
import lombok.Setter;
import idetector.dal.caching.bean.ref.ClassReference;
import idetector.dal.caching.converter.ClassRef2StringConverter;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "Extend")
public class Extend {

    @Id
    private String id;

    @Convert(converter = ClassRef2StringConverter.class)
    private ClassReference source;

    @Convert(converter = ClassRef2StringConverter.class)
    private ClassReference target;

    public static Extend newInstance(ClassReference source, ClassReference target){
        Extend extend = new Extend();
        extend.setId(UUID.randomUUID().toString());
        extend.setSource(source);
        extend.setTarget(target);
        return extend;
    }

}
