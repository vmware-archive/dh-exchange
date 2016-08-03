package io.pivotal.cf.dh;

import feign.Feign;
import feign.gson.GsonDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.*;

@Configuration
class Config {

    @Bean
    public KeyPairGenerator keyPairGenerator() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(Util.ELLIPTIC_KEY_TYPE);
        kpg.initialize(Util.KEY_SIZE);
        return kpg;
    }

    @Bean
    public KeyFactory keyFactory() throws NoSuchAlgorithmException {
        return KeyFactory.getInstance(Util.ELLIPTIC_KEY_TYPE);
    }

    @Bean
    public QuoteRepository quoteRepository() {
        return Feign
                .builder()
                .decoder(new GsonDecoder())
                .target(QuoteRepository.class,
                        "https://query.yahooapis.com/v1/public");
    }
}