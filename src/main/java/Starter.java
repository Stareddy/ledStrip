import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Starter {

    public static void main(String[] args) throws InterruptedException {

        final boolean[] flag = {false};

        final GpioController gpio = GpioFactory.getInstance();

        final GpioPinDigitalInput upperMovingSensor = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);

        upperMovingSensor.setShutdownOptions(true);

        upperMovingSensor.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState().equals(PinState.HIGH) && !flag[0] && (isEarlyMorning() || isLateNight())) {
                    flag[0] = true;
                    System.out.println("Upstairs - Sensor is on. Current hour is: " + getCurrentHour() + " earlyMorning is: " + isEarlyMorning() + " and lateNight is: " + isLateNight() + " at: " + getTime());
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

        lowerMovingSensor.setShutdownOptions(true);

        lowerMovingSensor.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState().equals(PinState.HIGH) && !flag[0] && (isEarlyMorning() || isLateNight())) {
                    flag[0] = true;
                    System.out.println("Downstairs - Sensor is on. Current hour is: " + getCurrentHour() + "earlyMorning is: " + isEarlyMorning() + " and lateNight is: " + isLateNight() + " at: " + getTime());
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

        while (true) {
            Thread.sleep(1000);
        }
    }

    private static String getTime() {
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm dd.MM.yyyy");

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        return hm.format(timestamp);
    }

    private static String getCurrentHour() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH");

        return (String.valueOf(Integer.parseInt(sdf.format(new Date()))));
    }

    private static boolean isLateNight() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH");

        int currentHour = Integer.parseInt(sdf.format(new Date()));

        return (currentHour >= 21 && currentHour <= 23);
    }

    private static boolean isEarlyMorning() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH");

        int currentHour = Integer.parseInt(sdf.format(new Date()));

        return (currentHour == 4 || currentHour == 5);
    }
}