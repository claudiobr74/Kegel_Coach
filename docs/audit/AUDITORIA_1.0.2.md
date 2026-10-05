# Auditoria Kegel_Coach — versão 1.0.2 (3)

Data: 05/10/2026. Pacote: `com.kegel_coach.myapp`. Repositório oficial: `claudiobr74/Kegel_Coach`, branch `main`. Nome público discreto: Pausa.

## Alcance e resultado

Revisão da implementação completa disponível: arquitetura, domínio, timer, serviço, armazenamento, lembretes, navegação, manual, acessibilidade, recursos gráficos, privacidade, testes e configuração de publicação. Os problemas descritos abaixo foram corrigidos. A validação automatizada e as renderizações nativas não equivalem a teste físico de vibração, medição de bateria, validação clínica ou aprovação da Play Store.

O Figma foi consultado pelo MCP: onboarding `23:648`, Home `23:770` e configurações `23:1923`, arquivo `NvslB6GbO1MlN3mq3wr0nz`. Tokens, tipografia Inter, temas e componentes foram preservados. O manual mantém as seis imagens realistas solicitadas pelo usuário. A nova identidade foi autorizada neste pedido.

## Marca integrada

Símbolo vetorial teal com duas curvas convergindo para dentro e elevação central abstrata. Sem anatomia, personagens ou números. Integrado ao launcher adaptativo, ao onboarding e à seção Sobre. O ícone temático utiliza uma camada monocromática. Notificações receberam recurso monocromático independente. No Compose, a cor acompanha o tema. Fonte mestre: `design/brand/kegel-coach-symbol.svg`.

## Achados corrigidos

| Gravidade | Problema | Correção / arquivos principais |
|---|---|---|
| Alta | Pausa após interrupção longa podia creditar tempo sem a continuidade das orientações. | A pausa verifica a mesma política do ticker e recupera o último estado observado em pausa. `WorkoutService`, `CueContinuity`, teste de pausa tardia. |
| Alta | Checkpoint inválido ou terminal podia manter sessão impossível de retomar. | Recuperação valida status e intervalo de tempo; checkpoint inválido removido sem histórico. `Preferences.kt`, `AppViewModel`, `WorkoutService`, `PersistenceTest`. |
| Média | Home confundia total de contrações com repetições por série e ocultava segundo bloco do treino misto. | Resumo exibe cada bloco, repetições por série, séries e total. `PausaApp`. |
| Média | Notificação podia abrir outra Activity; Iniciar após conclusão não iniciava um novo treino. | `singleTop` e tratamento da sessão concluída. Manifest e `PausaApp`. |
| Média | Edição de lembrete podia preservar um adiamento antigo. | Edição cancela alarmes anteriores antes de reagendar; reagendamento diário preserva adiamento explícito. `AppViewModel`, `ReminderTest`. |
| Média | Alarme diário atrasado podia notificar em dia não selecionado. | Receiver verifica o dia atual; adiamentos explícitos permanecem permitidos. `Reminders`. |
| Média | Mudar orientação ou pausar não interrompia som já iniciado. | Cancelamento háptico e `stopTone()` nos comandos. `WorkoutService`. |
| Média | Corrida ao atualizar orientação podia lançar exceção de início de serviço. | Falha capturada com mensagem recuperável. `AppViewModel`. |
| Média | TalkBack não recebia o contador da sessão. | Descrição inclui fase e segundos restantes, também no modo discreto. `PausaApp`, `VisualTest`. |
| Média | Feedback não se adaptava a fontes grandes; switches tinham identificação incompleta. | FlowRow para chips e descrições acessíveis. `PausaApp`. |
| Baixa | Versão Sobre fixa em 1.0.0. | Uso de BuildConfig, versão 1.0.2/code 3. Gradle, `PausaApp`, `BrandTest`. |
| Baixa | Ícone genérico e inadequado como smallIcon de notificação. | Marca vetorial adaptativa e recurso monocromático separado. Manifest e recursos drawable/mipmap. |
| Baixa | Preferências não tratavam falha de leitura de I/O; semana inválida podia persistir. | Fallback de leitura para IOException e limites do índice. `Preferences.kt`. |
| Baixa | Voltar pelo cabeçalho da conclusão deixava estado concluído em memória. | Estado limpo, conforme Concluir e retorno do sistema. `PausaApp`. |

## Verificação por área

| Área | Evidência | Limite |
|---|---|---|
| Arquitetura | presentation/domain/data, estado independente da UI; mudanças incrementais | Sem alteração de schema ou refatoração ampla. |
| Timer | Testes de duração, limites, relógio monotônico, fases, callbacks atrasados, séries, pause/resume, recuperação e conclusão | Políticas físicas de fabricantes não reproduzidas integralmente. |
| Histórico | Room: conclusão atômica/idempotente, cancelamento, feedback e exclusão | Banco local privado, sem sincronização. |
| Treinos | Cinco presets, personalizado e dois blocos do misto | Parâmetros de produto, não prescrição individualizada. |
| Progressão | Requer adesão e decisão explícita; limite de quatro semanas | Nenhum avanço automático. |
| Lembretes | Dias, horários, habilitação, DST, agendamento, cancelamento e adiamento | Alarmes inexatos podem ser atrasados pelo Android. |
| Modo bolso | Foreground service specialUse, notificação privada, WakeLock limitado | Teste de tela bloqueada, consumo e haptics em aparelho real pendente. |
| UI | Renderizações de onboarding, Home, programas, personalizado, progresso, configurações, bolso, manual e sessão | Renderizações Robolectric não são screenshots de aparelho físico. |
| Manual | Seis PNG 1200 × 1500, decodificação offline, contagem nativa, restauração e pausa | Validação clínica especializada pendente. |
| Acessibilidade | Semântica revisada; manual em 320dp e fonte 1,6×, redução de movimento | TalkBack físico e todos os tamanhos de tela ainda requerem aceite manual. |
| Tema | Claro, escuro e sistema; pares principais de texto calculados | Não constitui certificação integral de acessibilidade. |
| Distribuição | AAB com a chave de upload existente, pacote e versão conferidos | Sem publicação nem acesso à Play Console nesta tarefa. |

## Privacidade e segurança

Sem conta, permissão INTERNET, analytics, anúncios, sensores externos ou envio de informações de saúde. Room e DataStore permanecem no armazenamento privado. Backup automático desativado; regras de extração excluem os dados locais. Serviço e receiver de lembretes não exportados. Receiver de reagendamento aceita somente ações de sistema previstas. PendingIntents explícitos e imutáveis. Notificações usam mensagens discretas e versão pública sem finalidade clínica explícita.

Chave privada e senhas fora do Git. Certificado público de upload e Digital Asset Links podem ser versionados. O fingerprint fornecido no JSON é um identificador público, não uma chave privada. O AAB usa a chave de upload existente; a Play assina os APKs distribuídos com seu certificado de assinatura. Não foi substituída a chave após a recuperação do ambiente.

O manual orienta respiração, relaxamento completo, ausência de esforço para baixo, interrupção diante de dor e orientação profissional. Não promete cura. Referências femininas estão identificadas e não comprovam sozinhas resultados masculinos. Há fontes gerais NIDDK e orientação masculina Royal Free NHS. Não foi afirmada validação clínica dos presets.

## Dependências

Consulta à base pública OSV para 18 dependências com versão explicitamente declarada, incluindo bibliotecas de teste: nenhuma vulnerabilidade foi retornada para esses pares nome/versão na consulta de 05/10/2026. A requisição continha somente identificadores públicos de pacotes; nenhum código, segredo ou informação de saúde foi enviado. O resultado está em `DEPENDENCIAS_OSV_1.0.2.json`. Esse recorte não cobre todas as dependências transitivas, vulnerabilidades desconhecidas ou falhas do sistema Android; não é uma garantia de ausência de vulnerabilidades. [Documentação da API OSV](https://google.github.io/osv.dev/post-v1-querybatch/).

## Testes, build e incidentes

Comandos: `:domain:test :app:testDebugUnitTest :app:lintRelease :app:bundleRelease :app:assembleDebugAndroidTest`. JDK 17, Gradle 8.11.1, compile/target SDK 36, min SDK 26. Resultados finais, checksum e verificação de assinatura são registrados em `RESULTADOS_1.0.2.json` a partir dos arquivos efetivamente produzidos.

A primeira rodada executou 30 testes sem falhas. O cache de `compileReleaseArtProfile` apresentou inconsistência; rebuild forçado sozinho não resolveu, e um diretório novo de build permitiu compilar. Em seguida, manutenção automática removeu o workspace antes da entrega: o repositório foi recuperado do GitHub, a chave do backup persistido, e as alterações reaplicadas para repetir a validação. Não se reutilizou um AAB antigo como se fosse a nova versão.

O aviso de stripping de `libandroidx.graphics.path.so` e `libdatastore_shared_counter.so` é uma limitação do toolchain; as bibliotecas são empacotadas intactas. Instrumentação foi compilada, não executada localmente: não há `/dev/kvm`. O workflow prevê emuladores API 35/36, mas isso não prova sua execução nesta auditoria.

## Pendências para teste interno e publicação

1. Testar sessão completa em aparelho físico com tela bloqueada, retorno após atraso, pausa/retomada, cancelamento, troca de orientação e morte do processo. Esperado: pistas corretas, recuperação em pausa, nenhum treino incompleto creditado.
2. Medir consumo e intensidade háptica, inclusive em fabricante com otimização agressiva. O WakeLock é liberado ao pausar, encerrar, concluir e destruir o serviço; não elimina políticas de encerramento externas.
3. Conferir notificações permitidas/negadas, adiamentos 10/30/60 minutos, reinício, fuso/data e controles expandidos.
4. Conferir TalkBack, fonte grande, ícones temáticos e máscaras de launcher. Revisar o manual com profissional de saúde pélvica masculina antes de afirmar validação clínica.
5. Na Play Console: conferir chave de upload cadastrada, declaração de foreground service specialUse, categoria de saúde, segurança dos dados e política de privacidade. Nenhuma publicação foi realizada.
6. App Links: não foi informado domínio HTTPS para `/.well-known/assetlinks.json`. JSON preservado; nenhuma verificação de domínio ou filtro autoVerify foi inventado.

Fontes técnicas consultadas: [foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [permissão de notificações](https://developer.android.com/develop/ui/views/notifications/notification-permission), [ícones adaptativos](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive) e [NIDDK — Kegel Exercises](https://www.niddk.nih.gov/health-information/urologic-diseases/kegel-exercises). Código e resultados são evidência da auditoria técnica; documentação externa não substitui testes.

## Resultado final desta execução

30 testes aprovados, sem falhas, erros ou testes ignorados. Android Lint: zero erros e zero avisos. AAB 1.0.2/code 3 com 19.444.157 bytes; assinatura e integridade verificadas.

SHA-256: `355a662d23b5dc47f0f157f80ec4215bbc645ca4bfcb8347424ed67f083d5d66`.
