package config;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import helpers.FileHelper;
import org.junit.AfterClass;
import org.junit.runner.RunWith;


@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty", "html:target/cucumber-reports"},
        dryRun = false,
        features = "src/test/resources/features",
        glue = {"stepdefs"},
        tags = {"@Regression"}
        )

public class RunCukesAcceptanceTest {

        @AfterClass
        public static void teardown() {
                FileHelper.removeTemp();
        }
}