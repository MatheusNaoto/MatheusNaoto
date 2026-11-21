# 📱 Celular Mínimo

Deixar um celular Android antigo o mais leve possível **sem root e sem perder a garantia**,
com um launcher próprio que só mostra texto (sem ícones, sem animação, sem nada em segundo plano).

Tudo aqui é **reversível**.

---

## O que tem aqui

| Pasta | O que é |
|---|---|
| `launcher/` | Código do **Mínimo**, o launcher (~30 KB, zero bibliotecas, Android 5.0+) |
| `scripts/` | Scripts que limpam e otimizam o celular pelo computador (via ADB) |
| `scripts/listas/` | Apps que serão desativados, separados por marca — **edite à vontade** |

### Como é o launcher

```
 14:32
 sábado, 21 de novembro



 WhatsApp
 Câmera
 Spotify
 Nubank
 Chrome

 todos os apps  ›
```

- **Toque** num app: abre.
- **Toque longo** num app: fixar/remover da tela inicial, mover para cima/baixo, informações, desinstalar.
- **todos os apps**: lista em ordem alfabética com busca. Digitou e apertou Enter → abre o primeiro resultado.
- **Toque no relógio**: abre os alarmes.
- **Toque longo no espaço vazio**: configurações do Android / trocar launcher.
- Fundo **preto puro** → em tela AMOLED os pixels ficam desligados e economiza bateria.

---

## Passo a passo

### 1. Baixe o launcher (APK)
1. Aqui no GitHub, vá em **Actions → "Gerar APK do launcher"** → clique na execução mais recente.
2. Lá embaixo em **Artifacts**, baixe **MinimoLauncher** (vem em .zip, extraia o `MinimoLauncher.apk`).
3. Coloque o `MinimoLauncher.apk` dentro da pasta `scripts/`.

### 2. Prepare o computador
1. Baixe o **Android Platform Tools** (o ADB oficial do Google): pesquise "platform-tools download" no site developer.android.com.
2. Extraia e copie `adb.exe`, `AdbWinApi.dll` e `AdbWinUsbApi.dll` para dentro da pasta `scripts/` (no Windows).

### 3. Prepare o celular
1. **Configurações → Sobre o telefone →** toque 7 vezes em **Número da versão** (ou "Versão MIUI").
2. Volte, entre em **Opções do desenvolvedor** e ative **Depuração USB**.
   - Xiaomi: ative também "Depuração USB (configurações de segurança)".
3. Ligue no PC pelo cabo e aceite o pedido **"Permitir depuração USB?"** na tela do celular.

### 4. Antes de rodar: diga o que você usa
- Abra `scripts/listas/` e veja os arquivos. Se tem algum app ali que você usa, **apague a linha**
  ou coloque o nome dele em `scripts/manter.txt`.
- Em `scripts/sem-segundo-plano.txt`, coloque apps que você usa mas que não precisam ficar rodando
  escondidos (Instagram, TikTok, lojas…). **Não** coloque WhatsApp/Telegram/banco.

### 5. Rode
- **Windows:** dois cliques em `otimizar.bat`
  (ou `otimizar-pesado.bat` para também desativar o app Google, YouTube, Chrome, Fotos, etc.)
- **Linux/Mac:** `./otimizar.sh` (ou `./otimizar.sh --pesado`)

O script detecta a marca do celular sozinho e faz:

1. Desativa os apps das listas (Google + marca do aparelho). Inclui o serviço de **anúncios do sistema** da Xiaomi.
2. Animações em 0,5x (tudo parece abrir 2x mais rápido).
3. Desliga a varredura de Wi-Fi/Bluetooth em segundo plano.
4. Impede os apps de `sem-segundo-plano.txt` de rodarem escondidos.
5. Limpa o cache de todos os apps.
6. Instala o launcher Mínimo e define como padrão.
7. Recompila os apps instalados para rodarem mais rápido (demora, deixe no cabo).

Reinicie o celular no final.

### Deu ruim? Desfazer
`restaurar.bat` (Windows) ou `./restaurar.sh` reativa tudo que foi desativado e volta as animações.
Se apagou o `alterados.txt`, dá pra reativar um app específico com:
`adb shell cmd package install-existing nome.do.pacote`

---

## Ajustes manuais que fazem diferença (sem PC)

- **Espaço livre:** mantenha pelo menos **15–20% do armazenamento livre**. Celular cheio fica lento de verdade.
- **Apps Lite / navegador:** para redes sociais, use a versão Lite (se existir) ou o site no navegador
  e "Adicionar à tela inicial". Gasta muito menos RAM que o app.
- **Sem widgets e sem papel de parede animado.**
- **Opções do desenvolvedor →** "Limite de processos em segundo plano" → *no máximo 2–3 processos*
  (fica melhor ainda em celular com 2–3 GB de RAM; volta ao normal ao reiniciar em alguns aparelhos).
- **Bateria →** coloque como "restrito/otimizado" os apps que não precisam de notificação.
- **Teclado:** se o teclado for pesado (ex.: com GIFs, temas), troque por um simples e desative a vibração.
- **Atualizações automáticas** da Play Store: deixe "apenas Wi-Fi".
- **O melhor de todos:** se o celular está muito lento, faça um **backup e restaure de fábrica**,
  depois rode o script *antes* de instalar seus apps. É o mais perto de "celular novo" que dá pra chegar.

## O que NÃO dá pra fazer sem root
Overclock, trocar o kernel, mexer no governador da CPU, remover apps do sistema de verdade (só dá pra
desativar/remover para o usuário, o espaço deles não volta). Ir além disso seria instalar uma ROM
alternativa (LineageOS etc.) — funciona muito bem em aparelhos antigos, mas desbloquear o bootloader
apaga tudo e depende do modelo.
