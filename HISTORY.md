# DLSaver History

## 2026-05-25

- Release Android 1.15.4 com ajustes no modo Labs de preview de vídeo nos resultados.
- Downloads de vídeo agora usam a melhor qualidade disponível por padrão; seleção manual de qualidade virou opção experimental no Labs.
- Player de vídeo recebeu dimensões estáveis, controles sobrepostos no card, suporte a picture-in-picture e retorno mais seguro pela notificação.
- Preview de vídeo pode tocar o próximo resultado automaticamente quando a opção experimental estiver ativa.
- Busca passou a pré-carregar até 50 resultados por consulta e manter cache seguro em memória para consultas repetidas.
- Notificação de mídia passa a usar a thumbnail do vídeo em reprodução quando o preview vem da busca.

## 2026-05-04

- Inicializado repositório Git local para facilitar rastreio das próximas alterações.
- Release Android ajustado para declarar explicitamente ABI `arm64-v8a`, já que o APK atual não é universal.
- Scripts de build/publicação passaram a nomear o APK como `dlsaver_v<version>_arm64-v8a.apk`.
- Manifest de atualização passou a carregar campos `abi` e `abis`.
- Adicionado share target estático para melhorar a presença do DLSaver no Android Sharesheet ao receber links/texto.
- Download de áudio ficou mais defensivo: se o `aria2c` falhar ou não produzir arquivo, tenta novamente sem acelerador.
- Áudios baixados tentam conversão segura para FLAC via FFmpeg; se a conversão falhar, o arquivo original é mantido.
- `.gitignore` atualizado para evitar versionar caches, build local, APKs e arquivos sensíveis.
- Checagem de duplicidade ficou menos agressiva: downloads só são ignorados por URL exata, evitando que músicas longas/mixes com nomes parecidos sejam pulados antes de iniciar.
- Ao escolher baixar um item que já existe no mesmo formato, o app agora mostra diálogo oferecendo baixar no formato alternativo.
- Downloads de vídeo agora consultam qualidades disponíveis: mostra seletor quando há várias opções 720p+, ou baixa direto avisando quando só existe uma qualidade/abaixo de 720p.
