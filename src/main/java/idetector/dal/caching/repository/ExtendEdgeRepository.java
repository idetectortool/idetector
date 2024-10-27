package idetector.dal.caching.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import idetector.dal.caching.bean.edge.Extend;


public interface ExtendEdgeRepository extends CrudRepository<Extend, String> {

    @Query(value = "CALL CSVWRITE(:path, 'SELECT * FROM EXTEND')", nativeQuery=true)
    void save2Csv(@Param("path") String path);

    @Query(value = "select count(*) from EXTEND", nativeQuery=true)
    int countAll();
}
