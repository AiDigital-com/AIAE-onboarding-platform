// Marks Vite's content-hashed /assets/** build output as immutable and publicly
// cacheable for a year: a new deploy always ships new hashed
// filenames rather than mutating existing ones, so a stale cached copy under the
// old filename is simply never requested again. Coexists with — does not
// replace — Spring Boot's own default static resource handler, so unhashed files
// (e.g. root-level logo/illustration images) keep the framework default.

package com.aidigital.aionboarding.config;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Applies immutable, long-lived caching to the built frontend's hashed asset folder.
 */
@Configuration
public class StaticAssetCacheConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}
