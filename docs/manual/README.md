# Manual de execução — Kegel_Coach

A versão 1.0.1 substitui o personagem simplificado do pacote inicial por imagens fotorealistas para o público masculino, seguindo o novo prompt do usuário.

- Seis estados PNG 1200×1500: preparar, contrair, manter, relaxar e dois estados rápidos.
- Corpo, rosto, postura, cadeira e ambiente fixos; somente o indicador pélvico muda.
- Nenhum número ou texto na arte. Android controla contagem e seleção de estados com tempo monotônico.
- Demonstração lenta: 1 s preparação, 3 s contração/manutenção, 6 s relaxamento.
- Rápida: 1 s contração/1 s relaxamento.
- GIFs exportados no pacote de entrega; o app usa PNGs com controle nativo para sincronização.

Veja ASSETS_INTEGRADOS.md para recursos ativos e validação de invariância; PROMPT_VISUAL_V2.md registra a especificação da geração integrada. Os documentos clínicos, bibliografia e orientações de segurança do pacote inicial permanecem disponíveis nesta pasta. Os assets simplificados anteriores foram substituídos.

O Figma orienta os componentes da interface; a arte realista atende à instrução visual mais recente. Todos os recursos essenciais funcionam offline. O indicador é uma metáfora de fechar e elevar por dentro, sem movimento externo ou força para baixo.
