package runners4;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features/sgsm.feature",
        glue = {"steps4"},
        plugin = {"pretty", "html:target/cucumber-reports.html"},
        monochrome = true
        
)
public class sgsmrun {
}
