package it.unipi.myfuture.myfuture_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MyfutureBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyfutureBackendApplication.class, args);
	}

}
