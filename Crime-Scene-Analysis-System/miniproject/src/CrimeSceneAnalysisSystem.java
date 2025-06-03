import crimescene.demo.DemoStarter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.io.File;
import java.util.Optional;

/**
 * Main entry point for the Graph-Based Crime Scene Analysis System
 * 
 * This application provides graph-based analysis tools for crime scene investigation:
 * - Object Classification using Region Adjacency Graph approach
 * - Path Reconstruction using A* pathfinding algorithm
 * 
 * VM Arguments required:
 * --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml,javafx.swing
 * 
 * @author Kagiso
 * @version 1.0
 */
public class CrimeSceneAnalysisSystem {
    
    // Application metadata
    private static final String APP_NAME = "Graph-Based Crime Scene Analysis System";
    private static final String VERSION = "1.0";
    private static final String AUTHOR = "Kagiso";
    
    // System requirements
    private static final String MIN_JAVA_VERSION = "11";
    private static final String REQUIRED_JAVAFX_VERSION = "17";
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println(APP_NAME);
        System.out.println("Version: " + VERSION);
        System.out.println("Author: " + AUTHOR);
        System.out.println("=".repeat(60));
        
        try {
            // Perform system checks before launching
            performSystemChecks();
            
            // Initialize application directories
            initializeApplicationDirectories();
            
            // Set JavaFX system properties for better performance
            setJavaFXProperties();
            
            // Launch the JavaFX application using the DemoStarter class
            System.out.println("Launching application...");
            Application.launch(DemoStarter.class, args);
            
        } catch (Exception e) {
            handleStartupError(e);
            System.exit(1);
        }
    }
    
    /**
     * Performs system compatibility checks
     */
    private static void performSystemChecks() {
        System.out.println("Performing system checks...");
        
        // Check Java version
        String javaVersion = System.getProperty("java.version");
        System.out.println("Java Version: " + javaVersion);
        
        if (!isJavaVersionCompatible(javaVersion)) {
            throw new RuntimeException("Java " + MIN_JAVA_VERSION + " or higher is required. Current: " + javaVersion);
        }
        
        // Check if JavaFX is available
        try {
            Class.forName("javafx.application.Application");
            System.out.println("JavaFX: Available");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JavaFX not found. Please ensure JavaFX modules are properly configured.");
        }
        
        // Check available memory
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        
        System.out.println("Memory - Max: " + formatBytes(maxMemory) + 
                          ", Total: " + formatBytes(totalMemory) + 
                          ", Free: " + formatBytes(freeMemory));
        
        if (maxMemory < 512 * 1024 * 1024) { // Less than 512MB
            System.out.println("WARNING: Low memory available. Consider increasing heap size with -Xmx flag.");
        }
        
        // Check OS compatibility
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        System.out.println("Operating System: " + osName + " " + osVersion);
        
        System.out.println("System checks completed successfully.");
    }
    
    /**
     * Initializes required application directories
     */
    private static void initializeApplicationDirectories() {
        System.out.println("Initializing application directories...");
        
        // Create evidence directory
        File evidenceDir = new File("evidence");
        if (!evidenceDir.exists()) {
            if (evidenceDir.mkdirs()) {
                System.out.println("Created evidence directory: " + evidenceDir.getAbsolutePath());
            } else {
                System.err.println("WARNING: Could not create evidence directory");
            }
        } else {
            System.out.println("Evidence directory exists: " + evidenceDir.getAbsolutePath());
        }
        
        // Create logs directory
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            if (logsDir.mkdirs()) {
                System.out.println("Created logs directory: " + logsDir.getAbsolutePath());
            } else {
                System.err.println("WARNING: Could not create logs directory");
            }
        }
        
        // Create temp directory for processing
        File tempDir = new File("temp");
        if (!tempDir.exists()) {
            if (tempDir.mkdirs()) {
                System.out.println("Created temp directory: " + tempDir.getAbsolutePath());
            } else {
                System.err.println("WARNING: Could not create temp directory");
            }
        }
        
        System.out.println("Directory initialization completed.");
    }
    
    /**
     * Sets JavaFX system properties for optimal performance
     */
    private static void setJavaFXProperties() {
        System.out.println("Configuring JavaFX properties...");
        
        // Enable hardware acceleration if available
        System.setProperty("prism.order", "sw,d3d,es2");
        
        // Set text rendering for better quality
        System.setProperty("prism.text", "t2k");
        
        // Enable LCD text rendering on Windows
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.setProperty("prism.lcdtext", "true");
        }
        
        // Set thread pool size based on available processors
        int processors = Runtime.getRuntime().availableProcessors();
        System.setProperty("javafx.animation.pulse", "60");
        System.setProperty("javafx.animation.fullspeed", "true");
        
        System.out.println("JavaFX properties configured for " + processors + " processors.");
    }
    
    /**
     * Handles startup errors gracefully
     */
    private static void handleStartupError(Exception e) {
        System.err.println("=".repeat(60));
        System.err.println("STARTUP ERROR: " + APP_NAME);
        System.err.println("=".repeat(60));
        System.err.println("Error: " + e.getMessage());
        System.err.println();
        
        // Print stack trace for debugging
        e.printStackTrace();
        
        System.err.println();
        System.err.println("Troubleshooting Tips:");
        System.err.println("1. Ensure Java 11+ is installed");
        System.err.println("2. Verify JavaFX modules are properly configured");
        System.err.println("3. Check VM arguments: --module-path <javafx-path> --add-modules javafx.controls,javafx.fxml,javafx.swing");
        System.err.println("4. Ensure sufficient memory is available");
        System.err.println("5. Check file system permissions for creating directories");
        System.err.println();
        
        // Try to show JavaFX error dialog if possible
        try {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Startup Error - " + APP_NAME);
                alert.setHeaderText("Application failed to start");
                alert.setContentText("Error: " + e.getMessage() + "\n\nCheck console for detailed troubleshooting information.");
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    Platform.exit();
                }
            });
            
            // Give JavaFX time to show the dialog
            Thread.sleep(100);
            
        } catch (Exception dialogException) {
            // If we can't show JavaFX dialog, continue with console output
            System.err.println("Could not display error dialog. Check console output above.");
        }
    }
    
    /**
     * Checks if the current Java version meets minimum requirements
     */
    private static boolean isJavaVersionCompatible(String version) {
        try {
            // Extract major version number
            String[] parts = version.split("\\.");
            int majorVersion;
            
            if (parts[0].equals("1")) {
                // Java 8 format: 1.8.x
                majorVersion = Integer.parseInt(parts[1]);
            } else {
                // Java 9+ format: 9.x, 11.x, etc.
                majorVersion = Integer.parseInt(parts[0]);
            }
            
            return majorVersion >= Integer.parseInt(MIN_JAVA_VERSION);
            
        } catch (Exception e) {
            System.err.println("WARNING: Could not parse Java version: " + version);
            return true; // Assume compatible if we can't parse
        }
    }
    
    /**
     * Formats byte count into human-readable format
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Shutdown hook to clean up resources
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down " + APP_NAME + "...");
            
            // Clean up temp directory
            File tempDir = new File("temp");
            if (tempDir.exists()) {
                deleteDirectory(tempDir);
                System.out.println("Cleaned up temporary files.");
            }
            
            System.out.println("Application shutdown complete.");
        }));
    }
    
    /**
     * Recursively deletes a directory and its contents
     */
    private static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
}