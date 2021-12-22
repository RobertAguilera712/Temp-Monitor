package temperatura;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.JsonObject;
import io.dweet.DweetIO;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class FXMLDocumentController implements Initializable {

	@FXML
	private ProgressBar barTemperatura;

	@FXML
	private Label txtTemperatura;

	@FXML
	private Label txtFecha;

	@FXML
	private Label txtHora;

	@FXML
	private Button btnNube;

	private InputStream entrada;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		conexion();
		getFecha();
		getHora();
		if (entrada != null) {
			getTemp();
			enviarDatos();
		}

		btnNube.setOnAction(e -> {
			try {
				new ProcessBuilder("x-www-browser", "https://dweet.io/follow/kasparov").start();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});
	}

	private void conexion() {
		String puerto = "/dev/ttyUSB0";
		int baudios = 9600;
		SerialPort sp = SerialPort.getCommPort(puerto);
		sp.setBaudRate(baudios);
		sp.setNumDataBits(8);
		sp.setNumStopBits(1);
		sp.setParity(0);
		sp.openPort();
		entrada = sp.getInputStream();
	}

	private void getHora() {
		Thread thread = new Thread(() -> {
			SimpleDateFormat sd = new SimpleDateFormat("hh:mm:ss a");
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
				}
				final String hora = sd.format(new Date());
				Platform.runLater(() -> {
					txtHora.setText(hora);
				});
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	private void getFecha() {
		SimpleDateFormat sd = new SimpleDateFormat("dd/MM/yyyy");
		final String fecha = sd.format(new Date());
		txtFecha.setText(fecha);
	}

	private void getTemp() {
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(1000);
					StringBuilder sb = new StringBuilder();

					while (entrada.available() > 0) {
						sb.append((char) entrada.read());
					}

					final String temperatura = sb.toString().replaceAll("\\s", "");

					Platform.runLater(() -> {
						if (!temperatura.isEmpty()) {
							try {
								double temp = Double.parseDouble(temperatura) / 100;
								barTemperatura.setProgress(temp);
								txtTemperatura.setText(temperatura.concat("°C"));
							} catch (Exception e) {
							}
						}
					});
				} catch (IOException ex) {
					Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
				} catch (InterruptedException ex) {
					Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
				}

			}

		});
		thread.setDaemon(true);
		thread.start();
	}

	private void enviarDatos() {
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(100);
					String thingName = "kasparov";
					JsonObject js = new JsonObject();
					js.addProperty("Temperatura", txtTemperatura.getText().replaceFirst("°C", ""));
					DweetIO.publish(thingName, js);
				} catch (IOException ex) {
					Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
				} catch (InterruptedException ex) {
					Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
				}

			}
		});
		thread.setDaemon(true);
		thread.start();
	}

}
