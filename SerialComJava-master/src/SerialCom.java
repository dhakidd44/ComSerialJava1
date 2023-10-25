
/*
  Titre      : Communication Serie avec Java en utilisant la librairie JSSC
  Auteur     : Ernest Samuel Andre
  Date       : 16/10/2023
  Description: Le programme permet ainsi d'établir une communication série avec un dispositif connecté sur le port COM3,
               d'envoyer des données à ce dispositif, d'attendre une réponse, de lire la réponse et enfin de fermer le port série. 
               Il s'agit d'un exemple simple d'utilisation de JSSC pour la communication série en Java.
  Version    : 0.0.1
  Source du Code    : https://code.google.com/archive/p/java-simple-serial-connector/wikis/jSSC_examples.wiki
  Source des librairies jar : https://github.com/java-native/jssc/releases
*/
// Importation des librairies
import jssc.SerialPort;
import jssc.SerialPortException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerialCom {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String portPattern = "^COM[0-9]+$"; // Format attendu : COM suivi d'un ou plusieurs chiffres
        String serialPortName = null;
        boolean validPort = false;
        Pattern pattern = Pattern.compile(portPattern);

        while (!validPort) {
            System.out.print("Veuillez choisir un port (Exemple COM3, COM6, COM9): ");
            serialPortName = scanner.nextLine();
            Matcher matcher = pattern.matcher(serialPortName);

            if (matcher.matches()) {
                validPort = true;
                System.out.println("Vous avez choisi le port : " + serialPortName);
            } else {
                System.out.println("Format de port incorrect. Utilisez le format COM suivi de chiffres.");
            }
        }

        SerialPort serialPort = new SerialPort(serialPortName);
        // Connexion à la base de données PostgreSQL
        String url = "jdbc:postgresql://localhost:5432/meteo1";
        String utilisateur = "postgres";
        String motDePasse = "admin";

        Connection connection = null;

        try {
            // Ouvrir le port série
            serialPort.openPort();

            // Configurer les paramètres de communication (vitesse, bits de données, bits de
            // stop, etc.)
            serialPort.setParams(SerialPort.BAUDRATE_9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            connection = DriverManager.getConnection(url, utilisateur, motDePasse);

            // Boucle pour lire en continu les données de l'Arduino
            while (true) {
                String response = serialPort.readString();
                if (response != null && !response.isEmpty()) {
                    System.out.println("Temperature obtenue : " + response);

                    // Insérer la température dans la base de données
                    String insertQuery = "INSERT INTO temperature (temperature_value) VALUES (?)";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                        preparedStatement.setString(1, response);
                        preparedStatement.executeUpdate();
                        System.out.println("Données enregistrées dans la base de données.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                // Attendre 10 secondes pour la prochaine lecture
                Thread.sleep(10000);
            }
        } catch (SerialPortException | SQLException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Fermer le port série
            try {
                if (serialPort.isOpened()) {
                    serialPort.closePort();
                }
            } catch (SerialPortException e) {
                e.printStackTrace();
            }

            // Fermer la connexion à la base de données
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }
}
