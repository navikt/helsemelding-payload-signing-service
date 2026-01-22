# helsemelding-payload-signing-service

The service exposes the `POST /paylod` endpoint.  
A client service sends an outgoing message to the endpoint as a `byteArray` in `PayloadRequest`, and receives the signed message back in `PayloadResponse`.

Messages are signed with a certificate that is stored in a keystore together with the corresponding private key.  
The keystore file and the file password are retrieved from **Kubernetes Secrets**.

---

## Architecture
The service consists of the following main components:

- **ProcessingService**  
  Processes `PayloadRequest`. The service is designed with the assumption that there may be different processing for incoming and outgoing messages.

- **SigningService**  
  Signs XML documents.

- **KeyStoreManager**  
  Reads the keystore and retrieves certificates and private keys from it.

## Modules

| Module                   | Description                                        |
|--------------------------|----------------------------------------------------|
| `payload-signing-model`  | Shared model definitions used by client and server |
| `payload-signing-client` | Reusable HTTP client for Payload Signing Service   |

### payload-signing-model
Shared model definitions used by client and server.  
See: [payload-signing-model/README.md](payload-signing-model/README.md)

### payload-signing-client
Reusable HTTP client for Payload Signing Service.
See: [payload-signing-client/README.md](payload-signing-client/README.md)


