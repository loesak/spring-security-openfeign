# spring-security-openfeign
OpenFeign request interceptor that provides functionality similar to Spring Cloud Security's OAuth2FeignRequestInterceptor. 

Current implementation of Spring Cloud Security's OAuth2FeignRequestInterceptor is based on the now legacy/deprecated Spring Security OAuth which was a community developed Spring library.

No support has been added to Spring Cloud Security for Open Feign to backport the functionality to use the latest OAuth support built directly into Spring Security starting in Spring Security version 5.

See https://github.com/spring-cloud/spring-cloud-security/issues/173

For those of us that rely on this capability, and that do not want to rewrite a large portion of their codebase to use WebClient, thus significantly increasing their projects lines-of-code, this is for you.

# Features & Limitations

* Only supports client credentials grant type by default. You can configure it to support others if needed. Review the documentation on OAuth2AuthorizedClientManager at https://docs.spring.io/spring-security/site/docs/5.3.2.RELEASE/reference/html5/#oauth2client. You can pass in your own in the constructor if you so choose.

* Does not support OAuth Passthrough (a.k.a. Bearer Token Propagation).

* Failure scenarios are probably not handled properly

# Install
Add the following to your maven dependencies. Make sure to check the releases or Maven Central for the latest version.
```xml
<dependency>
  <groupId>org.loesak.springframework.security.openfeign</groupId>
  <artifactId>spring-security-openfeign</artifactId>
  <version>0.1.0</version>
</dependency>
```

# TODO:

* Add support for OAuth Passthrough (a.k.a. Bearer Token Propagation).
    
    See https://docs.spring.io/spring-security/site/docs/5.3.2.RELEASE/reference/html5/#bearer-token-propagation
    
# Usage

In addition to configuring your Spring Security OAuth Resource server, you can add a custom configuration to your OpenFeign clients to add a Feign Request Interceptor. The below uses a custom configuration per client meaning the fetching of an access token will only occur for calls from this Feign client. This is nice if you have other clients that interact with different services, or have different scopes for different APIs from the same service. This helps prevent unnecessary access token leaks.


```java
@FeignClient(
		url = "https://www.example.com/api",
		configuration = ExampleClient.Configuration.class)
public interface ExampleClient {

	@RequestMapping(
            method = RequestMethod.GET, 
            path = "/getWidget", 
            produces = MediaType.APPLICATION_JSON_VALUE)
	Widget getWidget(@RequestParam("id") String widgetId);

	class Configuration {
		@Bean
		public OAuth2FeignRequestInterceptor oAuth2FeignRequestInterceptor(
				ClientRegistrationRepository clientRegistrationRepository,
				OAuth2AuthorizedClientService authorizedClientService) {
			return new OAuth2FeignRequestInterceptor(
					clientRegistrationRepository,
					authorizedClientService,
					"example-dot-com-widget-read");
		}
	}

}
```

The string in the third argument of the OAuth2FeignRequestInterceptor constructor is the client registration ID of the registered client containing the appropriate credentials and scopes.

```yaml
---
spring:
  security:
    oauth2:
      client:
        registration:
          example-dot-com-widget-read:
            provider: okta
            client-id: ${my-client-id}
            client-secret: ${my-client-secret}
            authorization-grant-type: client_credentials
            scope: example-api:widget:read
```