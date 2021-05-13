package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.eventHandlers.Observable;
import it.polimi.ingsw.network.messages.Message;
import it.polimi.ingsw.network.views.cli.CLI;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class to handle client operations.
 */
public class Client extends Observable implements IClient {

    /**
     * Socket that connects and communicates with the server's socket.
     */
    private final Socket clientSocket;

    /**
     * Streams to exchange messages {@link Message} with
     * the server's socket.
     */
    private ObjectOutputStream output;
    private ObjectInputStream input;

    /**
     * Thread to enable reading from server.
     */
    private final ExecutorService serverListener;

    /**
     * Client constructor. Connects the client to the server's socket.
     *
     * @param port:       server's socket.
     * @param IP_Address: IP address.
     */
    public Client(int port, String IP_Address) throws IOException {

        this.clientSocket = new Socket();

        this.clientSocket.connect(new InetSocketAddress(IP_Address, port));

        this.serverListener = Executors.newSingleThreadExecutor();

        try {

            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());

            clientLogger.info(() -> "Connection successful.");

        } catch (IOException e) {
            clientLogger.severe(() -> "Couldn't connect to the host.");
        }
    }

    /**
     * Method to run an asynchronous thread to work as input listener, waiting for messages
     * sent by the server.
     */
    @Override
    public void readMessage() {

        serverListener.execute(() -> {
            while (!serverListener.isShutdown()) {

                Message message = null;

                try {
                    message = (Message) input.readObject();
                    clientLogger.info("Received: " + message + " from server.");
                } catch (IOException e) {
                    e.printStackTrace();

                    clientLogger.severe(() -> "Communication error. Critical error.");
                    disconnect();
                    serverListener.shutdown();

                } catch (ClassNotFoundException e) {

                    clientLogger.severe(() -> "Got an unexpected Object from server. Critical error.");
                    disconnect();
                    serverListener.shutdown();
                }

                if(message != null) notifyObserver(message);
            }
        });
    }

    @Override
    public void sendMessage(Message message) {

        try {
            output.writeObject(message);
            output.reset();

        } catch (IOException e) {

            clientLogger.severe(() -> "Unable to send the message.");
            disconnect();
        }
    }

    @Override
    public void disconnect() {

        if (!clientSocket.isClosed()) {
            try {

                clientSocket.close();
            } catch (IOException e) {

                clientLogger.severe(() -> "Could not disconnect. Critical error.");
            }
        }
    }

    //------------------------------------------ MAIN METHOD -----------------------------------------------

    /**
     * Main method of the client class, it can be launched in both cli or gui mode.
     * The default option is cli, it can be used in gui mode by adding "-gui" when running the jar file.
     *
     * user @ user:$ client.jar -cli
     */
    public static void main(String[] args) {

        String viewType = "-cli";

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-gui")) {
                viewType = "-gui";
                break;
            }
        }

        if (viewType.equalsIgnoreCase("-cli")) {

            CLI cliView = new CLI();
            ClientManager clientManager = new ClientManager(cliView);
            cliView.addObserver(clientManager);
            cliView.startCli();

        } else {

            //launch gui

        }
    }
}
