package de.vet.chat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public class ChatClient extends JDialog implements MessageListener<Notification> {

    private static final long serialVersionUID = 1L;
    private final String userName;
    private JList<String> userList;
    private GenericApplicationContext applicationContext;
    private HazelcastInstance hazelcastInstance;
    private ITopic<Object> chatTopic;
    private final Map<String, JTextPane> chats = new HashMap<String, JTextPane>();
    private JTabbedPane tabbedPane;
    private DefaultListModel<String> userModel;


    public static void main(final String[] args) throws InvocationTargetException, InterruptedException {
        String name = null;
        if (args.length == 0){
            name = JOptionPane.showInputDialog(null,"Enter your name",
                    "Whats your name",
                    JOptionPane.PLAIN_MESSAGE);

        }else{
            name = args[0];
        }
        if (name == null){
            return;
        }
        ChatClient chatClient = new ChatClient(name);
        chatClient.setVisible(true);
    }
    
    public ChatClient(final String string) throws InvocationTargetException, InterruptedException {
        userName = string;
        if (userName == null){
            
        }
        initClient();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                initGUI();
                chatTopic.publish(new NewUserNotification(userName));
                IList<String> distUserList = hazelcastInstance.getList("USERLIST");
                for (String user : distUserList) {
                    if (!userModel.contains(user)) {
                        userModel.addElement(user);
                    }
                }
                setVisible(true);
                pack();
            }
        });
    }

    private void initClient() {
        applicationContext = new GenericApplicationContext();
        PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
        Properties props = new Properties();
        props.put("NAME", userName);
        propertyPlaceholderConfigurer.setProperties(props);
        applicationContext.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
        XmlBeanDefinitionReader springReader = new XmlBeanDefinitionReader(applicationContext);
        springReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        springReader.loadBeanDefinitions("classpath:client-context.xml");
        applicationContext.refresh();
        hazelcastInstance = applicationContext.getBean(HazelcastInstance.class);
        chatTopic = hazelcastInstance.getTopic("Chat");
    }

    private void initGUI() {
        setTitle("VET Chat (" + userName + ")");
        setSize(500, 600);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        userModel = new DefaultListModel<String>();
        userList = new JList<String>(userModel);
        userList.setPreferredSize(new Dimension(250, 500));
        userList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {
                    String selectedUser = userList.getSelectedValue();
                    if (!selectedUser.equals(userName)) {
                        chatTopic.publish(new NewChatNotification(userName, selectedUser, userName + " <=> " + selectedUser));
                    }
                }
            }

        });
        add(new JScrollPane(userList), BorderLayout.WEST);
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addTab("Chat", createTextArea("Chat", null, null));
        tabbedPane.setPreferredSize(new Dimension(800, 500));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                chatTopic.publish(new LogoffUserNotification(userName));
            }

            @Override
            public void windowClosed(final WindowEvent e) {
                applicationContext.close();
                System.exit(0);
            }
        });
    }

    private JComponent createTextArea(final String chatName, final String user1, final String user2) {
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane jScrollPane = new JScrollPane();
        jScrollPane.setAutoscrolls(true);
        JTextPane textArea = new JTextPane();
        jScrollPane.getViewport().add(textArea);
        StyleContext context = new StyleContext();
        StyledDocument document = new DefaultStyledDocument(context);
        textArea.setDocument(document);
        panel.add(jScrollPane, BorderLayout.CENTER);
        panel.add(new JTextField() {

            private static final long serialVersionUID = 1L;

            {
                addKeyListener(new KeyListener() {

                    @Override
                    public void keyTyped(final KeyEvent e) {
                    }

                    @Override
                    public void keyReleased(final KeyEvent e) {
                        if (e.getKeyChar() == '\n') {
                            String text = getText();
                            ITopic<Object> topic = hazelcastInstance.getTopic(chatName);
                            topic.publish(new MessageNotification(userName, text));
                            setText("");
                        }
                    }

                    @Override
                    public void keyPressed(final KeyEvent e) {
                    }
                });
            }
        }, BorderLayout.SOUTH);
        chats.put(chatName, textArea);
        ITopic<Notification> topic = hazelcastInstance.getTopic(chatName);
        topic.addMessageListener(ChatClient.this);
        panel.putClientProperty("NAME", chatName);
        panel.putClientProperty("USER1", user1);
        panel.putClientProperty("USER2", user2);
        return panel;
    }


    @Override
    public void onMessage(final com.hazelcast.core.Message<Notification> message) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Notification notification = message.getMessageObject();
                    switch (message.getMessageObject().getType()) {
                        case MESSAGE: {
                            MessageNotification newMessage = (MessageNotification) notification;
                            String from = newMessage.getFrom();
                            String message2 = newMessage.getMessage();
                            String channel = (String) message.getSource();
                            JTextPane jTextArea = chats.get(channel);

                            SimpleAttributeSet attributes = new SimpleAttributeSet();
                            StyleConstants.setBold(attributes, true);
                            try {
                                jTextArea.getDocument().insertString(0, from + ": ", attributes);
                                jTextArea.getDocument().insertString(from.length() + 2, message2 + "\r\n", new SimpleAttributeSet());
                            } catch (BadLocationException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                        case NEW_CHAT: {
                            NewChatNotification newChatNotification = (NewChatNotification) notification;
                            if (newChatNotification.getTo().equals(userName) || newChatNotification.getFrom().equals(userName)) {
                                String channelName = newChatNotification.getChannelName();
                                String from = newChatNotification.getFrom();
                                String to = newChatNotification.getTo();
                                boolean found = false;
                                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                                    JComponent tabComponentAt = (JComponent) tabbedPane.getComponentAt(i);
                                    if (tabComponentAt != null) {
                                        if (channelName.equals(tabComponentAt.getClientProperty("NAME"))) {
                                            tabbedPane.setSelectedIndex(i);
                                            break;
                                        } else {
                                            String user1 = (String) tabComponentAt.getClientProperty("USER1");
                                            String user2 = (String) tabComponentAt.getClientProperty("USER2");
                                            if ((from.equals(user1) && to.equals(user2)) || (from.equals(user2) && to.equals(user1))) {
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (!found) {
                                    tabbedPane.addTab(channelName, createTextArea(channelName, newChatNotification.getFrom(), newChatNotification.getTo()));
                                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

                                }
                            }
                            break;
                        }
                        case NEW_USER: {
                            NewUserNotification newUserNotification = (NewUserNotification) notification;
                            if (!userModel.contains(newUserNotification.getUserName())) {
                                userModel.addElement(newUserNotification.getUserName());
                            }
                            break;
                        }
                        case USER_LEFT: {
                            LogoffUserNotification logoffUserNotification = (LogoffUserNotification) notification;
                            userModel.removeElement(logoffUserNotification.getUserName());
                            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                                JComponent tabComponentAt = (JComponent) tabbedPane.getComponentAt(i);
                                if (tabComponentAt != null) {
                                    String user1 = (String) tabComponentAt.getClientProperty("USER1");
                                    String user2 = (String) tabComponentAt.getClientProperty("USER2");
                                    if ((logoffUserNotification.getUserName()).equals(user1) || logoffUserNotification.getUserName().equals(user2)) {
                                        tabbedPane.removeTabAt(i);
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }

                }
            });
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
