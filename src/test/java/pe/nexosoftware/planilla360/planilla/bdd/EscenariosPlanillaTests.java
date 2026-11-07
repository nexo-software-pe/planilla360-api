package pe.nexosoftware.planilla360.planilla.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/** Ejecuta los escenarios Gherkin en español de las reglas de negocio de planilla. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/planilla")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "pe.nexosoftware.planilla360.planilla.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "summary")
public class EscenariosPlanillaTests {
}
