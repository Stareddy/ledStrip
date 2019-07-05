import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Starter {

    private static Connection conn = null;

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
//        Process p = Runtime.getRuntime().exec("python relais.py");

        SimpleDateFormat sdf = new SimpleDateFormat("HH");

        final boolean[] flag = {false};

        final int currentHour = Integer.parseInt(sdf.format(new Date()));

        final boolean lateNight = currentHour >= 20 && currentHour <= 23;

        final boolean earlyMorning = currentHour == 4 || currentHour == 5;



//        Class.forName("com.mysql.jdbc.Driver");
//
//        conn = DriverManager
//                .getConnection("jdbc:mysql://localhost/JaRasPi?",user, password);

        final GpioController gpio = GpioFactory.getInstance();

        // provision gpio pin #02 as an input pin with its internal pull down resistor enabled
        final GpioPinDigitalInput upperMovingSensor = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);

        // set shutdown state for this input pin
        upperMovingSensor.setShutdownOptions(true);

        // create and register gpio pin listener

        upperMovingSensor.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
                System.out.println("boolean: --> " + flag[0]);
                if (event.getState().equals(PinState.HIGH) && !flag[0] && (earlyMorning || lateNight)){
                    flag[0] = true;
                    System.out.println("Upstairs - Sensor is on");
                    try {
                        Process p = Runtime.getRuntime().exec("python relay_from_upstairs.py");
                        p.waitFor();
                        flag[0] = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });

        final GpioPinDigitalInput lowerMovingSensor = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);

        // set shutdown state for this input pin
        lowerMovingSensor.setShutdownOptions(true);

        lowerMovingSensor.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent (GpioPinDigitalStateChangeEvent event){
                // display pin state on console
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
                System.out.println("boolean: --> " + flag[0]);
                if (event.getState().equals(PinState.HIGH) && !flag[0] && (earlyMorning || lateNight)) {
                    flag[0] = true;
                    System.out.println("Downstairs - Sensor is on");
                    try {
                        Process p = Runtime.getRuntime().exec("python relay_from_downstairs.py");
                        p.waitFor();
                        flag[0] = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        System.out.println(" ... complete the GPIO #02 circuit and see the listener feedback here in the console.");

        // keep program running until user aborts (CTRL-C)
        while(true) {
            Thread.sleep(1000);
        }

    }

    private static void activateLEDStrip(GpioController gpio) throws InterruptedException {
        GpioPinDigitalMultipurpose led = gpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_01, PinMode.DIGITAL_INPUT);

        led.setMode(PinMode.DIGITAL_OUTPUT);
        Thread.sleep(1000);
        led.setShutdownOptions(true, PinState.LOW);
    }
}