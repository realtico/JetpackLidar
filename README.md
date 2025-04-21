# LiDAR Viewer Android App

Um visualizador de dados LiDAR em tempo real para Android, desenvolvido com **Jetpack Compose**. Este app se conecta via TCP a um servidor LiDAR (real ou simulado) e exibe os pontos em um radar interativo.

---

## 📦 Recursos

- **Renderização em Canvas**: grid circular com 24 raios e 8 círculos concêntricos.
- **Pontos LiDAR**: conversão polar → XY e desenho de pontos em vermelho.
- **Controles de Zoom**: botões ➕/➖ para ajustar escala (1 ↔ 8 m).
- **Tema Dinâmico**: suporte a **dark/light mode**.
- **Overlay de Métricas**: FPS, atraso (delay), escala atual e estado de conexão.
- **Diálogo de Configurações**:
  - Host e porta do servidor TCP
  - Slider de **Max FPS** (1–60)
  - Slider de **Reconnect Delay** (200 ms–1 s)
- **Persistência**: DataStore salva e restaura host, porta e reconnect delay.
- **Reconexão Inteligente**: reconecta automaticamente ao servidor com intervalo configurável.

---

## 🛠️ Pré-requisitos

- **Android Studio** (Arctic Fox ou superior)
- **Kotlin** 1.5+
- **Jetpack Compose** com **Material3**
- Dependências no `build.gradle.kts`:
  ```kotlin
  implementation(platform("androidx.compose:compose-bom:<versão>"))
  implementation("androidx.datastore:datastore-preferences")
  implementation(libs.androidx.material.icons.extended)
  // Compose UI, Material3, Activity Compose, etc.
  ```

---

## 🚀 Instalação

1. Clone o repositório:
   ```bash
   git clone https://github.com/realtico/JetpackLidar.git
   ```
2. Abra no Android Studio.
3. No `MainActivity.onCreate()`, configure o DataStore:
   ```kotlin
   override fun onCreate(...) {
     super.onCreate(...)  
     DataStoreUtil.init(applicationContext)
     setContent { ... }
   }
   ```
4. Sincronize o Gradle e rode o app em um emulador ou dispositivo.

---

## ⚙️ Configuração em Tempo de Execução

- Toque no ícone ⚙️ no canto superior para abrir o **HostPortDialog**.
- Ajuste:
  - **Host**: IP ou hostname do servidor LiDAR (ex: `10.0.2.2` para emulador).
  - **Porta**: porta TCP (padrão `9999`).
  - **Max FPS**: taxa máxima de atualização da UI.
  - **Reconnect Delay**: intervalo em ms entre tentativas de reconexão.
- Todas as configurações são salvas automaticamente e reaplicadas no próximo lançamento.

---

## 🎮 Uso

1. Rode o servidor LiDAR do projeto [InetLidar](https://github.com/realtico/InetLidar) (real ou simulado). Para simulação em C:
   ```bash
   gcc lidar_sim_tcp_bin.c -o lidar_sim && ./lidar_sim
   ```
2. Inicie o app e abra ⚙️.
3. Configure host, porta, FPS e delay.
4. Observe o radar desenhando os pontos em tempo real.
5. Use os botões ➕/➖ para zoom.
6. Desative o overlay ou alterne dark/light via UI.

---

## 🤝 Contribuições

1. Fork este repositório.
2. Crie uma branch (`git checkout -b feature/nome-da-feature`).
3. Faça suas alterações e commit (`git commit -m 'Adiciona X'`).
4. Push na branch (`git push origin feature/nome-da-feature`).
5. Abra um Pull Request.

---

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.

