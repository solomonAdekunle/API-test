package callers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jcraft.jsch.*;
import cucumber.api.Scenario;
import helpers.ApiHelper;
import helpers.Env;
import models.DataModel;
import models.Endpoint;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static com.jayway.restassured.RestAssured.given;
import static helpers.Env.LOCAL;
import static helpers.FileHelper.getTempSitemapFilesPath;
import static helpers.FileHelper.getTempSitemapPath;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class ServiceCaller extends ApiHelper {

    private static boolean serviceUp;
    private static String getCountry;
    private static String getPrefix;
    private static String getPlatform;

    private static TreeMap<String, String> results = new TreeMap<>();
    private static TreeMap<String, String> failedTests = new TreeMap<>();

    private static  String locValue;

    public ServiceCaller(DataModel dataModel) {
        super(dataModel);
    }

    public void addFailedTest (String scenario, String result){ failedTests.put(scenario, result); }

    public TreeMap<String, String> getFailedTests () { return failedTests; }

    public static Response getIndexableFiltersSitemap(String PATH){
        return given().auth().basic("user", "password")
                .baseUri(dataModel.getBaseURL())
                .when()
                .get(PATH);
    }

    public static Response getIndexableFiltersService(Endpoint endpoint, Map<String, ?> params, boolean addAuth){
        RequestSpecification request = given().header("Accept", "application/json");
        if(addAuth) {
            request.auth().basic("user", "password");
        }
        return request.baseUri(dataModel.getBaseURL())
                .when()
                .pathParams(params)
                .get(endpoint.getPath());
    }

    public static boolean isFiltersServiceResponseAsExpected(Response response, ArrayList fields, String basePath) throws IOException {
        boolean check = false;
        for(int i = 0; i < fields.size(); i++){
            Assert.assertTrue(!(response.jsonPath().get(basePath+fields.get(i).toString()).toString()).isEmpty());
            assertThat(response.jsonPath().get(basePath).toString()).contains(fields.get(i).toString());
            List field = response.jsonPath().get(basePath+fields.get(i).toString());
            assertThat(field.size()).isGreaterThan(0);
        }
        check = true;
        return check;
    }

    public Map<String, String> getPathParams(Endpoint endpoint, String locale, String categoryId) throws IOException {
        Map<String, String> params = new TreeMap<>();
        switch(endpoint) {
            case GET_FILTERS:
                params.put("categoryId", categoryId);
                params.put("storeId", getEnvProperties(locale + ".storeId"));
                params.put("languageId", getEnvProperties(locale + ".language"));
                break;
            case GET_GROUPS:
                params.put("locale", locale);
                params.put("pageKey", categoryId);
                break;
        }
        return params;
    }

    public boolean waitForServiceUp() throws Exception {
        int count = 15;
        while(!serviceUp && count > 0) {
            count--;
            String url = dataModel.getBaseURL();
            System.out.println("Performing service request to check status, " + url);
            Response response =  given()
                    .baseUri(url)
                    .get(Endpoint.CACHED_MARKETS.getPath());
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: "+ response.body().asString());
            if (response.statusCode() == 200 && allFilesScanned(response.body().asString())) {
                System.out.println("Service up!");
                serviceUp = true;
                Thread.sleep(5000);
            } else {
                Thread.sleep(2000);
            }
        }
        return serviceUp;
    }

    private boolean allFilesScanned(String content){
        return DataModel.getIndexableFilterMarkets().stream().allMatch(content::contains);
    }

    public void postToSlack(String resultMessage, String colourCode){
        if(System.getProperty("pipeline").equals("true")){
          // given().header("Content-Type", "application/json").body("{ \"attachments\": [ { \"color\": \""+colourCode+"\",  \"text\": \" " + resultMessage + "\", \"ts\": "+ getCurrentTime() +" } ] }").post(dataModel.POST_TO_SLACK_HOOK);
        }
    }

    public long getCurrentTime(){ return System.currentTimeMillis()/1000; }

    public void addTestResults(Scenario scenario){ results.put(scenario.getName(), scenario.getStatus().toUpperCase()); }

    public TreeMap<String, String> getTestResults(){ return  results; }

    public Object getFieldBySearchTerm(Response response, String searchTerm, String requiredField){

        Object value = null;
        List<String> searchTerms = response.jsonPath().getList("groups.searchTerm");
        for(int i=0; i<searchTerms.size(); i++){
            if(searchTerms.get(i).equals(searchTerm)){
                value = response.jsonPath().getList("groups." + requiredField);
                break;
            }
        }
        return value;
    }

    public String getExpectedJson(String filePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        FileInputStream fileInputStream = new FileInputStream(new File(classLoader.getResource(filePath).getFile()));
        return IOUtils.toString(fileInputStream, "UTF-8");
    }

    public JsonObject getJsonValue(String object){
        JsonParser parser = new JsonParser();
        return  (JsonObject) parser.parse(object);
    }

    public boolean assertAppliedDimensionsInAscendingOrder(String dimensions) {
        assertThat(getDimensionsFromCsv(dimensions))
                .as("Dimensions are not sorted in numerical order (lowest to highest):")
                .isSorted();
        return true;
    }

    public void getSitemapFilesFromServer() throws JSchException, SftpException, IOException {
        String destinationDirectory = getTempSitemapPath().toString();

        if (Env.get().equals(LOCAL)) {
            copyLocalSitemapFiles(destinationDirectory);
        } else {
            copyRemoteSitemapFiles(destinationDirectory);
        }
    }

    public void extractTarFile(File tarFile, File destination) throws IOException {
        System.out.println("Extracting tar files for " + getPlatform + "...");
        TarArchiveInputStream tar = null;
        tar = new TarArchiveInputStream(
                        new BufferedInputStream(
                                new FileInputStream(
                                        tarFile
                                )
                        )
        );

        TarArchiveEntry tarEntry = tar.getNextTarEntry();
        while (tarEntry != null){
            File destPath = new File(destination, tarEntry.getName());
            if(tarEntry.isDirectory()){
                destPath.mkdirs();
            } else {
                if(!destPath.getParentFile().exists()){
                    destPath.getParentFile().mkdirs();
                }
                destPath.createNewFile();
                byte[]btoRead = new byte[1024];
                BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destPath));
                int len = 0;
                while ((len = tar.read(btoRead)) != -1)
                {
                    bout.write(btoRead, 0 , len);
                }
                bout.close();
                btoRead = null;
            }
            tarEntry = tar.getNextTarEntry();
        }
        tar.close();
    }

    public void extractGzFile(File gzFile, File destination) throws IOException {
        System.out.println("Extracting gz files for " + getCountry + " " + getPlatform + "...");
        destination.mkdir();
        byte[] buffer = new byte[1024];
        GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(gzFile));

        File destPath = new File(destination, getPrefix + getCountry + "-indexable-filters0.xml");

        if(!destPath.getParentFile().exists()){
            destPath.getParentFile().mkdirs();
        }
        destPath.createNewFile();
        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destPath));

        int len;
        while ((len = gzis.read(buffer)) > 0){
            bout.write(buffer, 0, len);
        }
        gzis.close();
        bout.close();
    }

    public void extractTarFileForMobileOrWeb(String platform) throws IOException {
        String prefix = (platform.equals("mobile")) ? "m-" : "";
        getPrefix = prefix;
        getPlatform = platform;

        File tarFile = new File (getTempSitemapPath().toString() + "/" + prefix + "sitemaps.tar");
        extractTarFile(tarFile, getTempSitemapFilesPath().toFile());
    }

    public void extractGzFileForACountry(String country) throws IOException {
        getCountry = country;
        File gzFile = new File (getTempSitemapFilesPath().toString() + "/" + getPrefix + country +"-indexable-filters0.xml.gz");
        extractGzFile(gzFile, getTempSitemapFilesPath().toFile());
    }

    public void loadAndVerifySiteMap() throws IOException, SAXException, ParserConfigurationException {
        System.out.println("Verifying sitemaps content for " + getCountry + " " + getPlatform + "...");
        File xmlFile = new File(getTempSitemapFilesPath().toFile() + "/" + getPrefix + getCountry + "-indexable-filters0.xml");

        List<String> urlsetAttributes = Arrays.asList("xmlns", "xmlns:xhtml");
        List<String> xhtmlLinkAttributes = Arrays.asList("rel", "href", "hreflang");

        NodeList urlSet = documentBuilder(xmlFile).getElementsByTagName("urlset");
        checkAttributesInTag(urlSet, urlsetAttributes);

        Assert.assertTrue(urlSet.getLength() == 1);
        Assert.assertTrue(urlSet.item(0).hasChildNodes());

        NodeList urls = urlSet.item(0).getChildNodes();

        for(int i = 0; i < urls.getLength(); i++){
            NodeList nodesInUrl = urls.item(i).getChildNodes();
            Assert.assertTrue(nodesInUrl.getLength() != 0);
            Assert.assertTrue(nodesInUrl.getLength() != 2);
            if(nodesInUrl.getLength() > 1){
                checkNumberOfLocsIsOne(nodesInUrl);
            }
            if(getPlatform.equals("mobile")){
                Assert.assertTrue(nodesInUrl.getLength() == 1);
            }
            for(int k = 0; k < nodesInUrl.getLength(); k++){
                String nodeName = nodesInUrl.item(k).getNodeName();
                Assert.assertTrue(nodeName.equals("loc") || nodeName.equals("xhtml:link"));
                Element element = (Element) nodesInUrl.item(k);
                if(nodeName.equals("loc")){
                    String locNamespace = nodesInUrl.item(k).getTextContent();
                    locValue = locNamespace;
                    if(Env.get().equals("static1") || Env.get().equals("prod")){
                        if(getCountry.equals("f1")){ getCountry = "fr"; }
                        Assert.assertTrue(locNamespace.startsWith("https://" + getCountry +".rs-online.com") && locNamespace.contains("/" + getPlatform + "/"));
                    } else {
                        Assert.assertTrue(locNamespace.startsWith("https://") && locNamespace.contains("/" + getPlatform + "/"));
                    }
                    if(getPlatform.equals("mobile")){
                        Assert.assertTrue(!locNamespace.contains("/web/"));
                    } else if(getPlatform.equals("web")){
                        Assert.assertTrue(!locNamespace.contains("/mobile/"));
                    }
                    assertDimensionsInNumericalOrder(locNamespace);
                } else if(nodeName.equals("xhtml:link")){
                    if(getPlatform.equals("mobile")){ Assert.fail("Mobile sitemap should not have xhtml:link"); }
                    for(String attribute : xhtmlLinkAttributes){
                        isAttributeCorrect(attribute, element);
                    }
                    if(locValue.equals(getEnvProperties("loc")) && getCountry.equals(getEnvProperties("country"))){
                        List<String> xhtmlAttributes = Arrays.asList("hreflang","href");
                        for(String attributes : xhtmlAttributes)
                        verifyExpectedHreflangAndHrefs(attributes, element);
                    }
                    assertDimensionsInNumericalOrder(locValue);
                }
            }
        }
    }

    public void checkAttributesInTag(NodeList list, List<String> attributes){
        for(int i=0; i < list.getLength(); i++){
            Element element = (Element) list.item(i);
            for(String attribute : attributes){
                isAttributeCorrect(attribute, element);
            }
        }
    }

    public void isAttributeCorrect(String attribute, Element element){
        String attributeValue = element.getAttribute(attribute);
        switch (attribute){
            case "xmlns":
                Assert.assertTrue(attributeValue.equals("http://www.sitemaps.org/schemas/sitemap/0.9"));
                break;
            case "xmlns:xhtml":
                Assert.assertTrue(attributeValue.equals("http://www.w3.org/1999/xhtml"));
                break;
            case "rel":
                Assert.assertTrue(attributeValue.equals("alternate"));
                break;
            case "href":
                Assert.assertTrue(!attributeValue.isEmpty());
                Assert.assertTrue(attributeValue.contains("/" + getPlatform + "/"));
                if(getPlatform.equals("mobile")){
                    Assert.assertTrue(!attributeValue.contains("/web/"));
                } else if(getPlatform.equals("web")){
                    Assert.assertTrue(!attributeValue.contains("/mobile/"));
                }
                break;
            case "hreflang":
                Assert.assertTrue(!attributeValue.isEmpty());
                break;
            default:
                Assert.fail("Attribute not found");
        }
    }

    private void checkNumberOfLocsIsOne(NodeList nodeList){
        int numberOfLocs = 0;
        for (int j = 0; j < nodeList.getLength(); j++) {
            String nodeName = nodeList.item(j).getNodeName();
            if (nodeName.equals("loc")) {
                numberOfLocs = numberOfLocs + 1;
            }
        }
        Assert.assertTrue(numberOfLocs == 1);
    }

    public void loadAndVerifySitemapIndex() throws ParserConfigurationException, IOException, SAXException {
        System.out.println("Verifying sitemapindex for " + getCountry);
        File sitemapIndexFile = new File(getTempSitemapFilesPath().toFile() + "/" + getPrefix + getCountry + "-indexable-filters-sitemap.xml");
        NodeList sitemapIndex = documentBuilder(sitemapIndexFile).getElementsByTagName("sitemapindex");

        checkAttributesInTag(sitemapIndex, Arrays.asList("xmlns"));

        Assert.assertTrue(sitemapIndex.getLength() == 1);
        Assert.assertTrue(sitemapIndex.item(0).hasChildNodes());

        NodeList sitemaps = sitemapIndex.item(0).getChildNodes();

        for(int l = 0; sitemaps.getLength() < l; l++){
            String locNamespace = sitemaps.item(l).getChildNodes().item(0).getTextContent();
            Assert.assertTrue(sitemaps.item(l).getNodeName().equals("sitemap"));
            Assert.assertTrue(sitemaps.item(l).getChildNodes().getLength() == 1);
            Assert.assertTrue(sitemaps.item(l).getChildNodes().item(0).getNodeName().equals("loc"));
            Assert.assertTrue(locNamespace.startsWith("https://" + getCountry + ".rs-online.com/" + getPrefix + getCountry + "-indexable-filters") && locNamespace.endsWith(".xml.gz"));
        }
    }

    private org.w3c.dom.Document documentBuilder(File SitemapFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        org.w3c.dom.Document document = documentBuilder.parse(SitemapFile);

        Assert.assertTrue(document.getXmlEncoding().equals("UTF-8"));
        Assert.assertTrue(document.getXmlVersion().equals("1.0"));
        Assert.assertTrue(!document.getXmlStandalone());

        return document;
    }

    private boolean isSitemapSchemaValid(File XMLFileToValidate, File validatorFile){
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try{
            Schema schema = schemaFactory.newSchema(validatorFile);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(XMLFileToValidate));
            return true;

        } catch (SAXException | IOException e){
            e.printStackTrace();
            return false;
        }
    }

    public void sleep(int secs) throws InterruptedException {
        Thread.sleep(secs * 1000);
    }

    private void verifyExpectedHreflangAndHrefs(String attribute, Element element) throws IOException {
        String attributeValue = element.getAttribute(attribute);
        if(attribute.equals("hreflang")){
            Assert.assertTrue(getValuesFromPropsAsList("listOfExpectedHrefLang").contains(attributeValue));
        }else {
            Assert.assertTrue(getValuesFromPropsAsList("listOfExpectedHrefs").contains(attributeValue));
        }
    }

    public List<String> getValuesFromPropsAsList(String key) throws IOException {
       return   new ArrayList<>(Arrays.asList(getEnvProperties(key).trim().split(";")));
    }

    private void assertDimensionsInNumericalOrder(String url) {
        assertAppliedDimensionsInAscendingOrder(extractDimensionsFromUrl(url));
    }

    private String extractDimensionsFromUrl(String url) {
        String key = "applied-dimensions=";
        int startIndex = url.indexOf(key);
        if (startIndex > -1) {
            startIndex += key.length();
            int endIndex = url.indexOf("&", startIndex);
            if (endIndex == -1) {
                endIndex = url.length();
            }
            return url.substring(startIndex, endIndex);
        }
        return "";
    }

    private List<Long> getDimensionsFromCsv(String dimensionsCsv) {
        return Arrays.stream(dimensionsCsv.split(",")).map(Long::parseLong).collect(toList());
    }

    private void copyLocalSitemapFiles(String destinationDirectory) {
        String sitemapParentDirectory = getLocalSitemapParentDirectory();
        String webSitemapFile = sitemapParentDirectory + dataModel.getSitemapDateTime() + "/sitemaps.tar";
        String mobileSitemapFile = sitemapParentDirectory + dataModel.getSitemapDateTime() + "/m-sitemaps.tar";

        try {
            copyFileToDirectory(webSitemapFile, destinationDirectory);
            copyFileToDirectory(mobileSitemapFile, destinationDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFileToDirectory(String sourceFile, String destinationDirectory) throws IOException {
        FileUtils.copyFileToDirectory(new File(sourceFile), new File(destinationDirectory));
    }

    private void copyRemoteSitemapFiles(String destinationDirectory) throws IOException, JSchException, SftpException {
        String remoteDirectoryForMobileSitemap = "/opt/sitemap/output/" + dataModel.getSitemapDateTime() + "/m-sitemaps.tar";
        String remoteDirectoryForWebSitemap = "/opt/sitemap/output/" + dataModel.getSitemapDateTime() + "/sitemaps.tar";

        System.out.println("Connecting to server to collect sitemap tars files");
        String serverAddress = getEnvProperties("box.ip");
        String userId = getEnvProperties("box.user");
        String password = getEnvProperties("box.paswd");

        JSch jSch = new JSch();
        Session session = jSch.getSession(userId, serverAddress, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setPassword(password);
        session.connect();

        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        sftp.get(remoteDirectoryForMobileSitemap, destinationDirectory);
        sftp.get(remoteDirectoryForWebSitemap, destinationDirectory);

        session.disconnect();
    }

    private String getLocalSitemapParentDirectory() {
        if (Env.isLocalDocker()) {
            // Look for project name in the working directory and build from here to the docker shared path
            String normalizedPath = System.getProperty("user.dir").replace('\\', '/');
            String projectName = "indexable-filter-service";
            int index = normalizedPath.indexOf(projectName);
            if (index > -1) {
                return (normalizedPath.substring(0, index) + projectName + "/acceptance/target/tmp/");
            }
            return null;
        } else {
            return "/opt/sitemap/output/";
        }
    }
}
