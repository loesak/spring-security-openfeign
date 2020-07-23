# spring-security-openfeign
Bringing back Spring Security support for OpenFeign until they do

Current implementation of Spring Cloud Security's OAuth2FeignRequestInterceptor is based on the now legacy/deprecated Spring Security OAuth which was a community developed Spring library.

No support has been added to Spring Cloud Security for Open Feign to backport the functionality to use the latest OAuth support built directly into Spring Security starting in Spring Security version 5.

See https://github.com/spring-cloud/spring-cloud-security/issues/173

For those of us that rely on this capability, and that do not want to rewrite a large portion of their codebase to use WebClient, thus significantly increasing their projects lines-of-code, this is for you.
