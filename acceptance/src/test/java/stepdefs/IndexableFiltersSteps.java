package stepdefs;

import callers.ServiceCaller;
import com.google.gson.JsonObject;
import com.jayway.restassured.response.Response;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import helpers.ApiHelper;
import helpers.Env;
import models.DataModel;
import models.Endpoint;
import models.WithOrWithout;
import org.junit.Assert;

import java.io.*;
import java.util.*;

import static models.WithOrWithout.WITH;
import static org.hamcrest.Matchers.equalTo;

public class IndexableFiltersSteps {

    private Response response;
    private ServiceCaller serviceCaller;
    private DataModel dataModel;
    private ApiHelper apiHelper;
    private Endpoint endpoint;
    private String locale;
    private String categoryId;
    private static boolean postToSlack = true;
    private static String pipelineValue = System.getProperty("pipeline");
    private static String stageValue = System.getProperty("stage");
    private static boolean failedScenario = false;
    private String colourCode = "";

    public IndexableFiltersSteps(ServiceCaller serviceCaller, DataModel dataModel, ApiHelper apiHelper) {
        this.serviceCaller = serviceCaller;
        this.dataModel = dataModel;
        this.apiHelper = apiHelper;
    }

    @Before
    public void beforeScenario() throws Exception {
        if (postToSlack && pipelineValue.equals("true") && stageValue.equals("deployed")) {
            //  colourCode = "#CECBCC";
            //  serviceCaller.postToSlack("Acceptance Tests - RUNNING", colourCode);
            //  postToSlack = false;
        }
        Assert.assertTrue("Service status check failed - may not be up yet", serviceCaller.waitForServiceUp());
    }

    @Given("^I am on (.*) site$")
    public void iAmOnEnvironment(String setCountry) {
        Assert.assertTrue(!dataModel.getBaseURL().isEmpty());
        locale = setCountry;
    }

    @When("^I access IFS (.*) endpoint (.*) authentication and (.*)$")
    public void iAccessIndexableFiltersServiceWithVariable(Endpoint endpoint, WithOrWithout withAuth, String categoryId) throws IOException {
        this.endpoint = endpoint;
        this.categoryId = categoryId;
        response = ServiceCaller.getIndexableFiltersService(endpoint, serviceCaller.getPathParams(endpoint, locale, categoryId), WITH.equals(withAuth));
    }

    @Then("^I verify (.*) is correct$")
    public void theResponseCodeIsCorrect(int responseCode) {
        response.then().statusCode(responseCode);
    }

    @Then("^I verify cache-control has max-age (.*)$")
    public void theResponseCacheControlMaxAgeIs(int expected) {
        response.then().header("Cache-Control", equalTo("max-age=" + expected));
    }

    @And("^I verify response body returned contains expected (.*)$")
    public void iVerifyResponseBodyIsReturned(String brandName) {
        Arrays.stream(brandName.trim().split(",")).forEach(brand -> {
            Assert.assertTrue(response.body().asString().contains(brand));
        });
    }

    @And("^I verify (.*) are returned as expected")
    public void iVerifyGroupFieldsContainCorrectValues(String fields) throws IOException {
        List<String> groupsFields = new ArrayList<String>(Arrays.asList(fields.trim().split(",")));
        for (String field : groupsFields) {
            String expectedValue = apiHelper.getEnvProperties(categoryId + "." + field);
            String actualValue = response.jsonPath().get("groups." + field).toString();
            Assert.assertEquals(expectedValue, actualValue);
        }
    }

    @And("^I verify response schema is as expected$")
    public void iVerifyResponseSchema() throws IOException {
        if(Env.isEnvStatic1OrProd() && locale.equals("hhk02")){
            System.out.println("Skipping this test on Static 1 and Prod due to data limitation");
        } else {
            String pathToExpectedJson = String.format("data/jsons/%s_%s_%s.json", endpoint.getFilePrefix(), locale.toUpperCase(), categoryId);
            JsonObject actual = serviceCaller.getJsonValue(response.body().asString());
            JsonObject expected = serviceCaller.getJsonValue(serviceCaller.getExpectedJson(pathToExpectedJson));
            Assert.assertEquals(expected, actual);
        }
    }

    @And("^I verify row (.*) for that category is not returned in response$")
    public void iVerifyRemovedRowsForThatCategoryIsNotReturnedInResponse(String markedRemoved) {
        Assert.assertFalse(response.body().asString().contains(markedRemoved));
    }

    @And("^I verify the (.*) field is empty$")
    public void iVerifyFieldIsEmpty(String path) {
        if(Env.isEnvStatic1OrProd()){
            System.out.println("Skipping this test on Static 1 and Prod due to data limitation");
        } else {
            Assert.assertTrue(response.jsonPath().get(path).toString().equalsIgnoreCase("[]"));
        }
    }

    @And("^I verify (.*) values are as expected$")
    public void iVerifyExpectedValuesInResponse(String groupFields) throws IOException {
        List<String> groupsFields = new ArrayList<String>(Arrays.asList(groupFields.trim().split(",")));
        for (String fields : groupsFields) {
            String expectedValue = apiHelper.getEnvProperties(categoryId + "." + fields);
            String actualValue = response.jsonPath().get("groups." + fields).toString();
            Assert.assertEquals(expectedValue, actualValue);
        }
    }

    @And("^I verify the dimensions is in ascending order$")
    public void iVerifyTheDimensionIsInAscendingOrder() {
        String dimensions = "";
        List allDimensions = response.jsonPath().getList("groups.dimensions");
        for (Object dimension : allDimensions) {
            if (dimension.toString().contains(",")) {
                dimensions = dimension.toString().replaceAll("[\\[\\]\\s]", "");
                break;
            }
        }
        Assert.assertTrue(serviceCaller.assertAppliedDimensionsInAscendingOrder(dimensions));
    }

    @After()
    public void postTestResultsToSlack(Scenario scenario) throws Exception {
        if (scenario.isFailed() || !scenario.isFailed()) {
            serviceCaller.addTestResults(scenario);
        }

        if (scenario.isFailed()) {
            failedScenario = true;
        }

        if (serviceCaller.getTestResults().size() == 7) {
            String environment = System.getProperty("env");
            if (failedScenario || !serviceCaller.waitForServiceUp()) {
                colourCode = "eb1717";
                if (failedScenario) {
                    for (Map.Entry<String, String> entry : serviceCaller.getTestResults().entrySet()) {
                        if (entry.getValue().equals("FAILED")) {
                            serviceCaller.addFailedTest(entry.getKey(), entry.getValue());
                        }
                    }
                    String failedResults = "Acceptance Tests - FAILED  :worried: (" + environment + ") \n " + serviceCaller.getFailedTests().toString().replaceAll("[\\[\\](){}]", "");
                    serviceCaller.postToSlack(failedResults.replaceAll(",", "\n"), colourCode);
                } else if (!serviceCaller.waitForServiceUp()) {
                    serviceCaller.postToSlack("Acceptance Tests - FAILED | Service not up", colourCode);
                }
            } else {
                if (!failedScenario && stageValue.equals("deployed") && pipelineValue.equals("true")) {
                    colourCode = "36a64f";
                    serviceCaller.postToSlack("Acceptance Tests - PASSED  :+1: (" + environment + ")", colourCode);
                }
            }
        }
    }
}