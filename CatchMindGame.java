import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List; // List import
import javax.swing.text.*;

public class CatchMindGame extends JFrame {
    // 색상 팔레트
    private final Color BACKGROUND_COLOR = new Color(135, 206, 250); // 하늘색 배경
    private final Color PANEL_COLOR = new Color(100, 149, 237); // 패널 색상
    private final Color BUTTON_COLOR = new Color(100, 149, 237); // 버튼 색상
    
    // 컴포넌트들
    private JTextPane chatArea;
    private JTextField chatInput;
    private JPanel drawingPanel;
    private JLabel timerLabel;
    private JLabel pointsLabel; // [수정] 점수 라벨
    private JPanel playerListPanel;
    private JLabel wordLabel; // [추가] 정답/힌트 표시용 라벨
    
    // 그리기 관련 변수
    private Color currentColor = Color.BLACK;
    private Point lastPoint; // [수정] 마지막 점만 기억
    private boolean isDrawing = false;
    private boolean canDraw = false; // [추가] 서버가 그리라고 허락했는지
    private BufferedImage canvasImage; // [추가] 그림을 버퍼에 저장

    // --- [추가된 변수] ---
    private NetworkManager networkManager;
    private GameController gameController;
    private String playerName;

    public CatchMindGame(String playerName) {
        this.playerName = playerName;
        
        // [수정] Controller와 UI가 서로를 참조
        this.gameController = new GameController(this, playerName);

        setTitle("Catch Mind - " + playerName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);

        // 메인 패널 설정
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 상단 툴바
        JPanel topBar = createTopBar();
        mainPanel.add(topBar, BorderLayout.NORTH);
        
        // 중앙 컨테이너 (채팅, 그림판, 플레이어 목록)
        JPanel centerContainer = new JPanel(new GridBagLayout());
        centerContainer.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 왼쪽 채팅 패널
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.25;
        gbc.weighty = 1.0;
        centerContainer.add(createChatPanel(), gbc);
        
        // 중앙 그림판 패널
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        centerContainer.add(createDrawingPanel(), gbc);
        
        // 오른쪽 플레이어 목록
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.25;
        gbc.weighty = 1.0;
        centerContainer.add(createPlayerPanel(), gbc);
        
        mainPanel.add(centerContainer, BorderLayout.CENTER);
        
        add(mainPanel);
        
        // [추가] 윈도우 종료 시 서버에 알림
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (networkManager != null && networkManager.isConnected()) {
                    // networkManager.sendMessage("QUIT"); // (서버에 QUIT 프로토콜 구현 시)
                }
            }
        });
    }

    // --- [네트워크 연결용 메소드 수정] ---
    public void setupNetwork() {
        // [수정] NetworkManager 생성 시 GameController도 전달
        this.networkManager = new NetworkManager(this, this.gameController, this.playerName);
        
        boolean connected = networkManager.connect("localhost", 12345);
        
        if (connected) {
            addSystemMessage("서버에 성공적으로 연결되었습니다.");
            setVisible(true); // 연결 성공 시에만 창을 보여줌
        } else {
            // 연결 실패 시 프로그램 종료 (팝업은 NetworkManager가 띄움)
            System.exit(0);
        }
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        topBar.setOpaque(false);
        
        JLabel logo = new JLabel("Catch Mind");
        logo.setFont(new Font("SansSerif", Font.BOLD, 32));
        logo.setForeground(Color.WHITE);
        topBar.add(logo);
        
        JLabel subtitle = new JLabel("Network Project Version");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(Color.WHITE);
        topBar.add(subtitle);
        
        topBar.add(Box.createHorizontalStrut(300));
        
        String[] buttonTexts = {"Game Item", "Invite", "Full Screen", "Leave"};
        Color[] buttonColors = {BUTTON_COLOR, new Color(30, 144, 255), new Color(30, 144, 255), new Color(255, 105, 180)};
        
        for (int i = 0; i < buttonTexts.length; i++) {
            JButton button = createStyledButton(buttonTexts[i], buttonColors[i]);
            button.setForeground(Color.WHITE); // [수정] 텍스트 흰색으로
            topBar.add(button);
            
            // [추가] 'Leave' 버튼 기능
            if (buttonTexts[i].equals("Leave")) {
                button.addActionListener(e -> System.exit(0));
            }
        }
        
        return topBar;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(PANEL_COLOR);
        chatPanel.setBorder(createStyledBorder());
        
        JLabel chatTitle = new JLabel("CHATTING •••");
        chatTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        chatTitle.setForeground(Color.WHITE);
        chatPanel.add(chatTitle, BorderLayout.NORTH);
        
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(173, 216, 230));
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(100, 149, 237)));
        scrollPane.setPreferredSize(new Dimension(300, 400));
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        
        chatInput = new JTextField();
        chatInput.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatInput.setBorder(createStyledBorder());
        chatInput.setBackground(Color.WHITE);
        chatInput.setText("Type your guess here...");
        chatInput.setForeground(Color.GRAY);
        
        chatInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (chatInput.getText().equals("Type your guess here...")) {
                    chatInput.setText("");
                    chatInput.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (chatInput.getText().isEmpty()) {
                    chatInput.setText("Type your guess here...");
                    chatInput.setForeground(Color.GRAY);
                }
            }
        });
        
        // [수정] 채팅 로직 - 서버로 전송
        chatInput.addActionListener(e -> {
            String message = chatInput.getText().trim();
            if (!message.isEmpty() && !message.equals("Type your guess here...")) {
                
                // [수정] 로컬 에코 삭제!
                // addChatMessage("You", message, Color.BLUE); 
                
                // [수정] 서버로 메시지 전송
                if (networkManager != null && networkManager.isConnected()) {
                    networkManager.sendMessage("CHAT:" + message);
                }
                
                chatInput.setText("");
            }
        });
        
        chatPanel.add(chatInput, BorderLayout.SOUTH);
        
        return chatPanel;
    }

    private JPanel createDrawingPanel() {
        JPanel drawingContainer = new JPanel(new BorderLayout(5, 5));
        drawingContainer.setBackground(PANEL_COLOR);
        drawingContainer.setBorder(createStyledBorder());
        
        // [수정] 상단 메시지를 '정답/힌트' 라벨로 변경
        wordLabel = new JLabel("Waiting for game to start...", JLabel.CENTER);
        wordLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        wordLabel.setForeground(Color.WHITE);
        drawingContainer.add(wordLabel, BorderLayout.NORTH);
        
        // 그림판
        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // [수정] 버퍼에 저장된 이미지를 그림
                if (canvasImage != null) {
                    g.drawImage(canvasImage, 0, 0, null);
                } else {
                    // 버퍼가 없을 때 (초기)
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(Color.GRAY);
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 30));
                    String text = "PAINT BOARD";
                    FontMetrics fm = g2d.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(text)) / 2;
                    int y = (getHeight() + fm.getHeight()) / 2;
                    g2d.drawString(text, x, y);
                }
            }
        };
        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.setPreferredSize(new Dimension(500, 400));
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // [추가] 캔버스 이미지 버퍼 생성
        drawingPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (getWidth() > 0 && getHeight() > 0) {
                    canvasImage = new BufferedImage(drawingPanel.getWidth(), drawingPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = canvasImage.createGraphics();
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                    drawingPanel.repaint();
                }
            }
        });

        // 마우스 이벤트 추가
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (canDraw) { // [추가] 그리기 권한이 있을 때만
                    isDrawing = true;
                    lastPoint = e.getPoint();
                    // [수정] 로컬 드로잉 대신 서버로 "누름" 신호 전송
                    String colorStr = String.format("%d,%d,%d", currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
                    networkManager.sendMessage(String.format("DRAW:PRESS:%d:%d:%s", lastPoint.x, lastPoint.y, colorStr));
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isDrawing = false;
                lastPoint = null;
                // [수정] 서버로 "뗌" 신호 전송 (필요시)
                // networkManager.sendMessage("DRAW:RELEASE");
            }
        });
        
        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawing && canDraw) {
                    Point currentPoint = e.getPoint();
                    
                    // [수정] 로컬 드로잉 삭제!
                    // Graphics g = drawingPanel.getGraphics(); ... (이하 삭제)
                    
                    // [수정] 서버로 "드래그" 좌표 전송
                    String colorStr = String.format("%d,%d,%d", currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
                    networkManager.sendMessage(String.format("DRAW:DRAG:%d:%d:%d:%d:%s", 
                        lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y, colorStr));
                    
                    lastPoint = currentPoint;
                }
            }
        });
        
        drawingContainer.add(drawingPanel, BorderLayout.CENTER);
        
        // 하단 컨트롤
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        
        // 색상 팔레트
        JPanel colorPalette = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        colorPalette.setOpaque(false);
        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.BLACK, Color.WHITE /* 지우개 */};
        
        for (Color color : colors) {
            JButton colorButton = new JButton();
            colorButton.setBackground(color);
            colorButton.setPreferredSize(new Dimension(30, 30));
            colorButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
            colorButton.addActionListener(e -> currentColor = color);
            colorPalette.add(colorButton);
        }
        
        // 버튼들
        JButton clearBtn = createSmallButton("▭ Clear All");
        
        // [수정] 'Clear' 버튼 - 서버로 전송
        clearBtn.addActionListener(e -> {
            if (canDraw) {
                // 로컬에서 바로 지우지 않음!
                // points.clear();
                // drawingPanel.repaint();
                networkManager.sendMessage("CLEAR");
            }
        });
        
        colorPalette.add(Box.createHorizontalStrut(20));
        colorPalette.add(clearBtn);
        
        bottomPanel.add(colorPalette, BorderLayout.NORTH);
        
        // 타이머와 포인트
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        infoPanel.setOpaque(false);
        
        timerLabel = new JLabel("0:00"); // [수정] 초기값
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        timerLabel.setForeground(Color.GREEN);
        timerLabel.setOpaque(true);
        timerLabel.setBackground(Color.BLACK);
        timerLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        
        // [수정] 'START' 버튼으로 변경
        JButton startButton = createStyledButton("START GAME", new Color(0, 200, 0));
        startButton.addActionListener(e -> {
            networkManager.sendMessage("START");
        });
        
        infoPanel.add(timerLabel);
        infoPanel.add(startButton); // [수정] 포인트 라벨 대신 스타트 버튼
        
        bottomPanel.add(infoPanel, BorderLayout.SOUTH);
        drawingContainer.add(bottomPanel, BorderLayout.SOUTH);
        
        return drawingContainer;
    }

    private JPanel createPlayerPanel() {
        JPanel playerContainer = new JPanel(new BorderLayout(5, 5));
        playerContainer.setBackground(PANEL_COLOR);
        playerContainer.setBorder(createStyledBorder());
        
        playerListPanel = new JPanel();
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        playerListPanel.setOpaque(false);
        
        // [수정] 데모 플레이어 삭제. 서버가 목록을 주면 addPlayer가 호출됨
        
        JScrollPane scrollPane = new JScrollPane(playerListPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        playerContainer.add(scrollPane, BorderLayout.CENTER);
        
        return playerContainer;
    }
    
    // [수정] UI 업데이트용 메소드들
    public void addPlayer(String name, int score, boolean isLeader) {
        JPanel playerPanel = new JPanel(new BorderLayout(10, 5));
        playerPanel.setBackground(isLeader ? new Color(255, 200, 100) : new Color(150, 255, 150));
        playerPanel.setBorder(createStyledBorder());
        playerPanel.setMaximumSize(new Dimension(300, 80)); // 너비 조정
        
        JLabel avatar = new JLabel();
        avatar.setIcon(new ImageIcon(createColorCircle(isLeader ? Color.ORANGE : Color.GRAY)));
        playerPanel.add(avatar, BorderLayout.WEST);
        
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JLabel scoreLabel = new JLabel("Score: " + score);
        scoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        infoPanel.add(nameLabel);
        infoPanel.add(scoreLabel);
        
        playerPanel.add(infoPanel, BorderLayout.CENTER);
        
        if (isLeader) {
            JLabel leaderLabel = new JLabel("Room Leader");
            leaderLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            leaderLabel.setOpaque(true);
            playerPanel.add(leaderLabel, BorderLayout.NORTH);
        }
        
        playerListPanel.add(playerPanel);
        playerListPanel.add(Box.createVerticalStrut(10));
        playerListPanel.revalidate(); // UI 갱신
        playerListPanel.repaint();
    }
    
    // [새 메소드] 서버에서 받은 정보로 플레이어 목록 전체 갱신
    public void updatePlayerList(List<GameController.Player> players) {
        playerListPanel.removeAll();
        for (GameController.Player p : players) {
            // (임시) 방장 구분 로직 필요 (지금은 모두 false)
            addPlayer(p.getName(), p.getScore(), false);
        }
        playerListPanel.revalidate();
        playerListPanel.repaint();
    }

    private Image createAvatarImage() { /* ... (기존과 동일) ... */ return null; }
    private Image createColorCircle(Color color) { /* ... (기존과 동일) ... */ 
        int size = 50;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color.darker());
        g2d.fillOval(5, 5, size-10, size-10);
        g2d.dispose();
        return image;
    }

    private JButton createStyledButton(String text, Color bgColor) { /* ... (기존과 동일) ... */ 
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 둥근 모서리 효과
        button.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(15),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        
        return button;
    }
    
    private JButton createSmallButton(String text) { /* ... (기존과 동일) ... */ 
        JButton button = new JButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    // [새 메소드] 보더 생성기
    private Border createStyledBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
    }
    
    // [새 메소드] NetworkManager가 호출할 채팅 메소드
    public void addChatMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatArea.getStyledDocument();
            
            try {
                // 발신자 스타일
                SimpleAttributeSet senderStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(senderStyle, (sender.equals(this.playerName) ? Color.RED : Color.BLUE));
                StyleConstants.setBold(senderStyle, true);
                
                // 메시지 스타일
                SimpleAttributeSet messageStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(messageStyle, Color.BLACK);
                
                doc.insertString(doc.getLength(), sender + ": ", senderStyle);
                doc.insertString(doc.getLength(), message + "\n", messageStyle);
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    // [새 메소드] 시스템 메시지 (UI 스레드 보장)
    public void addSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatArea.getStyledDocument();
            
            try {
                SimpleAttributeSet systemStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(systemStyle, Color.ORANGE);
                StyleConstants.setItalic(systemStyle, true);
                
                doc.insertString(doc.getLength(), "System: " + message + "\n", systemStyle);
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    // [새 메소드] 서버가 보낸 그림 그리기
    public void drawFromServer(String data) {
        Graphics2D g2d = (Graphics2D) canvasImage.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        String[] parts = data.split(":");
        String type = parts[0];
        
        try {
            if (type.equals("PRESS")) {
                // "PRESS:x:y:r,g,b"
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                String[] rgb = parts[3].split(",");
                Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                
                g2d.setColor(color);
                g2d.fillOval(x - 2, y - 2, 4, 4); // 점 찍기
                
            } else if (type.equals("DRAG")) {
                // "DRAG:x1:y1:x2:y2:r,g,b"
                int x1 = Integer.parseInt(parts[1]);
                int y1 = Integer.parseInt(parts[2]);
                int x2 = Integer.parseInt(parts[3]);
                int y2 = Integer.parseInt(parts[4]);
                String[] rgb = parts[5].split(",");
                Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(x1, y1, x2, y2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        g2d.dispose();
        drawingPanel.repaint(); // 버퍼가 변경되었으니 다시 그리라고 알림
    }
    
    // [새 메소드] 서버가 보낸 캔버스 지우기
    public void clearDrawingPanel() {
        Graphics2D g2d = (Graphics2D) canvasImage.getGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
        g2d.dispose();
        drawingPanel.repaint();
    }
    
    // [새 메소드] 타이머 업데이트
    public void updateTimer(String time) {
        timerLabel.setText(time);
    }
    
    // [새 메소드] 정답/힌트 표시
    public void showWord(String word) {
        wordLabel.setText("Word: " + word);
    }
    
    // [새 메소드] 그리기 활성화/비활성화
    public void setDrawingEnabled(boolean enabled) {
        this.canDraw = enabled;
    }

    // 둥근 테두리를 위한 커스텀 Border 클래스
    class RoundedBorder implements Border { /* ... (기존과 동일) ... */ 
        private int radius;
        
        RoundedBorder(int radius) {
            this.radius = radius;
        }
        
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius + 1, this.radius + 1, this.radius + 2, this.radius);
        }
        
        public boolean isBorderOpaque() {
            return true;
        }
        
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // [수정] JOptionPane 대신 LoginWindow를 생성
            new LoginWindow();
            
            // --- [삭제] ---
            // 닉네임 입력받기 (이 로직은 LoginWindow로 이동됨)
            // String playerName = JOptionPane.showInputDialog(...);
            
            // [삭제] ---
            // if (playerName == null || ...) { ... }
            
            // [삭제] ---
            // CatchMindGame game = new CatchMindGame(playerName.trim());
            // game.setupNetwork(); 
        });
    }
}