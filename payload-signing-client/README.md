# payload-signing-client

Kotlin client library for interacting with the Payload Signing Service.

## Purpose
Simplify integration with `helsemelding-payload-signing-service` by hiding HTTP, serialization and error handling from consumers

## Usage
```
val scope = "api://dev-gcp.helsemelding.payload-signing-service/.default"
val payloadSigningServiceUrl = "https://helsemelding-payload-signing-service.intern.dev.nav.no"

val scopedClient = scopedAuthHttpClient(scope)
val payloadSigningClient = HttpPayloadSigningClient(scopedClient, payloadSigningServiceUrl)

val payloadRequest = PayloadRequest( ... )

val response = payloadSigningClient.signPayload(payloadRequest)

if(response.isRight()) {
    val payloadResponse = response.getOrNull()
    ...
} else {
    val error = response.leftOrNull()
    ...
}
```

## Dependencies

* Uses shared models from `payload-signing-model`
* Calls the HTTP API exposed by `helsemelding-payload-signing-service`
