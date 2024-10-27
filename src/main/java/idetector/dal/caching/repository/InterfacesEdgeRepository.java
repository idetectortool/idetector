package idetector.dal.caching.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import idetector.dal.caching.bean.edge.Interfaces;


public interface InterfacesEdgeRepository extends CrudRepository<Interfaces, String> {

    @Query(value = "CALL CSVWRITE(:path, 'SELECT * FROM INTERFACES')", nativeQuery=true)
    void save2Csv(@Param("path") String path);

    @Query(value = "select count(*) from INTERFACES", nativeQuery=true)
    int countAll();
}
