# Validação da evolução 1.2.0 (13)

Estado: **validação local aprovada** na branch `feat/professional-1.2.0`; envio público e integração à main aguardam autorização.

- **56/56 testes aprovados**, sem falhas, erros ou testes ignorados: 16 do domínio e 40 Android locais.
- **Lint release: zero erros e zero avisos.**
- Migração Room, cópias protegidas, treinos mistos, pausa, lembretes, retorno de navegação e retomada da etapa verificados.
- Build completo de 119 tarefas aprovado; APK release otimizado com R8 e instrumentação compilados.
- Capturas nativas inspecionadas em temas claro/escuro e fluxos principais; fonte ampliada coberta por testes.
- APK local sem assinatura; integridade ZIP aprovada. AAB assinado e execução em emuladores/aparelhos pendentes.
- Revisão clínica independente pendente.

Detalhes: [implementação](docs/audit/IMPLEMENTACAO_1.2.0.md) e [resultados estruturados](docs/audit/RESULTADOS_1.2.0.json).

Os resultados abaixo pertencem a versões anteriores.

---

# Validação · 5 de outubro de 2026

## Atualização 1.0.2 — marca e auditoria

Compilação recuperada aprovada: 124 tarefas, BUILD SUCCESSFUL. **30/30 testes aprovados**, sem erros, falhas ou testes ignorados. Lint release: **zero erros e zero warnings**. Instrumentação compilada; execução em aparelho ainda pendente. AAB **1.0.2/code 3**, 19.444.157 bytes, assinado com a mesma chave de upload, verificado por jarsigner e com ZIP íntegro. Os seis PNGs do manual coincidem byte a byte com os recursos do código.

SHA-256: `355a662d23b5dc47f0f157f80ec4215bbc645ca4bfcb8347424ed67f083d5d66`.

Nova marca vetorial no launcher adaptativo/temático, onboarding e Sobre. Correções de continuidade ao pausar, checkpoints inválidos, lembretes, resumo dos treinos e acessibilidade. A [auditoria](docs/audit/AUDITORIA_1.0.2.md) detalha achados, escopo, limites e pendências. [Resultados](docs/audit/RESULTADOS_1.0.2.json), [contraste](docs/audit/CONTRASTE_1.0.2.json) e [consulta OSV](docs/audit/DEPENDENCIAS_OSV_1.0.2.json) preservam evidência estruturada.


## Atualização 1.0.1 — manual realista

Build final offline a partir de pasta de build nova: 59 tarefas, BUILD SUCCESSFUL. Verificações anteriores do mesmo código: 22/22 testes aprovados e compilação instrumentada aprovada. Lint release final sem erros nem warnings. AAB 1.0.1 (versionCode 2) assinado e verificado com jarsigner; ZIP íntegro e somente seis PNGs locais 1200×1500. SHA-256: `d9870f6c171b1dfe89d3e763a8c13af833feedc43529597c5cc3d8b955a79e95`.

Arte substituída conforme novo prompt: adulto fotorealista de frente, base idêntica em todos os estados. Comparação de pixels limita todas as diferenças ao indicador pélvico (x526..674/y718..886). GIFs exportados de 10 s e 2 s sem texto/números. Android controla fases e contador nativo com numerais tabulares; sobreposição acima da cabeça em fonte padrão, abaixo da imagem em fonte ampliada.

Dois novos testes cobrem contagem 3/2/1 e 6/5/4/3/2/1, fronteiras preparar/contrair/manter/relaxar, ciclos rápidos e tempo real com callbacks atrasados. Captura nativa final do manual escuro inspecionada: personagem frontal e contador uniforme. Testes compactos em 1,6× aprovados. Pausa/background interrompem callbacks da demonstração e preservam o tempo acumulado. Testes em aparelho real permanecem pendentes.

O build incremental inicial falhou em compileReleaseArtProfile; recompilação completa regenerou o perfil e passou. Uma execução de verificação foi interrompida pelo acesso de rede às dependências e foi concluída offline. A inspeção do AAB identificou assets antigos retidos no cache; a compilação final em pasta nova eliminou todos os seis assets antigos. Aviso de sessões múltiplas de Kotlin não afetou o resultado.

## Escopo de testes da versão inicial

Domínio: duração de todos os presets, limites/valores inválidos, fase no tempo real, callbacks atrasados, pausa/retomada, restauração, intervalo entre séries, relaxamento final, cancelamento, progressão, streak e horários com mudança de semana/DST.

Persistência: round-trip de treino misto e checkpoint; conclusão transacional/idempotente; cancelamento sem histórico; lembretes independentes; feedback e exclusão.

Interface: navegação onboarding → Home → personalizado; renderização nativa em claro/escuro. O teste instrumentado cobre onboarding → treino rápido → pausa.

## Evidência da versão inicial 1.0.0

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

O ambiente local não oferece aceleração KVM. Portanto, o teste instrumentado local de tela bloqueada **não foi executado**. O workflow de CI oferece emuladores API 35 e 36; a continuidade háptica também deve ser avaliada em aparelho real.

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
| Manual: reproduzir/pausar e colocar em background | Frames PNG com fases/contador controlados pelo Android; pausa em background e com redução de movimento |
| Manual: rotação/recomposição | Seção aberta e opção de reprodução preservadas |
| TalkBack/fonte ampliada/animações desativadas | Controles compreensíveis, sem recorte e com animações reduzidas |

A presença de motor de vibração é verificada antes de iniciar o modo bolso. Intensidade, restrições de bateria e comportamento de notificações podem variar entre fabricantes.
