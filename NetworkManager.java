import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;

public class NetworkManager {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private CatchMindGame gameUI;
    private GameController gameController; // [추가] GameController 참조
    private String playerName;
    private boolean connected;

    // [수정] GameController를 받도록 생성자 변경
    public NetworkManager(CatchMindGame gameUI, GameController gameController, String playerName) {
        this.gameUI = gameUI;
        this.gameController = gameController;
        this.playerName = playerName;
        this.connected = false;
    }

    // 서버에 연결
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 플레이어 이름 전송
            out.println(playerName);

            // 메시지 수신 스레드 시작
            new Thread(new MessageReceiver()).start();

            connected = true;
            System.out.println("Connected to server: " + host + ":" + port);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(gameUI,
                "Failed to connect to server: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // 메시지 전송
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // 메시지 수신 내부 클래스
    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    processMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    System.out.println("Disconnected from server.");
                    gameUI.addSystemMessage("서버와의 연결이 끊겼습니다.");
                }
            } finally {
                connected = false;
            }
        }
        
        // [수정] 서버에서 받은 모든 메시지를 처리
        private void processMessage(String message) {
            // UI 변경은 Swing 스레드에서 처리 (일부 로직은 여기서 바로 처리)
            try {
                String[] parts = message.split(":", 3);
                String type = parts[0];

                switch (type) {
                    case "CHAT":
                        if (parts.length >= 3) {
                            String sender = parts[1];
                            String chatMsg = parts[2];
                            // [수정] 주석 해제!
                            SwingUtilities.invokeLater(() -> {
                                gameUI.addChatMessage(sender, chatMsg);
                            });
                        }
                        break;
                    
                    case "SYSTEM":
                        if (parts.length >= 2) {
                            SwingUtilities.invokeLater(() -> {
                                gameUI.addSystemMessage(parts[1]);
                            });
                        }
                        break;
                        
                    case "DRAW": // "DRAW:x1:y1:x2:y2:Color"
                        if (parts.length >= 2) {
                            SwingUtilities.invokeLater(() -> {
                                gameUI.drawFromServer(parts[1]);
                            });
                        }
                        break;
                        
                    case "CLEAR":
                        SwingUtilities.invokeLater(() -> {
                            gameUI.clearDrawingPanel();
                        });
                        break;

                    case "GAME": // "GAME:START" or "GAME:END"
                        if (parts[1].equals("START")) {
                            gameController.handleGameStart();
                        } else if (parts[1].equals("END")) {
                            gameController.handleGameEnd();
                        }
                        break;
                        
                    case "NEW:ROUND": // "NEW:ROUND:Drawer is PlayerA"
                        if (parts.length >= 2) {
                            gameController.handleNewRound(parts[1]);
                        }
                        break;

                    case "WORD": // "WORD:사과" (화가에게만)
                        if (parts.length >= 2) {
                            gameController.setWord(parts[1]);
                        }
                        break;

                    case "HINT": // "HINT:2" (정답자에게)
                        if (parts.length >= 2) {
                            gameController.setHint(Integer.parseInt(parts[1]));
                        }
                        break;

                    case "CORRECT": // "CORRECT:PlayerB (..."
                        if (parts.length >= 2) {
                            String winnerMsg = parts[1];
                            SwingUtilities.invokeLater(() -> {
                                gameUI.addSystemMessage(winnerMsg + " 님이 정답을 맞췄습니다!");
                            });
                        }
                        break;
                        
                    case "TIME": // "TIME:89"
                        if (parts.length >= 2) {
                            gameController.setTimer(Integer.parseInt(parts[1]));
                        }
                        break;
                        
                    case "PLAYERS": // "PLAYERS:PlayerA (0),PlayerB (0)"
                         if (parts.length >= 2) {
                            gameController.updatePlayerList(parts[1]);
                        }
                        break;

                    default:
                        System.out.println("Unknown message type: " + type);
                }
                
            } catch (Exception e) {
                System.out.println("Error processing message: " + message);
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getPlayerName() {
        return playerName;
    }
}