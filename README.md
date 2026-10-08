# Kegel_Coach · Pausa

Aplicativo Android nativo, offline e sem cadastro para pequenos treinos de contração e relaxamento do assoalho pélvico. A identidade Kegel Coach usa uma paleta verde discreta, tipografia Inter e componentes Android nativos. A interface 1.2.0 evolui independentemente do protótipo inicial.

## Executar

Abra o projeto no Android Studio com **JDK 17**, SDK Android 36 e Build Tools compatíveis (resolvidos pelo Gradle). Compatível com Android 8.0 (API 26) ou superior.

```bash
./gradlew :domain:test :app:testDebugUnitTest :app:lintRelease :app:bundleRelease
./gradlew :app:connectedDebugAndroidTest
```

A entrega é um Android App Bundle em `app/build/outputs/bundle/release/app-release.aab`. Consulte [distribution/README.md](distribution/README.md) para assinatura de upload e teste interno. Sem a chave privada de upload, o AAB de release é gerado não assinado. O pacote oficial é `com.kegel_coach.myapp`.

## Interface 1.2.0

- Navegação: Início, Treinos, Progresso e Ajustes.
- Um cartão principal para iniciar ou continuar o treino ativo.
- Contagem com uma volta do anel por fase, números tabulares e pausa sincronizada.
- Controles de sessão fixos e opções secundárias em painel inferior.
- Catálogo com treino selecionado e detalhes expansíveis.
- Personalização com edição numérica e duração/ações persistentes.
- Manual com passos numerados e ícones vetoriais consistentes.
- Regularidade nas quatro últimas semanas e estados vazios orientativos.
- Confirmação de gravação de treino e lembretes.

## Funcionalidades

- Manual de Execução com sete seções, demonstrações locais, pausa, redução de movimento e referências recolhidas. Acesso pelas Configurações e pelo onboarding.
- Onboarding com identificação da musculatura, postura, respiração, relaxamento, segurança e discrição.
- Treino do dia em um toque, cinco presets e personalização dos tempos, repetições, séries e intervalo.
- Sessão independente da UI, contagem real, pausa/retomada e confirmação de encerramento.
- Orientação por vibração + tela (padrão), vibração, tela, som ou voz portuguesa offline.
- Tela discreta, modo bolso e orientação por tela com opções temporárias da sessão; experimento dos padrões hápticos.
- Serviço em primeiro plano com notificação discreta e ações de pausa/retomada/encerramento.
- Lembretes locais por horário/dia, ativação individual e adiamento por 10, 30 ou 60 minutos.
- Treinos nomeados com dois blocos, biblioteca local, histórico por dia, feedback e métricas de regularidade.
- Cópias protegidas por senha e restauração após validação e confirmação.
- Progressão gradual sugerida após sete dias concluídos no nível; avanço depende de escolha explícita.
- Tema claro/escuro/sistema, TalkBack, alvos de toque de pelo menos 48 dp e redução de animações.
- Room para sessões/histórico/lembretes; DataStore para preferências. Sem permissão de internet, conta, telemetria ou backup automático.

## Arquitetura

| Área | Responsabilidade |
| --- | --- |
| `domain` | Modelos, validação de limites, timeline, relógio injetável, progresso e agenda |
| `app/.../presentation` | Compose, tokens Figma, navegação e ViewModel |
| `app/.../data` | Room, DataStore e serialização de checkpoints |
| `app/.../service` | Timer, háptica, som, lifecycle e notificação da sessão |
| `app/.../reminders` | AlarmManager, notificações e recuperação dos agendamentos |

### Tempo e recuperação

`WorkoutTimer` usa `SystemClock.elapsedRealtime()`: recomposições e mudanças no relógio civil não alteram a duração. A UI observa o estado; não dirige o treino. O serviço acorda no próximo segundo ou limite de fase, sem polling por frame.

Um WakeLock parcial existe apenas enquanto há sessão em execução, com timeout limitado ao tempo restante + margem. Pausa, encerramento, conclusão e destruição do serviço liberam o WakeLock.

Cada mudança de fase e ação de pausa salva um checkpoint. Se o processo for encerrado, o último checkpoint é recuperado **em pausa**, com retomada explícita. O app não assume que houve treino sem orientação. Se callbacks perderem fases ou sofrerem interrupção importante, a sessão é pausada no último estado observado, em vez de reproduzir vibrações atrasadas ou registrar conclusão.

O histórico é gravado somente após a última fase de relaxamento. Inserção com ID único e remoção do checkpoint ocorrem em uma transação Room, impedindo duplicação. Sessões encerradas antes do fim não entram no histórico.

### Android moderno

O serviço declara `specialUse` e sua justificativa no manifest, pois o produto é um timer háptico iniciado pelo usuário, sem sensores de saúde. O início ocorre por atividade visível ou ação explícita de notificação. Não há reinício automático do treino após reboot.

A publicação na Google Play exige declarar e submeter esse uso do foreground service à revisão da loja. Referência: [tipos de foreground service](https://developer.android.com/develop/background-work/services/fgs/service-types).

Lembretes usam alarmes **inexatos** com `setAndAllowWhileIdle`, sem pedir permissão especial para alarmes exatos. O Android pode atrasá-los em economia de bateria. Adiamentos usam relógio monotônico; horários diários usam data/hora e fuso locais. Boot, alteração de hora/fuso e atualização do aplicativo reprogramam os horários. As quatro ações estão disponíveis na notificação expandida.

## Origem do protótipo

Foram consultados Foundations, onboarding, Home, treino, personalizado, programas, progresso, lembretes, configurações e modo escuro pelo Figma MCP. Os links temporários de assets ficaram indisponíveis; os SVGs foram então exportados das **camadas originais pelo mesmo MCP**, sem redesenho. A correspondência entre camadas e arquivos está em `design/asset-map.json`.

Os assets ficam em `app/src/main/assets/figma`. A fonte Inter é empacotada localmente, com licença em `design/Inter-OFL.txt`. Não há URLs temporárias usadas pelo aplicativo.

Diferenças funcionais deliberadas em relação ao protótipo:

- Duração baseada nas fases reais, sem acrescentar os 20 s conceituais de preparação/transições.
- Início conservador em 3 s de contração / 6 s de relaxamento.
- Treino rápido padrão com 10 repetições, coerente com a orientação inicial do manual; personalização permanece disponível.
- Misto definido como 10 contrações 3/6 s, seguidas de 10 rápidas 1/1 s.
- Dados de exemplo do progresso foram substituídos por histórico real e estados vazios.
- Controles se reorganizam quando a fonte é ampliada.
- Barras de status e navegação do Android são as nativas, sem reproduzir relógio/bateria fictícios.

## Durações dos presets

| Preset | Ritmo | Séries | Duração |
| --- | --- | --- | --- |
| Iniciante | 3/6 s × 10 | 1 | 1 min 30 s |
| Intermediário | 5/5 s × 10 | 2, intervalo 30 s | 3 min 50 s |
| Avançado | 8/8 s × 10 | 3, intervalos 30 s | 9 min |
| Rápidas | 1/1 s × 10 | 1 | 20 s |
| Misto | 3/6 s × 10 + 1/1 s × 10 | 1 | 1 min 50 s |

## Validação

A versão 1.2.0 passou em 56 testes locais e no lint release, sem erros ou avisos. APK otimizado e testes instrumentados compilados; execução em dispositivos e AAB assinado pendentes. Veja a [implementação e seus limites](docs/audit/IMPLEMENTACAO_1.2.0.md).

Consulte [VALIDATION.md](VALIDATION.md) para resultados, diferenças verificadas e testes de aparelho pendentes. O workflow Android compila, testa o domínio/persistência, executa lint e oferece testes instrumentados em API 35 e 36.

## Evolução 1.2.0

Treinos nomeados são salvos na biblioteca local, inclusive com dois blocos. O modo bolso muda apenas as opções da sessão. A tela é mantida ligada quando a orientação visual está ativa. Preparação de 3 segundos e voz portuguesa offline são opcionais; voz ausente usa sinais sonoros. Interrupções de áudio pausam a orientação sonora.

Lembretes oferecem diagnóstico de permissão/canal e teste de aviso; horários continuam inexatos. Adiamentos são persistidos para recuperação após reinicialização. O histórico usa lista de itens visíveis, filtro por mês e consulta de sessões por dia.

Cópias `.kcb` são criptografadas com AES-256-GCM e chave derivada por PBKDF2-HMAC-SHA256 (210.000 iterações, salt aleatório). A senha não é armazenada pelo app. A importação valida formato, tamanho, limites e identificadores antes de substituir dados, mediante confirmação. Não restaura uma sessão ativa nem copia diagnósticos. Sem a senha, a cópia não pode ser recuperada.

O banco 2 inclui migração preservando histórico e lembretes da versão 1. Os novos testes cobrem migração, cópias, dados inválidos, pausa, blocos mistos, navegação e progressão com tolerância. O smoke instrumentado também verifica conclusão e preservação das preferências no modo bolso.

A faixa clínica dos exercícios continua exigindo revisão profissional individual. Personalizações extensas exibem aviso e confirmação; sugestões de progressão são suspensas após relatos recentes de dificuldade ou desconforto.
