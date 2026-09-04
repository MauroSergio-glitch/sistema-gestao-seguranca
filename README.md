# Ocorrências SST - Progressive Web App (PWA)
### Multiplataforma: iOS (iPhone / iPad), Android e Computadores (Web)

Este aplicativo foi desenvolvido seguindo os padrões oficiais de **Progressive Web Application (PWA)** para permitir que a equipe registre ocorrências e relatos de Segurança do Trabalho diretamente pelo navegador da internet, **sem precisar publicar ou pagar contas de desenvolvedor na Apple App Store ou Google Play Store**.

---

## 📱 Como Funciona nos Dispositivos

### 🍏 No iPhone e iPad (iOS - Navegador Safari):
1. Acesse o link do aplicativo no **Safari** (ex: `https://seu-dominio.web.app` ou Firebase/Vercel).
2. Toque no botão **Compartilhar** do Safari (ícone do quadrado com a seta para cima `⬆️`).
3. Role para baixo e selecione **"Adicionar à Tela de Início"** (Add to Home Screen).
4. Toque em **Adicionar**.
5. O ícone do app aparecerá na tela do iPhone ao lado dos outros aplicativos normais. Ao abrir, ele roda em tela cheia (sem barra de navegação do Safari), com suporte a câmera, microfone e armazenamento offline.

### 🤖 No Android (Google Chrome):
1. Abra o link no **Chrome**.
2. O navegador exibirá automaticamente um botão/banner: **"Instalar aplicativo"** ou toque nos três pontinhos verticais no canto superior direito e escolha **"Instalar aplicativo"**.
3. O app será instalado no sistema operacional com ícone próprio e abrirá de forma nativa e rápida.

### 💻 No Computador (Chrome / Edge / Windows / Mac):
1. Abra o link no navegador.
2. Clique no ícone de instalação na barra de endereço (lado direito da URL) e clique em **Instalar**.
3. O aplicativo abrirá em uma janela própria, como um software desktop.

---

## 🚀 Como Publicar na Web Gratuitamente em 2 Minutos

### Opção 1: Firebase Hosting (Já configurado neste projeto)
1. Instale o Firebase CLI no terminal: `npm install -g firebase-tools`
2. Faça login: `firebase login`
3. Na pasta raiz ou na pasta `pwa`, inicialize o hosting: `firebase init hosting`
   - Escolha a pasta pública: `pwa`
   - Configure como SPA: `Yes`
4. Execute o comando de publicação: `firebase deploy`
5. O Firebase gerará um link seguro `https://intrepid-carving-69v0l.web.app` pronto para todos os usuários no iPhone e Android!

### Opção 2: Vercel ou Netlify (Arrastar e Soltar)
1. Acesse [app.netlify.com/drop](https://app.netlify.com/drop) ou [vercel.com](https://vercel.com).
2. Arraste a pasta `pwa` para dentro do site.
3. Você receberá um link HTTPS instantâneo para compartilhar com os colaboradores e técnicos de campo.

### Opção 3: GitHub Pages
1. Suba os arquivos da pasta `pwa` para um repositório no GitHub.
2. Nas configurações do repositório, ative **GitHub Pages**.
3. O site estará disponível em `https://seu-usuario.github.io/seu-repositorio`.

---

## 🛠️ Recursos Incluídos nesta Versão PWA:
- **Captura Fotográfica Direta**: Ativação nativa da câmera do celular (traseira) no iPhone e Android.
- **Ditado por Voz**: Botão de microfone usando Web Speech API em português para preenchimento rápido de relatos em campo.
- **Funcionamento Offline (Service Worker)**: Salva em cache para carregar mesmo sem conexão ou em áreas sem sinal.
- **Compartilhamento Rápido**: Botão de envio formatado direto para o WhatsApp e E-mail da gerência/técnicos.
- **Histórico e Gestão**: Consulta, exclusão, marcação de status (Pendente / Resolvido).
- **Cadastro de Colaboradores e Matrículas**.
- **Geração e Impressão de Relatórios em PDF**.
