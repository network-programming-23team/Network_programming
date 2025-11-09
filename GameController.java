import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import java.awt.Color;

// [수정] GameController는 '두뇌'가 아니라 '메모장' 역할을 합니다.
// 서버가 알려주는 정보를 저장하고, UI에 업데이트하라고 지시합니다.
public class GameController {
    private CatchMindGame gameUI;
    private String myPlayerName;
    private String currentDrawer;
    private String currentWord; // '화가'일 경우에만 저장
    private boolean isMyTurn;
    
    // 플레이어 목록과 점수를 관리
    private Map<String, Player> players;

    // 게임 상태 enum
    public enum GameStatus {
        WAITING,    // 대기 중
        PLAYING     // 게임 진행 중
    }
    
    private GameStatus status;

    // 플레이어 정보 (클라이언트에서도 간단히 저장)
    public class Player {
        private String name;
        private int score;
        
        public Player(String name, int score) {
            this.name = name;
            this.score = score;
        }
        public String getName() { return name; }
        public int getScore() { return score; }
    }

    // [수정] 생성자 - UI 참조만 받음
    public GameController(CatchMindGame gameUI, String myPlayerName) {
        this.gameUI = gameUI;
        this.myPlayerName = myPlayerName;
        this.players = new HashMap<>();
        this.status = GameStatus.WAITING;
        this.isMyTurn = false;
    }

    // --- NetworkManager가 호출할 메소드들 ---
    
    // 서버가 "GAME:START" 메시지를 보냈을 때
    public void handleGameStart() {
        this.status = GameStatus.PLAYING;
        SwingUtilities.invokeLater(() -> {
            gameUI.clearDrawingPanel(); // 그림판 지우기
            gameUI.addSystemMessage("게임이 시작되었습니다!");
        });
    }
    
    // 서버가 "GAME:END" 메시지를 보냈을 때
    public void handleGameEnd() {
        this.status = GameStatus.WAITING;
        this.isMyTurn = false;
        SwingUtilities.invokeLater(() -> {
            gameUI.addSystemMessage("게임이 종료되었습니다.");
            gameUI.setDrawingEnabled(false); // 그리기 비활성화
        });
    }

    // 서버가 "NEW:ROUND:Drawer is PlayerA" 메시지를 보냈을 때
    public void handleNewRound(String message) {
        // "Drawer is PlayerA" 부분 파싱
        String drawerName = message.substring("Drawer is ".length());
        this.currentDrawer = drawerName;
        this.currentWord = null; // 새 라운드 시작 시 단어 초기화
        
        if (myPlayerName.equals(drawerName)) {
            this.isMyTurn = true;
            SwingUtilities.invokeLater(() -> {
                gameUI.addSystemMessage("당신이 그릴 차례입니다!");
                gameUI.setDrawingEnabled(true); // 그리기 활성화
            });
        } else {
            this.isMyTurn = false;
            SwingUtilities.invokeLater(() -> {
                gameUI.addSystemMessage(drawerName + " 님이 그릴 차례입니다.");
                gameUI.setDrawingEnabled(false); // 그리기 비활성화
            });
        }
    }

    // 서버가 "TIME:89" 메시지를 보냈을 때
    public void setTimer(int seconds) {
        String timeString = String.format("%d:%02d", seconds / 60, seconds % 60);
        SwingUtilities.invokeLater(() -> {
            gameUI.updateTimer(timeString);
        });
    }
    
    // 서버가 "WORD:사과" 메시지를 보냈을 때 (화가에게만 옴)
    public void setWord(String word) {
        this.currentWord = word;
        SwingUtilities.invokeLater(() -> {
            gameUI.showWord(word);
        });
    }

    // 서버가 "HINT:2" 메시지를 보냈을 때 (정답자에게만 옴)
    public void setHint(int length) {
        SwingUtilities.invokeLater(() -> {
            gameUI.showWord(length + "글자");
        });
    }
    
    // 서버가 "PLAYERS:..." 메시지를 보냈을 때
    public void updatePlayerList(String data) {
        players.clear();
        String[] playerData = data.split(",");
        
        for (String p : playerData) {
            // "PlayerA (100)" 형식 파싱
            String name = p.substring(0, p.lastIndexOf(" ("));
            int score = Integer.parseInt(p.substring(p.lastIndexOf(" (") + 2, p.length() - 1));
            players.put(name, new Player(name, score));
        }
        
        // UI 업데이트
        SwingUtilities.invokeLater(() -> {
            gameUI.updatePlayerList(new ArrayList<>(players.values()));
        });
    }
    
    public boolean isMyTurn() {
        return isMyTurn;
    }
}