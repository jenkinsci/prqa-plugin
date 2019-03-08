package net.praqma.prqa.parsers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsDataParser
        implements Serializable {

    private String filePath;
    private int rootLevel = 1;

    public ResultsDataParser(String filePath) {
        this.filePath = filePath;
    }

    public List<MessageGroup> parseResultsData()
            throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(new FileInputStream(filePath));
        return beginFileParsing(document);
    }

    private Map<String, Integer> getMessages(Element node) {
        Map<String, Integer> map = new HashMap<>();
        NodeList nList = node.getElementsByTagName("Message");
        for (int i = 0; i < nList.getLength(); ++i) {
            Node message = nList.item(i);
            map.put(message.getAttributes()
                           .getNamedItem("guid")
                           .getNodeValue(), Integer.parseInt(message.getAttributes()
                                                                    .getNamedItem("active")
                                                                    .getNodeValue()));
        }
        return map;
    }

    private List<MessageGroup> beginFileParsing(Document document)
            throws Exception {
        List<MessageGroup> messagesGroups = new ArrayList<>();
        document.getDocumentElement()
                .normalize();
        NodeList nList = document.getElementsByTagName("dataroot");
        for (int varDataRoot = 0; varDataRoot < nList.getLength(); varDataRoot++) {
            Node node = nList.item(varDataRoot);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getAttributes()
                        .getNamedItem("type")
                        .getNodeValue()
                        .equals("project")) {
                    Element pElement = (Element) node;
                    NodeList pNodeList = pElement.getElementsByTagName("tree");
                    for (int varTree = 0; varTree < pNodeList.getLength(); varTree++) {
                        Node cNode = pNodeList.item(varTree);
                        if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                            if (cNode.getAttributes()
                                     .getNamedItem("type")
                                     .getNodeValue()
                                     .equals("rules")) {
                                Element cElement = (Element) cNode;
                                NodeList cNodeList = cElement.getElementsByTagName("RuleGroup");
                                for (int varRGroup = 0; varRGroup < cNodeList.getLength(); varRGroup++) {
                                    Element ccNode = (Element) cNodeList.item(varRGroup);
                                    MessageGroup messageGroup = new MessageGroup(ccNode.getAttributes()
                                                                                       .getNamedItem("name")
                                                                                       .getNodeValue());
                                    NodeList ccNodeList = ccNode.getChildNodes();
                                    for (int varRule = 0; varRule < ccNodeList.getLength(); varRule++) {
                                        if (ccNodeList.item(varRule)
                                                      .getNodeType() != Node.ELEMENT_NODE) {
                                            continue;
                                        }
                                        Element cccNode = (Element) ccNodeList.item(varRule);
                                        if (!"Rule".equals(cccNode.getTagName())) {
                                            continue;
                                        }
                                        Rule violatedRule = new Rule(cccNode.getAttributes()
                                                                            .getNamedItem("id")
                                                                            .getNodeValue(), getMessages(cccNode));
                                        messageGroup.addViolatedRule(violatedRule);
                                    }
                                    messagesGroups.add(messageGroup);
                                }
                            }
                        }
                    }
                }
            }
        }
        return messagesGroups;
    }
}
