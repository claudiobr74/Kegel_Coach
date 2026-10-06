# Validação visual — Kegel_Coach 1.0.4 (código 5)

A Home ainda exibia um símbolo abstrato antigo no cabeçalho. A nova marca anteriormente integrada apenas em Boas-vindas e Sobre agora aparece de forma permanente acima da saudação: símbolo vetorial aprovado e nome Kegel Coach, com contraste ajustado ao tema e descrição para TalkBack.

Os ícones que continham molduras quadradas foram substituídos por vetores Android nativos. Home, Treino, Progresso, Configurações, Programas, Modo bolso, Lembretes, Voltar e controles de aumento/redução usam a mesma grade de 24 dp, traço de 1,8 dp, terminações e junções arredondadas. Atalhos da Home recebem superfícies circulares de 48 dp; a navegação mantém indicação de seleção, rótulos e áreas de toque existentes.

O Figma oficial foi consultado no frame Home 23:770. Cores, cartões, espaçamentos principais e navegação foram preservados. A substituição dos ícones e a inclusão da marca seguem o pedido explícito do usuário, que prevalece sobre os assets antigos do Figma.

Arquivos principais: PausaApp.kt, ic_brand_symbol.xml, ic_ui_*.xml, VisualTest.kt, BrandTest.kt e app/build.gradle.kts. A lógica de treino, vibração, modo bolso e o manual não foram modificados nesta versão.

Os testes existentes renderizam Home clara e escura e verificam que a marca está visível nos dois temas. Foram conferidos os renders da Home, navegação, Programas, Progresso, Configurações e Modo bolso. Execução física no aparelho do usuário permanece pendente.
