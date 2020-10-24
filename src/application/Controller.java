package application;

import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
			                    	 //pdf validation
			                    	 RandomAccessFile accessFile = new RandomAccessFile(new File(Signer.filepath), "r");
			                    	 PDFParser parser = new PDFParser(accessFile);
			                    	 parser.setLenient(false);
			                    	 parser.parse();
			                    	 parser.getPDDocument().close();
			                    	 
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
		
		canvas.setOnMouseDragged(e -> {
			double x = e.getX() - 1;
			double y = e.getY() - 1;
			g.setFill(javafx.scene.paint.Color.BLACK);
			g.fillRect(x, y, 2, 2);
			blank = false;
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
			int i = Signer.filepath.lastIndexOf("/");
			String parentFolder = Signer.filepath.substring(0, i); //parent folder of pdf file
			String pdfName = Signer.filepath.substring(i+1, Signer.filepath.length()-4); //name of the pdf
			
			//saves signature to disk
			try { 
				WritableImage wi = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
				
				Image snapshot = canvas.snapshot(null, wi);
				ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", new File(parentFolder, pdfName+"-SIG.png"));
				
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
			System.out.println(parentFolder+'/'+pdfName+"-SIG.png");
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
			Runtime.getRuntime().exec("cmd /c osk");
			nameField.requestFocus();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
