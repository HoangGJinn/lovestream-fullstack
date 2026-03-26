import java.sql.*;
import java.util.Properties;
import java.io.FileInputStream;

public class InspectStaticPagesSchema {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("application.properties")) {
            props.load(fis);
        }
        String url = props.getProperty("spring.datasource.url");
        String user = props.getProperty("spring.datasource.username");
        String pass = props.getProperty("spring.datasource.password");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String sql = "SELECT COLUMN_TYPE, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'static_pages' AND COLUMN_NAME = 'page_type'";
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("COLUMN_TYPE=" + rs.getString("COLUMN_TYPE"));
                    System.out.println("DATA_TYPE=" + rs.getString("DATA_TYPE"));
                    System.out.println("CHARACTER_MAXIMUM_LENGTH=" + rs.getString("CHARACTER_MAXIMUM_LENGTH"));
                    System.out.println("IS_NULLABLE=" + rs.getString("IS_NULLABLE"));
                }
            }
        }
    }
}
