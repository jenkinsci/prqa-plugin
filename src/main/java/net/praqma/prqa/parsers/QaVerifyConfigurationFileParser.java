package net.praqma.prqa.parsers;

import hudson.util.Secret;
import net.praqma.prqa.QAVerifyServerSettings;
import net.praqma.prqa.exceptions.PrqaException;
import org.apache.commons.io.FilenameUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.EscapeStrategy;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class QaVerifyConfigurationFileParser {

    private String qaVerifyFilePath;
    public static final String QA_VERIFIY_FILE_EXTENSION = "qcf";
    public static final String QAV_PROJECT_ELEMENT = "qav_project";
    public static final String AUTHENTICATION_ELEMENT = "authentication";
    public static final String AUTHENTICATION_ELEMENT_USERNAME_ATTRIBUTE = "username";
    public static final String AUTHENTICATION_ELEMENT_PASSWORD_ATTRIBUTE = "password";
    public static final String QAVERIFY_ELEMENT = "qaverify";
    public static final String QAVERIFY_ELEMENT_HOST_ATTRIBUTE = "host";
    public static final String QAVERIFY_ELEMENT_PORT_ATTRIBUTE = "port";
    public static final String QAVERIFY_ELEMENT_PROJECT_ATTRIBUTE = "project";

    public QaVerifyConfigurationFileParser(String qaVerifyFilePath) {
        this.qaVerifyFilePath = qaVerifyFilePath;
    }

    public void changeQaVerifyConfiFileSettings(QAVerifyServerSettings qaVerifySettings,
                                                String project)
            throws PrqaException, JDOMException, IOException {

        File qaVerifyFile = new File(qaVerifyFilePath);

        if (!qaVerifyFile.exists()) {
            throw new PrqaException("The provided QA·Verify Configuration file does not exist!");
        }
        if (!qaVerifyFile.isFile() || !(FilenameUtils.getExtension(qaVerifyFilePath)
                                                     .toLowerCase()
                                                     .equals(QA_VERIFIY_FILE_EXTENSION))) {
            throw new PrqaException(
                    "The QA·Verify Configuration file has to be a file with" + "'" + QA_VERIFIY_FILE_EXTENSION + "' extension!");
        }

        SAXBuilder saxBuilder = new SAXBuilder();

        Document document = saxBuilder.build(qaVerifyFile);
        Element rootElement = document.getRootElement();

        List<Element> childrens = rootElement.getChildren(QAV_PROJECT_ELEMENT);
        for (Element qav_projectElement : childrens) {

            Element authenticationElement = qav_projectElement.getChild(AUTHENTICATION_ELEMENT);
            authenticationElement.getAttribute(AUTHENTICATION_ELEMENT_USERNAME_ATTRIBUTE)
                                 .setValue(qaVerifySettings.user);
            authenticationElement.getAttribute(AUTHENTICATION_ELEMENT_PASSWORD_ATTRIBUTE)
                                 .setValue(Secret.toString(qaVerifySettings.password));

            Element qaverifyElement = qav_projectElement.getChild(QAVERIFY_ELEMENT);
            qaverifyElement.getAttribute(QAVERIFY_ELEMENT_HOST_ATTRIBUTE)
                           .setValue(qaVerifySettings.host);
            qaverifyElement.getAttribute(QAVERIFY_ELEMENT_PORT_ATTRIBUTE)
                           .setValue(String.valueOf(qaVerifySettings.port));
            qaverifyElement.getAttribute(QAVERIFY_ELEMENT_PROJECT_ATTRIBUTE)
                           .setValue(project);
        }
        Format format = Format.getPrettyFormat();
        format.setEncoding("UTF-8");
        format.setEscapeStrategy(new EscapeStrategy() {

            @Override
            public boolean shouldEscape(char ch) {
                return false;
            }
        });
        XMLOutputter xmlOutput = new XMLOutputter(format);
        xmlOutput.output(document, new FileWriter(qaVerifyFilePath));
    }
}
