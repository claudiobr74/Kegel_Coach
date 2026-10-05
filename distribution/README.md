# Distribuição para teste interno

Identificador oficial: `com.kegel_coach.myapp`. Versão atual: 1.0.1, código 2; conferir na Play Console se esse código já foi utilizado antes de enviar.

## AAB

`./gradlew :app:bundleRelease` gera `app/build/outputs/bundle/release/app-release.aab`.
Sem as variáveis abaixo, o bundle é **não assinado** e não pode ser enviado à Play Store.

- `KEGEL_UPLOAD_STORE`: caminho absoluto da chave privada de upload `.jks`/`.keystore`.
- `KEGEL_UPLOAD_STORE_PASSWORD`: senha do arquivo.
- `KEGEL_UPLOAD_ALIAS`: alias da chave.
- `KEGEL_UPLOAD_KEY_PASSWORD`: senha da chave.

Definir esses valores no ambiente local ou como secrets da CI; não colocar chaves/senhas no repositório. O workflow de release manual usa secrets para gerar o AAB assinado.

## Certificados enviados

O ZIP fornecido contém certificados **públicos**, sem chave privada:
- `deployment_cert.der`: SHA-256 `D4:5A:C3:1F:50:4E:7E:94:72:A2:3C:53:28:19:7F:7D:89:30:43:6B:EB:80:41:92:86:6D:E6:D0:01:A3:B3:03`.
- `hybrid_classical_cert.der`: SHA-256 `90:B1:0A:91:81:C5:BA:C4:37:A7:F6:FA:95:96:04:70:08:4F:C3:87:7D:32:60:8A:59:25:EA:89:8E:DA:0D:CF`.
- `hybrid_pqc_cert.der`: SHA-256 `FF:4E:7D:E4:20:D1:9E:08:34:07:0F:25:52:4C:B4:BC:97:83:7E:44:03:87:E6:6C:03:A2:02:78:41:17:CA:D4`.

O certificado de implantação corresponde ao JSON Digital Asset Links enviado. Ele não permite reconstruir a chave privada nem assinar o AAB. A chave de upload pode ser diferente da chave de assinatura gerenciada pelo Google; seu certificado deve estar registrado na Play Console.

## Chave de upload criada

O usuário autorizou a criação da chave em 5/10/2026. Alias: `kegel-coach-upload`. A versão atual foi compilada e assinada com essa mesma chave. O certificado público está em `Kegel_Coach_upload_certificate.pem`; a chave privada e a senha foram entregues em backup separado e não são armazenadas no Git. Se outra chave de upload estiver registrada na Play Console, registrar/redefinir o certificado antes do envio.

## Digital Asset Links

`assetlinks.json` preserva a associação fornecida. A ativação requer um domínio HTTPS definido, publicação do JSON em `/.well-known/assetlinks.json` nesse domínio e filtro `android:autoVerify` compatível no Manifest. Nenhum domínio foi fornecido, portanto links verificados não foram habilitados arbitrariamente.

## Antes de enviar

Validar assinatura com `jarsigner -verify -verbose -certs app-release.aab`, confirmar pacote/código de versão e registrar o upload certificate. Na Play Console preencher declarações exigidas, incluindo o serviço foreground `specialUse`, e distribuir na faixa de teste interno. A CI não publica automaticamente na loja.
