package runners3;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features/emp.feature",
        glue = {"steps3"},
        plugin = {"pretty", "html:target/cucumber-reports.html"},
        monochrome = true
        
)
public class Emprun {
}
