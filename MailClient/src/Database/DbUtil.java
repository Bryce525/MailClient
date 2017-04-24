package Database;

/**
 * Package used to convert SQL Result Sets to JTables for the MailClientGUI
 *
 * @author Christopher McClurg
 */
import java.awt.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.*;

public class DbUtil {

  /**
   * Function to convert a Result Set containing the message headers to a JTable
   *
   * @param rs - Result Set
   */
  public static TableModel HeadersRStoJT(ResultSet rs) throws SQLException {

    ResultSetMetaData metaData = rs.getMetaData();
    // idMessages, Sender, Title, Recieved, Unread
    int numberOfColumns = metaData.getColumnCount();

    Vector columnNames = new Vector();

    for (int column = 0; column < numberOfColumns; column++) {
      columnNames.addElement(metaData.getColumnLabel(column + 1));
    }

    Vector rows = new Vector();

    while (rs.next()) {
      Vector newRow = new Vector();

      for (int i = 1; i <= numberOfColumns; i++) {
        newRow.addElement(rs.getObject(i));
      }

      rows.addElement(newRow);
    }
    TableModel model = new DefaultTableModel(rows, columnNames) {
      public boolean isCellEditable(int row, int column) {
        return false;//This causes all cells to be not editable
      }
    };
    return model;
  }

  /**
   * Converts a Result Set containing Contacts to a JTable
   *
   * @param username - account name
   * @param password - account password
   * @throws SQLException if unable to perform database operation
   * @throws InvalidCredentialsException if credentials do not match database
   * @throws DisabledException if the user's account has been disabled
   *
   */
  public static TableModel ContactsRStoJT(ResultSet rs) throws SQLException {

    ResultSetMetaData metaData = rs.getMetaData();
    // idMessages, Sender, Title, Recieved, Unread
    int numberOfColumns = metaData.getColumnCount();

    Vector columnNames = new Vector();

    // Get the column names
    for (int column = 0; column < numberOfColumns; column++) {
      columnNames.addElement(metaData.getColumnLabel(column + 1));
    }
    // Get all rows.
    Vector rows = new Vector();

    while (rs.next()) {
      Vector newRow = new Vector();

      for (int i = 1; i <= numberOfColumns; i++) {
        newRow.addElement(rs.getObject(i));
      }

      rows.addElement(newRow);
    }

    TableModel model = new DefaultTableModel(rows, columnNames) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;//This causes all cells to be not editable
      }
    };
    return model;
  }

  /**
   * Function to bold rows which contain unread messages Not currently
   * implemented
   *
   * @param messages - account name
   */
  public static JTable boldUnread(TableModel messages) {
    JTable table = new JTable(messages) {
      public Component prepareRenderer(TableCellRenderer renderer,
              int Index_row, int Index_col) {
        Component comp = super.prepareRenderer(renderer, Index_row,
                Index_col);
        //even index, selected or not selected
        if (messages.getValueAt(Index_row, 4).equals(0)) {
          comp.setFont(getFont().deriveFont(Font.BOLD));
        }
        return comp;
      }
    };
    return table;
  }
}
