package com.example.demo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Controller
public class MainController {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/")
    public String index(Model model, OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient authorizedClient = this.getAuthorizedClient(authentication);
        model.addAttribute("userName", authentication.getName());
        model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());
        return "index";
    }


	@GetMapping("/userinfo")
    public String userinfo(Model model, OAuth2AuthenticationToken authentication, @AuthenticationPrincipal Jwt principal) {
        OAuth2AuthorizedClient authorizedClient = this.getAuthorizedClient(authentication);
        System.out.println("scopes: " + authorizedClient.getAccessToken().getScopes());
        System.out.println("ExpiresAt: " + authorizedClient.getAccessToken().getExpiresAt());
        System.out.println("TokenType: " + authorizedClient.getAccessToken().getTokenType());
        System.out.println("TokenValue: " + authorizedClient.getAccessToken().getTokenValue());
        Map userAttributes = Collections.emptyMap();
        String userInfoEndpointUri = authorizedClient.getClientRegistration()
            .getProviderDetails().getUserInfoEndpoint().getUri();
        
        if("solidcommunity".equals(authentication.getAuthorizedClientRegistrationId())) {
        	
        	userAttributes = new HashMap<Object,Object>();
            OAuth2User user = authentication.getPrincipal();

        	String obj = WebClient.builder()
                    .build()
                    .get()
                    .uri(user.getName())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        	
        	userAttributes.put("profile", obj );
        	
        }else {
            if (!StringUtils.isEmpty(userInfoEndpointUri)) {	// userInfoEndpointUri is optional for OIDC Clients
                userAttributes = WebClient.builder()
                    .filter(oauth2Credentials(authorizedClient))
                    .build()
                    .get()
                    .uri(userInfoEndpointUri)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            }
        }
        
        model.addAttribute("userAttributes", userAttributes);
        return "userinfo";
    }

    private OAuth2AuthorizedClient getAuthorizedClient(OAuth2AuthenticationToken authentication) {
        return this.authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(), authentication.getName());
    }

    private ExchangeFilterFunction oauth2Credentials(OAuth2AuthorizedClient authorizedClient) {
        return ExchangeFilterFunction.ofRequestProcessor(
            clientRequest -> {
                ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizedClient.getAccessToken().getTokenValue())
                    .build();
                return Mono.just(authorizedRequest);
            });
    }
}
