package idetector.dal.caching.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import idetector.dal.caching.bean.edge.*;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.dal.caching.repository.*;
import idetector.config.GlobalConfiguration;

import java.util.List;


@Service
public class RelationshipsService {

    @Autowired
    private AliasEdgeRepository aliasEdgeRepository;
    @Autowired
    private CallEdgeRepository callEdgeRepository;
    @Autowired
    private ExtendEdgeRepository extendEdgeRepository;
    @Autowired
    private HasEdgeRepository hasEdgeRepository;
    @Autowired
    private InterfacesEdgeRepository interfacesEdgeRepository;

    public <T> void saveEdge(T edge){
        if(edge instanceof Has){
            hasEdgeRepository.save((Has) edge);
        }else if(edge instanceof Call){
            callEdgeRepository.save((Call) edge);
        }else if(edge instanceof Interfaces){
            interfacesEdgeRepository.save((Interfaces) edge);
        }else if(edge instanceof Extend){
            extendEdgeRepository.save((Extend) edge);
        }else if(edge instanceof Alias){
            aliasEdgeRepository.save((Alias) edge);
        }
    }

    public void saveAllHasEdges(Iterable<Has> edges){
        hasEdgeRepository.saveAll(edges);
    }
    public void saveAllCallEdges(Iterable<Call> edges){
        callEdgeRepository.saveAll(edges);
    }
    public void saveAllExtendEdges(Iterable<Extend> edges){
        extendEdgeRepository.saveAll(edges);
    }
    public void saveAllAliasEdges(Iterable<Alias> edges){
        aliasEdgeRepository.saveAll(edges);
    }
    public void saveAllInterfacesEdges(Iterable<Interfaces> edges){
        interfacesEdgeRepository.saveAll(edges);
    }

    public void save2CSV(){
        aliasEdgeRepository.save2Csv(GlobalConfiguration.ALIAS_RELATIONSHIP_CACHE_PATH);
        hasEdgeRepository.save2Csv(GlobalConfiguration.HAS_RELATIONSHIP_CACHE_PATH);
        extendEdgeRepository.save2Csv(GlobalConfiguration.EXTEND_RELATIONSHIP_CACHE_PATH);
        callEdgeRepository.save2Csv(GlobalConfiguration.CALL_RELATIONSHIP_CACHE_PATH);
        interfacesEdgeRepository.save2Csv(GlobalConfiguration.INTERFACE_RELATIONSHIP_CACHE_PATH);
    }

    public int countAll(){
        int sum = 0;
        sum += aliasEdgeRepository.countAll();
        sum += hasEdgeRepository.countAll();
        sum += extendEdgeRepository.countAll();
        sum += callEdgeRepository.countAll();
        sum += interfacesEdgeRepository.countAll();
        return sum;
    }

    public List<String> findMethodReferenceByALIAS(MethodReference methodRef){
        return aliasEdgeRepository.findMethodReferenceFormALIASByTarget(methodRef);
    }
}
