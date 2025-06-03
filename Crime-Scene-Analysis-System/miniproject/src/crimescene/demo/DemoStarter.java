package crimescene.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import crimescene.ui.MainUI;

public class DemoStarter extends Application {
    private ProgressBar progressBar;
    private Label statusLabel;
    private Circle loadingSpinner;
    private Timeline loadingTimeline;
    
    @Override
    public void start(Stage primaryStage) {
        // Create splash screen
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        
        VBox splashRoot = new VBox();
        splashRoot.setAlignment(Pos.CENTER);
        splashRoot.setStyle("-fx-border-color: black; -fx-border-width: 2px; -fx-background-color: white;");
        
        Label titleLabel = new Label("Graph-Based Crime Scene Analysis System");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setPadding(new Insets(20));
        
        Label nameLabel = new Label("Kagiso");
        nameLabel.setFont(Font.font("Arial", 16));
        nameLabel.setPadding(new Insets(0, 0, 20, 0));
        
        // Enhanced progress bar
        progressBar = new ProgressBar(0);
        progressBar.setStyle("-fx-accent: #4CAF50; -fx-background-color: #E0E0E0;");
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(20);
        progressBar.setPadding(new Insets(0, 50, 10, 50));
        
        // Loading status label
        statusLabel = new Label("Initializing system...");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setPadding(new Insets(0, 0, 10, 0));
        
        // Loading spinner
        loadingSpinner = new Circle(10, Color.DODGERBLUE);
        loadingSpinner.setStroke(Color.LIGHTBLUE);
        loadingSpinner.setStrokeWidth(2);
        
        // Create spinning animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), loadingSpinner);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Timeline.INDEFINITE);
        rotateTransition.play();
        
        // Create pulsing animation
        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(0.8), loadingSpinner);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.3);
        scaleTransition.setToY(1.3);
        scaleTransition.setCycleCount(Timeline.INDEFINITE);
        scaleTransition.setAutoReverse(true);
        scaleTransition.play();
        
        HBox loadingBox = new HBox(10);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.getChildren().addAll(loadingSpinner, statusLabel);
        
        HBox demoPanel = createDemoPanel();
        
        splashRoot.getChildren().addAll(titleLabel, demoPanel, nameLabel, progressBar, loadingBox);
        
        Scene splashScene = new Scene(splashRoot, 650, 500); 
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();
        
        // Start loading animation
        startLoadingAnimation();
        
        // Launch main application after delay
        new Thread(() -> {
            try {
                Thread.sleep(6000);
                Platform.runLater(() -> {
                    if (loadingTimeline != null) {
                        loadingTimeline.stop();
                    }
                    splashStage.close();
                    MainUI mainUI = new MainUI();
                    mainUI.setMaximized(true);
                    mainUI.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (loadingTimeline != null) {
                        loadingTimeline.stop();
                    }
                    splashStage.close();
                    MainUI mainUI = new MainUI();
                    mainUI.setMaximized(true);
                    mainUI.show();
                });
            }
        }).start();
    }
    
    private void startLoadingAnimation() {
        String[] loadingMessages = {
            "Initializing system...",
            "Loading graph algorithms...",
            "Preparing classification models...",
            "Setting up pathfinding engine...",
            "Optimizing performance...",
            "Finalizing startup..."
        };
        
        loadingTimeline = new Timeline();
        
        for (int i = 0; i < loadingMessages.length; i++) {
            final int index = i;
            final double progress = (double) (i + 1) / loadingMessages.length;
            
            KeyFrame keyFrame = new KeyFrame(
                Duration.seconds(i * 1.0), // Change message every second
                e -> {
                    statusLabel.setText(loadingMessages[index]);
                    progressBar.setProgress(progress);
                    
                    // Add fade transition for smooth text changes
                    FadeTransition fade = new FadeTransition(Duration.millis(300), statusLabel);
                    fade.setFromValue(0.5);
                    fade.setToValue(1.0);
                    fade.play();
                }
            );
            loadingTimeline.getKeyFrames().add(keyFrame);
        }
        
        loadingTimeline.play();
    }
    
    private HBox createDemoPanel() {
        HBox panel = new HBox(15);  // Increased spacing
        panel.setPadding(new Insets(15));
        panel.setAlignment(Pos.CENTER);
        
        // Left side - Classification demo
        VBox classPanel = createDemoBox(
            "Object Classification", 
            "• Identify important evidence\n" +
            "• Classify weapons, tools, blood\n" +
            "• 93% accuracy in tests\n" +
            "• Region Adjacency Graph approach",
            Color.BLACK
        );
        
        // Right side - Pathfinding demo
        VBox pathPanel = createDemoBox(
            "Path Reconstruction", 
            "• Reconstruct criminal paths\n" +
            "• Optimize search patterns\n" +
            "• A* algorithm for speed\n" +
            "• Grid-based spatial analysis",
            Color.DARKGRAY
        );
        
        panel.getChildren().addAll(classPanel, pathPanel);
        return panel;
    }
    
    private VBox createDemoBox(String title, String text, Color bgColor) {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-border-color: gray; -fx-border-width: 1px; -fx-padding: 10;");
        panel.setAlignment(Pos.CENTER);
        
        Label panelTitle = new Label(title);
        panelTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        // Create demo image with subtle animation
        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(250, 150);
        imagePane.setStyle("-fx-background-color: " + toHex(bgColor) + ";");
        
        Rectangle rect = new Rectangle(150, 50, Color.YELLOW);
        rect.setStroke(Color.WHITE);
        rect.setStrokeWidth(2);
        
        Line line1 = new Line(20, 20, 230, 130);
        line1.setStroke(Color.LIGHTGRAY);
        line1.setStrokeWidth(2);
        
        Line line2 = new Line(230, 20, 20, 130);
        line2.setStroke(Color.LIGHTGRAY);
        line2.setStrokeWidth(2);
        
        Circle circle = new Circle(125, 75, 15, Color.RED);
        
        // Add subtle glow effect to the red circle
        FadeTransition glowTransition = new FadeTransition(Duration.seconds(2), circle);
        glowTransition.setFromValue(0.7);
        glowTransition.setToValue(1.0);
        glowTransition.setCycleCount(Timeline.INDEFINITE);
        glowTransition.setAutoReverse(true);
        glowTransition.play();
        
        imagePane.getChildren().addAll(line1, line2, rect, circle);
        
        Label panelLabel = new Label(title.replace(" ", "") + " Module");
        panelLabel.setFont(Font.font("Arial", 12));
        
        TextArea panelText = new TextArea(text);
        panelText.setEditable(false);
        panelText.setPrefRowCount(4);
        panelText.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        panel.getChildren().addAll(panelTitle, imagePane, panelLabel, panelText);
        return panel;
    }
    
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}