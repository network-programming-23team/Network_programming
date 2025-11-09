import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.Timer; // Swing Timer 사용

public class CatchMindServer {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private GameRoom gameRoom;
    private ExecutorService threadPool;

    public CatchMindServer() {
        clients = new CopyOnWriteArrayList<>(); // 스레드 안전 리스트로 변경
        gameRoom = new GameRoom(this); // GameRoom이 서버를 참조
        threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Catch Mind Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 모든 클라이언트에게 메시지 브로드캐스트
    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // 특정 클라이언트 제외하고 브로드캐스트
    public synchronized void broadcastExcept(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clients) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }

    // 클라이언트 제거
    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        gameRoom.removePlayer(client.getPlayerName());
        broadcast("SYSTEM:LEAVE:" + client.getPlayerName());
        broadcastPlayerList(); // 플레이어 목록 갱신
    }

    // 플레이어 목록 갱신하여 브로드캐스트
    public synchronized void broadcastPlayerList() {
        StringBuilder playerList = new StringBuilder("PLAYERS:");
        for (String player : gameRoom.getPlayerNames()) {
            int score = gameRoom.getScore(player);
            playerList.append(player).append(" (").append(score).append("),");
        }
        if (playerList.length() > 8) {
            playerList.deleteCharAt(playerList.length() - 1); // 마지막 쉼표 제거
        }
        broadcast(playerList.toString());
    }

    // 게임룸 반환
    public GameRoom getGameRoom() {
        return gameRoom;
    }

    // 클라이언트 핸들러 내부 클래스
    class ClientHandler implements Runnable {
        private Socket socket;
        private CatchMindServer server;
        private BufferedReader in;
        private PrintWriter out;
        private String playerName;

        public ClientHandler(Socket socket, CatchMindServer server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 클라이언트로부터 이름 받기
                playerName = in.readLine();
                if (playerName == null || playerName.trim().isEmpty()) {
                    playerName = "Guest" + new Random().nextInt(1000);
                }
                
                server.getGameRoom().addPlayer(playerName);

                // 입장 알림
                server.broadcast("SYSTEM:JOIN:" + playerName);
                
                // 현재 플레이어 목록 전송
                server.broadcastPlayerList();

                String message;
                while ((message = in.readLine()) != null) {
                    processMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + playerName);
            } finally {
                server.removeClient(this);
                closeConnection();
            }
        }

        private void processMessage(String message) {
            //System.out.println("Received from " + playerName + ": " + message);
            String[] parts = message.split(":", 2); // 첫 번째 : 로만 분리
            String type = parts[0];

            switch (type) {
                case "CHAT":
                    if (parts.length < 2) break;
                    String chatMsg = parts[1];
                    // 채팅 메시지 브로드캐스트
                    server.broadcast("CHAT:" + playerName + ":" + chatMsg);

                    // 정답 체크 (게임이 시작됐고, 화가가 아닌 경우에만)
                    if (server.getGameRoom().isGameStarted() && 
                        !playerName.equals(server.getGameRoom().getCurrentDrawer())) {
                        
                        if (server.getGameRoom().checkAnswer(chatMsg)) {
                            // 정답!
                            server.getGameRoom().processCorrectAnswer(playerName);
                        }
                    }
                    break;

                case "DRAW":
                    // 그림 데이터 브로드캐스트 (그리는 사람 제외)
                    server.broadcastExcept(message, this);
                    break;

                case "CLEAR":
                    // 캔버스 지우기
                    server.broadcast("CLEAR");
                    break;

                case "START":
                    // 게임 시작 (방장만 시작할 수 있게 하려면 추가 로직 필요)
                    if (server.getGameRoom().canStart()) {
                        server.getGameRoom().startGame();
                    }
                    break;

                default:
                    System.out.println("Unknown message type: " + type);
            }
        }

        // '화가'에게 정답을, '정답자'에게 힌트를 보냄
        public void sendWordAndHint() {
            String drawer = server.getGameRoom().getCurrentDrawer();
            String word = server.getGameRoom().getCurrentWord();

            if (this.playerName.equals(drawer)) {
                sendMessage("WORD:" + word); // 화가에게는 정답
            } else {
                sendMessage("HINT:" + word.length()); // 나머지는 글자 수
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        public String getPlayerName() {
            return playerName;
        }

        private void closeConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 게임룸 클래스
    class GameRoom {
        private CatchMindServer server;
        private List<String> players;
        private Map<String, Integer> scores;
        private List<String> words;
        private String currentWord;
        private String currentDrawer;
        private int currentDrawerIndex;
        private boolean gameStarted;
        private Timer gameTimer;
        private int timeRemaining;
        
        public GameRoom(CatchMindServer server) {
            this.server = server;
            players = new CopyOnWriteArrayList<>();
            scores = new ConcurrentHashMap<>();
            words = Arrays.asList(
                "apple", "banana", "car", "dog", "elephant",
                "flower", "guitar", "house", "ice cream", "jacket",
                "kite", "lion", "moon", "notebook", "ocean",
                "piano", "queen", "rainbow", "star", "tree"
            );
            currentDrawerIndex = -1;
            gameStarted = false;
            
            // 1초마다 실행되는 서버 타이머 설정
            setupTimer();
        }
        
        private void setupTimer() {
            gameTimer = new Timer(1000, e -> {
                if (gameStarted && timeRemaining > 0) {
                    timeRemaining--;
                    server.broadcast("TIME:" + timeRemaining);
                    
                    if (timeRemaining == 0) {
                        // 시간 초과!
                        server.broadcast("SYSTEM:Time's up! The answer was: " + currentWord);
                        nextRound();
                    }
                }
            });
        }

        public synchronized void addPlayer(String playerName) {
            if (!players.contains(playerName)) {
                players.add(playerName);
                scores.put(playerName, 0); // 새 플레이어 점수 0점으로 초기화
            }
        }

        public synchronized void removePlayer(String playerName) {
            players.remove(playerName);
            scores.remove(playerName);
            
            // 플레이어가 2명 미만이 되면 게임 중지
            if (players.size() < 2 && gameStarted) {
                gameStarted = false;
                gameTimer.stop();
                server.broadcast("GAME:END");
                server.broadcast("SYSTEM:Not enough players. Game stopped.");
            }
        }

        public synchronized List<String> getPlayerNames() {
            return new ArrayList<>(players);
        }
        
        public synchronized int getScore(String playerName) {
            return scores.getOrDefault(playerName, 0);
        }

        public boolean canStart() {
            return players.size() >= 2 && !gameStarted;
        }

        public void startGame() {
            gameStarted = true;
            server.broadcast("GAME:START");
            // 점수 초기화
            for (String player : players) {
                scores.put(player, 0);
            }
            server.broadcastPlayerList();
            nextRound();
        }
        
        // 정답 처리
        public void processCorrectAnswer(String winnerName) {
            // 점수 계산 (남은 시간 비례)
            int points = 50 + (timeRemaining * 2);
            scores.put(winnerName, scores.get(winnerName) + points);
            
            // '화가'에게도 점수 부여
            if (currentDrawer != null) {
                scores.put(currentDrawer, scores.get(currentDrawer) + 30);
            }
            
            server.broadcast("CORRECT:" + winnerName + " (+" + points + " pt)");
            server.broadcastPlayerList(); // 점수 갱신된 플레이어 목록 전송
            
            // 다음 라운드 진행
            nextRound();
        }

        public void nextRound() {
            if (players.isEmpty()) return;
            
            // 캔버스 초기화
            server.broadcast("CLEAR");
            
            // 다음 화가 지정 (턴 시스템)
            currentDrawerIndex = (currentDrawerIndex + 1) % players.size();
            currentDrawer = players.get(currentDrawerIndex);

            // 랜덤 단어 선택
            Random random = new Random();
            currentWord = words.get(random.nextInt(words.size()));

            // 라운드 정보 방송
            server.broadcast("NEW:ROUND:Drawer is " + currentDrawer);
            System.out.println("New round - Drawer: " + currentDrawer + ", Word: " + currentWord);

            // 타이머 재시작
            timeRemaining = 90; // 90초
            server.broadcast("TIME:" + timeRemaining);
            gameTimer.start();
            
            // '화가'와 '정답자'에게 각각 메시지 전송
            for (ClientHandler client : server.clients) {
                client.sendWordAndHint();
            }
        }

        public boolean checkAnswer(String answer) {
            return currentWord != null && currentWord.equalsIgnoreCase(answer.trim());
        }
        
        public boolean isGameStarted() {
            return gameStarted;
        }

        public String getCurrentWord() {
            return currentWord;
        }

        public String getCurrentDrawer() {
            return currentDrawer;
        }
    }

    public static void main(String[] args) {
        CatchMindServer server = new CatchMindServer();
        server.start();
    }
}