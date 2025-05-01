import java.util.Objects;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiClient extends Application{
	//Thread that handles communication to the server
	Client clientConnection;

	//Login Scene FX Components
	Button newAccountButton;
	Text instructions;

	Text usernameInstructions;
	TextField usernameInput;
	HBox usernameBox;
	Text passwordInstructions;
	TextField passwordInput;
	HBox passwordBox;
	HBox loginInputBox;

	Text loginErrorInstructions;
	Button loginEnterButton;

	VBox loginBox;
	BorderPane loginScenePane;
	Scene loginScene;

	//New Login Scene FX Components
	Button backButton;
	Text newInstructions;
	Text newSubInstructions;

	Text newUsernameInstructions;
	TextField newUsernameInput;
	HBox newUsernameBox;
	Text newPasswordInstructions;
	TextField newPasswordInput;
	HBox newPasswordBox;
	HBox newLoginInputBox;

	Text newLoginErrorInstructions;
	Button newLoginEnterButton;

	VBox newLoginBox;
	BorderPane newLoginScenePane;
	Scene newLoginScene;

	//Main Scene
	Text mainTitle;
	Button playButton;
	boolean searching = false;
	VBox mainMenu;

	TextField messageTextField;
	Button messageSendButton;
	BorderPane messageBox;

	ListView<String> mainLog;
	VBox mainLogBox;

	HBox mainSceneBox;
	Scene mainScene;

	//Game Scene
	Circle[][] circles;
	Group circlesGroup;
	VBox circlesBox;

	Button[] buttons;
	HBox buttonsBox;

	Button rematchButton;
	Button returnToMainMenuButton;
	HBox postGameBox;
	HBox tempBox;

	VBox gameBox;
	HBox gameSceneBox;
	Scene gameScene;

	public static void main(String[] args){launch(args);}

	@Override
	public void start(Stage primaryStage) throws Exception{
		//Opens client thread; Used to communicate with the server
		clientConnection = new Client(data->{
			Platform.runLater(()->{
				switch(data.type){
					case LOGIN:
						if(Objects.equals(data.string1, "Accept")){
							primaryStage.setScene(mainScene);
						}else{
							loginErrorInstructions.setText(data.string2);
							loginEnterButton.setDisable(false);
						}
						break;
					case NEWLOGIN:
						if(Objects.equals(data.string1, "Accept")){
							primaryStage.setScene(mainScene);
						}else{
							newLoginErrorInstructions.setText(data.string2);
							newLoginEnterButton.setDisable(false);
						}
						break;
					case TEXT:
						mainLog.getItems().add(data.string1);
						break;
					case GAMEREQUEST:
						if(Objects.equals(data.string1, "Start")){		//Starting new game
							for(int i = 0; i < 7; i++)						//Reset board
								for(int j = 0; j < 6; j++)
									circles[i][j].setFill(Color.WHITE);
							if(!gameSceneBox.getChildren().contains(mainLogBox)) gameSceneBox.getChildren().add(mainLogBox);	//JavaFX doesn't like adding a component to a container that already has it
							if(!tempBox.getChildren().contains(postGameBox)) tempBox.getChildren().add(postGameBox);
							if(!gameBox.getChildren().contains(buttonsBox)) gameBox.getChildren().add(buttonsBox);
							messageSendButton.setDisable(false);
							primaryStage.setScene(gameScene);

							playButton.setText("Play");		//Reset play for when user returns
							searching = false;
						}else if(Objects.equals(data.string1, "End")){	//Post game options
							if(!tempBox.getChildren().contains(buttonsBox)) tempBox.getChildren().add(buttonsBox);
							if(!gameBox.getChildren().contains(postGameBox)) gameBox.getChildren().add(postGameBox);
							rematchButton.setDisable(false);
						}else{												//Return to main menu
							if(!mainSceneBox.getChildren().contains(mainLogBox)) mainSceneBox.getChildren().add(mainLogBox);
							messageSendButton.setDisable(true);
							primaryStage.setScene(mainScene);
						}
						break;
					case TURN:
						if(Objects.equals(data.string1, "Yes")){			//If it is client's turn, enable all turn buttons that are valid moves
							if(circles[0][5].getFill() == Color.WHITE)
								buttons[0].setDisable(false);
							if(circles[1][5].getFill() == Color.WHITE)
								buttons[1].setDisable(false);
							if(circles[2][5].getFill() == Color.WHITE)
								buttons[2].setDisable(false);
							if(circles[3][5].getFill() == Color.WHITE)
								buttons[3].setDisable(false);
							if(circles[4][5].getFill() == Color.WHITE)
								buttons[4].setDisable(false);
							if(circles[5][5].getFill() == Color.WHITE)
								buttons[5].setDisable(false);
							if(circles[6][5].getFill() == Color.WHITE)
								buttons[6].setDisable(false);
						}else if(Objects.equals(data.string1, "No")){	//If it is not client's turn, disable all turn buttons
							buttons[0].setDisable(true);
							buttons[1].setDisable(true);
							buttons[2].setDisable(true);
							buttons[3].setDisable(true);
							buttons[4].setDisable(true);
							buttons[5].setDisable(true);
							buttons[6].setDisable(true);
						}else{												//Opponent turns set the lowest circle in chosen column to red
							int column = Integer.parseInt(data.string1);
							for(int j = 0; j < 6; j++)
								if(circles[column][j].getFill() == Color.WHITE){
									circles[column][j].setFill(Color.RED);
									break;
								}
						}
				}
			});
		});
		clientConnection.start();

		setupLoginScene();
		setupNewLoginScene();
		setupMainScene();
		setupGameScene();
		mainSceneBox.getChildren().add(mainLogBox);		//Main scene and game scene both use the same log so they need to be moved whenever the scene changes

		newAccountButton.setOnAction(e -> {
			primaryStage.setScene(newLoginScene);
		});
		backButton.setOnAction(e -> {
			primaryStage.setScene(loginScene);
		});

		//Login button sends username and password that user input to the server to be checked
		loginEnterButton.setOnAction(e -> {
			clientConnection.send(new Message(MessageType.LOGIN, usernameInput.getText(), passwordInput.getText()));
			passwordInput.clear();
			loginEnterButton.setDisable(true);
		});

		//New login button checks the format of the new username and password that user input and if correct, sends them to the server to be checked
		newLoginEnterButton.setOnAction(e -> {
			if(newUsernameInput.getText().isEmpty() || newUsernameInput.getText().length() > 10 || newUsernameInput.getText().contains(" ")){
				newLoginErrorInstructions.setText("Invalid Username");
			}else if(newPasswordInput.getText().isEmpty() || newPasswordInput.getText().length() > 10 || newPasswordInput.getText().contains(" ")){
				newLoginErrorInstructions.setText("Invalid Password");
			}else{
				clientConnection.send(new Message(MessageType.NEWLOGIN, newUsernameInput.getText(), newPasswordInput.getText()));
				newPasswordInput.clear();
				newLoginEnterButton.setDisable(true);
			}
		});

		//Play button sends requests to look for an opponent and switches to a cancel button; cancel button cancels request for an opponent and switches back to a play button
		playButton.setOnAction(e -> {
			if(!searching){
				clientConnection.send(new Message(MessageType.GAMEREQUEST, "Start"));
				playButton.setText("Cancel");
				searching = true;
			}else{
				clientConnection.send(new Message(MessageType.GAMEREQUEST, "Cancel"));
				playButton.setText("Play");
				searching = false;
			}
		});

		//For all turn buttons: set the lowest circle in chosen column to blue and sends move to server
		buttons[0].setOnAction(e -> {
			for(int j = 0; j < 6; j++)
				if(circles[0][j].getFill() == Color.WHITE){
					circles[0][j].setFill(Color.BLUE);
					clientConnection.send(new Message(MessageType.TURN, "0"));
					break;
				}
		});
		buttons[1].setOnAction(e -> {
			for(int j = 0; j < 6; j++)
				if(circles[1][j].getFill() == Color.WHITE){
					circles[1][j].setFill(Color.BLUE);
					clientConnection.send(new Message(MessageType.TURN, "1"));
					break;
				}
		});
		buttons[2].setOnAction(e -> {
			for(int j = 0; j < 6; j++)
				if(circles[2][j].getFill() == Color.WHITE){
					circles[2][j].setFill(Color.BLUE);
					clientConnection.send(new Message(MessageType.TURN, "2"));
					break;
				}
		});
		buttons[3].setOnAction(e -> {
			for(int j = 0; j < 6; j++)
				if(circles[3][j].getFill() == Color.WHITE){
					circles[3][j].setFill(Color.BLUE);
					clientConnection.send(new Message(MessageType.TURN, "3"));
					break;
				}
		});
		buttons[4].setOnAction(e -> {
			for(int j = 0; j < 6; j++)
				if(circles[4][j].getFill() == Color.WHITE){
					circles[4][j].setFill(Color.BLUE);
					clientConnection.send(new Message(MessageType.TURN, "4"));
					break;
				}
		});
		buttons[5].setOnAction(e -> {
			for(int j = 0; j < 6; j++)
				if(circles[5][j].getFill() == Color.WHITE){
					circles[5][j].setFill(Color.BLUE);
					clientConnection.send(new Message(MessageType.TURN, "5"));
					break;
				}
		});
		buttons[6].setOnAction(e -> {
			for(int j = 0; j < 6; j++)
				if(circles[6][j].getFill() == Color.WHITE){
					circles[6][j].setFill(Color.BLUE);
					clientConnection.send(new Message(MessageType.TURN, "6"));
					break;
				}
		});

		//Rematch button tells server that user wants a rematch and then disables itself
		rematchButton.setOnAction(e -> {
			clientConnection.send(new Message(MessageType.GAMEREQUEST, "Rematch", "Yes"));
			rematchButton.setDisable(true);
		});

		//Return to main menu button tells server that user doesn't want a rematch; can still be pressed after rematch button has been pressed
		returnToMainMenuButton.setOnAction(e -> {
			clientConnection.send(new Message(MessageType.GAMEREQUEST, "Rematch", "No"));
		});

		//Message send button sends the written message to the server to send to the opponent
		messageSendButton.setOnAction(e -> {
			clientConnection.send(new Message(MessageType.TEXT, messageTextField.getText()));
			messageTextField.clear();
		});

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>(){
            @Override
            public void handle(WindowEvent t){
                Platform.exit();
                System.exit(0);
            }
        });

		primaryStage.setScene(loginScene);
		primaryStage.setTitle("Connect 4 Client");
		primaryStage.show();
	}

	public void setupLoginScene(){
		//New account button is in the top left corner
		newAccountButton = new Button("Create New Account");

		//Login instructions on the top, username input on the left, password input on the right, login error messages and enter button on the bottom
		instructions = new Text("Enter Username and Password");
		instructions.setFont(Font.font(25));

		usernameInstructions = new Text("Username:");
		usernameInstructions.setFont(Font.font(15));
		usernameInput = new TextField();
		usernameBox = new HBox(usernameInstructions, usernameInput);
		usernameBox.setAlignment(Pos.CENTER);

		passwordInstructions = new Text("Password:");
		passwordInstructions.setFont(Font.font(15));
		passwordInput = new TextField();
		passwordBox = new HBox(passwordInstructions, passwordInput);
		passwordBox.setAlignment(Pos.CENTER);

		loginInputBox = new HBox(usernameBox, passwordBox);
		loginInputBox.setSpacing(30);
		loginInputBox.setAlignment(Pos.CENTER);

		loginErrorInstructions = new Text("");
		loginErrorInstructions.setFont(Font.font(15));
		loginErrorInstructions.setFill(Color.RED);

		loginEnterButton = new Button("Enter");

		loginBox = new VBox(instructions, loginInputBox, loginErrorInstructions, loginEnterButton);
		loginBox.setSpacing(20);
		loginBox.setAlignment(Pos.CENTER);

		loginScenePane = new BorderPane();
		loginScenePane.setTop(newAccountButton);
		loginScenePane.setCenter(loginBox);
		loginScenePane.setPadding(new Insets(5, 5, 5, 5));
		loginScenePane.setStyle("-fx-background-color: lightsteelblue;");
		loginScene = new Scene(loginScenePane, 1100,600);
	}

	public void setupNewLoginScene(){
		//New account button is in the top left corner
		backButton = new Button("Back");

		//Login instructions on the top, sub-instructions beneath, username input on the left, password input on the right, login error messages and enter button on the bottom
		newInstructions = new Text("Enter New Username and Password");
		newInstructions.setFont(Font.font(25));
		newSubInstructions = new Text("Username and password must be between 1-10 characters and cannot contain spaces");
		newSubInstructions.setFont(Font.font(15));

		newUsernameInstructions = new Text("Username:");
		newUsernameInstructions.setFont(Font.font(15));
		newUsernameInput = new TextField();
		newUsernameBox = new HBox(newUsernameInstructions, newUsernameInput);
		newUsernameBox.setAlignment(Pos.CENTER);

		newPasswordInstructions = new Text("Password:");
		newPasswordInstructions.setFont(Font.font(15));
		newPasswordInput = new TextField();
		newPasswordBox = new HBox(newPasswordInstructions, newPasswordInput);
		newPasswordBox.setAlignment(Pos.CENTER);

		newLoginInputBox = new HBox(newUsernameBox, newPasswordBox);
		newLoginInputBox.setSpacing(30);
		newLoginInputBox.setAlignment(Pos.CENTER);

		newLoginErrorInstructions = new Text("");
		newLoginErrorInstructions.setFont(Font.font(15));
		newLoginErrorInstructions.setFill(Color.RED);

		newLoginEnterButton = new Button("Enter");

		newLoginBox = new VBox(newInstructions, newSubInstructions, newLoginInputBox, newLoginErrorInstructions, newLoginEnterButton);
		newLoginBox.setSpacing(20);
		newLoginBox.setAlignment(Pos.CENTER);

		newLoginScenePane = new BorderPane();
		newLoginScenePane.setTop(backButton);
		newLoginScenePane.setCenter(newLoginBox);
		newLoginScenePane.setPadding(new Insets(5, 5, 5, 5));
		newLoginScenePane.setStyle("-fx-background-color: lightsteelblue;");
		newLoginScene = new Scene(newLoginScenePane, 1100,600);
	}

	public void setupMainScene(){
		mainTitle = new Text("Connect 4");
		mainTitle.setFont(Font.font(50));

		playButton = new Button("Play");

		mainMenu = new VBox(mainTitle, playButton);
		mainMenu.setStyle("-fx-background-color: lightsteelblue;");
		mainMenu.setAlignment(Pos.CENTER);
		mainMenu.setMinWidth(800);

		messageTextField = new TextField();
		messageTextField.setMinWidth(225);
		messageSendButton = new Button("Send");
		messageSendButton.setDisable(true);
		messageBox = new BorderPane();
		messageBox.setLeft(messageTextField);
		messageBox.setRight(messageSendButton);
		messageBox.setPadding(new Insets(5, 5, 5, 5));

		mainLog = new ListView<>();
		mainLog.setMinHeight(550);

		mainLogBox = new VBox(mainLog, messageBox);
		mainLogBox.setSpacing(10);
		mainLogBox.setMinWidth(300);
		mainLogBox.setStyle("-fx-background-color: antiquewhite;");

		mainSceneBox = new HBox(mainMenu, mainLogBox);
		mainScene = new Scene(mainSceneBox, 1100,600);
	}

	public void setupGameScene(){
		circles = new Circle[7][6];
		circlesGroup = new Group();
		for(int i = 0; i < 7; i++){
			for(int j = 0; j < 6; j++){
				circles[i][j] = new Circle();
				circles[i][j].setRadius(20);
				circles[i][j].setCenterX((i + 1) * 100);
				circles[i][j].setCenterY((5 - j + 1) * 70);
				circles[i][j].setFill(Color.WHITE);
				circlesGroup.getChildren().add(circles[i][j]);
			}
		}

		circlesBox = new VBox(circlesGroup);
		circlesBox.setAlignment(Pos.CENTER);
		circlesBox.setMinWidth(800);

		buttons = new Button[7];
		for(int i = 0; i < 7; i++){
			buttons[i] = new Button(String.valueOf(i + 1));
			buttons[i].setFont(Font.font(15));
		}
		buttonsBox = new HBox(buttons);
		buttonsBox.setAlignment(Pos.CENTER);
		buttonsBox.setSpacing(70);

		gameBox = new VBox(circlesBox, buttonsBox);
		gameBox.setAlignment(Pos.CENTER);
		gameBox.setSpacing(30);
		gameBox.setStyle("-fx-background-color: antiquewhite;");
		gameBox.setMinWidth(800);

		rematchButton = new Button("Rematch");
		rematchButton.setFont(Font.font(15));
		returnToMainMenuButton = new Button("Return to Main Menu");
		returnToMainMenuButton.setFont(Font.font(15));
		postGameBox = new HBox(rematchButton, returnToMainMenuButton);
		postGameBox.setAlignment(Pos.CENTER);
		postGameBox.setSpacing(20);
		tempBox = new HBox(postGameBox);		//Temp box used to store in-game buttons and post-game buttons when not in use

		gameSceneBox = new HBox(gameBox, mainLogBox);
		gameScene = new Scene(gameSceneBox, 1100,600);
	}
}