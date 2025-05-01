import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application{
	Server serverConnection;
	
	ListView<String> listItems;
	ListView<String> listUsers;
	HBox lists;
	
	public static void main(String[] args){launch(args);}

	@Override
	public void start(Stage primaryStage) throws Exception{
		serverConnection = new Server(data->{
			Platform.runLater(()->{
				switch(data.type){
					case TEXT:
						listItems.getItems().add(data.string1);
						break;
					case NEWLOGIN:
						listUsers.getItems().add(data.string1);
						break;
					case LOGIN:
						listUsers.getItems().remove(data.string1);
						break;
				}
			});
		});

		listItems = new ListView<String>();
		listItems.setMinWidth(700);
		listUsers = new ListView<String>();
		lists = new HBox(listUsers, listItems);

		BorderPane serverPane = new BorderPane();
		serverPane.setPadding(new Insets(50));
		serverPane.setCenter(lists);

		//Write user data back to data file when server is closed
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>(){
            @Override
            public void handle(WindowEvent t){
				serverConnection.writeUserData();
                Platform.exit();
                System.exit(0);
            }
        });

		primaryStage.setScene(new Scene(serverPane, 1000, 1000));
		primaryStage.setTitle("Connect 4 Server");
		primaryStage.show();
	}
}