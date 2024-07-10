import jakarta.jms.*;
import jakarta.jms.Queue;

import javax.naming.*;
import java.util.*;
import java.util.HashMap;

public class Server {

    private static Map<String, City> cities = new HashMap<>();

    static {
        cities.put("Frankfurt", new City("Frankfurt"));
        cities.put("Berlin", new City("Berlin"));
        cities.put("Leipzig", new City("Leipzig"));
    }

    public static void main(String[] args) throws Exception {
        Hashtable<String, String> properties = new Hashtable<>();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.put(Context.PROVIDER_URL, "tcp://localhost:61616");

        Context context = new InitialContext(properties);
        QueueConnectionFactory connFactory = (QueueConnectionFactory) context.lookup("ConnectionFactory");

        QueueConnection conn = connFactory.createQueueConnection();
        QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue requestQueue = (Queue) context.lookup("dynamicQueues/requestQueue");

        QueueReceiver receiver = session.createReceiver(requestQueue);

        receiver.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                try {
                    if (msg instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) msg;
                        String response = handleRequest(textMessage.getText());
                        TextMessage responseMessage = session.createTextMessage(response);
                        responseMessage.setJMSCorrelationID(textMessage.getJMSCorrelationID());
                        QueueSender sender = session.createSender((Queue) textMessage.getJMSReplyTo());
                        sender.send(responseMessage);
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });

        conn.start();

        System.out.println("Server is running...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        session.close();
        conn.close();
        scanner.close();
    }

    private static String handleRequest(String request) {
        String[] params = request.split(":");
        String action = params[0];
        String cityName = params[1];

        switch (action) {
            case "ADD":
                String name = params[2];
                String dob = params[3];
                String ms = params[4];
                Inhabitant inhabitant = new Inhabitant(name, dob, ms);
                cities.get(cityName).addInhabitant(inhabitant);
                return "Inhabitant added successfully";
            case "GET_ALL_DOBS":
                return cities.get(cityName).getAllDOBs();
            default:
                return "Invalid request";
        }
    }
}
