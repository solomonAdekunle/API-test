package stepdefs;

import callers.ServiceCaller;
import com.jayway.restassured.response.Response;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import models.DataModel;
import models.Endpoint;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class IndexableFiltersSitemapSteps {

    private ServiceCaller serviceCaller;
    private DataModel dataModel;
    private Response response;

    public IndexableFiltersSitemapSteps(ServiceCaller serviceCaller, DataModel dataModel){
        this.serviceCaller = serviceCaller;
        this.dataModel = dataModel;
    }

    @Given("^I call indexable filters sitemap service$")
    public void iCallIndexableFiltersSitemapService(){
        response = serviceCaller.getIndexableFiltersSitemap(Endpoint.SITEMAP.getPath());
        dataModel.setSitemapDateTime(response.asString().substring(20, 37));
        response.then().statusCode(200);
    }

    @When("^I retrieve mobile and desktop sitemaps from server$")
    public void iRetrieveSitemapTarFileFromServer() throws SftpException, JSchException, IOException {
        serviceCaller.getSitemapFilesFromServer();
    }

    @Then("^I extract and verify the output sitemaps for all expected countries$")
    public void iExtractAndVerifyTheOutputSitemaps() throws IOException, ParserConfigurationException, SAXException {
        List<String> countries = serviceCaller.getValuesFromPropsAsList("CountriesExpectedToHAveIndexableFiltersSitemap");
        List<String> platforms = Arrays.asList("mobile", "web");
        for(String platform : platforms){
            serviceCaller.extractTarFileForMobileOrWeb(platform);
            for(String country : countries){
                    serviceCaller.extractGzFileForACountry(country);
                    serviceCaller.loadAndVerifySitemapIndex();
                    serviceCaller.loadAndVerifySiteMap();
                }
            }
    }
}