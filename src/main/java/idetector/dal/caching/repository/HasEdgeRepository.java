package idetector.dal.caching.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import idetector.dal.caching.bean.edge.Has;


public interface HasEdgeRepository extends CrudRepository<Has, String> {

    @Query(value = "CALL CSVWRITE(:path, 'SELECT * FROM HAS')", nativeQuery=true)
    void save2Csv(@Param("path") String path);

    @Query(value = "select count(*) from HAS", nativeQuery=true)
    int countAll();
}
