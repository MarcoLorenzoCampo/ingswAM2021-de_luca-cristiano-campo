package it.polimi.ingsw.network.server;

import it.polimi.ingsw.enumerations.PossibleGameStates;
import it.polimi.ingsw.enumerations.PossibleMessages;
import it.polimi.ingsw.model.game.PlayingGame;
import it.polimi.ingsw.network.messages.Message;

import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Class to handle clients server-side when one connects to the server's socket.
 * Implements Runnable to be able to execute threads (overriding run()) and IClientHandler.
 */
public class ClientHandler implements Runnable, IClientHandler {

    private String nickname;

    private final SocketServer socketServer;
    private final Socket clientSocket;

    /**
     * Boolean value to determine player status.
     * Used for advanced functionality: resilience to disconnections.
     */
    private boolean isConnected;

    private final Object inputLock;
    private final Object outputLock;

    private ObjectOutputStream output;
    private ObjectInputStream input;


    Scanner in;
    PrintWriter out;

    /**
     * Custom constructor.
     * @param socketServer: reference to the server's socket to communicate
     *                    with the server in case of disconnection.
     * @param clientSocket: reference to the client's socket to communicate via Output and Input streams.
     */
    public ClientHandler(SocketServer socketServer, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.socketServer = socketServer;

        this.inputLock = new Object();
        this.outputLock = new Object();

        this.isConnected = true;

        try {

            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.flush();
            //this.input = new ObjectInputStream(clientSocket.getInputStream());

        } catch (IOException e) {
            Server.LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Overriding default run() method. Calling disconnect() method if an IOException
     * is thrown.
     */
    @Override
    public void run() {
        try {
            handleUserMessages();
        } catch (IOException | NoSuchElementException e) {
            Server.LOGGER.info("Client disconnected.");
            disconnect();
        }
    }

    /**
     * After the client is connected and associated to a handler, a specific thread is run
     * to keep up with sent messages.
     *
     * There may be ping messages which will be ignored.
     */
    private void handleUserMessages() throws IOException {
        try {

            in = new Scanner(clientSocket.getInputStream());
            out = new PrintWriter(clientSocket.getOutputStream());

            while (!Thread.currentThread().isInterrupted()) {

                synchronized (inputLock) {
                    String line = in.nextLine();

                    System.out.println("Client: " + clientSocket.getLocalSocketAddress() + ", message: " + line);
                    //Message message = (Message) input.readObject();

                    /*if(message.getMessageType().equals(PossibleMessages.PING_MESSAGE)) {
                        continue;
                    }*/

                    //handle messages here;
                }
            }

        } catch (ClassCastException e) {

            Server.LOGGER.severe("Invalid stream");
        }

        clientSocket.close();
    }

    /**
     * Method to check the user's connection status.
     * @return: true if it's still connected, false otherwise
     */
    @Override
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Disconnects a client from the server's socket.
     */
    @Override
    public void disconnect() {
        if (isConnected) {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                Server.LOGGER.severe(e.getMessage());
            }
            isConnected = false;
            Thread.currentThread().interrupt();

            socketServer.onDisconnect(this);
        }
    }

    /**
     * Method to send messages to the client through socket.
     * @param message: data sent.
     */
    @Override
    public void sendMessage(Message message) {
        try {
            synchronized (outputLock) {
                output.writeObject(message);
                output.reset();
                Server.LOGGER.info(() -> "Sent: " + message);
            }
        } catch (IOException e) {
            Server.LOGGER.severe(e.getMessage());
            disconnect();
        }
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}
