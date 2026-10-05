# Validação · 5 de outubro de 2026

## Escopo de testes

Domínio: duração de todos os presets, limites/valores inválidos, fase no tempo real, callbacks atrasados, pausa/retomada, restauração, intervalo entre séries, relaxamento final, cancelamento, progressão, streak e horários com mudança de semana/DST.

Persistência: round-trip de treino misto e checkpoint; conclusão transacional/idempotente; cancelamento sem histórico; lembretes independentes; feedback e exclusão.

Interface: navegação onboarding → Home → personalizado; renderização nativa em claro/escuro. O teste instrumentado cobre onboarding → treino rápido → pausa.

## Evidência

Build final de release concluído com sucesso em 5/10/2026:

`./gradlew :domain:test :app:bundleRelease :app:testDebugUnitTest :app:lintRelease :app:assembleDebugAndroidTest`

| Verificação | Resultado |
| --- | --- |
| Domínio | 10 testes aprovados |
| Room e serialização | 6 testes aprovados |
| Manual: assets, tela compacta/fonte ampliada/restauração/temas | 3 testes aprovados |
| Navegação e renderização nativa | 1 teste aprovado |
| Total | **20/20**, nenhuma falha |
| Lint release | **0 erros e 0 warnings** |
| Compilação instrumentada | Aprovada; execução em dispositivo pendente |
| Release AAB | Gerado, ZIP íntegro, pacote `com.kegel_coach.myapp`, **assinado com chave de upload** |

O bundle contém os seis recursos locais do manual. SHA-256 do AAB final assinado: `2629b76161c45bf780d01cfa24f1c69295e3ca5b0c94aee60b422c362c48c1f2`.

As capturas nativas em `app/build/visuals/` foram inspecionadas contra os tokens e componentes Figma: Home clara/escura, onboarding, personalizado, programas, progresso, configurações, manual claro/escuro e demonstração lenta, modo bolso e fases contração/relaxamento. Ajustados indicador de navegação, resumo semanal, espaçamentos e cores de suporte Material 3.

O manual preserva o padrão de “Como realizar” (`23:2156`): Inter, conteúdo com padding 20 dp/gap 16 dp, nota em Aqua e cards reutilizados do projeto. As seções recolhíveis e controles de demonstração estendem esse padrão para o novo conteúdo do pacote. Referências ficam fechadas inicialmente. A ilustração neutra foi exportada das camadas originais.

Os testes compactos usam largura 320 dp, altura 568 dp e fonte 1,6×. Validam o aspect ratio 0,8, descrição acessível, controle desativado com animações reduzidas, restauração de seção e rolagem até referências. O teste de integração usa 360×800 dp e verifica retorno correto do manual ao onboarding e às Configurações. O decoder Android reconheceu ambos os WebP como `AnimatedImageDrawable` 960×1200; PNGs e GIFs locais também foram decodificados. Reprodução efetiva, TalkBack e rotação em aparelhos reais continuam na matriz abaixo.

## Assinatura e distribuição

`certificates.zip` contém três certificados públicos X.509. `deployment_cert.der` coincide com o SHA-256 enviado no JSON Digital Asset Links. Esses certificados não incluem a chave privada de upload; não podem assinar o AAB. A configuração de release e o workflow manual aceitam a chave privada por secrets/variáveis de ambiente. Em 5/10/2026, com autorização explícita do usuário, foi criada uma nova chave de upload JKS (RSA 2048, alias `kegel-coach-upload`). O bundle foi novamente compilado com essa chave e passou na verificação `jarsigner -verify`. SHA-256 do certificado de upload: `DF:51:B7:F3:87:55:54:8C:F4:12:CE:34:2C:2E:24:66:D1:16:C0:1B:02:0E:9C:74:92:B6:B1:C2:D9:D6:95:9F`. A chave privada e a senha são entregues separadamente e não fazem parte do Git. Se a Play Console já tiver outra chave de upload cadastrada, será necessário registrar/redefinir o certificado de upload antes de aceitar esse AAB. O certificado de assinatura do Google e o JSON Digital Asset Links permanecem os fornecidos pelo usuário. A publicação na Play Console não foi realizada.

## Verificações em aparelho

O ambiente local não oferece aceleração KVM e o emulador API 35 exige mais espaço de partição do que o disponível. Portanto, o teste instrumentado local de tela bloqueada **não foi executado**. O workflow de CI oferece emuladores API 35 e 36; a continuidade háptica também deve ser avaliada em aparelho real.

| Cenário | Resultado esperado |
| --- | --- |
| Bloquear a tela durante contração | Serviço permanece ativo, próximo relaxamento em duas vibrações |
| Guardar no bolso por toda a sessão | Sequência percebida com fim distinto e sem interação visual |
| Pausar pela notificação | Sem novas vibrações e sem WakeLock; retomada mantém tempo restante |
| Encerrar pela notificação | Remove sessão ativa, sem adicionar histórico |
| Matar o processo e reabrir | Recupera checkpoint em pausa; não marca conclusão automaticamente |
| Rotacionar/alterar configuração | Sessão e tempo continuam no serviço |
| Revogar notificações | App explica permissão; treino de tela continua disponível |
| Alterar fuso/hora e reiniciar aparelho | Horários locais de lembretes são reagendados |
| Manual: reproduzir/pausar e colocar em background | WebP animado em API 28+, GIF em API 26–27; pausa em background e com redução de movimento |
| Manual: rotação/recomposição | Seção aberta e opção de reprodução preservadas |
| TalkBack/fonte ampliada/animações desativadas | Controles compreensíveis, sem recorte e com animações reduzidas |

A presença de motor de vibração é verificada antes de iniciar o modo bolso. Intensidade, restrições de bateria e comportamento de notificações podem variar entre fabricantes.
