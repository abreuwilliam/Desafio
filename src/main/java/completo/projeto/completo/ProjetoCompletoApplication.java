package completo.projeto.completo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableAspectJAutoProxy
@OpenAPIDefinition(info = @Info(title = "API", version = "1.0"))
@EnableCaching
public class ProjetoCompletoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjetoCompletoApplication.class, args);

	}
}
