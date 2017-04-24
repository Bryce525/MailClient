package MailPackage;

import MailClient.MailClientGUI;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;

/**
 * This class is the poller that runs in a separate thread to update the mailbox
 *
 * @author Bryce, Chris, Hunter, Tristan
 *
 */
public class Poller extends Thread {

  private final boolean mSuspend;
  private final long mPollTime;
  private final MailClientGUI mClient;

  /**
   * Creates new thread to update polling
   *
   * @param client - client to update
   * @param pollTime - time in ms to update mailbox
   */
  public Poller(MailClientGUI client, long pollTime) {
    mClient = client;
    mPollTime = pollTime;
    mSuspend = false;
  }

  //Thread function to update selected mailbox in mail client
  @Override
  public void run() {
    if (!mSuspend) {
      try {
        //Sleep for designated time
        Thread.sleep(mPollTime);
        //Wake up and update mailbox
        mClient.updateMailbox();
      } catch (InterruptedException ex) {
        Logger.getLogger(Poller.class.getName()).log(Level.SEVERE, null, ex);
      }
      //Restart run    
      run();
    }
  }
}
