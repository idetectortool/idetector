package idetector.dal.caching.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import idetector.dal.caching.bean.edge.Alias;
import idetector.dal.caching.bean.ref.ClassReference;
import idetector.dal.caching.bean.ref.MethodReference;

import java.util.List;


public interface AliasEdgeRepository extends CrudRepository<Alias, String> {

    @Query(value = "CALL CSVWRITE(:path, 'SELECT * FROM ALIAS')", nativeQuery=true)
    void save2Csv(@Param("path") String path);

    @Query(value = "select count(*) from ALIAS", nativeQuery=true)
    int countAll();

    @Query(value = "select source from ALIAS where target = :methodRef", nativeQuery = true)
    List<String> findMethodReferenceFormALIASByTarget(MethodReference methodRef);
}