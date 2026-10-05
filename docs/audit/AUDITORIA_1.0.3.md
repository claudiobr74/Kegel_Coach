# Correção da vibração — Kegel_Coach 1.0.3 (código 4)

A implementação anterior emitia vibrações sem atributos de uso. O Android restringe esse tipo de chamada quando o aplicativo está em segundo plano. O serviço de treino e o botão “Testar vibrações” agora compartilham uma implementação que declara sinais temporizados como USAGE_ALARM: VibrationAttributes no Android 13+ e AudioAttributes nas versões anteriores. Não são usados mecanismos para contornar as preferências de vibração ou Não Perturbe do sistema.

Ao trocar o modo de orientação durante uma sessão ativa, o serviço interrompe a orientação anterior e emite o sinal da fase atual. Alterações em preferências sem mudança de orientação não cancelam uma vibração. Sessões pausadas não emitem esse sinal.

Arquivos principais: HapticGuidance.kt, WorkoutService.kt, PausaApp.kt, HapticGuidanceTest.kt, BrandTest.kt e app/build.gradle.kts. As imagens do manual foram preservadas. A nova logo enviada pelo usuário foi aplicada em Boas-vindas, Sobre e nos ícones adaptativo, monocromático e de notificação. O PNG original foi preservado, e os ícones usam o contorno vetorizado do símbolo aprovado. Os espaços e as cores existentes foram consultados no Figma (Boas-vindas 23:648).

Cinco testes de regressão verificam atributos de alarme para as quatro fases e para a demonstração, padrões sem repetição automática, cancelamento, ausência de motor vibratório e política de mudança de orientação. Os testes Android usam Robolectric com API 35. A compatibilidade API 26–32 é compilada, mas não foi executada em dispositivo dessas versões.

Validação física pendente: instalar pela faixa de teste interno, abrir Modo bolso → Testar vibrações, iniciar uma sessão e verificar os sinais com a tela bloqueada; pausar/retomar e encerrar. Não há aparelho Android físico conectado neste ambiente.

Referência técnica: https://developer.android.com/reference/android/os/Vibrator#vibrate(android.os.VibrationEffect,android.os.VibrationAttributes)
