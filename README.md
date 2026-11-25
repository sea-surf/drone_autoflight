# Mavic 3M AutoFlight

Aplicativo Android desenvolvido com o **DJI Mobile SDK V5 (`5.20.1`)** para voo autônomo do **DJI Mavic 3M (Multispectral)** diretamente no controle remoto (**DJI RC Pro Enterprise / DJI RC Plus**).

## O que o app faz
- **Telemetria em tempo real:** Conexão, Altura (m), Velocidade horizontal/vertical (m/s), Bateria (%), Satélites GPS e Modo de Voo.
- **Missão Autônoma** (via *Virtual Stick* em Modo Avançado):
  1. Decola (`KeyStartTakeoff`)
  2. Sobe até **3,0 metros**
  3. Avança **2,0 metros** para frente (`0.5 m/s` por `4 s`)
  4. Retorna **2,0 metros** para trás (`-0.5 m/s` por `4 s`)
  5. Pousa automaticamente (`KeyStartAutoLanding`)
- **Segurança:** Botão **PARAR / PAIRAR** para cancelar a missão imediatamente e devolver o controle ao modo manual.

---

## 1. Setup após o clone

Como as pastas `build/`, `.gradle/`, `.idea/` e `local.properties` são ignoradas pelo Git, ao clonar o repositório em outro computador siga estes passos:

### Pré-requisitos
- **Android Studio** (com suporte a AGP `8.7.0` e Kotlin `2.1.0`)
- **Android SDK Platform 34** (instalado via *SDK Manager* no Android Studio)
- **Conexão com a internet** no primeiro *Gradle Sync* para baixar as dependências do **DJI Mobile SDK V5 (`5.20.1`)**

### Passo a passo
1. Clone o repositório e abra a pasta `Mavic3MAutoFlight` diretamente no **Android Studio** (*File → Open*).
2. O Android Studio vai criar automaticamente o `local.properties` (com o caminho do seu Android SDK), `.idea/` e `.gradle/`, e iniciar o **Gradle Sync** para baixar o DJI MSDK V5 (`dji-sdk-v5-aircraft:5.20.1`, `dji-sdk-v5-networkImp:5.20.1`, etc.).
3. No arquivo `app/src/main/AndroidManifest.xml`, verifique se a sua App Key da **DJI Developers** está preenchida em:
   ```xml
   <meta-data
       android:name="com.dji.sdk.API_KEY"
       android:value="SUA_APP_KEY_AQUI" />
   ```
   *(O Package Name cadastrado no portal da DJI deve ser `com.dji.mavic3mauto`.)*
4. Clique em **Assemble app** (ou rode `.\gradlew assembleDebug` no Terminal) para gerar o APK em `app/build/outputs/apk/debug/app-debug.apk`.

---

## 2. Como instalar no controle remoto via cabo USB (no dia do voo)

### Passo 1 — Ativar Depuração USB no controle remoto DJI
1. Ligue o controle remoto (**DJI RC Pro / RC Plus**).
2. Deslize a barra superior para baixo e abra **Configurações (⚙️)**.
3. Vá em **Sobre o dispositivo** (ou *About*) e toque **7 vezes** em **Número da versão** (*Build number*) até aparecer *"Você agora é um desenvolvedor"*.
4. Volte em **Sistema → Opções do desenvolvedor** (*Developer options*) e ative **Depuração USB** (*USB Debugging*).

### Passo 2 — Conectar o notebook ao controle
1. Conecte o cabo USB-C do notebook na porta USB-C do controle remoto.
2. Na tela do controle, se aparecer o aviso **"Permitir depuração USB?"**, marque *"Sempre permitir"* e toque em **OK**.
3. No **Android Studio** (com o projeto `Mavic3MAutoFlight` aberto), o controle vai aparecer na lista de dispositivos no topo. Basta clicar no botão **Run (▶)** para instalar e abrir o app direto no controle.

> **Alternativa sem Android Studio (via APK):**
> O APK já compilado fica em `app/build/outputs/apk/debug/app-debug.apk`. Você pode copiá-lo para o controle ou instalar pelo terminal com `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

---

## 3. Checklist de Segurança
- [ ] **Local aberto**, sem obstáculos, árvores, fios ou pessoas num raio de pelo menos 5 metros.
- [ ] **GPS conectado** (aguarde pelo menos 10+ satélites na tela do app antes de iniciar).
- [ ] **Primeiro registro do SDK:** na primeira vez que abrir o app, o controle precisa de **acesso à internet (Wi-Fi ou roteador do celular)** para validar a App Key com os servidores da DJI. Depois de registrado, funciona offline.
- [ ] **Feche o DJI Pilot 2** antes de abrir o `Mavic 3M AutoFlight` (dois apps não podem controlar o drone ao mesmo tempo).
- [ ] Mantenha as mãos nos **sticks do controle** — se precisar assumir o controle manual a qualquer momento, toque em **PARAR / PAIRAR** ou mova os sticks.
