# payload-processing-service

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
