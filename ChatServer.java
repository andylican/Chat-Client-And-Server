/* [ChatServer.java]
 * You will need to modify this so that received messages are broadcast to all clients
 * @author Mangat
 * @ version 1.0a
 */

//imports for network communication
import java.io.*;
import java.net.*;
import java.util.*;

class ChatServer {
    
    ServerSocket serverSock;// server socket for connection
    static Boolean running = true;  // controls if the server is accepting clients
    static String[][] messages = new String[1000000][3]; //msg, sender, reciever (blankk reciever = broadcast to all)
    
    static ArrayList<String>users = new ArrayList<String>();
    static int nextIndex = 0;
   
    /** Main
      * @param args parameters from command line
      */
    public static void main(String[] args) { 
        for(int i=0; i<1000000; i++) {
            messages[i][0] = "";
            messages[i][1] = "";
            messages[i][2] = "";
        }
        new ChatServer().go(); //start the server
    }
    
    /** Go
      * Starts the server
      */
    public void go() { 
        System.out.println("Waiting for a client connection..");
        
        Socket client = null;//hold the client connection
        
        try {
            serverSock = new ServerSocket(5000);  //assigns an port to the server
            //serverSock.setSoTimeout(15000);  //15 second timeout
            while(running) {  //this loops to accept multiple clients
                client = serverSock.accept();  //wait for connection
                System.out.println("Client connected");
                //Note: you might want to keep references to all clients if you plan to broadcast messages
                //Also: Queues are good tools to buffer incoming/outgoing messages
                Thread t = new Thread(new ConnectionHandler(client)); //create a thread for the new client and pass in the socket
                t.start(); //start the new thread
            }
        }catch(Exception e) { 
            //System.out.println("Error accepting connection");
            //close all and quit
            try {
                client.close();
            }catch (Exception e1) { 
                System.out.println("Failed to close socket");
            }
            System.exit(-1);
        }
    }
    
    //***** Inner class - thread for client connection
    class ConnectionHandler implements Runnable { 
        private PrintWriter output; //assign printwriter to network stream
        private BufferedReader input; //Stream for network input
        private Socket client;  //keeps track of the client socket
        private boolean running;
        /* ConnectionHandler
         * Constructor
         * @param the socket belonging to this client connection
         */    
        ConnectionHandler(Socket s) { 
            this.client = s;  //constructor assigns client to this    
            try {  //assign all connections to client
                this.output = new PrintWriter(client.getOutputStream());
                InputStreamReader stream = new InputStreamReader(client.getInputStream());
                this.input = new BufferedReader(stream);
            }catch(IOException e) {
                e.printStackTrace();        
            }            
            running=true;
        } //end of constructor
        
        
        /* run
         * executed on start of thread
         */
        public void run() {
            
            //Variables
            boolean gaveUsername = false;
            String username = "";
            String msg="";
            int msgIndex = 0;
            ArrayList<String>visibleUsers = new ArrayList<String>();
            
            //Asks for username
            while(!gaveUsername) {
                try {
                    if (input.ready()) { //check for an incoming messge
                        username = input.readLine();
                        gaveUsername = true;
                        users.add(username);
                        
                    }
                }catch (IOException e) { 
                    System.out.println("Failed to receive username from the client");
                    e.printStackTrace();
                } 
            }
            System.out.println(username+" has connected.");
            
            //Give list of connected users
             synchronized(users) {
                 String list = "";
                for(int i=0; i<users.size(); i++) {
                    if(!users.get(i).equals(username)) {
                       list += users.get(i)+"/";
                      
                       //System.out.println(users.get(i));
                        visibleUsers.add(users.get(i));
                    }
                }
               output.println(list);
               output.flush();
            }
            
            
            while(running) {  
                synchronized(users) {
                    
                    //Update any newly connected people
                    for(int i=users.indexOf(username)+1; i<users.size(); i++) {
                        if(!visibleUsers.contains(users.get(i))) {
                            output.println("/new/"+users.get(i));
                            output.flush();
                            visibleUsers.add(users.get(i));
                        }
                    }
                    
                    //Update any people who left
                    for(int i=0; i<visibleUsers.size(); i++) {
                        if(!users.contains(visibleUsers.get(i))) {
                            output.println("/delete/"+visibleUsers.get(i));
                            output.flush();
                            visibleUsers.remove(visibleUsers.get(i));
                            i--;
                           
                        }
                    }
                }
                
                //Broadcast any unread messages
                if(msgIndex < nextIndex) {
                    if(!messages[msgIndex][1].equals(username)) {
                       
                        //Direct message to this user
                        if(messages[msgIndex][2].equals(username)) {
                            output.println("/dm/"+messages[msgIndex][1]+"/"+messages[msgIndex][0]);
                             output.flush();
                            
                        } else if(messages[msgIndex][2].equals("")){
                            System.out.println(username+" hears: "+messages[msgIndex][0]);
                            //General chat message
                            output.println("/general/"+messages[msgIndex][1]+"/"+messages[msgIndex][0]);
                             output.flush();
                        }
                        
                       
                       
                    }
                    msgIndex++;
                    
                }
                try {
                    if (input.ready()) { //check for an incoming messge
                        synchronized(messages) {
                            msg = input.readLine();  //get a message from the client
                            if(msg.equals("/quit")) {
                                
                                running = false;
                            } else if(msg.substring(0,3).equals("/dm")) {
                                
                                int slashIndex = msg.indexOf("/",4);
                                String reciever = msg.substring(4,slashIndex);
                                String actualMsg = msg.substring(slashIndex+1,msg.length());
                                messages[nextIndex][0] = actualMsg;
                                messages[nextIndex][1] = username;
                                messages[nextIndex][2] = reciever;
                                nextIndex++;
                            } else {
                                
                               // System.out.println(msg);
                                int slashIndex = msg.indexOf("/",9);
                                 String reciever = msg.substring(9,slashIndex);
                                String actualMsg = msg.substring(slashIndex+1,msg.length());
                                messages[nextIndex][0] = actualMsg;
                                messages[nextIndex][1] = username;
                                nextIndex++;
                            }
                            
                            
                        }
                        
                    }
                }
                catch (IOException e) { 
                    System.out.println("Failed to receive msg from the client");
                    e.printStackTrace();
                }
            }
            
            //Send a message to the client
            // output.println("We got your message! Goodbye.");
            //output.flush(); 
            
            //close the socket
            try {
                System.out.println(username+" has left.");
                input.close();
                output.close();
                client.close();
                users.remove(username);
            }catch (Exception e) { 
                System.out.println("Failed to close socket");
            }
        } // end of run()
    } //end of inner class   
} //end of Class