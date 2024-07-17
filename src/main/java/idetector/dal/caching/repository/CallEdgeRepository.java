package idetector.dal.caching.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import idetector.dal.caching.bean.edge.Call;


public interface CallEdgeRepository extends CrudRepository<Call, String> {
    @Query(value = "CALL CSVWRITE(:path, 'SELECT * FROM CALL')", nativeQuery=true)
    void save2Csv(@Param("path") String path);

    @Query(value = "select count(*) from CALL", nativeQuery=true)
    int countAll();
}
