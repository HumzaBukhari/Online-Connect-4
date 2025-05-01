import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Consumer;

public class Server{
	ServerAcceptThread serverAcceptThread;
	private Consumer<Message> callback;

	ArrayList<ClientThread> clients = new ArrayList<>();
	ArrayList<GameThread> games = new ArrayList<>();
	HashMap<String, User> users;

	Server(Consumer<Message> call){
		gatherUserData();
		callback = call;
		serverAcceptThread = new ServerAcceptThread();
		serverAcceptThread.start();
	}

	public class ServerAcceptThread extends Thread{
		public void run(){
			try(ServerSocket mySocket = new ServerSocket(5555);){
		    	System.out.println("Server started");
				while(true){
					ClientThread c = new ClientThread(mySocket.accept());
					callback.accept(new Message(MessageType.TEXT, "New client connected"));
					clients.add(c);
					c.start();
				}
			}catch(Exception e){
				System.out.println("Server did not start");
				e.printStackTrace();
			}
		}
	}

	class ClientThread extends Thread{
		Socket connection;
		ObjectInputStream in;
		ObjectOutputStream out;

		String username;
		GameThread currentGame;

		ClientThread(Socket s){
			this.connection = s;
		}

		public void handleMessage(Message message){
			switch(message.type){
				case LOGIN:		//Login Attempt
					try{
						if(users.containsKey(message.string1)){									//Username is recognized
							if(users.get(message.string1).password.equals(message.string2)){	//Password is recognized
								if(!users.get(message.string1).active) {						//User is not active
									out.writeObject(new Message(MessageType.LOGIN, "Accept"));
									callback.accept(new Message(MessageType.TEXT, message.string1 + " has logged in"));
									callback.accept(new Message(MessageType.NEWLOGIN, message.string1));
									users.get(message.string1).active = true;
									username = message.string1;
									welcomeMessage();
								}else{															//User is active
									out.writeObject(new Message(MessageType.LOGIN, "Failed", "Account is already logged in"));
									callback.accept(new Message(MessageType.TEXT, "Failed login attempt"));
								}
							}else{																//Password is not recognized
								out.writeObject(new Message(MessageType.LOGIN, "Failed", "Password is incorrect"));
								callback.accept(new Message(MessageType.TEXT, "Failed login attempt"));
							}
						}else{																	//Username is not recognized
							out.writeObject(new Message(MessageType.LOGIN, "Failed", "Username does not exist"));
							callback.accept(new Message(MessageType.TEXT, "Failed login attempt"));
						}
					}catch(IOException e){System.err.println("Login Message Error");}
					break;
				case NEWLOGIN:	//New Login Attempt
					try{
						if(users.containsKey(message.string1)){			//Duplicate username
							out.writeObject(new Message(MessageType.NEWLOGIN, "Failed", "Username already exist"));
							callback.accept(new Message(MessageType.TEXT, "Failed new login attempt"));
						}else{											//Unique username
							out.writeObject(new Message(MessageType.NEWLOGIN, "Accept"));
							callback.accept(new Message(MessageType.TEXT,  "New user " + message.string1 + " has logged in"));
							callback.accept(new Message(MessageType.NEWLOGIN, message.string1));

							User user = new User();
							user.username = message.string1;
							user.password = message.string2;
							user.active = true;
							users.put(message.string1, user);

							username = message.string1;
							welcomeMessage();
						}
					}catch(IOException e){System.err.println("New Login Message Error");}
					break;
				case GAMEREQUEST:
					if(message.string1.equals("Start")){										//Searching for new game
						if(games.isEmpty() || games.get(games.size() - 1).player2 != null){		//Need to make new game
							GameThread g = new GameThread(this);
							callback.accept(new Message(MessageType.TEXT, username + " is waiting for an opponent"));
							games.add(g);
							currentGame = g;
							g.start();
						}else{																	//Already someone waiting
							GameThread lastGame = games.get(games.size() - 1);
							synchronized(lastGame){
								lastGame.player2 = this;
								lastGame.notify();
							}
							currentGame = lastGame;
							callback.accept(new Message(MessageType.TEXT, username + " has joined the game against " + games.get(games.size() - 1).player1.username));
						}
					}else if(message.string1.equals("Cancel")){									//Canceling search
						currentGame.interrupt();
						games.remove(currentGame);
						currentGame = null;
						callback.accept(new Message(MessageType.TEXT, username + " stopped waiting for an opponent"));
					}else{																		//Rematch message
						int clientNumber3;
						if(currentGame.player1 == this)
							clientNumber3 = 1;
						else
							clientNumber3 = 2;
						if(message.string2.equals("Yes")){										//Client wants rematch
							try{
								out.writeObject(new Message(MessageType.TEXT, "[Server] - " + username + "  has requested a rematch"));
								if(clientNumber3 == 1)
									currentGame.player2.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + username + "  has requested a rematch"));
								else
									currentGame.player1.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + username + "  has requested a rematch"));
							}catch(IOException e){System.err.println("Text Message Error");}

							if(clientNumber3 == 1)
								currentGame.player1Rematch = true;
							else
								currentGame.player2Rematch = true;
							synchronized(currentGame){currentGame.notify();}

							callback.accept(new Message(MessageType.TEXT, username + " has requested a rematch"));
						}else{																	//Client doesn't want rematch
							try{
								out.writeObject(new Message(MessageType.GAMEREQUEST, "Return to Main Menu"));
								if(clientNumber3 == 1){
									currentGame.player2.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + username + " has declined a rematch"));
									currentGame.player2.out.writeObject(new Message(MessageType.GAMEREQUEST, "Return to Main Menu"));
									currentGame.player2.currentGame = null;
								}else{
									currentGame.player1.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + username + " has declined a rematch"));
									currentGame.player1.out.writeObject(new Message(MessageType.GAMEREQUEST, "Return to Main Menu"));
									currentGame.player1.currentGame = null;
								}
								currentGame.interrupt();
								games.remove(currentGame);
								currentGame = null;
							}catch(IOException e){System.err.println("Text Message Error");}
							callback.accept(new Message(MessageType.TEXT, username + " has decline the rematch"));
						}
					}
					break;
				case TEXT:		//Send text messages to opponent, server log, and back at user
					int clientNumber1;		//Different switch cases use different client number variables because switch statements are weird and each case isn't a different scope
					if(currentGame.player1 == this)
						clientNumber1 = 1;
					else
						clientNumber1 = 2;

					try{
						out.writeObject(new Message(MessageType.TEXT, username + " - " + message.string1));
						if(clientNumber1 == 1)
							currentGame.player2.out.writeObject(new Message(MessageType.TEXT, username + " - " + message.string1));
						else
							currentGame.player1.out.writeObject(new Message(MessageType.TEXT, username + " - " + message.string1));
					}catch(IOException e){System.err.println("Text Message Error");}
					callback.accept(new Message(MessageType.TEXT, username + " - " + message.string1));
					break;
				case TURN:
					int column = Integer.parseInt(message.string1);		//Client's turn set the lowest field in chosen column to their number

					int clientNumber2;
					if(currentGame.player1 == this)
						clientNumber2 = 1;
					else
						clientNumber2 = 2;
					for(int j = 0; j < 6; j++)
						if(currentGame.field[column][j] == 0){
							currentGame.field[column][j] = clientNumber2;
							break;
						}

					try{
						out.writeObject(new Message(MessageType.TEXT, username + " played " + message.string1));
						if(clientNumber2 == 1){
							currentGame.player2.out.writeObject(new Message(MessageType.TEXT, username + " played " + message.string1));
							currentGame.player2.out.writeObject(new Message(MessageType.TURN, message.string1));
						}else{
							currentGame.player1.out.writeObject(new Message(MessageType.TEXT, username + " played " + message.string1));
							currentGame.player1.out.writeObject(new Message(MessageType.TURN, message.string1));
						}
					}catch(IOException e){System.err.println("Turn Message Error");}
					callback.accept(new Message(MessageType.TEXT, username + " played " + message.string1));

					synchronized(currentGame){currentGame.notify();}	//Tell game thread to stop stalling
					break;
			}
		}

		public void welcomeMessage() throws IOException{
			out.writeObject(new Message(MessageType.TEXT,
				"[Server] - Welcome " + username + "\n" +
				"Games Played: " + users.get(username).gamesPlayed + "\n" +
				"Wins: " + users.get(username).wins + "\n" +
				"Losses: " + users.get(username).losses + "\n" +
				"Draws: " + users.get(username).draws
			));
		}

		public void run(){
			try{
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			}catch(Exception e){System.out.println("Streams not open");}

			while(true){
				try{
					Message data = (Message) in.readObject();
					handleMessage(data);
				}catch(Exception e){		//Client Disconnected
					if(currentGame != null){	//Client disconnected while in a game
						try{
							if(currentGame.player1 == this){
								currentGame.player2.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + username + " has disconnected"));
								currentGame.player2.out.writeObject(new Message(MessageType.GAMEREQUEST, "Return to Main Menu"));
								currentGame.player2.currentGame = null;
							}else{
								currentGame.player1.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + username + " has disconnected"));
								currentGame.player1.out.writeObject(new Message(MessageType.GAMEREQUEST, "Return to Main Menu"));
								currentGame.player1.currentGame = null;
							}
						}catch(IOException e2){System.err.println("Opponent Disconnect Error");}
						currentGame.interrupt();
						games.remove(currentGame);

						callback.accept(new Message(MessageType.TEXT, username + " has disconnected"));
						callback.accept(new Message(MessageType.LOGIN, username));
						users.get(username).active = false;
					}else if(username != null){
						callback.accept(new Message(MessageType.TEXT, username + " has disconnected"));
						callback.accept(new Message(MessageType.LOGIN, username));
						users.get(username).active = false;
					}else
						callback.accept(new Message(MessageType.TEXT, "Client has disconnected without logging in"));

					clients.remove(this);
					break;
				}
			}
		}
	}

	class GameThread extends Thread{
		ClientThread player1;
		ClientThread player2;
		volatile boolean player1Rematch = false;
		volatile boolean player2Rematch = false;

		int[][] field = new int[7][6];

		GameThread(ClientThread player1){
			this.player1 = player1;
		}

		GameThread(ClientThread player1, ClientThread player2){
			this.player1 = player1;
			this.player2 = player2;
		}

		public void run(){
            try{
				if(player2 == null){
					player1.out.writeObject(new Message(MessageType.TEXT, "[Server] - Waiting for opponent"));
					synchronized(this){
						while(player2 == null){        //Stall until opponent is found
							try{wait();}catch(InterruptedException e){}
						}
					}
				}

				player1.out.writeObject(new Message(MessageType.TEXT, "[Server] - Match against " + player2.username));
				player2.out.writeObject(new Message(MessageType.TEXT, "[Server] - Match against " + player1.username));

				player1.out.writeObject(new Message(MessageType.GAMEREQUEST, "Start"));
				player2.out.writeObject(new Message(MessageType.GAMEREQUEST, "Start"));

				boolean player1Turn = true;
				while(true){
					if(player1Turn){
						player1.out.writeObject(new Message(MessageType.TURN, "Yes"));
						player2.out.writeObject(new Message(MessageType.TURN, "No"));
					}else{
						player1.out.writeObject(new Message(MessageType.TURN, "No"));
						player2.out.writeObject(new Message(MessageType.TURN, "Yes"));
					}

					synchronized(this){try{wait();}catch(InterruptedException e){}}		//Stall until turn is played

					if(winCheck() || drawCheck()){break;}		//Check if move resulted in a win or draw

					player1Turn = !player1Turn;
				}

				users.get(player1.username).gamesPlayed++;
				users.get(player2.username).gamesPlayed++;
				if(player1Turn && winCheck()){			//Player 1 wins
					users.get(player1.username).wins++;
					users.get(player2.username).losses++;

					player1.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + player1.username + " wins against " +  player2.username));
					player2.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + player1.username + " wins against " +  player2.username));
					callback.accept(new Message(MessageType.TEXT, player1.username + " wins against " +  player2.username));
				}else if(!player1Turn && winCheck()){	//Player 2 wins
					users.get(player1.username).losses++;
					users.get(player2.username).wins++;

					player1.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + player2.username + " wins against " +  player1.username));
					player2.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + player2.username + " wins against " +  player1.username));
					callback.accept(new Message(MessageType.TEXT, player2.username + " wins against " +  player1.username));
				}else{									//Draw
					users.get(player1.username).draws++;
					users.get(player2.username).draws++;

					player1.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + player1.username + " draws against " +  player2.username));
					player2.out.writeObject(new Message(MessageType.TEXT, "[Server] - " + player1.username + " draws against " +  player2.username));
					callback.accept(new Message(MessageType.TEXT, player1.username + " draws against " +  player2.username));
				}

				player1.out.writeObject(new Message(MessageType.GAMEREQUEST, "End"));
				player2.out.writeObject(new Message(MessageType.GAMEREQUEST, "End"));

				//Wait for both users to try to rematch
				synchronized(this){
					while(!player1Rematch || !player2Rematch){
						try{wait();}catch(InterruptedException e){}
					}
				}

				//Start new game thread and remove current finished one
				GameThread g = new GameThread(player1, player2);
				games.add(g);
				g.start();
				callback.accept(new Message(MessageType.TEXT, "Start rematch between " + player1.username + " and " +  player2.username));

				player1.currentGame = g;
				player2.currentGame = g;
				games.remove(this);
			}catch (IOException e){System.err.println("GameThread Error");}
		}

		//Checks if latest move causes a win
		public boolean winCheck(){
			for(int i = 0; i < 7; i++){
				for(int j = 0; j < 6; j++){
					if(j <= 2)					//Up check
						if(field[i][j] != 0 && field[i][j] == field[i][j + 1] && field[i][j] == field[i][j + 2] && field[i][j] == field[i][j + 3])
							return true;
					if(j <= 2 && i <= 3)		//Right Diagonal check
						if(field[i][j] != 0 && field[i][j] == field[i + 1][j + 1] && field[i][j] == field[i + 2][j + 2] && field[i][j] == field[i + 3][j + 3])
							return true;
					if(j <= 2 && i >= 3)		//Left Diagonal check
						if(field[i][j] != 0 && field[i][j] == field[i - 1][j + 1] && field[i][j] == field[i - 2][j + 2] && field[i][j] == field[i - 3][j + 3])
							return true;
					if(i <= 3)					//Right check
						if(field[i][j] != 0 && field[i][j] == field[i + 1][j] && field[i][j] == field[i + 2][j] && field[i][j] == field[i + 3][j])
							return true;
				}
			}
			return false;
		}

		//Checks if field is completely full (draw)
		public boolean drawCheck(){
			for(int i = 0; i < 7; i++)
				for(int j = 0; j < 6; j++)
					if(field[i][j] == 0)
						return false;
			return true;
		}
	}

	//Gets all user data that is stored in the data file
	public void gatherUserData(){
		users = new HashMap<>();
		try{
			File dataFile = new File("data.txt");
			Scanner dataReader = new Scanner(dataFile);
			while(dataReader.hasNextLine()){
				User user = new User();
				user.username = dataReader.nextLine();
				user.password = dataReader.nextLine();
				user.gamesPlayed = Integer.parseInt(dataReader.nextLine());
				user.wins = Integer.parseInt(dataReader.nextLine());
				user.losses = Integer.parseInt(dataReader.nextLine());
				user.draws = Integer.parseInt(dataReader.nextLine());
				user.active = false;
				users.put(user.username, user);
			}
			dataReader.close();
		}catch(FileNotFoundException e){
			System.out.println("Data file not found");
			e.printStackTrace();
		}
	}

	//Writes all user data back onto the data file
	public void writeUserData(){
		try{
			FileWriter dataWriter = new FileWriter("data.txt", false);	//Overwrite old data
			for(User user : users.values()){
				dataWriter.write(user.username + "\n");
				dataWriter.write(user.password + "\n");
				dataWriter.write(user.gamesPlayed + "\n");
				dataWriter.write(user.wins + "\n");
				dataWriter.write(user.losses + "\n");
				dataWriter.write(user.draws + "\n");
			}
			dataWriter.close();
		}catch (IOException e){
			System.out.println("Data file write error");
			e.printStackTrace();
		}
	}
}