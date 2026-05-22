package io.sapient;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAdminServer
public class JavaSapientTestHarnessApplication {
    public static void main(String[] args) {
        SpringApplication.run(JavaSapientTestHarnessApplication.class, args);
    }
}
