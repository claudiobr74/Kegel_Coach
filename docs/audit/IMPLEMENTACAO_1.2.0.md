# Implementação 1.2.0 (13)

## Mudanças

- Treino visual mantém a tela ligada enquanto executa; pausa orienta relaxamento.
- Modo bolso, tela discreta e orientação por tela podem ser usados somente na sessão, preservando preferências globais.
- Preparação opcional antes do primeiro sinal; voz portuguesa local com alternativa sonora; interrupção de áudio pausa o treino.
- Lembretes têm diagnóstico de permissão/canal, teste, horário claramente identificado e adiamento persistido.
- Operações de dados têm salvamento, confirmação e erro; editor de lembrete permanece aberto até sucesso.
- Navegação preserva origem; rascunhos modificados pedem confirmação antes de descartar.
- Início mostra conclusão do dia, calendário usa abreviações completas e destaca hoje.
- Programa separado de treinos avulsos, retorno/redução de etapa e progressão explícita considerando relatos recentes. Retomar a etapa atual preserva a janela de progresso.
- Treinos podem ser nomeados, salvos, editados e removidos; editor preserva os dois blocos do treino misto.
- Calendário abre sessões do dia; histórico oferece filtro de mês e renderização dos itens visíveis.
- Cópias protegidas por senha com validação antes de restaurar; confirmação informa a substituição dos dados.
- Ajuda e exportação voluntária de diagnóstico técnico sem nomes de treino, feedback ou senhas.
- Migração de banco 1→2 preserva registros e lembretes, acrescentando biblioteca e índice de data.
- Abertura abreviada, onboarding enxuto e opções de Ajustes em painéis com rolagem para fonte ampliada. Confirmações redundantes de preferências foram removidas para liberar os controles.

## Validação local aprovada

Compilação completa de **119 tarefas**, com `BUILD SUCCESSFUL`: domínio, testes Android locais, lint release, APK com R8 e instrumentação.

- **56/56 testes aprovados**: 16 de domínio e 40 de persistência, interface, cópias e lembretes; zero falhas, erros ou testes ignorados.
- **Lint release: zero erros e zero avisos.**
- Migração Room 1→2, preservação de treinos mistos, restauração protegida e retomada do programa sem reiniciar a janela de progresso verificadas.
- Capturas nativas de início claro/escuro, onboarding, ajustes e personalização inspecionadas.
- Testes instrumentados compilados. Execução em emuladores/aparelhos e smoke da release em dispositivo continuam pendentes.
- APK release otimizado gerado **sem assinatura**; integridade ZIP aprovada. AAB assinado não gerado nesta etapa.

A [evidência estruturada](RESULTADOS_1.2.0.json) registra suites, artefato e hash. Verificação sintática adicional: 41 arquivos Kotlin, zero erros. O SDK local incluiu duas bibliotecas nativas sem remover símbolos; isso não impediu a compilação.

O envio desta branch ao repositório público foi bloqueado pela revisão automática de autorização. A implementação está na branch local `feat/professional-1.2.0`, sem integração à main. A publicação e os checks em emuladores no GitHub aguardam autorização específica.

## Compatibilidade e limites

O app continua offline e sem conta. Os lembretes permanecem inexatos; podem atrasar. A voz depende de um recurso português offline instalado e usa sons quando indisponível. A senha da cópia não é salva e não pode ser recuperada. A restauração substitui os dados locais após confirmação e exige ausência de sessão ativa.

O app registra tempo de uso, não força muscular nem correção da técnica. Personalizações extensas exigem confirmação; as faixas e orientações clínicas continuam dependendo de revisão profissional individual.
