package idetector.dal.caching.bean.ref;

import com.google.common.hash.Hashing;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import soot.SootClass;
import soot.SootField;
import idetector.config.GlobalConfiguration;
import idetector.dal.caching.bean.edge.Extend;
import idetector.dal.caching.bean.edge.Has;
import idetector.dal.caching.bean.edge.Interfaces;
import idetector.dal.caching.converter.List2JsonStringConverter;
import idetector.dal.caching.converter.Set2JsonStringConverter;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Data
@Entity
@Table(name = "classes")
public class ClassReference {

    @Id
    private String id;
//    @Column(unique = true)
    private String name;
    private String superClass;

    private boolean isPhantom = false;
    private boolean isInterface = false;
    private boolean hasSuperClass = false;
    private boolean hasInterfaces = false;
    private boolean isInitialed = false;
    private boolean isSerializable = false;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = Set2JsonStringConverter.class)
    private Set<String> fields = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = List2JsonStringConverter.class)
    private List<String> interfaces = new ArrayList<>();

    // neo4j relationships
    /**
     */
    @Transient
    private transient Extend extendEdge = null;

    /**
     */
    @Transient
    private transient List<Has> hasEdge = new ArrayList<>();

    /**
     */
    @Transient
    private transient Set<Interfaces> interfaceEdge = new HashSet<>();


    public static ClassReference newInstance(String name){
        ClassReference classRef = new ClassReference();
        String id = Hashing.sha256()
                .hashString(name, StandardCharsets.UTF_8)
                .toString();
        classRef.setId(id);
        classRef.setName(name);
        classRef.setInterfaces(new ArrayList<>());
        classRef.setFields(new HashSet<>());
        return classRef;
    }


    public static ClassReference newInstance(SootClass cls){
        ClassReference classRef = newInstance(cls.getName());
        classRef.setInterface(cls.isInterface());


        if(cls.getFieldCount() > 0){
            for (SootField field : cls.getFields()) {
                List<String> fieldInfo = new ArrayList<>();
                fieldInfo.add(field.getName());
                fieldInfo.add(field.getModifiers() + "");
                fieldInfo.add(field.getType().toString());
                classRef.getFields().add(GlobalConfiguration.GSON.toJson(fieldInfo));
            }
        }

        if(cls.hasSuperclass() && !cls.getSuperclass().getName().equals("java.lang.Object")){

            classRef.setHasSuperClass(cls.hasSuperclass());
            classRef.setSuperClass(cls.getSuperclass().getName());
        }

        if(cls.getInterfaceCount() > 0){
            classRef.setHasInterfaces(true);
            for (SootClass intface : cls.getInterfaces()) {
                classRef.getInterfaces().add(intface.getName());
            }
        }
        return classRef;
    }

    public void setName(String name){
        // fix name too long error
        if(name.length() >= 255){
            this.name = name.substring(0, 254);
        }else{
            this.name = name;
        }
    }


}
