/* 
 * Project: ChatClient.java
 * Version: 1.0
 * Author: Theo Liu
 * Date: 05-29-2020
 * Description: Opens a GUI that allows a client to connect with a server and chat with other people connected in Direct messages
 * which are private and only between two people, but can only be opened when both are online. Or through a general server chat that anyone
 * can message in and talk to with. Paired with Andy Li's Server.
 */

//imports
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.BorderLayout;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.Socket;

import javax.swing.Timer;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;

import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;

import java.util.ArrayList;

//Main Class
class ChatClient {
    //Variables
    //GUI
    private JFrame window, settingsTab, chatTab,serverMessageWindow;
    private JButton sendButton, clearButton,loginInButton,settingsButton, confirmButton, logOutButton;
    private JLabel introLabel, usernameLabel, portLabel, addressLabel, hubIntroLabel, onlineLabel,guidelineLabel, errorLabel;
    private JTextField typeField, loginField, portField, addressField,serverMsgField;
    private JTextArea serverMsgArea;  
    private JPanel mainPanel, southPanel, hubPanel; 
    //Networking
    private Socket mySocket; //socket for connection
    private BufferedReader input; //reader for network stream
    private PrintWriter output;  //printwriter for network output
    //UserInputs
    private String inputAddress, inputName; //Client username and ip address to server
    private int inputPort; //port
    //Booleans
    private boolean load = true;  //checks if the person is logged in or not
    private boolean running = true;  //allows messages from the server to come
    //ArrayLists
    private ArrayList<String> online; //holds online users' names
    private ArrayList<String> onlineChats;
    private ArrayList<JTextArea> messageAreas; 
    private ArrayList<JTextField> typeFields;
    private ArrayList<JButton> userButtons;
    private ArrayList<JFrame> chatTabs;
    private ArrayList<String> savedMessages; //holds saved messages from DMs incase you closed already
    private String savedServerMessages; //holds saved messages from the general chat
    //Threads
    private Thread hubThread;
  
    // Main Method
    public static void main(String[] args) { 
      new ChatClient().go();
    }
  
    /*
     * Method: go ()
     * Description: launches the Chat Client
     * @param: null
     * @return: null
     */
    public void go() {
        //starting window
        window = new JFrame("Chat Client");
        //serverMessageWindow to be defined later.
        serverMessageWindow = null; 
        
        //Panels
        mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); //Starting Panel
        //part of Chat Panel to be defined later
        southPanel = new JPanel();
        southPanel.setLayout(new GridLayout(2,0));
        
        //default settins for port and address
        inputPort = 5000;
        inputAddress = "127.0.0.1";
        
        //arraylists holding clients
        online = new ArrayList<String>();
        onlineChats = new ArrayList<String>();
        messageAreas = new ArrayList<JTextArea>();
        typeFields = new ArrayList<JTextField>();
        chatTabs = new ArrayList<JFrame>();
        savedMessages = new ArrayList<String>();
        savedServerMessages = "";
        
        //JButtons for starting and settings window
        loginInButton = new JButton("LOGIN");
        loginInButton.addActionListener(new LogInButtonListener());
        settingsButton = new JButton("SETTINGS");
        settingsButton.addActionListener(new SettingsButtonListener());
        confirmButton = new JButton("CONFIRM");
        confirmButton.addActionListener(new ConfirmButtonListener());
        
        //JLabels for starting and settings window
        introLabel = new JLabel("Welcome to the RHHS Messenger! Enter a username to login.");
        usernameLabel = new JLabel("Username:");
        portLabel = new JLabel("Enter port #:");
        addressLabel = new JLabel("Enter address:");
        guidelineLabel = new JLabel("Please note that usernames can't contain '/'. Copyright: ICS4UE");
        errorLabel = new JLabel("");
        
        //JTextFields for starting and settings window
        loginField = new JTextField(30);
        portField = new JTextField("5000");
        addressField = new JTextField("127.0.0.1");
        
        //add everything to the mainPanel.
        mainPanel.add(introLabel);
        mainPanel.add(guidelineLabel);
        mainPanel.add(usernameLabel);
        mainPanel.add(loginField);
        mainPanel.add(loginInButton);
        mainPanel.add(settingsButton);
        //add main panel to the window and set visible.
        window.add(mainPanel);
        window.setSize(400,400);
        window.setVisible(true);
    }
    
    /*
     * Method: connect()
     * Description: Attempts to connect to the server and creates the socket and streams
     * @param: the ip address and the port
     * @return: the socket
     */
    public Socket connect(String ip, int port) { 
        System.out.println("Attempting to make a connection..");
      
        try {
            mySocket = new Socket(ip, port); //attempt socket connection (local address). This will wait until a connection is made
        
            InputStreamReader stream1= new InputStreamReader(mySocket.getInputStream()); //Stream for network input
            input = new BufferedReader(stream1);     
            output = new PrintWriter(mySocket.getOutputStream()); //assign printwriter to network stream
        } catch (IOException e) {  //connection error occured
            load = false; //doesnt login
            System.out.println("Connection to Server Failed");
            e.printStackTrace();
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING)); //closes program if connection fails.
        }
        System.out.println("Connection made.");
        return mySocket;
    } //end of run() method
    
    
    /*
     * Method: readMessagesFromServer()
     * Description: waits for server input (either a Direct Message, General Message, New User Logged in, or User has logged out
     * and then updates the UI. It is called once a while instead of using a loop, as to not interfere with the GUI.
     * @param: null
     * @return: null
     */
    public void readMessagesFromServer() { 
      
        if(running) {  
            try {
                if (input.ready()) { //check for an incoming messge
                    String msg;
                    msg = input.readLine(); //read the messages
            
                    //Checks if the Message is a DM or not
                    if (msg.substring(0,3).equals("/dm")){
                        int slashIndex = msg.indexOf("/",4);
                        String sender = msg.substring(4,slashIndex); //gets the sender of the DM.
                        String actualMsg = msg.substring(slashIndex+1,msg.length()); //gets the DMs content.
                      
                        if (onlineChats.indexOf(sender) == -1){ //If the Chat Window is not open
                            ChatPanel c = new ChatPanel(sender); //Create the panel.
                        
                            for (int i = 0; i < messageAreas.size();i++){ //find the appropriate JTextArea and append the message
                                String s = messageAreas.get(i).getText();
                                if ( (s).substring(35,s.indexOf("\n")).equals(sender)) {
                                    messageAreas.get(i).append(sender+":"+actualMsg+"\n");
                                    i = messageAreas.size();
                                } 
                            }
                        
                        } else { //If the chat window is already open.
                            for (int i = 0; i < messageAreas.size();i++){ //find the appropriate JTextArea and append the message.
                                String s = messageAreas.get(i).getText();
                                if ( (s).substring(35,s.indexOf("\n")).equals(sender)) {
                                    messageAreas.get(i).append(sender+":"+actualMsg+"\n");
                                    i = messageAreas.size();
                                } 
                            }
                        
                        }
                    } else if (msg.substring(0,4).equals("/new")){ //if the server message says a new user has logged in.
                        String newUser = msg.substring(5); //get the user's name.
                      
                        //create the new button with listener
                        JButton newUserButton = new JButton(newUser); 
                        newUserButton.addActionListener(new ChatButtonListener());
                    
                        //add the button the arraylist of buttons and to the hubPanel
                        userButtons.add(newUserButton);
                        hubPanel.add(newUserButton);
                    
                        //update GUI
                        hubPanel.revalidate();
                        hubPanel.repaint();
                      
                    } else if (msg.substring(0,7).equals("/delete")){ //if the server message says an old user has logged out.
                      
                        String oldUser = msg.substring(8); //get the old user's name
                        for (int i = 0; i < userButtons.size(); i++){ //find the old user's button.
                            if (userButtons.get(i).getText().equals(oldUser)){
                                chatTabs.get(i).dispose();
                                messageAreas.remove(i);
                                typeFields.remove(i);
                                userButtons.get(i).setVisible(false);  //remove the button from the panel
                                userButtons.remove(i); //remove the button from the arraylist.
                                i = userButtons.size();
                            }
                        }
                    
                        //update GUI
                        hubPanel.revalidate();
                        hubPanel.repaint();
                      
                    } else if (msg.substring(0,8).equals("/general")){ //if the server message is a general chat message.
                        if (serverMessageWindow == null){ //if the generalChat isnt opened, then create it.
                            new ServerChatFrame();
                        } 
                        int slashIndex = msg.indexOf("/",9);
                        String sender = msg.substring(9,slashIndex); //get the sender's name.
                        String actualMsg = msg.substring(slashIndex+1); //get the actual message.
                        serverMsgArea.append(sender+":"+actualMsg+"\n"); //append to the general JTextArea
                    }
                }                                 
            } catch (IOException e) { 
                 System.out.println("Failed to receive msg from the server");
                 e.printStackTrace();
            }
        } else { //if running == false
            try {  //after leaving the main loop we need to close all the sockets
                input.close();
                output.close();
                mySocket.close();
            } catch (Exception e) { 
                System.out.println("Failed to close socket");
            }
        }
    } //end of readMessagesFromServer() method
    
    
    //****** Inner Classes for Action Listeners ****
    
    // SendButton - send msg to server, then clears the JTextField
    class SendButtonListener implements ActionListener { 
        String name = null; //person that message is being sent to.
        public void actionPerformed(ActionEvent event)  {
            if (name == null){ //if name is null then this is the general chat.
                serverMsgArea.append("You:"+serverMsgField.getText()+"\n"); //append message from the JTextField
          
                output.println("/general/"+inputName+"/"+serverMsgField.getText()); //send message to server.
                output.flush();
                serverMsgField.setText(""); //reset JTextField
            } else {
          
                for (int i = 0; i < messageAreas.size();i++){ //look through the JTextAreas arraylist.
                    String s = messageAreas.get(i).getText();
                                     
                    if ( (s).substring(35,s.indexOf("\n")).equals(name)) { //find the appropriate message area.
                        messageAreas.get(i).append("You:"+ typeFields.get(i).getText()+"\n"); //append the message to client box.
                        output.println("/dm/"+name+"/"+typeFields.get(i).getText()); //send message to server.
                        output.flush();
                        typeFields.get(i).setText(""); //reset textfield.
                        i = messageAreas.size();
                    } 
                }
            }
        }
    } //end of send button
    
    // ClearButton - clears the user inputted stuff in the JTextField.
    class ClearButtonListener implements ActionListener { 
        String name = null; //name of chat window that needs to be cleared.
      
        public void actionPerformed(ActionEvent event)  {
            if (name == null){ //if null, then clear the general server chat textfield.
                serverMsgField.setText("");
            }
        
            for (int i = 0; i < messageAreas.size();i++){ //find appropriate message area.
                String s = messageAreas.get(i).getText();
                if ( (s).substring(35,s.indexOf("\n")).equals(name)) {
                    typeFields.get(i).setText("");  //reset textfield.
                } 
            }
        }     
    } //end of clear button
    
    // LogInButtonListener - closes starting screen, gets online users, starts hub thread.
    class LogInButtonListener implements ActionListener { 
        public void actionPerformed(ActionEvent event)  {
            //get inputted username
            inputName = loginField.getText();
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING)); //close starting window.
            //connect to server.
            connect(inputAddress, inputPort);
            //give username to server.
            output.println(inputName);
            output.flush();
            //change with way to get online users
            String userNames="";
            boolean gotUserNames = false;
            while (!gotUserNames){ //get all online users from server.
                try {
                    if (input.ready()){
                        userNames = input.readLine();
                        gotUserNames = true;
                    }
                } catch (IOException e){
                    System.out.println("Failed to retrieve Usernames");
                };
            }
        
            int oldPosition = 0;
            for (int i = 0 ; i < userNames.length(); i++){
                if (userNames.charAt(i) == '/'){ //usernames are separated by '/'
                    online.add(userNames.substring(oldPosition,i)); //add to arraylist of online users.
                    oldPosition = i+1; 
                } else if (i == userNames.length()-1){
                    online.add(userNames.substring(oldPosition,i+1)); 
                }
            }
            //start the main hub GUI. (where users see who's online, log out, etc)
            hubThread = new Thread(new HubThread());
            hubThread.start();
        }     
    }
    
    // SettingsButtonListener - Opens settings frame
    class SettingsButtonListener implements ActionListener { 
        public void actionPerformed(ActionEvent event)  {
            settingsTab = new JFrame("Settings");
            JPanel settingsWindow = new JPanel();
            settingsWindow.setBackground(Color.BLACK);
            settingsWindow.add(addressLabel);
            settingsWindow.add(addressField);
            settingsWindow.add(portLabel);
            settingsWindow.add(portField);
            settingsWindow.add(confirmButton);
            settingsTab.add(settingsWindow);
            settingsTab.setSize(400,400);
            settingsTab.setVisible(true);
        }     
    }
    
    // ConfirmButtonListener - confirms settings
    class ConfirmButtonListener implements ActionListener { 
        public void actionPerformed(ActionEvent event)  {
            inputAddress = addressField.getText();
            inputPort = Integer.parseInt(portField.getText());
            settingsTab.dispatchEvent(new WindowEvent(settingsTab, WindowEvent.WINDOW_CLOSING));
        }     
    }
    
    // ChatButtonListener - creates a chat
    class ChatButtonListener implements ActionListener{ 
        public void actionPerformed(ActionEvent event)  {
            String openChat = event.getActionCommand(); //get the name of the user.
            boolean check = true;
            for (int i = 0; i < onlineChats.size(); i++){
                if (onlineChats.get(i) == openChat){ //check if the user's chat window is already opened or not
                    check = false;
                    i = onlineChats.size();
                }
            }
            if (check == true){ //if not open create a new chat panel
                ChatPanel c = new ChatPanel(openChat);
            }
        }
    } // end of Chat Button
    
    //ServerChatButton - creates the ServerChat Window.
    class ServerChatButtonListener implements ActionListener{ 
        public void actionPerformed(ActionEvent event)  {
            new ServerChatFrame();
        }
    } //end of serverchatbutton
    
    //LogOutButton - sends message to server to quit the user. Closes all windows.
    class LogOutButtonListener implements ActionListener{ 
        public void actionPerformed(ActionEvent event)  {
            output.println("/quit");
            output.flush();
            try {
                System.exit(0);
            } catch(Exception e){};
        }
    }//end of logoutbutton
    
    //****** Inner Classes for JFrames ****
    public class ServerChatFrame {
        // constructor 
        public ServerChatFrame(){
            if (serverMessageWindow == null){ //if not opened, create a server chat frame.
                serverMessageWindow = new JFrame("Server Chat");
                //add message area
                serverMsgArea = new JTextArea();
                //if there are saved messages, append them.
                if (savedServerMessages.equals("")){
                    serverMsgArea.append("This is the start of your chat with the server!\n");
                } else {
                    serverMsgArea.append(savedServerMessages);
                }
                
                //adds window listener that checks if window is closed.
                serverMessageWindow.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e){
                        savedServerMessages = serverMsgArea.getText();
                        serverMessageWindow.dispose();
                        serverMessageWindow = null;
                    }
                });
          
                //add scrollbar
                JScrollPane scroll = new JScrollPane(serverMsgArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
          
                //create all the JButtons
                clearButton = new JButton("CLEAR");
                ClearButtonListener c = new ClearButtonListener();
                clearButton.addActionListener(c);
                sendButton = new JButton("SEND");
                SendButtonListener s = new SendButtonListener();
                sendButton.addActionListener(s);
                
                //add all the fields, buttons to the panel.
                JPanel southPanel = new JPanel();
                serverMsgField = new JTextField(20);
                southPanel.add(serverMsgField);
                southPanel.add(sendButton);
                southPanel.add(errorLabel);
                southPanel.add(clearButton);
                //add the panels to the window
                serverMessageWindow.add(BorderLayout.CENTER,scroll);
                serverMessageWindow.add(BorderLayout.SOUTH,southPanel);
                serverMessageWindow.setSize(400,400);
                serverMessageWindow.setVisible(true);
                serverMessageWindow.setLocation(800,0);
            }
        }
    } //end of ServerChatFrame class
    
    //Creates the individual DM Chat Frames
    public class ChatPanel {
        public ChatPanel(String name) {
            String openChat = name;
            //message area
            JTextArea msgArea = new JTextArea();
            boolean check = true;
            for (int i = 0; i < savedMessages.size(); i++){
                if ((savedMessages.get(i)).substring(35,savedMessages.get(i).indexOf("\n")).equals(name)){
                    check = false;
                    msgArea.append(savedMessages.get(i));
                    i = savedMessages.size();
                }
            }
            if (check == true){
                msgArea.append("This is the start of your chat with"+openChat);
                msgArea.append("\n");
            }
            messageAreas.add(msgArea);
            //scrollbar
            JScrollPane scroll = new JScrollPane(msgArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            //create frame and add listener to check if it closes or not
            JFrame chatTab = new JFrame(openChat);
            chatTabs.add(chatTab);
            chatTab.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e){
                    onlineChats.remove(openChat);  //if closed, remove from onlineChats arraylist
                    for (int i = 0; i < messageAreas.size(); i++){
                        String s = messageAreas.get(i).getText();
                        if ((s).substring(35,s.indexOf("\n")).equals(openChat)){ 
                            String oldMessages = messageAreas.get(i).getText();
                            for (int j = 0; j < savedMessages.size(); j++){
                                if (savedMessages.get(j).substring(35,s.indexOf("\n")).equals(openChat)){
                                    savedMessages.remove(j);
                                }
                            }
                            savedMessages.add(oldMessages);
                            System.out.println(oldMessages);
                            messageAreas.remove(i); //also remove from message area and type fields arraylist
                            typeFields.remove(i);
                            i = messageAreas.size();
                        }
                    }  
                }
            });
            //create all the jbuttons
            clearButton = new JButton("CLEAR");
            ClearButtonListener c = new ClearButtonListener();
            c.name = openChat;
            clearButton.addActionListener(c);
            sendButton = new JButton("SEND");
            SendButtonListener s = new SendButtonListener();
            s.name = openChat;
            sendButton.addActionListener(s);
          
            //add buttons and fields to the panels
            JPanel southPanel = new JPanel();
            JTextField typeField = new JTextField(20);
            typeFields.add(typeField);
            southPanel.add(typeField);
            southPanel.add(sendButton);
            southPanel.add(errorLabel);
            southPanel.add(clearButton);
            //add panels to the window.
            chatTab.add(BorderLayout.CENTER,scroll);
            chatTab.add(BorderLayout.SOUTH,southPanel);
            chatTab.setSize(400,400);
            chatTab.setVisible(true);
            chatTab.setLocation(800,0);   
            onlineChats.add(openChat);
        }
    } // end of ChatPanel class
    
    //Using a thread, creates the main hub GUI.
    public class HubThread implements Runnable{
        JFrame thisFrame = new JFrame(); //main frame.
      
        public void run(){
            if (load == true){ //if the user is connected.
                thisFrame.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e){
                        output.println("/quit"); //if closed, send quit to the server.
                        output.flush();
                        System.exit(0);
                    }
                });
                //create new JPanel
                hubPanel = new JPanel();
                //create jlabels and jbuttons
                hubIntroLabel = new JLabel("Hello, "+inputName+"\n");
                onlineLabel = new JLabel("Online:");
                JButton logOutButton = new JButton("LOG OUT");
                logOutButton.addActionListener(new LogOutButtonListener());
                JButton serverChatButton = new JButton("SERVER CHAT");
                serverChatButton.addActionListener(new ServerChatButtonListener());
          
                //add everything to the panel.
                hubPanel.add(hubIntroLabel);
                hubPanel.add(logOutButton);
                hubPanel.add(serverChatButton);
                hubPanel.add(onlineLabel);
                hubPanel.setLayout(new BoxLayout(hubPanel, BoxLayout.Y_AXIS));
                
                //create scrollbar.
                JScrollPane scroll = new JScrollPane(hubPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                
                //create all the buttons based on who is online.
                userButtons = new ArrayList<JButton>();
                for (int i = 0; i < online.size(); i++){
                    JButton newOne = new JButton(online.get(i));
                    newOne.addActionListener(new ChatButtonListener());
                    hubPanel.add(newOne);
                    userButtons.add(newOne);
                }
                
                // add everything to the frame.
                thisFrame.add(scroll);
                thisFrame.setSize(400,400);
                thisFrame.setVisible(true);
                //set a timer to read messages from the server every so often
                //this is done to prevent the GUI from freezing.
                Timer timer = new Timer(100, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        readMessagesFromServer();
                    }
                });
                timer.start();
            }
        }
    } // end of HubThread Class
} // end of ChatClient Program