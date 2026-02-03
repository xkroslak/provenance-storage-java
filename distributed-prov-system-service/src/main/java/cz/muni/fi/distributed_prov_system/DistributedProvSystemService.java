package cz.muni.fi.distributed_prov_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DistributedProvSystemService {

    public static void main(String[] args) {
        SpringApplication.run(DistributedProvSystemService.class, args);
    }

}
