package crimescene.ui;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import crimescene.classification.ObjectClassifier;
import crimescene.pathfinding.PathFinder;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainUI extends Stage {
    private TabPane tabPane;
    private Tab classificationTab;
    private Tab pathfindingTab;
    
    private ImageView classificationImageView;
    private ImageView pathfindingImageView;
    
    private BufferedImage classificationImage;
    private BufferedImage pathfindingImage;
    private BufferedImage pathfindingImageCopy;
    private BufferedImage classificationImageCopy;
    
    private ObjectClassifier objectClassifier;
    private PathFinder pathFinder;
    
    private javafx.scene.shape.Circle startCircle;
    private javafx.scene.shape.Circle endCircle;
    private javafx.scene.shape.Circle greenCircle;
    private javafx.scene.shape.Circle redCircle;
    private Rectangle blueLine;
    
    // Performance metrics
    private long classificationStartTime;
    private long pathfindingStartTime;
    private Label classificationTimeLabel;
    private Label pathfindingTimeLabel;
    private Label classificationAccuracyLabel;
    
    private ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Evidence directory management
    private static final String EVIDENCE_DIR = "evidence";
    private List<String> savedFiles = new ArrayList<>();
    
    // Legend toggle states
    private boolean classificationLegendVisible = true;
    private boolean pathfindingLegendVisible = true;
    private BufferedImage classificationImageNoLegend;
    private BufferedImage pathfindingImageNoLegend;
    
    public MainUI() {
        objectClassifier = new ObjectClassifier();
        pathFinder = new PathFinder();
        createEvidenceDirectory();
        setupUI();
    }
    
    private void createEvidenceDirectory() {
        File evidenceDir = new File(EVIDENCE_DIR);
        if (!evidenceDir.exists()) {
            evidenceDir.mkdirs();
        }
    }
    
    private void setupUI() {
        setTitle("Graph-Based Crime Scene Analysis System");
        
        tabPane = new TabPane();
        setupClassificationTab();
        setupPathfindingTab();
        
        tabPane.getTabs().addAll(classificationTab, pathfindingTab);
        Scene scene = new Scene(tabPane);
        setScene(scene);
    }
    
    private void setupClassificationTab() {
        classificationTab = new Tab("Object Classification");
        
        classificationImageView = new ImageView();
        classificationImageView.setFitHeight(600);
        classificationImageView.setFitWidth(600);
        classificationImageView.setStyle("-fx-border-color: black;");
        
        StackPane imageContainer = new StackPane(classificationImageView);
        ScrollPane scrollPane = new ScrollPane(imageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        Button loadButton = new Button("Load Image");
        Button classifyButton = new Button("Classify Objects");
        Button clearButton = new Button("Clear Objects");
        Button saveButton = new Button("Save to Evidence");
        Button deleteButton = new Button("Delete Evidence Files");
        Button toggleLegendButton = new Button("Toggle Legend");
        
        HBox buttonBox = new HBox(10, loadButton, classifyButton, clearButton, saveButton, deleteButton, toggleLegendButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Metrics panel
        classificationTimeLabel = new Label("Processing Time: N/A");
        classificationAccuracyLabel = new Label("Estimated Accuracy: 93% (test data)");
        
        HBox metricsBox = new HBox(20, classificationTimeLabel, classificationAccuracyLabel);
        metricsBox.setAlignment(Pos.CENTER);
        
        TitledPane metricsPane = new TitledPane("Performance Metrics", metricsBox);
        metricsPane.setCollapsible(false);
        metricsPane.setAlignment(Pos.CENTER);
        
        VBox bottomBox = new VBox(10, buttonBox, metricsPane);
        bottomBox.setAlignment(Pos.CENTER);
        
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(scrollPane);
        mainPane.setBottom(bottomBox);
        
        classificationTab.setContent(mainPane);
        
        loadButton.setOnAction(e -> loadClassificationImage());
        classifyButton.setOnAction(e -> {
            classificationStartTime = System.currentTimeMillis();
            classifyObjects();
        });
        clearButton.setOnAction(e -> clearClassification());
        saveButton.setOnAction(e -> saveClassificationResults());
        deleteButton.setOnAction(e -> deleteEvidenceFiles());
        toggleLegendButton.setOnAction(e -> toggleClassificationLegend());
    }
    
    private void setupPathfindingTab() {
        pathfindingTab = new Tab("Path Reconstruction");
        pathfindingImageView = new ImageView();
        pathfindingImageView.setPreserveRatio(true);
        pathfindingImageView.setFitWidth(700);
        pathfindingImageView.setFitHeight(700);
        pathfindingImageView.setStyle("-fx-border-color: black;");
        
        // Initialize circles for path points
        startCircle = new javafx.scene.shape.Circle(0, 0, 6, javafx.scene.paint.Color.GREEN);
        endCircle = new javafx.scene.shape.Circle(0, 0, 6, javafx.scene.paint.Color.RED);
        startCircle.setVisible(false);
        endCircle.setVisible(false);     
        
        StackPane imageContainer = new StackPane(pathfindingImageView, startCircle, endCircle);
        ScrollPane scrollPane = new ScrollPane(imageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        Button loadButton = new Button("Load Floor Plan");
        Button clearButton = new Button("Clear Path");
        Button saveButton = new Button("Save to Evidence");
        Button deleteButton = new Button("Delete Evidence Files");
        Button toggleLegendButton = new Button("Toggle Legend");
        
        HBox buttonBox = new HBox(10, loadButton, clearButton, saveButton, deleteButton, toggleLegendButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Metrics panel
        pathfindingTimeLabel = new Label("Processing Time: N/A");
        
        TitledPane metricsPane = new TitledPane("Performance Metrics", pathfindingTimeLabel);
        metricsPane.setCollapsible(false);
        
        VBox bottomBox = new VBox(10, buttonBox, metricsPane);
        bottomBox.setPadding(new Insets(10));
        
        // Instructions
        Label instructionLabel = new Label("Click to set start point, then click again to set end point");
        
        // Info on symbols
        Label greenCircleLabel = new Label("   Start");
        Label redCircleLabel = new Label("   End");
        Label blueLineLabel = new Label("  Path Taken");
        
        greenCircle = new javafx.scene.shape.Circle(0, 0, 6, javafx.scene.paint.Color.GREEN);
        redCircle = new javafx.scene.shape.Circle(0, 0, 6, javafx.scene.paint.Color.RED);
        blueLine = new javafx.scene.shape.Rectangle(15, 4, javafx.scene.paint.Color.BLUE);
        greenCircle.setVisible(true);
        redCircle.setVisible(true);
        blueLine.setVisible(true);
        
        HBox infoBoxH1 = new HBox(greenCircle, greenCircleLabel);
        infoBoxH1.setAlignment(Pos.CENTER_LEFT);
        HBox infoBoxH2 = new HBox(redCircle, redCircleLabel);
        infoBoxH2.setAlignment(Pos.CENTER_LEFT);
        HBox infoBoxH3 = new HBox(blueLine, blueLineLabel);
        infoBoxH3.setAlignment(Pos.CENTER_LEFT);
        
        VBox infoBox = new VBox(infoBoxH1, infoBoxH2, infoBoxH3);
        infoBox.setAlignment(Pos.TOP_RIGHT);
    
        VBox topBox = new VBox(instructionLabel, infoBox);
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER);
        
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(scrollPane);
        mainPane.setBottom(bottomBox);
        mainPane.setTop(topBox);
        
        pathfindingTab.setContent(mainPane);
        
        // Mouse click handler for path points
        pathfindingImageView.setOnMouseClicked(e -> {
            if (pathfindingImage == null) return;
            
            if (!startCircle.isVisible()) {
                startCircle.setCenterX(e.getX());
                startCircle.setCenterY(e.getY());
                startCircle.setVisible(true);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Path Selection");
                alert.setHeaderText(null);
                alert.setContentText("Start point selected. Now select the end point.");
                alert.showAndWait();
            } else if (!endCircle.isVisible()) {
                pathfindingStartTime = System.currentTimeMillis();
                endCircle.setCenterX(e.getX());
                endCircle.setCenterY(e.getY());
                endCircle.setVisible(true);
                
                findPath();
                
                // Reset for next path
                startCircle.setVisible(false);
                endCircle.setVisible(false);
            }
        });
        
        loadButton.setOnAction(e -> loadFloorPlan());
        clearButton.setOnAction(e -> clearPath());
        saveButton.setOnAction(e -> savePathResults());
        deleteButton.setOnAction(e -> deleteEvidenceFiles());
        toggleLegendButton.setOnAction(e -> togglePathfindingLegend());
    }
    
    private void classifyObjects() {
        if (classificationImage == null) {
            showAlert("No Image", "Please load an image first.", Alert.AlertType.WARNING);
            return;
        }
        
        ProgressDialog progressDialog = new ProgressDialog("Processing", "Classifying objects...");
        
        executorService.submit(() -> {
            try {
                BufferedImage result = objectClassifier.classifyObjects(classificationImage);
                
                Platform.runLater(() -> {
                    // Store the image without legend
                    classificationImageNoLegend = result;
                    
                    // Add legend to the processed image if enabled
                    BufferedImage imageToDisplay = classificationLegendVisible ? 
                        addLegendToImage(result, "classification") : result;
                    displayClassificationImage(imageToDisplay);
                    classificationImage = imageToDisplay;
                    
                    // Update metrics
                    long timeMs = System.currentTimeMillis() - classificationStartTime;
                    classificationTimeLabel.setText(String.format("Processing Time: %d ms", timeMs));
                    
                    progressDialog.close();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    progressDialog.close();
                    showAlert("Classification Error", 
                            "Error during classification: " + ex.getMessage(), 
                            Alert.AlertType.ERROR);
                });
            }
        });
        
        progressDialog.show();
    }
    
    private void findPath() {
        if (pathfindingImage == null || !startCircle.isVisible() || !endCircle.isVisible()) {
            showAlert("Missing Data", 
                    "Please load a floor plan and select start/end points.", 
                    Alert.AlertType.WARNING);
            return;
        }
        
        ProgressDialog progressDialog = new ProgressDialog("Processing", "Finding optimal path...");
        
        executorService.submit(() -> {
            try {
                Point start = new Point((int)startCircle.getCenterX(), (int)startCircle.getCenterY());
                Point end = new Point((int)endCircle.getCenterX(), (int)endCircle.getCenterY());
                
                BufferedImage result = pathFinder.findOptimalPath(pathfindingImage, start, end);
                
                Platform.runLater(() -> {
                    // Store the image without legend
                    pathfindingImageNoLegend = result;
                    
                    // Add legend to the processed image if enabled
                    BufferedImage imageToDisplay = pathfindingLegendVisible ? 
                        addLegendToImage(result, "pathfinding") : result;
                    displayFloorPlan(imageToDisplay);
                    pathfindingImage = imageToDisplay;
                    
                    // Update metrics
                    long timeMs = System.currentTimeMillis() - pathfindingStartTime;
                    pathfindingTimeLabel.setText(String.format("Processing Time: %d ms", timeMs));
                    
                    progressDialog.close();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    progressDialog.close();
                    showAlert("Pathfinding Error", 
                            "Error during path finding: " + ex.getMessage(), 
                            Alert.AlertType.ERROR);
                });
            }
        });
        
        progressDialog.show();
    }
    
    private void loadClassificationImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image files", "*.jpg", "*.jpeg", "*.png", "*.bmp"));
        
        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                classificationImage = ImageIO.read(file);
                classificationImageCopy = deepCopyBufferedImage(classificationImage);
                displayClassificationImage(classificationImage);
                setTitle("Graph-Based Crime Scene Analysis System - " + file.getName());
                // Reset metrics when new image is loaded
                classificationTimeLabel.setText("Processing Time: N/A");
            } catch (Exception ex) {
                showAlert("Error", "Error loading image: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    private void displayClassificationImage(BufferedImage img) {
        if (img != null) {
            Image fxImage = SwingFXUtils.toFXImage(img, null);
            classificationImageView.setImage(fxImage);
        }
    }
    
    private void saveClassificationResults() {
        if (classificationImage == null) {
            showAlert("No Results", "No results to save.", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "classification_" + timestamp + ".png";
            File file = new File(EVIDENCE_DIR, filename);
            
            ImageIO.write(classificationImage, "png", file);
            savedFiles.add(file.getAbsolutePath());
            
            showAlert("Save Complete", 
                    "Classification results saved to evidence folder:\n" + filename, 
                    Alert.AlertType.INFORMATION);
        } catch (Exception ex) {
            showAlert("Save Error", "Error saving results: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void loadFloorPlan() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image files", "*.jpg", "*.jpeg", "*.png", "*.bmp"));
        
        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                pathfindingImage = ImageIO.read(file);
                pathfindingImageCopy = deepCopyBufferedImage(pathfindingImage);
                startCircle.setVisible(false);
                endCircle.setVisible(false);
                displayFloorPlan(pathfindingImage); 
       
                // Reset metrics when new floor plan is loaded
                pathfindingTimeLabel.setText("Processing Time: N/A");
                
                showAlert("Floor Plan Loaded", 
                        "Floor plan loaded. Click to set start point.", 
                        Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                showAlert("Error", "Error loading floor plan: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    private void displayFloorPlan(BufferedImage img) {
        if (img != null) {
            Image fxImage = SwingFXUtils.toFXImage(img, null);
            pathfindingImageView.setImage(fxImage);  
        }
    }
    
    private void clearPath() {
        if (pathfindingImageCopy != null) {
            displayFloorPlan(pathfindingImageCopy);
            pathfindingImage = deepCopyBufferedImage(pathfindingImageCopy);
            showAlert("Path Cleared", "Path cleared successfully.", Alert.AlertType.INFORMATION);
        }
    }
    
    private void clearClassification() {
        if (classificationImageCopy != null) {
            displayClassificationImage(classificationImageCopy);
            classificationImage = deepCopyBufferedImage(classificationImageCopy);
            showAlert("Classifications Cleared", "Classifications removed successfully.", Alert.AlertType.INFORMATION);
        }
    }
    
    private void savePathResults() {
        if (pathfindingImage == null) {
            showAlert("No Results", "No path results to save.", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "pathfinding_" + timestamp + ".png";
            File file = new File(EVIDENCE_DIR, filename);
            
            ImageIO.write(pathfindingImage, "png", file);
            savedFiles.add(file.getAbsolutePath());
            
            showAlert("Save Complete", 
                    "Path results saved to evidence folder:\n" + filename, 
                    Alert.AlertType.INFORMATION);
        } catch (Exception ex) {
            showAlert("Save Error", "Error saving path results: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void deleteEvidenceFiles() {
        File evidenceDir = new File(EVIDENCE_DIR);
        
        if (!evidenceDir.exists() || !evidenceDir.isDirectory()) {
            showAlert("No Evidence Folder", "Evidence folder does not exist.", Alert.AlertType.WARNING);
            return;
        }
        
        File[] files = evidenceDir.listFiles();
        if (files == null || files.length == 0) {
            showAlert("No Files", "No files found in evidence folder.", Alert.AlertType.INFORMATION);
            return;
        }
        
        // Create selection dialog
        Dialog<List<File>> dialog = new Dialog<>();
        dialog.setTitle("Delete Evidence Files");
        dialog.setHeaderText("Select files to delete from evidence folder:");
        
        // Create checkboxes for each file
        VBox checkBoxContainer = new VBox(5);
        List<CheckBox> checkBoxes = new ArrayList<>();
        
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().matches(".*\\.(png|jpg|jpeg|bmp)$")) {
                CheckBox checkBox = new CheckBox(file.getName() + " (" + formatFileSize(file.length()) + ")");
                checkBox.setUserData(file);
                checkBoxes.add(checkBox);
                checkBoxContainer.getChildren().add(checkBox);
            }
        }
        
        if (checkBoxes.isEmpty()) {
            showAlert("No Image Files", "No image files found in evidence folder.", Alert.AlertType.INFORMATION);
            return;
        }
        
        // Add Select All / Deselect All buttons
        HBox selectButtons = new HBox(10);
        Button selectAllBtn = new Button("Select All");
        Button deselectAllBtn = new Button("Deselect All");
        
        selectAllBtn.setOnAction(e -> checkBoxes.forEach(cb -> cb.setSelected(true)));
        deselectAllBtn.setOnAction(e -> checkBoxes.forEach(cb -> cb.setSelected(false)));
        
        selectButtons.getChildren().addAll(selectAllBtn, deselectAllBtn);
        selectButtons.setAlignment(Pos.CENTER);
        
        ScrollPane scrollPane = new ScrollPane(checkBoxContainer);
        scrollPane.setMaxHeight(300);
        scrollPane.setFitToWidth(true);
        
        VBox content = new VBox(10, selectButtons, scrollPane);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                List<File> selectedFiles = new ArrayList<>();
                for (CheckBox checkBox : checkBoxes) {
                    if (checkBox.isSelected()) {
                        selectedFiles.add((File) checkBox.getUserData());
                    }
                }
                return selectedFiles;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(selectedFiles -> {
            if (selectedFiles.isEmpty()) {
                showAlert("No Selection", "No files selected for deletion.", Alert.AlertType.INFORMATION);
                return;
            }
            
            // Confirm deletion
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Deletion");
            confirmAlert.setHeaderText("Delete Selected Files");
            confirmAlert.setContentText("Are you sure you want to delete " + selectedFiles.size() + " file(s)?");
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    int deletedCount = 0;
                    int failedCount = 0;
                    StringBuilder failedFiles = new StringBuilder();
                    
                    for (File file : selectedFiles) {
                        if (file.delete()) {
                            deletedCount++;
                            savedFiles.remove(file.getAbsolutePath());
                        } else {
                            failedCount++;
                            failedFiles.append(file.getName()).append("\n");
                        }
                    }
                    
                    String message = deletedCount + " file(s) deleted successfully.";
                    if (failedCount > 0) {
                        message += "\n" + failedCount + " file(s) could not be deleted:\n" + failedFiles.toString();
                    }
                    
                    showAlert("Deletion Complete", message, 
                            failedCount > 0 ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
                }
            });
        });
    }
    
    private void toggleClassificationLegend() {
        if (classificationImageNoLegend == null) {
            showAlert("No Processed Image", "Please classify objects first.", Alert.AlertType.WARNING);
            return;
        }
        
        classificationLegendVisible = !classificationLegendVisible;
        
        BufferedImage imageToDisplay = classificationLegendVisible ? 
            addLegendToImage(classificationImageNoLegend, "classification") : classificationImageNoLegend;
        
        displayClassificationImage(imageToDisplay);
        classificationImage = imageToDisplay;
        
        showAlert("Legend " + (classificationLegendVisible ? "Added" : "Removed"), 
                "Legend has been " + (classificationLegendVisible ? "added to" : "removed from") + " the image.", 
                Alert.AlertType.INFORMATION);
    }
    
    private void togglePathfindingLegend() {
        if (pathfindingImageNoLegend == null) {
            showAlert("No Processed Image", "Please find a path first.", Alert.AlertType.WARNING);
            return;
        }
        
        pathfindingLegendVisible = !pathfindingLegendVisible;
        
        BufferedImage imageToDisplay = pathfindingLegendVisible ? 
            addLegendToImage(pathfindingImageNoLegend, "pathfinding") : pathfindingImageNoLegend;
        
        displayFloorPlan(imageToDisplay);
        pathfindingImage = imageToDisplay;
        
        showAlert("Legend " + (pathfindingLegendVisible ? "Added" : "Removed"), 
                "Legend has been " + (pathfindingLegendVisible ? "added to" : "removed from") + " the image.", 
                Alert.AlertType.INFORMATION);
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    private BufferedImage addLegendToImage(BufferedImage originalImage, String imageType) {
        if (originalImage == null) return null;
        
        // Create a new image with extra space for the legend
        int legendHeight = 120;
        int newHeight = originalImage.getHeight() + legendHeight;
        BufferedImage imageWithLegend = new BufferedImage(originalImage.getWidth(), newHeight, BufferedImage.TYPE_INT_RGB);
        
        java.awt.Graphics2D g2d = imageWithLegend.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw the original image
        g2d.drawImage(originalImage, 0, 0, null);
        
        // Create legend background
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, originalImage.getHeight(), originalImage.getWidth(), legendHeight);
        g2d.setColor(java.awt.Color.BLACK);
        g2d.drawRect(0, originalImage.getHeight(), originalImage.getWidth() - 1, legendHeight - 1);
        
        // Set font for legend
        java.awt.Font titleFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 16);
        java.awt.Font itemFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 12);
        
        int legendY = originalImage.getHeight() + 15;
        int itemHeight = 20;
        
        if (imageType.equals("pathfinding")) {
            // Legend for pathfinding
            g2d.setFont(titleFont);
            g2d.drawString("Path Analysis Legend:", 10, legendY);
            
            g2d.setFont(itemFont);
            legendY += 25;
            
            // Start point (Green circle)
            g2d.setColor(java.awt.Color.GREEN);
            g2d.fillOval(15, legendY - 8, 12, 12);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawOval(15, legendY - 8, 12, 12);
            g2d.drawString("Start Point - Origin of movement", 35, legendY);
            
            // End point (Red circle)
            legendY += itemHeight;
            g2d.setColor(java.awt.Color.RED);
            g2d.fillOval(15, legendY - 8, 12, 12);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawOval(15, legendY - 8, 12, 12);
            g2d.drawString("End Point - Final destination", 35, legendY);
            
            // Path line (Blue line)
            legendY += itemHeight;
            g2d.setColor(java.awt.Color.BLUE);
            g2d.setStroke(new java.awt.BasicStroke(3));
            g2d.drawLine(15, legendY - 4, 27, legendY - 4);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.setStroke(new java.awt.BasicStroke(1));
            g2d.drawString("Optimal Path - Calculated route using A* algorithm", 35, legendY);
            
            // Add timestamp and algorithm info
            legendY += itemHeight;
            g2d.setColor(java.awt.Color.GRAY);
            java.awt.Font smallFont = new java.awt.Font("Arial", java.awt.Font.ITALIC, 10);
            g2d.setFont(smallFont);
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            g2d.drawString("Generated: " + timestamp + " | Algorithm: A* Pathfinding | Grid-based spatial analysis", 10, legendY);
            
        } else if (imageType.equals("classification")) {
            // Legend for classification
            g2d.setFont(titleFont);
            g2d.drawString("Object Classification Legend:", 10, legendY);
            
            g2d.setFont(itemFont);
            legendY += 25;
            
            // Detected objects (example colors - adjust based on your classifier)
            g2d.setColor(java.awt.Color.YELLOW);
            g2d.fillRect(15, legendY - 10, 15, 12);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawRect(15, legendY - 10, 15, 12);
            g2d.drawString("Evidence Objects - Highlighted potential evidence", 35, legendY);
            
            legendY += itemHeight;
            g2d.setColor(java.awt.Color.ORANGE);
            g2d.fillRect(15, legendY - 10, 15, 12);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawRect(15, legendY - 10, 15, 12);
            g2d.drawString("Weapons/Tools - Classified dangerous objects", 35, legendY);
            
            legendY += itemHeight;
            g2d.setColor(java.awt.Color.MAGENTA);
            g2d.fillRect(15, legendY - 10, 15, 12);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawRect(15, legendY - 10, 15, 12);
            g2d.drawString("Biological Evidence - Blood, tissue samples", 35, legendY);
            
            // Add timestamp and accuracy info
            legendY += itemHeight;
            g2d.setColor(java.awt.Color.GRAY);
            java.awt.Font smallFont = new java.awt.Font("Arial", java.awt.Font.ITALIC, 10);
            g2d.setFont(smallFont);
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            g2d.drawString("Generated: " + timestamp + " | Accuracy: 93% | Region Adjacency Graph approach", 10, legendY);
        }
        
        g2d.dispose();
        return imageWithLegend;
    }
    
    private BufferedImage deepCopyBufferedImage(BufferedImage original) {
        if (original == null) return null;
        
        BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        copy.getGraphics().drawImage(original, 0, 0, null);
        return copy;
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private static class ProgressDialog extends Stage {
        public ProgressDialog(String title, String message) {
            setTitle(title);
            initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            ProgressIndicator progressIndicator = new ProgressIndicator();
            Label label = new Label(message);
            
            VBox box = new VBox(20, progressIndicator, label);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(20));
            
            Scene scene = new Scene(box, 300, 150);
            setScene(scene);
        }
    }
}