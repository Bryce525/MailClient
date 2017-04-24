package Database;

/**
 *
 * @author Bryce Johnston, Steven McClurg, Hunter Cowing, Tristan Cone
 */
public class UserExistsException extends Exception {

  public UserExistsException(String userExists) {
    super(userExists);
  }

}
