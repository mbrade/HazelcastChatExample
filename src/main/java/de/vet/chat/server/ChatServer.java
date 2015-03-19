package de.vet.chat.server;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import de.vet.chat.skif.LogoffUserNotification;
import de.vet.chat.skif.NewUserNotification;
import de.vet.chat.skif.Notification;

public class ChatServer {

    private static HazelcastInstance hazelcastInstance;
    private static IList<String> userList;
    private static ITopic<Notification> topic;

    public static void main(final String[] args) {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.registerShutdownHook();
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(applicationContext);
        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions("classpath:application-context.xml");
        applicationContext.refresh();
        hazelcastInstance = applicationContext.getBean(HazelcastInstance.class);
        userList = hazelcastInstance.getList("USERLIST");
        if (userList.isEmpty()){
            System.out.println("Userlist is empty");
        }
        for (String string : userList) {
            System.out.println("Known user:" +string);
        }
        topic = hazelcastInstance.getTopic("Chat");
        topic.addMessageListener(new ChatListener());
        String input = "";
        do {
            input = convertStreamToString(System.in);
        } while (!input.trim().equalsIgnoreCase("exit"));
        applicationContext.destroy();
    }

    static String convertStreamToString(final java.io.InputStream is) {
        @SuppressWarnings("resource")
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\n");
        return s.hasNext() ? s.next() : "";
    }
    
    private static class ChatListener implements MessageListener<Notification> {

        @Override
        public void onMessage(final Message<Notification> message) {
            Object messageObject = message.getMessageObject();
            if (messageObject instanceof NewUserNotification) {
                if (!userList.contains(((NewUserNotification) messageObject).getUserName())){
                    userList.add(((NewUserNotification) messageObject).getUserName());
                }
            }
            if (messageObject instanceof LogoffUserNotification) {
                userList.remove(((LogoffUserNotification) messageObject).getUserName());
            }
        }
    }
    

}
