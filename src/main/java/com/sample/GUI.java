package com.sample;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.kie.api.runtime.KieSession;


public class GUI {

    private KieSession kSession;
    private JFrame frame;
    private JLabel questionLabel;
    private JButton yesButton;
    private JButton noButton;

    public GUI(KieSession kSession) {
        this.kSession = kSession; // Przechowujemy sesjê Drools
        initializeGUI(); // Inicjalizacja interfejsu graficznego
    }

    private void initializeGUI() {
        frame = new JFrame("Questionnaire");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        yesButton = new JButton("Tak");
        noButton = new JButton("Nie");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);

        frame.add(questionLabel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setSize(500, 300);
        frame.setVisible(true);

        yesButton.addActionListener(new AnswerListener(true));
        noButton.addActionListener(new AnswerListener(false));

        showNextQuestion();
    }

    private void showNextQuestion() {
        kSession.fireAllRules();
        boolean questionFound = false;

        for (Object obj : kSession.getObjects()) {
            if (obj.getClass().getSimpleName().equals("Pytanie")) {
                questionFound = true;
                try {
                    String id = (String) obj.getClass().getMethod("getId").invoke(obj);
                    String tresc = (String) obj.getClass().getMethod("getTresc").invoke(obj);
                    
                    questionLabel.setText(tresc);
                    yesButton.setActionCommand(id + "|true");
                    noButton.setActionCommand(id + "|false");

                    kSession.delete(kSession.getFactHandle(obj));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        if (!questionFound) {
            displayRecommendation();
        }
    }

    private void displayRecommendation() {
        StringBuilder recommendations = new StringBuilder("Rekomendacja:\n");
        boolean hasRecommendations = false;

        for (Object obj : kSession.getObjects()) {
            if (obj.getClass().getSimpleName().equals("Rekomendacja")) {
                hasRecommendations = true;
                try {
                    String produkt = (String) obj.getClass().getMethod("getProdukt").invoke(obj);
                    recommendations.append(produkt).append("\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (hasRecommendations) {
            JOptionPane.showMessageDialog(frame, recommendations.toString(), "Rekomendacje", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, "Brak rekomendacji na podstawie Twoich odpowiedzi.", "Rekomendacje", JOptionPane.INFORMATION_MESSAGE);
        }

        kSession.dispose();
        frame.dispose();
    }

    private class AnswerListener implements ActionListener {
        private final boolean answer;

        public AnswerListener(boolean answer) {
            this.answer = answer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String[] commandParts = e.getActionCommand().split("\\|");
                String id = commandParts[0];
                //String odpowiedz = commandParts[1];

                Object preferencja = kSession.getKieBase().getFactType("com.sample", "Preferencja").newInstance();
                preferencja.getClass().getMethod("setId", String.class).invoke(preferencja, id);
                preferencja.getClass().getMethod("setOdpowiedz", boolean.class).invoke(preferencja, answer);

                kSession.insert(preferencja);
                showNextQuestion();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
