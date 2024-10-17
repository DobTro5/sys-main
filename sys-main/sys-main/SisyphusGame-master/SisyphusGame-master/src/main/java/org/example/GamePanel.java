package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.*;

class GamePanel extends JPanel {

    private Image character;
    private Image rock;
    private Image background;
    private Image descentBackground;
    private Image checkpointTexture;
    private Image cloudTexture;
    private Image snowTexture;
    private Image windTexture;
    private Image fallingRockTexture; // Текстура падающего камня
    private Image fallingSnowTexture; // Текстура падающего снега
    private Image snowOverlay; // Полупрозрачный налет снега
    private Image rainTexture; // Текстура дождя

    private double backgroundOffsetX;
    private double backgroundOffsetY;
    private double targetOffsetX;
    private double targetOffsetY;
    private double stepSize = 0.5;
    private double rainStepSize = 0.25;
    private double descentStepSize = 1.0;
    private double snowStepSize = 0.15; // Увеличенная скорость падения снега
    private double windStepSize = 0.5;

    private List<Point> checkpoints;
    private List<Point> fallingRocks;
    private List<Point> snowflakes;
    private List<Point> windGusts;
    private List<Point> rainDrops; // Список капель дождя для анимации

    private int heightCounter = 0;
    private int recordHeight = 0;
    private int lastCheckpoint = 0;

    private Timer inactivityTimer;
    private Timer checkpointTimer;
    private Timer rainTimer;
    private Timer rockRainTimer;
    private Timer descentTimer;
    private Timer snowTimer;
    private Timer windTimer;
    private Timer eventTimer;
    private Timer rainAnimationTimer; // Таймер для анимации капель дождя

    private JButton resetButton; // Кнопка сброса результата

    private boolean isAlive = true;
    private boolean showSaveMessage = false;
    private boolean isRaining = false;
    private boolean isRockRaining = false;
    private boolean isDescending = false;
    private boolean isSnowing = false;
    private boolean isWindy = false;
    private boolean hasDeductedPointsForRockRain = false;

    private boolean isRainActive = false;
    private boolean isRockRainActive = false;
    private boolean isDescentActive = false;
    private boolean isSnowActive = false;
    private boolean isWindActive = false;

    private Random random;
    private Clip rainClip;
    private Clip rockfallClip;
    private Clip descentClip;
    private Clip ascendClip;
    private Clip snowClip;
    private Clip windClip;

    private float snowAlpha = 0.3f;

    private double rockAngle = 0; // Угол вращения камня

    public GamePanel() {
        setLayout(new BorderLayout());

        try {
            character = new ImageIcon(getClass().getClassLoader().getResource("character.png")).getImage();
            rock = new ImageIcon(getClass().getClassLoader().getResource("rock.png")).getImage();
            checkpointTexture = new ImageIcon(getClass().getClassLoader().getResource("checkpoint.png")).getImage();
            background = new ImageIcon(getClass().getClassLoader().getResource("background.jpg")).getImage();
            descentBackground = new ImageIcon(getClass().getClassLoader().getResource("descent_background.jpg")).getImage();
            cloudTexture = new ImageIcon(getClass().getClassLoader().getResource("cloud.png")).getImage();
            fallingRockTexture = new ImageIcon(getClass().getClassLoader().getResource("falling_rock.png")).getImage(); // Падающий камень
            fallingSnowTexture = new ImageIcon(getClass().getClassLoader().getResource("falling_snow.png")).getImage(); // Падающий снег
            snowOverlay = new ImageIcon(getClass().getClassLoader().getResource("snow_overlay.png")).getImage(); // Полупрозрачный налет
            rainTexture = new ImageIcon(getClass().getClassLoader().getResource("rain.png")).getImage(); // Текстура дождя
            loadTextures();
        } catch (NullPointerException e) {
            System.err.println("Изображение не найдено");
            System.exit(1);
        }

        snowflakes = new ArrayList<>();
        windGusts = new ArrayList<>();
        rainDrops = new ArrayList<>(); // Инициализация списка капель дождя
        backgroundOffsetX = 0;
        backgroundOffsetY = 0;
        checkpoints = new ArrayList<>();
        fallingRocks = new ArrayList<>();
        random = new Random();
        loadGameData();

        inactivityTimer = new Timer(2000, e -> triggerDeath());
        inactivityTimer.setRepeats(false);

        checkpointTimer = new Timer(20000, e -> {
            int newCheckpointY = heightCounter + random.nextInt(200) + 100;
            checkpoints.add(new Point(random.nextInt(getWidth()), newCheckpointY));
            lastCheckpoint = newCheckpointY;
            showSaveMessage = true;
            Timer saveMessageTimer = new Timer(5000, evt -> showSaveMessage = false);
            saveMessageTimer.setRepeats(false);
            saveMessageTimer.start();
        });
        checkpointTimer.start();

        rainTimer = new Timer(30000, e -> startRain());
        rainTimer.setInitialDelay(random.nextInt(20000) + 10000);
        rainTimer.start();

        rockRainTimer = new Timer(40000, e -> startRockRain());
        rockRainTimer.setInitialDelay(random.nextInt(30000) + 15000);
        rockRainTimer.start();

        descentTimer = new Timer(random.nextInt(30000) + 20000, e -> startDescent());
        descentTimer.setRepeats(true);
        descentTimer.start();

        snowTimer = new Timer(30000, e -> startSnow());
        snowTimer.setInitialDelay(random.nextInt(20000) + 10000);
        snowTimer.start();

        windTimer = new Timer(40000, e -> startWind());
        windTimer.setInitialDelay(random.nextInt(30000) + 15000);
        windTimer.start();

        eventTimer = new Timer(10000, e -> {
            isSnowing = false; // Убрать снег после события
            snowflakes.clear(); // Очистка снега
            isRockRaining = false; // Убрать камни после события
            fallingRocks.clear(); // Очистка падающих камней
            isRainActive = false; // Убрать дождь после события
            rainDrops.clear(); // Очистка капель дождя
        });
        eventTimer.setRepeats(false);

        rainClip = loadSound("rain.wav");
        rockfallClip = loadSound("rockfall.wav");
        descentClip = loadSound("descent.wav");
        ascendClip = loadSound("ascend.wav");
        snowClip = loadSound("snow.wav");
        windClip = loadSound("wind.wav");

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isAlive) return;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        targetOffsetX += stepSize * 8;
                        break;
                    case KeyEvent.VK_RIGHT:
                        targetOffsetX -= stepSize * 8;
                        break;
                    case KeyEvent.VK_UP:
                        moveBackground(true);
                        break;
                    case KeyEvent.VK_DOWN:
                        moveBackground(false);
                        break;
                }
            }
        });
        setFocusable(true);

        resetButton = new JButton("Сброс результата");
        resetButton.addActionListener(e -> resetGame());
        add(resetButton, BorderLayout.SOUTH);

        // Таймер для анимации дождя
        rainAnimationTimer = new Timer(50, e -> animateRain());
        rainAnimationTimer.start();
    }

    private void startSnow() {
        if (eventTimer.isRunning()) return;
        eventTimer.restart();
        if (isSnowActive) return;
        isSnowActive = true;
        isSnowing = true;
        playSound(snowClip);

        Timer snowGenerationTimer = new Timer(50, e -> { //метод без объявления
            if (random.nextInt(10) < 3) {
                snowflakes.add(new Point(random.nextInt(getWidth()), 0));
            }

            for (Point snowflake : snowflakes) {
                snowflake.y += 5; // Увеличенная скорость падения снега
            }

            snowflakes.removeIf(snowflake -> snowflake.y > getHeight());
            repaint();
        });

        snowGenerationTimer.setRepeats(true);
        snowGenerationTimer.start();

        Timer stopSnowTimer = new Timer(10000, e -> {
            isSnowActive = false;
            isSnowing = false;
            snowGenerationTimer.stop();
        });
        stopSnowTimer.setRepeats(false);
        stopSnowTimer.start();
    }

    private void loadTextures() {
        snowTexture = new ImageIcon(getClass().getClassLoader().getResource("snow.png")).getImage();
    }

    private Clip loadSound(String soundFileName) {
        try {
            URL soundURL = getClass().getClassLoader().getResource(soundFileName);
            if (soundURL == null) {
                System.err.println("Sound file not found: " + soundFileName);
                return null;
            }
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private void startRain() {
        if (eventTimer.isRunning()) return;
        eventTimer.restart();
        if (isRainActive) return;
        isRainActive = true;
        isRaining = true;
        playSound(rainClip);

        // Генерация капель дождя
        Timer rainGenerationTimer = new Timer(100, e -> {
            if (random.nextInt(10) < 3) {
                rainDrops.add(new Point(random.nextInt(getWidth()), 0));
            }
            for (Point rainDrops : rainDrops) {
                rainDrops.y += 2;
            }

            rainDrops.removeIf(rainDrops -> rainDrops.y > getHeight());
            repaint();
        });
        rainGenerationTimer.start();

        Timer stopRainTimer = new Timer(10000, e -> {
            isRainActive = false;
            isRaining = false;
            rainGenerationTimer.stop();
        });
        stopRainTimer.setRepeats(false);
        stopRainTimer.start();
    }

    private void startRockRain() {
        if (eventTimer.isRunning()) return;
        eventTimer.restart();
        if (isRockRainActive) return;
        isRockRainActive = true;
        isRockRaining = true;
        hasDeductedPointsForRockRain = false;
        fallingRocks.clear();
        playSound(rockfallClip);
        Timer rockFallTimer = new Timer(100, e -> {
            if (random.nextInt(10) < 3) {
                fallingRocks.add(new Point(random.nextInt(getWidth()), 0));
            }
            for (Point rock : fallingRocks) {
                rock.y += 5; // Увеличенная скорость падения камней
            }
            fallingRocks.removeIf(rock -> rock.y > getHeight());
            repaint();
        });
        rockFallTimer.setRepeats(true);
        rockFallTimer.start();

        Timer stopRockRainTimer = new Timer(10000, e -> {
            isRockRainActive = false;
            isRockRaining = false;
            rockFallTimer.stop();
        });
        stopRockRainTimer.setRepeats(false);
        stopRockRainTimer.start();
    }

    private void startDescent() {
        if (eventTimer.isRunning()) return;
        eventTimer.restart();
        if (isDescentActive) return;
        isDescentActive = true;
        isDescending = true;
        playSound(descentClip);
        Timer stopDescentTimer = new Timer(random.nextInt(3000) + 5000, e -> {
            isDescentActive = false;
            isDescending = false;
        });
        stopDescentTimer.setRepeats(false);
        stopDescentTimer.start();
    }

    private void startWind() {
        if (eventTimer.isRunning()) return;
        eventTimer.restart();
        if (isWindActive) return;
        isWindActive = true;
        isWindy = true;
        playSound(windClip);
        Timer windGenerationTimer = new Timer(100, e -> {
            if (random.nextInt(10) < 3) {
                windGusts.add(new Point(random.nextInt(getWidth()), 0));
            }

            for (Point windGust : windGusts) {
                windGust.y += 2;
            }

            windGusts.removeIf(windGust -> windGust.y > getHeight());
            repaint();
        });

        windGenerationTimer.setRepeats(true);
        windGenerationTimer.start();

        Timer stopWindTimer = new Timer(10000, e -> {
            isWindActive = false;
            isWindy = false;
            windGenerationTimer.stop();
        });
        stopWindTimer.setRepeats(false);
        stopWindTimer.start();
    }

    private void animateRain() {
        for (Point rainDrop : new ArrayList<>(rainDrops)) {
            rainDrop.y += 5; // Увеличенная скорость падения дождевых капель
        }
        rainDrops.removeIf(rainDrop -> rainDrop.y > getHeight());
        repaint(); // Перерисовка для обновления анимации
    }

    public void moveBackground(boolean isMousePressed) {
        if (!isAlive) return;

        if (isMousePressed) {
            playSound(ascendClip);
            inactivityTimer.restart();
            rockAngle += 10; // Увеличиваем угол вращения при каждом клике
            double currentStepSize = isRaining || isRockRaining || isSnowing || isWindy ? rainStepSize : stepSize;
            if (isDescending) {
                targetOffsetX -= descentStepSize * 8;
                targetOffsetY -= descentStepSize * 8;
                heightCounter -= 4;
            } else {
                targetOffsetX -= currentStepSize * 8;
                targetOffsetY += currentStepSize * 8;
                heightCounter += 2;
            }
        }

        if (isRockRaining && !hasDeductedPointsForRockRain && heightCounter >= 150) {
            heightCounter -= 150;
            hasDeductedPointsForRockRain = true;
        }

        if (isSnowing) {
            backgroundOffsetX += (targetOffsetX - backgroundOffsetX) * snowStepSize;
            backgroundOffsetY += (targetOffsetY - backgroundOffsetY) * snowStepSize;
        } else if (isWindy) {
            backgroundOffsetX += (targetOffsetX - backgroundOffsetX) * windStepSize;
            backgroundOffsetY += (targetOffsetY - backgroundOffsetY) * windStepSize;
        } else {
            backgroundOffsetX += (targetOffsetX - backgroundOffsetX) * 0.1;
            backgroundOffsetY += (targetOffsetY - backgroundOffsetY) * 0.1;
        }

        if (backgroundOffsetY <= -getHeight() || backgroundOffsetY >= getHeight()) {
            backgroundOffsetY = 0;
            targetOffsetY = 0;
        }
        if (backgroundOffsetX <= -getWidth() || backgroundOffsetX >= getWidth()) {
            backgroundOffsetX = 0;
            targetOffsetX = 0;
        }

        if (heightCounter > recordHeight) {
            recordHeight = heightCounter;
        }

        repaint();
    }

    private void triggerDeath() {
        isAlive = false;
        Timer fallAnimationTimer = new Timer(50, e -> {
            backgroundOffsetY -= stepSize * 2;
            repaint();
        });
        fallAnimationTimer.start();

        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showOptionDialog(
                    this,
                    "Вас снесло! Продолжить с последнего чекпоинта?",
                    "Игра окончена",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Продолжить с чекпоинта", "Начать заново"},
                    "Продолжить с чекпоинта"
            );

            fallAnimationTimer.stop();

            if (option == JOptionPane.YES_OPTION) {
                resetToLastCheckpoint();
            } else {
                resetGame();
            }

            saveGameData();
        });
    }

    private void resetToLastCheckpoint() {
        heightCounter = lastCheckpoint;
        backgroundOffsetY = lastCheckpoint;
        isAlive = true;
        inactivityTimer.restart();
    }

    private void resetGame() {
        heightCounter = 0;
        backgroundOffsetY = 0;
        lastCheckpoint = 0;
        checkpoints.clear();
        rainDrops.clear(); // Очистка капель дождя
        isAlive = true;
        inactivityTimer.restart();
    }

    private void saveGameData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("game_data.txt"))) {
            writer.write(lastCheckpoint + "\n");
            writer.write(recordHeight + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGameData() {
        try (BufferedReader reader = new BufferedReader(new FileReader("game_data.txt"))) {
            lastCheckpoint = Integer.parseInt(reader.readLine());
            recordHeight = Integer.parseInt(reader.readLine());
        } catch (IOException | NumberFormatException e) {
            lastCheckpoint = 0;
            recordHeight = 0;
        }
    }

    private void clearRecord() {
        recordHeight = 0;
        saveGameData();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Image currentBackground = isDescending ? descentBackground : background;

        int width = getWidth();
        int height = getHeight();

        int xOffset = (int) backgroundOffsetX % width;
        int yOffset = (int) backgroundOffsetY % height;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                g.drawImage(currentBackground, xOffset + x * width, yOffset + y * height, width, height, this);
            }
        }

        // Маска над текстурой земли -----------------
        Path2D path2D = new Path2D.Float();
        if (isDescending) {
            path2D.moveTo(0, 0);
            path2D.lineTo(width, 0);
            path2D.lineTo(width, height / 2 + 200);
            path2D.lineTo(0, height / 2 - 100);
        } else {
            path2D.moveTo(0, 0);
            path2D.lineTo(width, 0);
            path2D.lineTo(width, height / 2 - 100);
            path2D.lineTo(0, height / 2 + 200);
        }
        path2D.closePath();

        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setPaint(Color.ORANGE);
        graphics2D.fill(path2D);

        int characterY = getHeight() / 2;
        g.drawImage(character, getWidth() / 2 - 25, characterY, 50, 58, this);

        // Вращение камня
        Graphics2D g2d = (Graphics2D) g;
        g2d.rotate(Math.toRadians(rockAngle), getWidth() / 2 + 20 + 35, characterY - 30 + 35);
        g.drawImage(rock, getWidth() / 2 + 20, characterY - 30, 70, 70, this);
        g2d.rotate(-Math.toRadians(rockAngle), getWidth() / 2 + 20 + 35, characterY - 30 + 35); // Возврат к первоначальному состоянию

        for (Point checkpoint : checkpoints) {
            g.drawImage(checkpointTexture, checkpoint.x, checkpoint.y - (int) backgroundOffsetY, 100, 100, this);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Высота: " + heightCounter, 10, 30);
        g.drawString("Рекорд: " + recordHeight, 10, 60);

        if (showSaveMessage) {
            g.drawString("Сохранение", getWidth() - 150, 30);
        }

        // Отображение снега
        g.setColor(Color.WHITE);
        for (Point snowflake : snowflakes) {
            g.drawImage(fallingSnowTexture, snowflake.x, snowflake.y, 5, 5, this); // Рисуем падающий снег
        }

        // Отображение падающих камней
        for (Point rock : fallingRocks) {
            g.drawImage(fallingRockTexture, rock.x, rock.y, 40, 40, this); // Рисуем падающие камни
        }

        // полупрозрачный снег
        if (isSnowing) {
            g.setColor(new Color(255, 255, 255, 192)); // Полупрозрачный белый цвет (75% прозрачности)
            g.fillRect(0, 0, width, height);
        }

        // Отображение ветра
        if (isWindy) {
            g.setColor(Color.BLUE);
            for (Point windGust : windGusts) {
                g.drawLine(windGust.x, windGust.y, windGust.x + 20, windGust.y); // Рисуем порывы ветра
            }
        }
        if (isRaining) {
            g.setColor(Color.WHITE);
            for (Point rainDrops : rainDrops) {
                g.drawLine(rainDrops.x, rainDrops.y, rainDrops.x + 10, rainDrops.y);
            }
        }
    }
}