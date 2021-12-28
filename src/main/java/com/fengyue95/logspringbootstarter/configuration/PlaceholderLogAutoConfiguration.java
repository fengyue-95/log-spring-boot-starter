package com.fengyue95.logspringbootstarter.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "placeholder.log",value = "enable",matchIfMissing = true)
public class PlaceholderLogAutoConfiguration {

}
