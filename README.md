# 2026-09-09 | Bootiful Spring Security @ KCDC


## part 1
* start.spring.io
* auth(entication,orization)
* `MeController`
* default user/pw
* AuthenticationManager hierarchy
* `InMemoryUserDetailsService`
* aside: `PasswordEncoder` (Password4j, Bouncycastle, Bcrypt is the onyl good OOTB encoder, etc.)
* `JdbcUserDetailsManager`
* password history
* password migration 
* Rob: "but passwords... bad. :grunt:"
* `SecurityFilterChain` => `Customizer<HttpSecurity>`
* One Time Tokens
* WebAuthn
* the system knows who we are, but who can access what?
* authorization
* convenience methods 
* `access()`
* MFA globally
* `AuthorizationManagerFactory` as a pluggable implementation for convenience methdos like `hasRole` and `hasAuthority`
* MFA on particular paths (&& `AuthorizationManagerFactory`)
* MFA duration
* nice.. but its all in one place. real systems have many moving parts. 
* why OAuth
	* brief interlude to talk about yelp.com having required folks to enter their gmail username/password to promote the socialization / gamification of reviews ~16 years ago
* delegated authoirzation, _but_ people started doing authentication with it 
* enter: OIDC
* Spring Auth Server: central place to vend and validate tokens
* setup OAuth client 
* setup OAuth client programatically 
* oauth client (http)
	* RestClient + header 
	* RestClient + interceptor && attributes
	* declarative interface client http service thingamabobber 
	* `OAuth*GroupConfigurer`
* resource server (http)
* gateway - oauth client
* `ui/index.html`
* TestJars

## part 2: protocol parade 
* improved test support: TestJars
* multitenancy w/ Arconia + OAuth 
* is the entire world just HTTP? 
* GraphQL + "defense in depth" method security (`@EnableMethodSecurity` vs the old  `@EnableGlobalMethodSecurity`)
* MCP 
* gRPC 
* Messaging (AMQP + Spring Integration)
* Spring Shell + Device Code Flow
* Desktop + PKCE + JavaFX

## Changelog

- 2026-09-09 16:34:39 - the outlien rob and i r working on