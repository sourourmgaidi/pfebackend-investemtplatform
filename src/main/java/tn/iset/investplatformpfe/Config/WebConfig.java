package tn.iset.investplatformpfe.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path.profile-photos:uploads/profile-photos}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadPath);
        String uploadAbsolutePath = uploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/profile-photos/**")
                .addResourceLocations("file:" + uploadAbsolutePath + "/");
    }
}
