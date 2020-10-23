package application;

import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
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
	
	@FXML
	
	
	/**
	 * checks whether the signature field is blank
	 */
	private boolean blank = true;
	
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
			                    	 //file doesn't exist. invalid/corrupt files are handled natively, only need to print to stderr
			                    	 System.err.println("Error opening " + Signer.filepath + ": " + e);
			                         Alert alert = new Alert(Alert.AlertType.ERROR);
			                         DialogPane dialogPane = alert.getDialogPane();
			                         alert.setTitle("PDF Signer");
			                         alert.setHeaderText("Error opening PDF file");
			                         alert.setContentText("There was an error opening the PDF file. The specified file does not exist.");
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
		
		canvas.setOnMouseDragged(e -> {
			double x = e.getX() - 1;
			double y = e.getY() - 1;
			g.setFill(javafx.scene.paint.Color.BLACK);
			g.fillRect(x, y, 2, 2);
			blank = false;
		});
	}
	     
	/**
	 * saves the drawn signature
	 */
	public void saveSignature() {
		//if one or both of the fields are empty
		if (blank || nameField.getText().isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
            DialogPane dialogPane = alert.getDialogPane();
            alert.setTitle("PDF Signer");
            alert.setHeaderText("Empty Fields");
            alert.setContentText("Please provide your signature and printed full name");
            alert.showAndWait();
		} else {
			
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
			Runtime.getRuntime().exec("cmd /c osk");
			nameField.requestFocus();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
