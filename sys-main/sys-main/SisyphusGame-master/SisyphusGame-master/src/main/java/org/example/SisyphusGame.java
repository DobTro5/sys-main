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

public class SisyphusGame extends JFrame {

    private GamePanel gamePanel;

    public SisyphusGame() {
        setTitle("Sisyphus Game");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        gamePanel = new GamePanel();
        add(gamePanel);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                gamePanel.moveBackground(true);
                gamePanel.repaint();
            }
        });

        setVisible(true);
        new Thread(this::gameLoop).start();
    }

    private void gameLoop() {
        while (true) {
            gamePanel.moveBackground(false);
            gamePanel.repaint();
            sleep(16);
        }
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new SisyphusGame();
    }

}