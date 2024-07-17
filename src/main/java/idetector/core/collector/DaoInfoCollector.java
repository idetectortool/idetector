package idetector.core.collector;

import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import idetector.core.container.DaoContainer;
import idetector.core.container.DataContainer;
import idetector.dal.caching.bean.ref.ClassReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DaoInfoCollector {

    @Autowired
    private DaoContainer daoContainer;

    public void collect(Map<String, String> daoPaths) {
        for (String path : daoPaths.values()) {

            if (!(path.toLowerCase().contains("mapper")||path.toLowerCase().contains("dao")))
                continue;
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document document = db.parse(path);
                NodeList mapperNodes = document.getElementsByTagName("mapper");
                for (int i = 0; i < mapperNodes.getLength(); i++) {
                    Node mapperNode = mapperNodes.item(i);
                    if (mapperNode.getNodeType() == Node.ELEMENT_NODE) {
                        String namespace = ((Element) mapperNode).getAttribute("namespace");
                        NodeList childNodes = mapperNode.getChildNodes();
                        for (int j = 0; j < childNodes.getLength(); j++) {
                            Node childNode = childNodes.item(j);

                            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                                switch (childNode.getNodeName()) {
                                    case "insert":
                                    case "update":
                                    case "delete":
                                    case "select":
                                        String id = ((Element) childNode).getAttribute("id");
                                        String content = childNode.getTextContent();
                                        Matcher matcher = Pattern.compile("\\$\\{([\\w\\.]*?)\\}").matcher(content);
                                        while (matcher.find()) {
                                            String var = matcher.group(1);
                                            HashSet<String> vars = new HashSet<>();
                                            if (daoContainer.getMappers().contains(namespace, id)) {
                                                vars.addAll(daoContainer.getMappers().get(namespace, id));
                                            }
                                            vars.add(var);
                                            daoContainer.getMappers().put(namespace, id, vars);
                                        }

                                        NodeList includeNodes = ((DeferredElementImpl) childNode).getElementsByTagName("include");
                                        if (includeNodes.getLength() == 1) {
                                            Node refid = includeNodes.item(0).getAttributes().getNamedItem("refid");
                                            if (refid != null && refid.getNodeValue().equals("net.mingsoft.base.dao.IBaseDao.sqlWhere")) {
                                                HashSet<String> vars = new HashSet<>();
                                                if (daoContainer.getMappers().contains(namespace, id)) {
                                                    vars.addAll(daoContainer.getMappers().get(namespace, id));
                                                }
                                                vars.add("item.field");
                                                daoContainer.getMappers().put(namespace, id, vars);
                                            }
                                        }
                                        break;
                                }

                            }
                        }
                    }
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
    }
}
