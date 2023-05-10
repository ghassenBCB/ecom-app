package org.sid.ecomapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor
class Product{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private double price;
}

interface ProductRepository extends JpaRepository<Product, Long>{
}

@Controller
class ProductController{
	@Autowired
	private ProductRepository productRepository;

	@GetMapping("/")
	public String index(){
		return "index";
	}

	@GetMapping("/products")
	public String products(Model model){
		model.addAttribute("products", productRepository.findAll());
		return "products";
	}

}



@Controller
class SecurityController{

	@Autowired
	private AdapterDeploymentContext adapterDeploymentContext;

	@GetMapping("/logout")
	public String logout(HttpServletRequest request) throws ServletException {
		request.logout();
		return "redirect:/";
	}

	@GetMapping("/changePassword")
	public String cpw(RedirectAttributes attributes, HttpServletRequest request,
					  HttpServletResponse response) throws ServletException {
		HttpFacade facade=new SimpleHttpFacade(request,response);
		KeycloakDeployment deployment = adapterDeploymentContext.resolveDeployment(facade);
		attributes.addAttribute("referrer", deployment.getResourceName());
		return "redirect:" + deployment.getAccountUrl() + "/password";
	}
}

@SpringBootApplication
public class EcomAppApplication {

	public static void main(String[] args) { SpringApplication.run(EcomAppApplication.class, args); }

	@Bean
	CommandLineRunner start(ProductRepository productRepository){
		return args -> {
			productRepository.save(new Product(null, "HP 564", 8000));
			productRepository.save(new Product(null, "Imprimante LX 11", 7000));
			productRepository.save(new Product(null, "Smart Phone Iphone X", 6000));
			productRepository.findAll().forEach(
					p -> {
						System.out.println("Le nom du produit: "+p.getName() + " et le prix est : "+p.getPrice());
					}
			);

		};
	}

}

@Configuration
class keycloakConfig{
	@Bean
	KeycloakSpringBootConfigResolver configResolver(){
		return new KeycloakSpringBootConfigResolver();
	}
}

@KeycloakConfiguration
class keycloakSpringSecurityConfig extends KeycloakWebSecurityConfigurerAdapter{
	@Override
	protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception{
		auth.authenticationProvider(keycloakAuthenticationProvider());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		super.configure(http);
		http.authorizeRequests().antMatchers("/products/**").authenticated();
	}
}
