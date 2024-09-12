package idetector.dal.caching.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import idetector.dal.caching.bean.ref.ClassReference;
import idetector.dal.caching.repository.ClassRepository;
import idetector.config.GlobalConfiguration;

import java.util.List;



@Slf4j
@Service
public class ClassRefService {

    @Autowired
    private ClassRepository classRepository;

    public ClassReference getClassRefByName(String name){
        return classRepository.findClassReferenceByName(name);
    }

    public void save(ClassReference ref){
        classRepository.save(ref);
    }

    public void save(Iterable<ClassReference> refs){
        classRepository.saveAll(refs);
    }

    public void save2Csv(){
        classRepository.save2Csv(GlobalConfiguration.CLASSES_CACHE_PATH);
    }

    public List<ClassReference> loadNecessaryClassRefs(){
        return classRepository.findAllNecessaryClassRefs();
    }

    public int countAll(){
        return classRepository.countAll();
    }

}
