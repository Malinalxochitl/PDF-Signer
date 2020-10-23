package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
/**
 * Java program for signing pdf documents. Prints absolute path to signature to console on success. Prints -1 to console on failure.
 * @author Joel Kurian
 * 
 */
public class Signer extends Application {
	
	/**
	 * filepath of the pdf
	 */
	static String filepath = null;

	/**
	 * @param primaryStage the primary stage of the application
	 * @throws Exception if stage does not start
	 */
	@Override
	public void start(Stage stage) throws Exception {
		
		if (filepath == null) { //invalid number of arguments provided
			System.err.println("Error: invalid number of arguments provided");
			System.out.println("-1");
			Alert alert = new Alert(Alert.AlertType.ERROR);
            DialogPane dialogPane = alert.getDialogPane();
            alert.setTitle("PDF Signer");
            alert.setHeaderText("Error opening PDF file");
            alert.setContentText("Invalid program arguments.");
            alert.showAndWait();
            return;
		}
		stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("display.fxml"))));
		stage.setTitle("PDF Signer");
		stage.show();
	}
	
	/**
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		
		if(args.length == 1) { //only accepts one command line argument, the file path of the pdf
			filepath = args[0];
		}
		Application.launch(args);
	}

}