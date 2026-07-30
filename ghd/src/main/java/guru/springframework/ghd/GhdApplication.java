package guru.springframework.ghd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "guru.springframework.ghd.repositories")
@EntityScan(basePackages = "guru.springframework.ghd.entities")
public class GhdApplication {

	public static void main(String[] args) {
		SpringApplication.run(GhdApplication.class, args);
	}

}
