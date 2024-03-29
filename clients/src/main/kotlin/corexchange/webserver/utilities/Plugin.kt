package corexchange.webserver.utilities

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import net.corda.client.jackson.JacksonSupport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class Plugin
{
    @Bean
    open fun registerModule(): ObjectMapper
    {
        return JacksonSupport.createNonRpcMapper()
    }
}