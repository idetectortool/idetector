package idetector.dal.neo4j.repository;

import org.neo4j.driver.internal.value.BooleanValue;
import org.neo4j.driver.internal.value.ListValue;
import org.neo4j.driver.internal.value.MapValue;
import org.neo4j.driver.internal.value.StringValue;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import idetector.dal.neo4j.entity.MethodEntity;

import java.util.List;



@Repository
public interface MethodRefRepository extends Neo4jRepository<MethodEntity, String> {

    @Query("CALL apoc.periodic.iterate(\"CALL apoc.load.csv('file://\"+$path+\"', {header:true, mapping:{ IS_SINK: {type:'boolean'}, IS_SOURCE: {type:'boolean'}, IS_STATIC: {type:'boolean'}, IS_POLLUTED: {type:'boolean'}, HAS_PARAMETERS:{type:'boolean'}, IS_INITIALED: { type: 'boolean'}, ACTION_INITIALED: { type: 'boolean'}, IS_IGNORE: { type: 'boolean'}, IS_SERIALIZABLE:{type:'boolean'}, MODIFIERS:{type:'int'}, PARAMETER_SIZE:{type:'int'}}}) YIELD map AS row RETURN row\", \"MERGE(m:Method {ID:row.ID} ) ON CREATE SET m = row\", {batchSize:5000, iterateList:true, parallel:true}) yield total")
    int loadMethodRefFromCSV(String path);

    @Query("CALL apoc.periodic.iterate(\"CALL apoc.load.csv('file://\"+$path+\"', {header:true}) YIELD map AS row RETURN row\",\"MATCH ( m1:Method {ID:row.SOURCE} ) MATCH ( m2:Method {ID:row.TARGET }) MERGE (m1)-[e:CALL {ID:row.ID, LINE_NUM:row.LINE_NUM, INVOKER_TYPE:row.INVOKER_TYPE, POLLUTED_POSITION:row.POLLUTED_POSITION, REAL_CALL_TYPE:row.REAL_CALL_TYPE }]->(m2)\", {batchSize:5000, iterateList:true, parallel:false}) yield total")
    int loadCallEdgeFromCSV(String path);

    @Query("CALL apoc.periodic.iterate(\"CALL apoc.load.csv('file://\"+$path+\"', {header:true}) YIELD map AS row RETURN row\",\"MATCH ( m1:Method {ID:row.SOURCE} ) MATCH ( m2:Method {ID:row.TARGET }) MERGE (m1)-[e:ALIAS {ID:row.ID}]-(m2)\", {batchSize:1000, iterateList:true, parallel:false}) yield total")
    int loadAliasEdgeFromCSV(String path);

    @Query("match (sink:Method {IS_SINK:true} )<-[r:CALL]-(m1:Method) return distinct [sink.SIGNATURE, sink.VUL]")
    List<ListValue> findALlSink();

    @Query("match (sink:Method {SIGNATURE:$signature})<-[r:CALL]-(m1:Method) return [m1.SIGNATURE,r.LINE_NUM]")
    List<ListValue> findCallBySignature(String signature);

    @Query("match (sink:Method {SIGNATURE:$signature}) return sink.IS_SOURCE")
    List<BooleanValue> isSourceBySignature(String signature);
}