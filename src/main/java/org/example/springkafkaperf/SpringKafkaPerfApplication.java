package org.example.springkafkaperf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringKafkaPerfApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringKafkaPerfApplication.class, args);
    }

}
