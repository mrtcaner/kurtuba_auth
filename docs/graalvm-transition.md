# GraalVM Native Transition

## Purpose

This document records the first GraalVM native-image transition pass for `kurtuba-auth`.

It is intentionally written as a transition story, not only as final operating instructions. The goal is to preserve what was learned: where the migration started, which failures appeared, why they happened, what changed in the codebase, and what still needs attention before treating native execution as a routine deployment path.

## Starting Point

The service was already a Java 21 Spring Boot application with a fairly real production shape:

- Spring MVC controllers for user-facing and admin APIs
- Spring Security with JWT resource-server behavior
- persisted users, tokens, registered clients, and metadata through JPA
- Spring Session JDBC for browser/admin sessions
- Thymeleaf admin pages
- mail and Twilio SMS integrations
- Redis-backed rate limiting through Bucket4j
- Springdoc/OpenAPI and Swagger UI
- scheduled message delivery jobs

That meant the transition was not just a question of building a native binary. The useful target was a native binary that still ran the real application surface.

The first successful direction was to add a dedicated Maven native profile and use GraalVM 21 with Spring Boot AOT processing, while keeping the normal Spring Boot application model intact.

## Native Build Path

The native build is handled through the Maven `native` profile using the GraalVM Native Build Tools plugin.

Typical build command:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-21.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/graalvm-21.jdk/Contents/Home/bin:$PATH \
SPRING_PROFILES_ACTIVE=native \
mvn -Pnative -DskipTests package
```

Typical run command:

```bash
SPRING_PROFILES_ACTIVE=native ./target/kurtuba_auth
```

The native profile currently keeps the runtime feature set close to the normal application:

- rate limiting is enabled
- Redis client support is enabled
- logging aspect is enabled
- Springdoc API docs are enabled
- Swagger UI is enabled
- Spring Session JDBC is enabled
- scheduled jobs are not disabled by the native profile

The one intentional native-profile disablement is:

```yaml
spring:
  data:
    redis:
      repositories:
        enabled: false
```

That is because the app uses Redis through Lettuce/Bucket4j infrastructure, not Spring Data Redis repositories. Disabling Redis repositories avoids unnecessary repository scanning without disabling Redis itself.

## Problem 1: Raw `ResponseEntity` and JSON Rendering

One of the first runtime failures appeared on endpoints returning raw `ResponseEntity`.

The visible symptom was:

```text
org.springframework.web.HttpMediaTypeNotAcceptableException: No acceptable representation
```

The failure appeared for `/auth/user/info`, but the underlying risk was broader: raw response types give Spring and Jackson less useful type information at runtime. On the JVM that often still works because reflection is broadly available. In a native image, missing or ambiguous type information can turn into message-converter failures.

The fix was to make controller and exception-handler responses explicit:

- `ResponseEntity<TokensResponseDto>`
- `ResponseEntity<RegistrationResponseDto>`
- `ResponseEntity<UserDto>`
- `ResponseEntity<UserMetaChangeDto>`
- `ResponseEntity<ResponseErrorDto>`
- `ResponseEntity<Void>`
- other concrete response types where appropriate

This made the API surface easier for Spring MVC, Jackson, and native-image analysis to reason about.

### Watch Point

Most user-facing raw responses were fixed during the transition. A small number of admin API methods still return raw `ResponseEntity` and should be cleaned up before considering the migration fully polished.

## Problem 2: Runtime Hints for Reflection, Resources, and Serialization

Native image compilation removes the assumption that arbitrary classes, constructors, fields, and resources are available through reflection at runtime.

The project now imports a central runtime-hints registrar through the application entry point. The hints cover the concrete places where runtime reflection or resource lookup was required:

- custom Jakarta validation annotations
- API/admin DTOs that Jackson serializes or deserializes
- JJWT implementation classes loaded dynamically
- Twilio SDK response/error models
- Google ID token parser classes
- Springdoc conversion-service internals
- libphonenumber metadata resources
- the JWK resource
- selected session/security serialization types

The important lesson is that hints should be tied to observed framework/library behavior, not used as a blanket replacement for design fixes. Where the application could be made more explicit, such as typed controller responses, that was preferred.

## Problem 3: Spring Session and Java Serialization

Spring Session JDBC initially failed because session attributes were being serialized in a way that required Java serialization metadata unavailable in native mode.

The visible failures involved types such as:

- `CopyOnWriteArrayList`
- `FlashMap`
- Spring Security authentication/session types

The durable fix was to switch Spring Session JDBC attribute conversion to JSON using a dedicated bean named `springSessionConversionService`.

The session mapper:

- copies the application `ObjectMapper`
- registers Spring Security Jackson modules
- enables Spring Security default typing for session objects
- adds mixins for a few session container types that need explicit type metadata
- keeps the existing JDBC table shape and `ATTRIBUTE_BYTES` column

This avoided a database migration while making native session handling practical.

### Watch Point

Old `SPRING_SESSION` rows written with Java serialization may not deserialize after switching to JSON-backed session attributes.

In this application, Spring Session is only used for admin pages, not user-facing mobile/API traffic. The practical impact is low: old admin sessions can be cleared or admins can simply log in again after the transition.

## Problem 4: Thymeleaf Admin Pages

Thymeleaf is supported in Spring native applications, but the original admin pages were passing rich framework objects and relying on template-time method/property access.

The failures were not because Thymeleaf itself was unusable. The issue was that templates were asking native execution to introspect objects such as pages, collections, registered clients, and nested DTO structures in ways that were fragile under native-image constraints.

The fix was to make admin page models more template-friendly:

- expose lists directly instead of `Page` objects
- compute pagination values in controllers
- expose booleans such as `hasUsers`, `hasTokens`, `hasMessages`, and `hasClients`
- map registered clients to a simple `RegisteredClientListRowDto`
- add simple DTO helper methods such as `hasUserRoles()`

This kept Thymeleaf in place but moved reflection-heavy decisions out of the templates and into normal Java code.

### Lesson

For native execution, Thymeleaf pages should receive simple model values. Templates should render decisions that controllers already prepared instead of navigating complex framework objects.

## Problem 5: Redis and Rate Limiting

The first native path temporarily reduced moving parts, but the real target needed rate limiting back on.

Redis support was restored without enabling Spring Data Redis repositories. The app uses:

- Lettuce `RedisClient`
- Bucket4j Redis proxy manager
- a Spring MVC rate-limit interceptor

The native profile now enables `kurtuba.rate-limit.enabled=true`, while keeping Redis repository scanning disabled.

This path was validated against the password-change rate limit flow.

## Problem 6: Springdoc and Swagger UI

Swagger/OpenAPI caused several distinct issues.

First, `/v3/api-docs` needed reflection access to Spring conversion-service internals used by springdoc. Runtime hints were added for the relevant `GenericConversionService` types.

Second, a dependency mismatch caused:

```text
java.lang.NoSuchMethodError: io.swagger.v3.oas.annotations.media.Schema.$dynamicRef()
```

The cause was an older direct Swagger annotations dependency conflicting with the newer springdoc version. The cleaner fix was to remove the stale direct dependency and let the Springdoc starter bring the compatible Swagger dependencies.

Third, Swagger UI initially rendered the Petstore fallback. The API docs JSON endpoint worked, but the UI could not load its local config endpoint automatically. Security configuration was updated to permit:

- `/v3/api-docs`
- `/v3/api-docs/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/webjars/**`

After that, Swagger UI picked up the local API docs as expected.

### Lesson

Springdoc support should stay on the normal dependency path. Hiding it in a native-only profile made the setup harder to reason about. Version alignment and security matchers were the important pieces.

## Problem 7: Logging Aspect and Spring Data Proxies

Re-enabling the logging aspect caused native startup to fail while Spring was creating a repository proxy.

The visible native-image error complained about missing reflection metadata for a dynamic proxy involving `MessageJobRepository`.

The problem was not that `@Service`, `@Controller`, or Spring components in general needed manual proxy hints. The logging aspect pointcut was too broad, so AOP evaluated repository infrastructure that the aspect did not actually need to log.

The fix was to narrow the pointcut to application service and controller packages:

```java
((within(com.kurtuba..service..*) && execution(* *(..))) ||
 (within(com.kurtuba..controller..*) && execution(* *(..))))
```

Repository proxies are no longer pulled into the aspect path, and the aspect remains enabled under the native profile.

### Watch Point

`LoggingAspect.toLogNode` still uses Jackson `valueToTree` as a fallback for arbitrary objects. It already handles servlet requests, responses, binding results, principals, simple values, arrays, iterables, and maps more carefully, but the safest long-term direction is to keep it conservative:

- log request DTOs and simple values
- summarize framework objects
- avoid serializing JPA entities, proxies, streams, resources, or arbitrary third-party SDK objects

The current tests with email, SMS, rate limit, Swagger, and admin pages passed with the aspect enabled.

## Problem 8: Twilio SDK Models

Twilio verification calls introduced native/Jackson construction issues for SDK response models.

Runtime hints were added for:

- Twilio REST exceptions
- SMS message model
- Verify `Verification`
- Verify `VerificationCheck`
- relevant nested enum/model types

The Twilio error handling was also made more null-safe by comparing API error codes through `Objects.equals`.

The verified result:

- real Twilio SMS send worked
- failed Twilio attempts were mapped and handled correctly

## Problem 9: Google ID Token Parsing

Google social registration/token parsing required runtime hints for Google API Client token classes.

Hints were added for:

- `JsonWebToken.Header`
- `JsonWebSignature.Header`
- `JsonWebToken.Payload`
- `IdToken.Payload`
- `GoogleIdToken.Payload`

The lesson is similar to Twilio: SDKs that parse JSON into their own model classes may need explicit native metadata even when the application code itself does not directly instantiate those classes.

## Other Cleanup

One small cleanup replaced `org.apache.commons.lang3.NotImplementedException` with the standard `UnsupportedOperationException` in unfinished SMS notification code.

This was not a central GraalVM fix, but it removes an unnecessary dependency-shaped edge from an incomplete path.

## Current Validation Status

The native transition has been validated through both build-time and runtime checks.

Build-time checks performed during the transition included:

- regular compile
- Spring Boot AOT processing under the native profile
- full native package builds

Runtime checks performed manually included:

- native application startup
- email send through Mailpit
- rate limiting on password change
- Swagger UI rendering
- `/v3/api-docs` JSON output
- real Twilio SMS send
- failed Twilio attempt handling
- admin page navigation
- the same flows while `LoggingAspect` was enabled

This is a strong smoke-test set, but it is not the same thing as full regression coverage.

## Known Warnings and Non-Issues

Some warnings appeared during native runs that are not currently treated as blockers:

- Flyway may warn about scanning `/db/migration` with an unsupported native resource protocol.
- Logback may warn about unknown or mismatched versions.
- Hibernate warns that the Spring Session composite id class does not override `equals()` and `hashCode()`.

These should be watched, but they were not the source of the runtime failures fixed in this transition.

## Remaining Watch List

Before treating native mode as a normal release target, keep the following items visible:

- Type the remaining raw admin `ResponseEntity` methods.
- Move any literal third-party credentials back to environment variables or secret management before committing or releasing.
- Decide whether deployment should clear old admin Spring Session rows.
- Keep `LoggingAspect.toLogNode` conservative as new controller/service method arguments are introduced.
- Retest Google social registration after any dependency or hint changes.
- Retest hosted admin POST/redirect flows after any Spring Session or Thymeleaf changes.
- Keep Springdoc and Swagger dependencies aligned through the Springdoc starter.
- Avoid broad AOP pointcuts that include repositories or framework infrastructure.

## Final State of the First Transition Pass

The native transition is no longer blocked by the major runtime issues discovered during the first pass.

The application now has a native build path that keeps the important runtime pieces active:

- API controllers
- admin pages
- Swagger/OpenAPI
- rate limiting
- Redis client integration
- Spring Session JDBC
- logging aspect
- mail
- Twilio SMS
- JWT/JWKS behavior

The main remaining work is hardening and documentation: clean up the last raw response types, remove test secrets from committed config, document the build/run/smoke-test workflow, and keep the watch list close to future native changes.
