package hub.ebb.jblcluster.eventservice.generator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.squareup.javapoet.*;
import hub.jbl.core.dto.jps.event.JpsEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.lang.model.element.Modifier;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Petar Tseperski on 27/07/2017.
 */
public class JblFactoryGenerator {

    public static final String PACKAGE_EVENT_BASE = "hub.jbl.core.dto.jps.event.";
    private static final JblFactoryGenerator INSTANCE = new JblFactoryGenerator();
    private List<NodeList> codeGroups;
    private Multimap<String, Node> codesGroupedByCodeGroups;
    private Map<String, String> namesGroupedBySpecCodes;

    TypeVariableName typeVariableName = TypeVariableName.get("T", ClassName.get(JpsEvent.class));
    private String bitmaskTypeValue;
    private String bitmaskSubTypeValue;

    private JblFactoryGenerator() {
        codeGroups = new ArrayList<>();
        codesGroupedByCodeGroups = ArrayListMultimap.create();
        namesGroupedBySpecCodes = new HashMap<>();
    }

    public static JblFactoryGenerator getInstance() {
        return INSTANCE;
    }

    public void generateFactory(String rootEventServicePath) throws IOException, SAXException, ParserConfigurationException {
        readXmlFiles(rootEventServicePath);

        FieldSpec singleInstance = FieldSpec.builder(TypeName.OBJECT, "INSTANCE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new JblEventFactoryContainer()")
                .build();

        ParameterSpec eventSpecCode = ParameterSpec.builder(String.class, "eventSpecCode")
                .addModifiers(Modifier.FINAL)
                .build();

        ParameterSpec eventType = ParameterSpec.builder(String.class, "eventType")
                .addModifiers(Modifier.FINAL)
                .build();

        ParameterSpec event = ParameterSpec.builder(TypeVariableName.get("T", typeVariableName), "event")
                .build();

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build();

        MethodSpec getInstance = MethodSpec.methodBuilder("getInstance")
                .addComment("After obtaining the instance, it should be cast to JblEventFactoryContainer type if necessary.")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.OBJECT)
                .addStatement("return INSTANCE")
                .build();

        MethodSpec.Builder getEventTypeBuilder = MethodSpec.methodBuilder("getEventType")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(eventType)
                .returns(TypeName.OBJECT)
                .beginControlFlow("switch (eventType)");

        MethodSpec.Builder jpsEventFactoryBuilder = MethodSpec.methodBuilder("jpsEventFactory")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(eventSpecCode)
                .addParameter(eventType)
                .returns(TypeName.OBJECT)
                .beginControlFlow("switch (eventSpecCode)");

        MethodSpec.Builder initEventBuilder = MethodSpec.methodBuilder("initEvent")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(event)
                .addTypeVariable(typeVariableName)
                .returns(typeVariableName)
                .addStatement("String className = event.getClass().getSimpleName()")
                .addStatement("long bitmaskType = new java.math.BigInteger( $S, 16 ).longValue()", bitmaskTypeValue)
                .addStatement("long bitmaskSubType =  new java.math.BigInteger( $S, 16 ).longValue()", bitmaskSubTypeValue)
                .beginControlFlow("switch (className)");


        Collection<Node> types = codesGroupedByCodeGroups.get("Types");
        Collection<Node> subTypes = codesGroupedByCodeGroups.get("SubTypes");
        Collection<Node> specCodes = codesGroupedByCodeGroups.get("SpecCodes");
        Collection<Node> devices = codesGroupedByCodeGroups.get("Devices");
        Collection<Node> services = codesGroupedByCodeGroups.get("Services");
        Collection<Node> apps = codesGroupedByCodeGroups.get("Applications");

        for (Node node : types) {
            String specCode = node.getAttributes().item(3).getNodeValue();
            String className = node.getAttributes().item(1).getNodeValue();
            String isJblConcreteClass = node.getAttributes().item(0).getNodeValue();

            if (isJblConcreteClass.equals("true")) {
                getEventTypeBuilder.addStatement("case $S: return new $L()", specCode, className);
            }
        }


        for (Node node : subTypes) {
            String specCode = node.getAttributes().item(3).getNodeValue();
            Node zAttr = node.getAttributes().item(4);
            String className = node.getAttributes().item(1).getNodeValue();
            if(zAttr == null || zAttr.getNodeValue().equals("false"))
                initEventBuilder.addStatement("case $S: event.setEventSpecCode(\"" + specCode + "\"); break", className);


            String isJblConcreteClass = node.getAttributes().item(0).getNodeValue();

            if (isJblConcreteClass.equals("true")) {
                getEventTypeBuilder.addStatement("case $S: return new $L()", specCode, className);
            }


        }

        String sqlScript = "";
        String csvScript = "";
        Collection<Node> jpsGenericAlarms = new ArrayList<>();
        Collection<Node> jpsDevAlarms = new ArrayList<>();
        Collection<Node> jpsAppAlarms = new ArrayList<>();
        Collection<Node> jpsServAlarms = new ArrayList<>();
        Collection<Node> jpsAlarms = new ArrayList<>();

        for (Node node : specCodes) {
            String specCode = node.getAttributes().item(3).getNodeValue();
            Node zAttr = node.getAttributes().item(4);
            String className = node.getAttributes().item(1).getNodeValue();
            if(zAttr == null || zAttr.getNodeValue().equals("false"))
                initEventBuilder.addStatement("case $S: event.setEventSpecCode(\"" + specCode + "\"); break", PACKAGE_EVENT_BASE+className);

            String isJblConcreteClass = node.getAttributes().item(0).getNodeValue();



            if (isJblConcreteClass.equals("true")) {
                jpsEventFactoryBuilder.addStatement("case $S: return new $L()", specCode, className);
            } else {
                jpsEventFactoryBuilder.addStatement("case $S: return getEventType(eventType)", specCode);
            }



            if (className.startsWith("JpsAppAlrm")) {
                jpsAppAlarms.add(node);
            } else if (className.startsWith("JpsSrvAlrm")) {
                jpsServAlarms.add(node);
            } else if (className.startsWith("JpsDevAlrm")) {
                jpsDevAlarms.add(node);
            } else if (className.startsWith("JpsAlrm")) {
                jpsGenericAlarms.add(node);
            } else if (className.contains("Alrm")) {
                jpsAlarms.add(node);
            }
        }


        for (Node node : devices) {

            String specCode = node.getAttributes().item(3).getNodeValue();
            String deviceName = node.getAttributes().item(1).getNodeValue();
            Long longSpecCode = Long.decode(specCode);

            for (Node genericAlarmNode : jpsGenericAlarms) {
                String alrmSpecCode = genericAlarmNode.getAttributes().item(3).getNodeValue();
                String alrmName = genericAlarmNode.getAttributes().item(1).getNodeValue();
                Long alrmLongSpecCode = Long.decode(alrmSpecCode);

                sqlScript = sqlScript + "select janus.lpinsertorupdatemsmessage('jbl'," + (longSpecCode | alrmLongSpecCode) + ",true\t,0,'" + alrmName + "." + deviceName + "')\n";
                sqlScript = sqlScript + "select janus.lpinsertorupdatemslocalizedmessage('en-US', 'jbl'," + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + deviceName + "')\n";

                csvScript = csvScript + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + deviceName + "'\n";
            }

            for (Node genericAlarmNode : jpsDevAlarms) {
                String alrmSpecCode = genericAlarmNode.getAttributes().item(3).getNodeValue();
                String alrmName = genericAlarmNode.getAttributes().item(1).getNodeValue();
                Long alrmLongSpecCode = Long.decode(alrmSpecCode);

                sqlScript = sqlScript + "select janus.lpinsertorupdatemsmessage('jbl'," + (longSpecCode | alrmLongSpecCode) + ",true\t,0,'" + alrmName + "." + deviceName + "')\n";
                sqlScript = sqlScript + "select janus.lpinsertorupdatemslocalizedmessage('en-US', 'jbl'," + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + deviceName + "')\n";

                csvScript = csvScript + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + deviceName + "'\n";

            }
        }

        for (Node node : services) {

            String specCode = node.getAttributes().item(3).getNodeValue();
            String serviceName = node.getAttributes().item(1).getNodeValue();
            Long longSpecCode = Long.decode(specCode);

            for (Node genericAlarmNode : jpsGenericAlarms) {
                String alrmSpecCode = genericAlarmNode.getAttributes().item(3).getNodeValue();
                String alrmName = genericAlarmNode.getAttributes().item(1).getNodeValue();
                Long alrmLongSpecCode = Long.decode(alrmSpecCode);

                sqlScript = sqlScript + "select janus.lpinsertorupdatemsmessage('jbl'," + (longSpecCode | alrmLongSpecCode) + ",true\t,0,'" + alrmName + "." + serviceName + "')\n";
                sqlScript = sqlScript + "select janus.lpinsertorupdatemslocalizedmessage('en-US', 'jbl'," + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + serviceName + "')\n";

                csvScript = csvScript + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + serviceName + "'\n";

            }

            for (Node genericAlarmNode : jpsServAlarms) {
                String alrmSpecCode = genericAlarmNode.getAttributes().item(3).getNodeValue();
                String alrmName = genericAlarmNode.getAttributes().item(1).getNodeValue();
                Long alrmLongSpecCode = Long.decode(alrmSpecCode);

                sqlScript = sqlScript + "select janus.lpinsertorupdatemsmessage('jbl'," + (longSpecCode | alrmLongSpecCode) + ",true\t,0,'" + alrmName + "." + serviceName + "')\n";
                sqlScript = sqlScript + "select janus.lpinsertorupdatemslocalizedmessage('en-US', 'jbl'," + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + serviceName + "')\n";

                csvScript = csvScript + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + serviceName + "'\n";

            }
        }

        for (Node node : apps) {

            String specCode = node.getAttributes().item(3).getNodeValue();
            String appName = node.getAttributes().item(1).getNodeValue();
            Long longSpecCode = Long.decode(specCode);

            for (Node genericAlarmNode : jpsGenericAlarms) {
                String alrmSpecCode = genericAlarmNode.getAttributes().item(3).getNodeValue();
                String alrmName = genericAlarmNode.getAttributes().item(1).getNodeValue();
                Long alrmLongSpecCode = Long.decode(alrmSpecCode);

                sqlScript = sqlScript + "select janus.lpinsertorupdatemsmessage('jbl'," + (longSpecCode | alrmLongSpecCode) + ",true\t,0,'" + alrmName + "." + appName + "')\n";
                sqlScript = sqlScript + "select janus.lpinsertorupdatemslocalizedmessage('en-US', 'jbl'," + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + appName + "')\n";

                csvScript = csvScript + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + appName + "'\n";

            }

            for (Node genericAlarmNode : jpsAppAlarms) {
                String alrmSpecCode = genericAlarmNode.getAttributes().item(3).getNodeValue();
                String alrmName = genericAlarmNode.getAttributes().item(1).getNodeValue();
                Long alrmLongSpecCode = Long.decode(alrmSpecCode);

                sqlScript = sqlScript + "select janus.lpinsertorupdatemsmessage('jbl'," + (longSpecCode | alrmLongSpecCode) + ",true\t,0,'" + alrmName + "." + appName + "')\n";
                sqlScript = sqlScript + "select janus.lpinsertorupdatemslocalizedmessage('en-US', 'jbl'," + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + appName + "')\n";

                csvScript = csvScript + (longSpecCode | alrmLongSpecCode) + ",'" + alrmName + "." + appName + "'\n";

            }
        }


        for (Node genericAlarmNode : jpsAlarms) {
            String alrmSpecCode = genericAlarmNode.getAttributes().item(3).getNodeValue();
            String alrmName = genericAlarmNode.getAttributes().item(1).getNodeValue();
            Long alrmLongSpecCode = Long.decode(alrmSpecCode);

            sqlScript = sqlScript + "select janus.lpinsertorupdatemsmessage('jbl'," + (alrmLongSpecCode) + ",true\t,0,'" + alrmName + "')\n";
            sqlScript = sqlScript + "select janus.lpinsertorupdatemslocalizedmessage('en-US', 'jbl'," + (alrmLongSpecCode) + ",'" + alrmName + "')\n";

            csvScript = csvScript + (alrmLongSpecCode) + ",'" + alrmName + "." + alrmName + "'\n";

        }


        sqlScript = sqlScript + "\n\n\n";


        MethodSpec getEventType = getEventTypeBuilder
                .addStatement("default: throw new $T(\"Error\", eventType)", InvalidTypeException.class)
                .endControlFlow()
                .addException(InvalidTypeException.class)
                .build();

        MethodSpec jpsEventFactory = jpsEventFactoryBuilder
                .addStatement("default: throw new $T(\"Error\", eventType)", InvalidTypeException.class)
                .endControlFlow()
                .addException(InvalidTypeException.class)
                .build();

        MethodSpec initEvent = initEventBuilder
                .addStatement("default: throw new $T(\"Error\", event.getEventType())", InvalidTypeException.class)
                .endControlFlow()
                .addException(InvalidTypeException.class)
                .addStatement("event.setEventType(\"\"+( (bitmaskType & Long.decode(event.getEventSpecCode())| bitmaskSubType)& Long.decode(event.getEventSpecCode())))")
                .addStatement("return event")
                .build();



        TypeSpec JblEventFactoryContainer = TypeSpec.classBuilder("JblEventFactoryContainer")
                .addModifiers(Modifier.PUBLIC)
                .addField(singleInstance)
                .addMethod(constructor)
                .addMethod(jpsEventFactory)
                .addMethod(initEvent)
                .addMethod(getEventType)
                .addMethod(getInstance)
                .addJavadoc(sqlScript)
                .addJavadoc(csvScript)
                .build();

        JavaFile javaFile = JavaFile.builder("hub.ebb.jblcluster.eventservice.model", JblEventFactoryContainer)
                .indent("\t")
                .build();


//        File sourcePath = new File("source-generator/jps-generated-factory/src/main/java");
        String eventMicroservicesPath = rootEventServicePath + "/src/main/java";


        File sourcePath = new File(eventMicroservicesPath);
        javaFile.writeTo(sourcePath);

    }

    private void readXmlFiles(String rootEventServicePath) throws ParserConfigurationException, IOException, SAXException {

        String eventMicroservicesPath = rootEventServicePath + "/src/main/resources";

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        File jblDictionary = new File(eventMicroservicesPath + "/" + "JblDictionary.xml");
        Document jblDocument = documentBuilder.parse(jblDictionary);
        Element jblDocRootElement = jblDocument.getDocumentElement();

        fillMapFromXml(jblDocRootElement);

        fillSpecCodesMap("Types");
        fillSpecCodesMap("SubTypes");
        fillSpecCodesMap("SpecCodes");
        fillSpecCodesMap("Devices");
    }

    private void fillMapFromXml(Element rootElement) {
        for (int i = 0; i < rootElement.getElementsByTagName("CodeGroup").getLength(); i++) {
            NodeList currentCodeGroup = rootElement.getElementsByTagName("CodeGroup").item(i).getChildNodes();
            codeGroups.add(currentCodeGroup);
            String currentCodeGroupName = ((Element) currentCodeGroup).getAttributes().getNamedItem("name").getNodeValue();
            Node bit_mask = ((Element) currentCodeGroup).getAttributes().getNamedItem("bit_mask");

            if (currentCodeGroupName.equals("SubTypes")) {
                bitmaskSubTypeValue = bit_mask.getNodeValue();
            }
            if (currentCodeGroupName.equals("Types")) {
                bitmaskTypeValue = bit_mask.getNodeValue();
            }

            for (int j = 0; j < currentCodeGroup.getLength(); j++) {
                if (!currentCodeGroup.item(j).hasAttributes()) {
                    continue;
                } else {
                    codesGroupedByCodeGroups.put(currentCodeGroupName, currentCodeGroup.item(j));
                }
            }
        }
    }

    private void fillSpecCodesMap(String codeGroup) {
        for (Node node : codesGroupedByCodeGroups.get(codeGroup)) {
            String specCode = node.getAttributes().item(3).getNodeValue();
            String className = node.getAttributes().item(1).getNodeValue();
            namesGroupedBySpecCodes.put(specCode, className);
        }
    }
}
