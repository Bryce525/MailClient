package Database;

import MailPackage.Message;
import java.sql.*;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * Provides all database connection and data query operations.
 *
 * @author Bryce Johnston, Steven McClurg, Hunter Cowing, Tristan Cone
 */
public class QuerySubmitter {

  private static final QuerySubmitter mInstance = new QuerySubmitter();
  private Connection mConnection;
  private PreparedStatement mPreparedStatement;
  private Statement mStatement;
  private ResultSet mResultSet;
  private String mUsername, mPassword, mPermissions;
  private DbUtil dbutil;

  /**
   * Singleton constructor
   */
  private QuerySubmitter() {
  }

  /**
   * Function to return the single instance of the QuerySubmitter
   *
   * @return the reference to the only instance of this class
   */
  public static QuerySubmitter getInstance() {
    return mInstance;
  }

  /**
   * Connects to Amazon RDS database.
   *
   * @throws SQLException if unable to connect to the database
   */
  public void connect() throws SQLException {
    String address = "jdbc:mysql://hmc0016-bcht-mail.cby0bpnet7e8.us-east-1.rds.amazonaws.com:3306/StandaloneMessaging?user=root&password=r00tpa$$word";
    mConnection = DriverManager.getConnection(address);
    mStatement = mConnection.createStatement();
  }

  /**
   * Function to login with a specific user
   *
   * @param username account name
   * @param password account password
   * @throws SQLException if unable to perform database operation
   * @throws InvalidCredentialsException if credentials do not match database
   * @throws DisabledException if the user's account has been disabled
   *
   */
  public void login(String username, String password) throws SQLException,
          InvalidCredentialsException,
          DisabledException {
    String query = "SELECT user, Permissions FROM Users WHERE user=? AND pass=?";
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, username);
    mPreparedStatement.setString(2, password);
    ResultSet rs = mPreparedStatement.executeQuery();
    if (rs.next()) {
      if (rs.getNString(2).equals("Disabled")) {
        throw new DisabledException("User" + username + " is disabled");
      }
      mUsername = username;
      mPassword = password;
    } else {
      throw new InvalidCredentialsException("Invalid Credentials");
    }
  }

  /**
   * Function to register a new user
   *
   * @param username account name
   * @param password account password
   * @param FirstName the user's first name
   * @param LastName the user's last name
   * @param permissions sets to an Admin or Standard user
   * @throws SQLException
   */
  public void register(String username, String password, String FirstName,
          String LastName, String permissions) throws SQLException, UserExistsException {
    String query = "SELECT user FROM Users WHERE user=?";
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, username);
    ResultSet rs = mPreparedStatement.executeQuery();
    if (!rs.next()) {
      String query1 = "INSERT INTO Users (`user`,`pass`,`FirstName`,`LastName`,`Permissions`)"
              + " VALUES (?,?,?,?,?)";
      mPreparedStatement = mConnection.prepareStatement(query1);
      mPreparedStatement.setString(1, username);
      mPreparedStatement.setString(2, password);
      mPreparedStatement.setString(3, FirstName);
      mPreparedStatement.setString(4, LastName);
      mPreparedStatement.setString(5, permissions);
      mPreparedStatement.execute();
      mUsername = username;
      mPassword = password;
      mPermissions = permissions;
    } else {
      throw new UserExistsException("This user already exists.");
    }
  }

  /**
   * Query the database to verify if the user's password matches their username
   *
   * @param username
   * @param password
   * @return true if a match was found
   * @throws SQLException
   */
  public boolean verifyPassword(String username, String password) throws
          SQLException {
    String query = "SELECT pass FROM Users WHERE user='" + username + "';";
    ResultSet rs = mStatement.executeQuery(query);
    rs.next();
    String result = rs.getString(1);
    return (password.equals(result));
  }

  /**
   * Deletes an account and its associated messages.
   *
   * @throws SQLException
   */
  public void deleteAccount() throws SQLException {
    String query = "DELETE FROM Users WHERE user='" + mUsername + "';";
    String query1 = "DELETE FROM Outbox WHERE Sender='" + mUsername + "';";
    String query2 = "DELETE FROM Inbox WHERE Recipients='" + mUsername + "';";
    mStatement.executeUpdate(query);
    mStatement.executeUpdate(query1);
    mStatement.executeUpdate(query2);
  }

  /**
   * Function to submit a security question
   *
   * @param question a security question
   * @param answer the answer to the security question
   * @throws SQLException
   */
  public void setSecQuestion(String question, String answer) throws
          SQLException {
    String query = "UPDATE Users SET `SecQuestion`='" + question
            + "',`Answer`='" + answer + "' WHERE user='" + mUsername
            + "'; ";
    mStatement.executeUpdate(query);
  }

  /**
   * Function to get security question
   *
   * @param username
   * @return security question string
   * @throws SQLException
   */
  public String getSecQuestion(String username) throws SQLException {
    String question = null;
    //TO-DO get sec question from SQL database and return
    String query = "SELECT SecQuestion FROM Users WHERE user='" + username
            + "';";
    ResultSet rs = mStatement.executeQuery(query);
    rs.next();
    question = rs.getString(1);
    return question;
  }

  /**
   * Function to verify sec question answer
   *
   * @param username
   * @param answer
   * @return true if good answer, false otherwise
   * @throws java.sql.SQLException
   */
  public boolean verifySecAnswer(String username, String answer) throws
          SQLException {
    String query = "SELECT Answer FROM Users WHERE user='" + username + "';";
    ResultSet rs = mStatement.executeQuery(query);
    rs.next();
    String result = rs.getString(1);
    return (answer.equals(result));
  }

  /**
   * Function to delete an account by an administrator
   *
   * @param user username of individual to delete
   * @throws SQLException
   */
  public void deleteAccount(String user) throws SQLException {
    if (!"root".equals(user)) {
      String query = "DELETE FROM Users WHERE user=?";
      String query1 = "DELETE FROM Inbox WHERE Recipients=?";
      mPreparedStatement = mConnection.prepareStatement(query);
      mPreparedStatement.setString(1, user);
      mPreparedStatement.execute();
      mPreparedStatement = mConnection.prepareStatement(query1);
      mPreparedStatement.setString(1, user);
      mPreparedStatement.execute();
    }
  }

  /**
   * Function to get user's inbox
   *
   * @return jTable with messages
   * @throws java.sql.SQLException
   */
  public JTable getInbox() throws SQLException {
    String query
            = "SELECT idMessages, Title, Sender, Received, Unread FROM Inbox WHERE Recipients=? ORDER BY Received DESC;";
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, mUsername);
    ResultSet rs = mPreparedStatement.executeQuery();
    TableModel tm = DbUtil.HeadersRStoJT(rs);
    JTable inbox = DbUtil.boldUnread(tm);
    return inbox;
  }

  /**
   * Function to get user outbox
   *
   * @return jTable with messages
   * @throws SQLException
   */
  public JTable getOutbox() throws SQLException {
    String query
            = "SELECT idMessages, Title, Recipients, Received, Unread FROM Outbox WHERE Sender='"
            + mUsername + "' ORDER BY Received DESC;";
    ResultSet rs = mStatement.executeQuery(query);
    JTable outbox = new JTable(DbUtil.HeadersRStoJT(rs));
    return outbox;
  }

  /**
   * Function to get contacts
   *
   * @return jTable with contacts
   * @throws java.sql.SQLException
   * @throws Database.WrongTableException
   */
  public JTable getContacts() throws SQLException {
    String query
            = "SELECT user, FirstName, LastName FROM Users ORDER BY LastName, FirstName;";
    ResultSet rs = (mStatement.executeQuery(query));
    JTable contacts = new JTable(DbUtil.ContactsRStoJT(rs));
    return contacts;
  }

  /**
   * Function to send a message to user on database
   *
   * @param msg - message to send
   * @throws java.sql.SQLException
   */
  public void sendMessage(Message msg) throws SQLException {
    String failures = "";
    String successes = "";
    for (int i = 0; i < msg.mTo.length; i++) {
      String test_recip
              = "SELECT FirstName, LastName, user FROM Users WHERE user='"
              + msg.mTo[i] + "';";
      ResultSet rs = mStatement.executeQuery(test_recip);
      if (!rs.next()) {
        failures += msg.mTo[i] + "\n";
      } else {
        if (i >= 1) {
          successes += ", " + msg.mTo[i];
        } else {
          successes += msg.mTo[i];
        }
        String query = "INSERT INTO Inbox (Recipients, Sender, Title, Body)"
                + " VALUES (?,?,?,?)";
        mPreparedStatement = mConnection.prepareStatement(query);
        mPreparedStatement.setString(1, msg.mTo[i]);
        mPreparedStatement.setString(2, msg.mFrom);
        mPreparedStatement.setString(3, msg.mSubject);
        mPreparedStatement.setString(4, msg.mBody);
        mPreparedStatement.execute();
      }
    }
    String query = "INSERT INTO Outbox (Recipients, Sender, Title, Body) VALUES (?,?,?,?)";
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, successes);
    mPreparedStatement.setString(2, msg.mFrom);
    mPreparedStatement.setString(3, msg.mSubject);
    mPreparedStatement.setString(4, msg.mBody);
    mPreparedStatement.execute();

    if (!failures.equals("")) {
      String DaemonMessage
              = "Mail delivery has failed to the following recipient(s):\n"
              + failures;
      String fail_query
              = "INSERT INTO Inbox (Recipients, Sender, Title, Body) VALUES ('"
              + mUsername
              + "', 'MailerDaemon', 'Message Delivery Failed', '"
              + DaemonMessage + "');";
      mStatement.executeUpdate(fail_query);
    }
  }

  /**
   * Function to retrieve a message for current user
   *
   * @param id
   * @return message
   * @throws java.sql.SQLException
   */
  public Message getMessage(int id, String state) throws SQLException {
    String query
            = "SELECT Recipients, Sender, Title, Body, Received FROM " + state + " WHERE idMessages ='"
            + id + "';";
    ResultSet rs = mStatement.executeQuery(query);
    rs.next();
    Message msg = new Message();
    msg.mTo = new String[1];
    msg.mTo[0] = rs.getString(1);
    msg.mFrom = rs.getString(2);
    msg.mSubject = rs.getString(3);
    msg.mBody = rs.getString(4);
    msg.mTimestamp = rs.getTimestamp(5);
    msg.mId = id;

    return msg;
  }

  /**
   * Function to delete a specific message
   *
   * @param msg - Message details to use to delete specific message
   * @throws java.sql.SQLException
   */
  public void deleteMessage(int id, String state) throws SQLException {
    String query = "DELETE FROM " + state + " WHERE idMessages='" + id
            + "';";
    mStatement.executeUpdate(query);
  }

  /**
   * Function to reset a user password by the administrator
   *
   * @param user - user to reset password for
   * @param password
   * @throws java.sql.SQLException
   */
  public void resetUserPassword(String user, String password) throws
          SQLException {
    String query = "UPDATE Users SET pass='" + password + "' WHERE user='"
            + user + "';";
    mStatement.executeUpdate(query);
  }

  /**
   * Resets the current user's password
   *
   * @param password new password
   * @throws SQLException
   */
  public void resetUserPassword(String password) throws SQLException {
    String query = "UPDATE Users SET pass='" + password + "' WHERE user='"
            + mUsername + "';";
    mStatement.executeUpdate(query);
  }

  /**
   * Function to change user permission by admin
   *
   * @param user
   * @param changeToAdmin
   * @throws java.sql.SQLException
   */
  public void changeUserPermission(String user, boolean changeToAdmin) throws
          SQLException { //Admin, Standard, or Disabled
    String query;
    if (changeToAdmin) {
      query = "UPDATE Users SET Permissions='Admin' WHERE user =?";
    } else {
      query = "UPDATE Users SET Permissions='Standard' WHERE user=?";
    }
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, user);
    mPreparedStatement.execute();
  }

  /**
   * Tests if a user is an administrator
   *
   * @param user the username to check permissions
   * @return true if admin
   * @throws SQLException
   */
  public boolean isAdmin(String user) throws SQLException {
    String query;
    query = "SELECT user FROM Users WHERE user=? AND Permissions='Admin';";
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, user);
    ResultSet rs = mPreparedStatement.executeQuery();
    return rs.next();
  }

  /**
   * Checks if an account has been disabled
   *
   * @param user username to check
   * @return true if account has been disabled
   * @throws SQLException
   */
  public boolean isDisabled(String user) throws SQLException {
    String query = "SELECT user FROM Users WHERE  user=? AND Permissions='Disabled';";
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, user);
    ResultSet rs = mPreparedStatement.executeQuery();
    return rs.next();
  }

  /**
   * To disable a user account by an administrator
   *
   * @param user
   * @throws SQLException
   */
  public void disableUser(String user) throws SQLException {
    String query = "UPDATE Users SET Permissions='Disabled' WHERE user=?";
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, user);
    mPreparedStatement.execute();
  }

  /**
   * Change a user status to enabled
   *
   * @param user
   * @param permissions
   * @throws SQLException
   */
  public void enableUser(String user, String permissions) throws SQLException {
    String query = "UPDATE Users SET Permissions=? WHERE user=?";
    mPreparedStatement = mConnection.prepareStatement(query);
    mPreparedStatement.setString(1, permissions);
    mPreparedStatement.setString(2, user);
    mPreparedStatement.execute();
  }

  /**
   * Sets the username for the session
   *
   * @param user
   */
  public void setUser(String user) {
    mUsername = user;
  }

  /**
   * Returns the current session user's username
   *
   * @return username
   */
  public String getUser() {
    return mUsername;
  }

  /**
   * Update a message by ID as having been read
   *
   * @param id the ID tag of the message to update
   * @throws SQLException
   */
  public void markRead(int id) throws SQLException {
    String query = "UPDATE Inbox SET Unread=1 WHERE idMessages='" + id + "';";
    mStatement.executeUpdate(query);
  }
}
