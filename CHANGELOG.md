# 1.2.0 (13) — evolução profissional

- Correção da orientação de pausa e tela ativa durante o treino visual.
- Opções de modo bolso, tela discreta e orientação por tela limitadas à sessão; preferências preservadas.
- Diagnóstico de notificações, teste de aviso, salvamento com retorno e adiamento persistido.
- Navegação com retorno à origem, confirmação de descarte e início mais compacto.
- Biblioteca de treinos nomeados e editor de dois blocos.
- Calendário consultável, filtro por mês e lista de histórico com itens visíveis.
- Cópia/restauração protegida por senha (AES-GCM), com validação e confirmação.
- Preparação opcional de 3 segundos e instruções por voz offline, com alternativa sonora.
- Progressão explícita que considera relatos recentes; retomada preserva os dias contabilizados; aviso em personalizações extensas.
- Migração do banco 1→2, testes de dados e ampliação dos percursos de validação.

# Changelog

## 1.0.2 — nova marca e auditoria

- Logo vetorial minimalista, launcher adaptativo, ícone temático e recurso próprio para notificações.
- Marca integrada ao onboarding e à seção Sobre; versão exibida deriva do build.
- Continuidade da orientação verificada também ao pausar; recuperação rejeita checkpoints inválidos ou terminais.
- Lembretes respeitam dias selecionados e limpam adiamentos antigos após edição.
- Home apresenta corretamente os blocos, repetições por série e total de contrações.
- Melhorias no TalkBack, identificação de switches, feedback com fonte grande e retorno após conclusão.
- Auditoria e evidências em `docs/audit/`.

## 1.0.1 — manual realista e contador nativo

- Substitui as ilustrações anteriores por seis estados fotorealistas 1200×1500, com personagem adulto sempre de frente e externamente imóvel.
- Apenas o indicador pélvico muda; imagens não contêm texto nem números.
- Demonstração controlada pelo tempo monotônico no Android: preparar 1 s, contrair/manter 3 s, relaxar 6 s; rápidas 1/1 s.
- Contador com fonte tabular, posição e tamanho fixos; pausa e background congelam a demonstração.
- Imagens educativas fornecidas por geração de imagem, sem anatomia interna inventada.

## 1.0.0 — preparação para teste interno

- Aplicativo Android nativo, offline e sem cadastro, com treino do dia, presets e personalização.
- Cronômetro real, pausa/retomada, háptica, modo discreto e modo bolso com serviço foreground.
- Lembretes locais, histórico, calendário, progressão sugerida e temas claro/escuro/sistema.
- Novo Manual de Execução, com identificação da musculatura, posição, contrações lentas e rápidas, boa técnica, erros comuns, segurança e referências recolhidas.
- Demonstrações animadas locais, controles de reprodução/pausa e respeito à redução de movimento do sistema.
- Entrada pelas Configurações e acesso opcional no onboarding, com retorno à etapa anterior.

Conteúdo educativo; não substitui avaliação médica ou fisioterapêutica individual.
A assinatura final de upload e a publicação na faixa de teste interno são etapas separadas do build.
