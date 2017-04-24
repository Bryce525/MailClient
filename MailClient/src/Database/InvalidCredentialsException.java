package Database;

/**
 *
 * @author Bryce Johnston, Steven McClurg, Hunter Cowing, Tristan Cone
 */
public class InvalidCredentialsException extends Exception {

  public InvalidCredentialsException(String invalid_Credentials) {
    super(invalid_Credentials);
  }

}
