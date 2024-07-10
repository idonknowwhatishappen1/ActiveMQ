import jakarta.jms.*;
import javax.naming.*;
import java.util.*;
import jakarta.jms.Queue;

public class Client {

    private static Map<String, String> receivedMessages = new HashMap<>();
    private static Set<String> pendingRequests = new HashSet<>();

    public static void main(String[] args) throws Exception {
        Hashtable<String, String> properties = new Hashtable<>();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.put(Context.PROVIDER_URL, "tcp://localhost:61616");

        Context context = new InitialContext(properties);
        QueueConnectionFactory connFactory = (QueueConnectionFactory) context.lookup("ConnectionFactory");

        QueueConnection conn = connFactory.createQueueConnection();
        QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue requestQueue = (Queue) context.lookup("dynamicQueues/requestQueue");
        Queue replyQueue = (Queue) context.lookup("dynamicQueues/replyQueue");

        QueueSender sender = session.createSender(requestQueue);
        QueueReceiver receiver = session.createReceiver(replyQueue);

        conn.start();

        // Create a message listener for receiving replies
        receiver.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                try {
                    if (msg instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) msg;
                        String correlationID = textMessage.getJMSCorrelationID();
                        synchronized (pendingRequests) {
                            if (pendingRequests.contains(correlationID)) {
                                receivedMessages.put(correlationID, textMessage.getText());
                                pendingRequests.remove(correlationID);
                                pendingRequests.notifyAll(); // Notify waiting thread
                            }
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter city name (Frankfurt/Berlin/Leipzig):");
            String cityName = scanner.nextLine();

            System.out.println("Choose an action: \n 1. Add Inhabitant \n 2. Get All Dates of Birth \n 3. Exit");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            if (choice == 3) {
                System.out.println("Exiting...");
                break;
            }

            String request = "";
            String correlationID = UUID.randomUUID().toString();

            if (choice == 1) {
                System.out.println("Enter inhabitant name:");
                String name = scanner.nextLine();
                System.out.println("Enter date of birth:");
                String dob = scanner.nextLine();
                System.out.println("Enter marital status:");
                String ms = scanner.nextLine();

                request = "ADD:" + cityName + ":" + name + ":" + dob + ":" + ms;

            } else if (choice == 2) {
                request = "GET_ALL_DOBS:" + cityName;
            } else {
                System.out.println("Invalid choice");
                continue;
            }

            TextMessage message = session.createTextMessage(request);
            message.setJMSReplyTo(replyQueue);
            message.setJMSCorrelationID(correlationID);
            synchronized (pendingRequests) {
                pendingRequests.add(correlationID);
            }
            sender.send(message);

            // Wait for the reply to be received
            synchronized (pendingRequests) {
                while (pendingRequests.contains(correlationID)) {
                    pendingRequests.wait(); // Wait until the reply is received
                }
            }

            // Print the received reply
            String reply = receivedMessages.remove(correlationID);
            System.out.println("Received reply: " + reply);
        }

        session.close();
        conn.close();
        scanner.close();
    }
}
