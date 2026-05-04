# DLSaver History

## 2026-05-04

- Inicializado repositório Git local para facilitar rastreio das próximas alterações.
- Release Android ajustado para declarar explicitamente ABI `arm64-v8a`, já que o APK atual não é universal.
- Scripts de build/publicação passaram a nomear o APK como `dlsaver_v<version>_arm64-v8a.apk`.
- Manifest de atualização passou a carregar campos `abi` e `abis`.
- Adicionado share target estático para melhorar a presença do DLSaver no Android Sharesheet ao receber links/texto.
- Download de áudio ficou mais defensivo: se o `aria2c` falhar ou não produzir arquivo, tenta novamente sem acelerador.
- Áudios baixados tentam conversão segura para FLAC via FFmpeg; se a conversão falhar, o arquivo original é mantido.
- `.gitignore` atualizado para evitar versionar caches, build local, APKs e arquivos sensíveis.
