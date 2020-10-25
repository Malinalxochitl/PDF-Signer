package application;

import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

/**
 * Controller class for the GUI
 * @author Joel Kurian
 */
public class Controller implements Initializable {
	
	/**
	 * drawing area for signature
	 */
	@FXML
	private Canvas canvas;
	
	/**
	 * textfield for print name
	 */
	@FXML
	private TextField nameField;
	
	/**
	 * webview for displaying the pdf
	 */
	@FXML
	private WebView web;
	
	/**
	 * checks whether the signature field is blank
	 */
	private boolean blank = true;
	
	/**
	 * previous mouse coordinates for drawing
	 */
	private double prevX = -1, prevY = -1;
	
	/**
	 * application stage
	 */
	private static Stage stage = null;
	
	/**
	 * application scene
	 */
	private static Scene scene = null;
	
	/**
	 * application control
	 */
	private static Controller control = null;
	
	/**
	 * starts the application
	 * @param primaryStage the application stage
	 * @throws Exception if the stage does not start
	 */
	public static void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(Controller.class.getResource("display.fxml"));
		Parent root = loader.load();
		scene = new Scene(root);
		control = loader.getController();
		Controller.stage = primaryStage;
		
		//makes the application start where the mouse is located
		double x = -1, y = -1;
		try {
			Point p = MouseInfo.getPointerInfo().getLocation();
			List<Screen> screenList = Screen.getScreens();
			
			if (p != null && screenList != null && screenList.size() > 1) {
				Rectangle2D bounds;
				
				for (Screen screen : screenList) {
					bounds = screen.getVisualBounds();
					
					if (bounds.contains(p.getX(), p.getY())) {
						x = bounds.getMinX() + ((bounds.getMaxX() - bounds.getMinX() - scene.getWidth()) / 2);
						y = bounds.getMinY() + ((bounds.getMaxY() - bounds.getMinY() - scene.getHeight()) / 2);
						
						stage.setX(x);
						stage.setY(y);
						break;
					}
				}
			}
		} catch (HeadlessException e) {
			e.printStackTrace();
		}
				
		
		stage.centerOnScreen();
		stage.setTitle("PDF Signer");
		stage.setScene(scene);
		stage.show();
	}
	
	/**
	 * initializes all of the UI elements within the application
	 * @param URL location unused
	 * @param ResourceBundle resources unused
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		//initializing the pdf reader
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				 WebEngine engine = web.getEngine();
			     String url = getClass().getResource("/web/viewer.html").toExternalForm();

			     engine.setUserStyleSheetLocation(getClass().getResource("/web.css").toExternalForm());

			     engine.setJavaScriptEnabled(true);
			     engine.load(url);

			     engine.getLoadWorker()
			             .stateProperty()
			             .addListener((observable, oldValue, newValue) -> {
			                 // to debug JS code by showing console.log() calls in IDE console
			                 //JSObject window = (JSObject) engine.executeScript("window");
			                 //window.setMember("java", new JSLogListener());
			                 //engine.executeScript("console.log = function(message){ java.log(message); };");

			                 // this pdf file will be opened on application startup
			                 if (newValue == Worker.State.SUCCEEDED) {
			                     try {
			                         // readFileToByteArray() comes from commons-io library 
			                         byte[] data = FileUtils.readFileToByteArray(new File(Signer.filepath));
			                         String base64 = Base64.getEncoder().encodeToString(data);
			                         // call JS function from Java code
			                         engine.executeScript("openFileFromBase64('" + base64 + "')");
			                         
			                     } catch (IOException e) {
			                    	 //file validation fails or file doesn't exist
			                    	 System.err.println("Error opening " + Signer.filepath + ": " + e);
			                         Alert alert = new Alert(Alert.AlertType.ERROR);
			                         alert.setTitle("PDF Signer");
			                         alert.setHeaderText("Error opening PDF file");
			                         alert.setContentText("There was an error opening the PDF file. The specified file does not exist or is corrupt.");
			                         alert.showAndWait();
			                         System.out.println("-1");
			                         System.exit(0);
			                     }
			                 }
			             });
			}
		});
		
		
		//initializing the signature drawing area
		GraphicsContext g = canvas.getGraphicsContext2D();
		
		//draws a line when you drag the mouse
		canvas.setOnMouseDragged(e -> {
			
			double x = e.getX() - 1; //x coordinate
			double y = e.getY() - 1; //y coordinate
			
			g.setFill(javafx.scene.paint.Color.BLACK);
			
			if (prevX == -1) { //start of line
				g.fillRect(x, y, 2, 2);
				prevX = x;
				prevY = y;
			} else { //continuing the line
				g.strokeLine(prevX, prevY, x, y);
				prevX = x;
				prevY = y;
			}
			blank = false;
		});
		
		//lets you draw a single dot
		canvas.setOnMousePressed(e -> {
			
			double x = e.getX() - 1; //x coordinate
			double y = e.getY() - 1; //y coordinate
			
			g.setFill(javafx.scene.paint.Color.BLACK);
			
			g.fillRect(x, y, 2, 2);
			
			blank = false;
		});
		
		//resets previous values when line ends
		canvas.setOnMouseReleased(e -> {
			prevX = -1;
			prevY = -1;
		});
	}
	     
	/**
	 * saves the signature and name to disk, and then exits application
	 */
	public void saveSignature() {
		//if one or both of the fields are empty
		if (blank || nameField.getText().isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("PDF Signer");
            alert.setHeaderText("Empty Fields");
            alert.setContentText("Please provide your signature and printed full name");
            alert.showAndWait();
            
        //saves information and exit the application
		} else {
			File temp = new File(Signer.filepath);
			String parentFolder = temp.getParent();
			String pdfName = temp.getName().substring(0, temp.getName().length()-4);
			//System.out.println(parentFolder);
			//System.out.println(pdfName);
			
			//saves signature to disk
			try { 
				WritableImage wi = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
				
				Image snapshot = canvas.snapshot(null, wi);
				ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png",temp = new File(parentFolder, pdfName+"-SIG.png"));
				
			//signature failed to save
			} catch (IOException e) {
				System.err.println("Failed to save image: " + e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("PDF Signer");
                alert.setHeaderText("Error saving image");
                alert.setContentText("There was an error saving the signature.");
                alert.showAndWait();
                System.out.println("-1");
                System.exit(0);
			}
			
			//saves the name to disk
			try {
				File file = new File(parentFolder, pdfName+"-NAME.txt");
				file.delete();
				file.createNewFile();
				FileWriter writer = new FileWriter(file, false);
				writer.write(nameField.getText());
				writer.close();
			//name failed to save
			} catch (IOException e) {
				System.err.println("Failed to save name: " + e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("PDF Signer");
                alert.setHeaderText("Error saving name");
                alert.setContentText("There was an error saving the printed name.");
                alert.showAndWait();
                System.out.println("-1");
                System.exit(0);
			}
			
			//return before exit
			System.out.println(parentFolder+'\\'+pdfName+"-SIG.png");
			stage.close();
		}
	}
	
	/**
	 * clears the signature area
	 */
	public void clearCanvas() {
		GraphicsContext g = canvas.getGraphicsContext2D();
		g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
		blank = true;
	}
	
	/**
	 * displays the virtual keyboard
	 */
	public void showKeyboard() {
		try {
			Runtime.getRuntime().exec("cmd /c C:\\Windows\\System32\\osk.exe");
			nameField.requestFocus();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
