package pe.nexosoftware.planilla360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Planilla360ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(Planilla360ApiApplication.class, args);
    }
}
