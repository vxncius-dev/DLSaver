powershell -ExecutionPolicy Bypass -File "D:\Vini\HTML\dlsaver\tools\publish-github-release.ps1" -FilePath "D:\Vini\Kotlin\DLSaver\releases\dlsaver_v1.14.8.apk" -Platform android

powershell -ExecutionPolicy Bypass -File .\publish-github-release.ps1 -SkipBuild

O APK Android atual do DLSaver não é universal. Inspecionei o release v1.15.0 e dentro do APK só existe lib/arm64-v8a, então ele é apenas para arm64-v8a.

Corrige o build/release Android para gerar um APK universal, incluindo pelo menos arm64-v8a, armeabi-v7a, x86 e x86_64 se o projeto suportar. Se a intenção for distribuir só arm64, então mantém assim, mas nomeia/declara o artefato como arm64-v8a em vez de universal.

Também atualiza o manifest de release para incluir a arquitetura/ABI do APK, por exemplo:
"abi": "arm64-v8a"
ou
"abis": ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"]

Assim o site e o updater conseguem mostrar a arquitetura correta sem chute.

Verifica a configuração Android/Gradle relacionada a splits por ABI, abiFilters e packaging. O objetivo é que o APK publicado corresponda ao que o manifest anuncia.


# AuthX Settings Design Pattern

Guia extraido apenas da tela de Configuracoes.

## Base visual

| Token | Valor | Uso |
| --- | ---: | --- |
| `background` | `#000000` | Fundo da tela, header e footer fixo |
| `textPrimary` | `#FFFFFF` | Titulo principal da tela |
| `rowText` | `#F4F4F4` | Texto principal dos itens da lista |
| `mutedText` | `#8A8A8A` | Sublabels, versao, copyright e icones secundarios |
| `mutedText80` | `#8A8A8A` com `alpha 0.8` | Texto de copyright |
| `divider` | `#242424` | Divisores entre itens |
| `success` | `#00E676` | Check/estado positivo |

## Fonte

A tela usa `FontFamily.SansSerif` em todos os textos.

Como nao ha tipografia customizada no tema, os tamanhos efetivos seguem os defaults do Material 3:

| Uso | Style | Peso | Tamanho | Line height |
| --- | --- | --- | ---: | ---: |
| Header/titulo da tela | `titleLarge` | `Bold` | `22sp` | `28sp` |
| Label do item | `titleMedium` | `Normal` | `16sp` | `24sp` |
| Sublabel do item | `bodyMedium` | `Normal` | `14sp` | `20sp` |
| Footer/copy | `bodyMedium.copy(fontSize = 12.sp)` | `Normal` | `12sp` | herdado de `bodyMedium` |

Regras:

- Use sempre Sans Serif.
- Header sempre em bold.
- Itens de lista sempre em peso normal.
- Nao use letter spacing customizado.
- Nao use caixa alta nos itens da lista.

## Custom Header

O header da tela de Configuracoes segue a mesma estrutura do `AuthXHeader`, com bottom padding maior.

Medidas:

| Propriedade | Valor |
| --- | ---: |
| Largura | `fillMaxWidth()` |
| Fundo | `#000000` |
| Status bar | `statusBarsPadding()` |
| Padding start | `20dp` |
| Padding end | `20dp` |
| Padding top | `28dp` |
| Padding bottom na Configuracoes | `18dp` |
| Padding bottom padrao do `AuthXHeader(title)` | `8dp` |
| Texto | `titleLarge`, `22sp`, bold, branco |

Proporcao recomendada:

```kotlin
Modifier
    .fillMaxWidth()
    .background(Color.Black)
    .statusBarsPadding()
    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 18.dp)
```

Use `18dp` de bottom quando a tela comecar direto em uma lista. Use `8dp` quando houver conteudo mais compacto logo abaixo.

## Lista de configuracoes

Cada item ocupa uma linha fixa de `72dp`.

| Propriedade | Valor |
| --- | ---: |
| Altura da linha | `72dp` |
| Padding horizontal | `20dp` |
| Alinhamento vertical | `CenterVertically` |
| Fundo | transparente sobre `#000000` |
| Divisor | `#242424` |
| Padding horizontal do divisor | `20dp` |
| Espaco entre label e sublabel | `2dp` |

Estrutura padrao:

```kotlin
Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(...)
            Spacer(modifier = Modifier.height(2.dp))
            Text(...)
        }
        Icon(...)
    }
    Divider(
        color = Color(0xFF242424),
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}
```

## Largura maxima dos itens

A listagem nao usa `maxWidth`: ela ocupa a largura total da tela.

Formula do conteudo:

- Linha sem icone: `largura da tela - 40dp`
- Linha com icone de `24dp`: `largura da tela - 40dp - 24dp`
- Linha com placeholder/check de `28dp`: `largura da tela - 40dp - 28dp`

Na pratica, em uma tela de `360dp`:

- Sem icone: `320dp`
- Com icone: `296dp`
- Com check/placeholder: `292dp`

Se for aplicar em telas maiores, mantenha o mesmo comportamento full width para configuracoes. So introduza `maxWidth` se a tela virar tablet/desktop; nesse caso use `560dp` a `640dp` centralizado, preservando `20dp` de padding interno.

## Icones

| Uso | Tamanho | Cor/opacidade |
| --- | ---: | --- |
| Icones de acao secundarios | `24dp` | `#8A8A8A` |
| Check de sucesso | `28dp` | `#00E676` |
| Placeholder quando nao ha check | `28dp` de largura | invisivel, apenas reserva espaco |

Regras:

- Use icones Material preenchidos (`Icons.Default.*`).
- Icones de import/export ficam no fim da linha.
- Check positivo usa `28dp`, maior que os icones comuns para reforcar estado.
- Quando uma linha pode alternar entre texto e check, mantenha o placeholder de `28dp` para a UI nao deslocar.

## Sublabels

| Propriedade | Valor |
| --- | ---: |
| Cor | `#8A8A8A` |
| Opacidade | `1.0` |
| Style | `bodyMedium` |
| Tamanho efetivo | `14sp` |
| Line height efetivo | `20sp` |
| Distancia do label | `2dp` |

Padrao:

```kotlin
Text(
    text = subtitle,
    color = Color(0xFF8A8A8A),
    style = MaterialTheme.typography.bodyMedium,
    fontFamily = FontFamily.SansSerif
)
```

## Footer e copy

O footer fica fixo no rodape, sobre o conteudo rolavel.

| Propriedade | Valor |
| --- | ---: |
| Alinhamento | `BottomCenter` |
| Largura | `fillMaxWidth()` |
| Fundo | `#000000` |
| Padding horizontal | `24dp` |
| Padding vertical | `20dp` |
| Texto | centralizado |
| Tamanho | `12sp` |
| Cor versao | `#8A8A8A` |
| Cor copyright | `#8A8A8A` com `alpha 0.8` |
| Espaco entre linhas | `6dp` |

O conteudo rolavel precisa reservar espaco para o footer:

```kotlin
.padding(bottom = 110.dp)
```

## Receita para repetir o pattern

1. Tela sempre com `Box(fillMaxSize().background(#000000))`.
2. Conteudo principal em `Column(fillMaxSize().verticalScroll(...).padding(bottom = 110.dp))`.
3. Header no topo com `20dp / 20dp / 28dp / 18dp`.
4. Lista com linhas fixas de `72dp`.
5. Divisores sempre `#242424` com padding horizontal de `20dp`.
6. Label em `#F4F4F4`, `titleMedium`, Sans Serif, peso normal.
7. Sublabel em `#8A8A8A`, `bodyMedium`, Sans Serif.
8. Icones secundarios em `24dp`, `#8A8A8A`.
9. Estados positivos em `28dp`, `#00E676`.
10. Footer fixo com copy em `12sp`, centralizado, `#8A8A8A`.

