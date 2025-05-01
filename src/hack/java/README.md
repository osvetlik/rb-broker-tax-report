# Hack

I didn't want to add a dependency just because you can't prevent the openapi generator from adding

```java
import org.openapitools.jackson.nullable.JsonNullableModule;
```

to the generated ApiClient.
