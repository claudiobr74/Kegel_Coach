# Manual visual — revisão 1.0.1

O novo prompt do usuário substitui a estética das ilustrações anteriores. O Figma continua sendo referência para os componentes do manual; o personagem e a composição fotorealista seguem explicitamente a orientação nova.

## Recursos ativos

Seis PNGs 1200×1500 (4:5), em app/src/main/assets/manual:

- 01_prepare.png — círculo neutro, sem seta.
- 02_contraia.png — indicador teal, fechamento e seta curta para cima.
- 03_mantenha.png — mesmo estado de contração.
- 04_relaxe.png — retorna ao mesmo estado neutro.
- 05_rapida_contraia.png — mesmo estado de contração.
- 06_rapida_relaxe.png — mesmo estado neutro.

Personagem adulto fotorealista, de frente, sentado, mãos nas coxas, pés no chão. A base foi gerada com a ferramenta integrada de geração de imagens. A edição de contração alterou somente o indicador; a composição com máscara fixou todos os pixels fora da região didática. Não foi criada anatomia interna.

A comparação dos PNGs confirma diferenças apenas no retângulo x526..674, y718..886 que circunscreve a máscara do indicador. O relaxamento usa a mesma base de preparação. Repetir a mesma arte na manutenção e nos estados rápidos é intencional: nenhum movimento externo ocorre.

## Reprodução Android

O app seleciona os estados PNG com manualDemoFrame(), usando SystemClock.elapsedRealtime(). Nenhum número ou palavra é incorporado aos pixels. O contador usa Inter, tamanho e alinhamento uniformes e numerais tabulares. Contração lenta: preparar 1 s; contrair 1 s + manter 2 s; relaxar 6 s. Rápida: 1 s contração/1 s relaxamento.

Pausa/background preservam o tempo acumulado. Com redução de movimento, permanece o primeiro estado estático e a reprodução fica desativada. A demonstração não inicia treino, não vibra e não grava histórico.

O pacote entregue também contém dois GIFs sem contador, montados com paleta compartilhada: ciclo lento de 10 s e rápido de 2 s. O encoder funde os estados idênticos de contração/manutenção no GIF lento; o Android mantém a distinção lógica e o texto instrucional.

Os seis recursos antigos de personagem simplificado foram removidos do app. As sugestões antigas de fontes/licenças permanecem documentação de referência; nenhum recurso externo dessas fontes foi incorporado nesta revisão.
